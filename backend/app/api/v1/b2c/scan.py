import asyncio

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy import select
from sqlalchemy.dialects.postgresql import insert as pg_insert
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.database import get_db
from app.core.security import get_current_user
from app.models.error_code import ErrorCode
from app.models.scan_session import ErrorOccurrence, ScanSession
from app.models.user import User
from app.models.vehicle import Vehicle
from app.schemas.b2c import (
    CodeResult,
    CodeResultFree,
    CodeResultPremium,
    ScanAnalyzeRequest,
    ScanAnalyzeResponse,
)
from app.services.ai.ai_service import B2CCodeContext, explain_code_b2c
from app.services.ai.rule_engine import evaluate_code, evaluate_session

router = APIRouter()


@router.post("/analyze", response_model=ScanAnalyzeResponse)
async def analyze_scan(
    request: ScanAnalyzeRequest,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
) -> ScanAnalyzeResponse:
    """
    Main B2C endpoint. Analyzes OBD scan from ELM327 adapter.

    Free tier:  Rule Engine only — severity, can_drive, description.
    Premium:    + AI explanation (simple language, causes, recommended action).
    """
    # Verify the vehicle belongs to this user
    result = await db.execute(
        select(Vehicle).where(
            Vehicle.id == request.vehicle_id,
            Vehicle.user_id == current_user.id,
        )
    )
    vehicle = result.scalar_one_or_none()
    if vehicle is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Vehicle not found")

    is_premium = current_user.subscription_status == "premium"

    # Fetch known DTC codes from DB for descriptions
    if request.codes:
        code_strings = [c.code.upper() for c in request.codes]
        db_result = await db.execute(
            select(ErrorCode).where(ErrorCode.code.in_(code_strings))
        )
        known_codes: dict[str, ErrorCode] = {ec.code: ec for ec in db_result.scalars()}
    else:
        known_codes = {}

    # Ensure all scanned codes exist in error_codes (FK requirement).
    # Codes absent from our dataset get a minimal placeholder record.
    if request.codes:
        missing = [c for c in code_strings if c not in known_codes]
        if missing:
            await db.execute(
                pg_insert(ErrorCode)
                .values([
                    {
                        "code": c,
                        "standard_description": "Unknown fault code",
                        "category": c[0] if c else "P",
                        "system": "unknown",
                    }
                    for c in missing
                ])
                .on_conflict_do_nothing(index_elements=["code"])
            )
            await db.flush()

    # Create scan session
    session = ScanSession(
        vehicle_id=vehicle.id,
        mileage=request.mileage,
        raw_obd_data={"codes": [c.code for c in request.codes]},
    )
    db.add(session)
    await db.flush()  # get session.id without committing

    # Build Rule Engine results for all codes
    rule_results = []
    code_contexts = []

    for scanned in request.codes:
        code_upper = scanned.code.upper()
        db_code = known_codes.get(code_upper)
        rule = evaluate_code(code=code_upper, system=db_code.system if db_code else "unknown")
        rule_results.append({"severity": rule.severity})
        code_contexts.append((scanned, code_upper, db_code, rule))

    # For premium users: call AI for all codes concurrently
    ai_results: list[dict | None] = [None] * len(code_contexts)
    if is_premium and code_contexts:
        ai_tasks = []
        for _, code_upper, db_code, rule in code_contexts:
            ctx = B2CCodeContext(
                code=code_upper,
                standard_description=db_code.standard_description if db_code else code_upper,
                system=db_code.system if db_code else "unknown",
                severity_level=rule.severity,
                can_drive_flag=rule.can_drive,
                make=vehicle.make,
                model=vehicle.model,
                year=vehicle.year,
                engine_type=vehicle.engine_type or "unknown",
                region=current_user.region,
                language=current_user.language,
            )
            ai_tasks.append(explain_code_b2c(ctx))
        ai_results = list(await asyncio.gather(*ai_tasks))

    # Build response and persist occurrences
    code_results: list[CodeResult] = []

    for i, (scanned, code_upper, db_code, rule) in enumerate(code_contexts):
        description = db_code.standard_description if db_code else "Unknown fault code"
        category = db_code.category if db_code else (code_upper[0] if code_upper else "P")

        free_result = CodeResultFree(
            description=description,
            category=category,
            severity=rule.severity,
            can_drive=rule.can_drive,
        )

        ai_data = ai_results[i]
        premium_result: CodeResultPremium | None = None
        if is_premium and ai_data:
            premium_result = CodeResultPremium(
                simple_explanation=ai_data.get("simple_explanation", description),
                main_causes=ai_data.get("main_causes", []),
                causes_probability=ai_data.get("causes_probability", []),
                what_happens_if_ignored=ai_data.get("what_happens_if_ignored", ""),
                recommended_action=ai_data.get("recommended_action", ""),
            )

        occurrence = ErrorOccurrence(
            session_id=session.id,
            code=code_upper,
            freeze_frame=scanned.freeze_frame.model_dump(exclude_none=True),
            ai_result=ai_data or {"severity": rule.severity, "can_drive": rule.can_drive},
        )
        db.add(occurrence)

        code_results.append(CodeResult(code=code_upper, free=free_result, premium=premium_result))

    overall_risk = evaluate_session(rule_results)
    session.risk_score = overall_risk
    await db.commit()

    return ScanAnalyzeResponse(
        session_id=session.id,
        overall_risk=overall_risk,
        safe_to_drive=overall_risk not in ("high", "critical"),
        is_premium=is_premium,
        codes=code_results,
    )
