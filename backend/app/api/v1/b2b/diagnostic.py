from fastapi import APIRouter, Depends
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.database import get_db
from app.core.security import get_current_shop_user
from app.models.diagnostic_case import DiagnosticCase
from app.schemas.b2b import DiagnosticAnalyzeRequest, DiagnosticAnalyzeResponse
from app.services.ai.ai_service import B2BDiagnosticContext, analyze_diagnostic_b2b
from app.services.ai.output_formatter import parse_llm_response

router = APIRouter()


@router.post("/analyze", response_model=DiagnosticAnalyzeResponse)
async def analyze_diagnostic(
    request: DiagnosticAnalyzeRequest,
    current=Depends(get_current_shop_user),
    db: AsyncSession = Depends(get_db),
) -> DiagnosticAnalyzeResponse:
    """
    Main B2B endpoint. Professional diagnostic analysis for mechanics.

    Input:  DTC codes + freeze frame + symptoms + vehicle info
    Output: probable causes ranked by %, diagnostic checklist, labor estimate

    Always routes to Claude Sonnet for maximum quality.
    If save_as_case=True (default), persists the result as a DiagnosticCase.
    """
    shop_user, shop = current

    ctx = B2BDiagnosticContext(
        codes=request.codes,
        vehicle_info=request.vehicle_info,
        symptoms=request.symptoms,
        freeze_frame=request.freeze_frame.model_dump(exclude_none=True),
        language="en",  # TODO: from shop profile
    )

    ai_data = await analyze_diagnostic_b2b(ctx)
    if ai_data is None:
        ai_data = parse_llm_response("{}", "b2b_diagnostic")

    case_id = None
    if request.save_as_case:
        case = DiagnosticCase(
            shop_id=shop.id,
            vehicle_info=request.vehicle_info,
            input_codes=request.codes,
            symptoms_text=request.symptoms,
            freeze_frame=request.freeze_frame.model_dump(exclude_none=True),
            ai_result=ai_data,
        )
        db.add(case)
        await db.commit()
        await db.refresh(case)
        case_id = case.id

    return DiagnosticAnalyzeResponse(
        case_id=case_id,
        probable_causes=ai_data.get("probable_causes", []),
        diagnostic_sequence=ai_data.get("diagnostic_sequence", []),
        estimated_labor_hours=ai_data.get("estimated_labor_hours", 0.0),
        parts_likely_needed=ai_data.get("parts_likely_needed", []),
        tsb_references=ai_data.get("tsb_references", []),
        urgency=ai_data.get("urgency", "within_week"),
        additional_notes=ai_data.get("additional_notes", ""),
    )
