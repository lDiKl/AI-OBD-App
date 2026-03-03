import logging

from fastapi import APIRouter, Depends, HTTPException, status
from fastapi.responses import Response
from pydantic import BaseModel
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.database import get_db
from app.core.security import get_current_shop_user
from app.models.diagnostic_case import DiagnosticCase
from app.services.ai.ai_service import B2BEstimateContext, suggest_estimate_b2b
from app.services.ai.output_formatter import get_fallback_response

logger = logging.getLogger(__name__)

router = APIRouter()


# ── Schemas ───────────────────────────────────────────────────────────────────

class EstimateSuggestRequest(BaseModel):
    confirmed_diagnosis: str
    repair_description: str
    region: str = "EU"


class EstimatePartItem(BaseModel):
    name: str
    quantity: int = 1
    unit_price: float
    note: str = ""


class EstimateSaveRequest(BaseModel):
    parts: list[EstimatePartItem]
    labor_hours: float
    labor_rate_eur: float = 65.0
    markup_pct: float = 20.0
    notes: str = ""
    currency: str = "EUR"


# ── Endpoints ─────────────────────────────────────────────────────────────────

@router.post("/{case_id}/estimate/suggest")
async def suggest_estimate(
    case_id: str,
    body: EstimateSuggestRequest,
    current=Depends(get_current_shop_user),
    db: AsyncSession = Depends(get_db),
):
    """
    AI suggests parts list and labor hours for a case (Prompt 5).
    Returns suggestion only — mechanic edits and saves via PUT /estimate.
    """
    shop_user, shop = current

    result = await db.execute(
        select(DiagnosticCase).where(
            DiagnosticCase.id == case_id,
            DiagnosticCase.shop_id == shop.id,
        )
    )
    case = result.scalar_one_or_none()
    if case is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Case not found")

    ctx = B2BEstimateContext(
        vehicle_info=case.vehicle_info,
        confirmed_diagnosis=body.confirmed_diagnosis,
        repair_description=body.repair_description,
        region=body.region,
    )

    estimate_data = await suggest_estimate_b2b(ctx)
    if estimate_data is None:
        logger.warning("suggest_estimate_b2b returned None for case %s — using fallback", case_id)
        estimate_data = get_fallback_response("b2b_estimate")

    return estimate_data


@router.put("/{case_id}/estimate")
async def save_estimate(
    case_id: str,
    body: EstimateSaveRequest,
    current=Depends(get_current_shop_user),
    db: AsyncSession = Depends(get_db),
):
    """Save mechanic-edited estimate to the case."""
    _, shop = current

    result = await db.execute(
        select(DiagnosticCase).where(
            DiagnosticCase.id == case_id,
            DiagnosticCase.shop_id == shop.id,
        )
    )
    case = result.scalar_one_or_none()
    if case is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Case not found")

    # Calculate totals
    parts_total = sum(p.unit_price * p.quantity for p in body.parts)
    labor_total = body.labor_hours * body.labor_rate_eur
    subtotal = parts_total + labor_total
    total = subtotal * (1 + body.markup_pct / 100)

    case.estimate = {
        "parts": [p.model_dump() for p in body.parts],
        "labor_hours": body.labor_hours,
        "labor_rate_eur": body.labor_rate_eur,
        "markup_pct": body.markup_pct,
        "parts_total": round(parts_total, 2),
        "labor_total": round(labor_total, 2),
        "subtotal": round(subtotal, 2),
        "total": round(total, 2),
        "currency": body.currency,
        "notes": body.notes,
    }
    await db.commit()

    return case.estimate


@router.get("/{case_id}/estimate/pdf")
async def download_estimate_pdf(
    case_id: str,
    current=Depends(get_current_shop_user),
    db: AsyncSession = Depends(get_db),
):
    """Download the saved estimate as a PDF file."""
    _, shop = current

    result = await db.execute(
        select(DiagnosticCase).where(
            DiagnosticCase.id == case_id,
            DiagnosticCase.shop_id == shop.id,
        )
    )
    case = result.scalar_one_or_none()
    if case is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Case not found")
    if not case.estimate:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="No estimate saved yet. Save the estimate first.",
        )

    from app.services.pdf_service import generate_estimate_pdf  # lazy: weasyprint only needed here
    pdf_bytes = generate_estimate_pdf(case, shop)
    filename = f"estimate_{case_id[:8]}.pdf"
    return Response(
        content=pdf_bytes,
        media_type="application/pdf",
        headers={"Content-Disposition": f'attachment; filename="{filename}"'},
    )
