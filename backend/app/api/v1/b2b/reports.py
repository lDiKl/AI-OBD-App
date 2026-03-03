import logging

from fastapi import APIRouter, Depends, HTTPException, status
from fastapi.responses import Response
from pydantic import BaseModel
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.database import get_db
from app.core.security import get_current_shop_user
from app.models.diagnostic_case import DiagnosticCase
from app.services.ai.ai_service import B2BReportContext, generate_client_report
from app.services.ai.output_formatter import get_fallback_response

logger = logging.getLogger(__name__)

router = APIRouter()


# ── Schemas ───────────────────────────────────────────────────────────────────

class ReportGenerateRequest(BaseModel):
    confirmed_diagnosis: str
    repair_description: str
    parts_list: list[str] = []
    language: str = "en"


class ReportOut(BaseModel):
    report_title: str
    issue_summary: str
    what_we_did: str
    why_it_matters: str
    parts_replaced: list[str]
    next_steps: str
    disclaimer: str


# ── Endpoints ─────────────────────────────────────────────────────────────────

@router.post("/{case_id}/report/generate", response_model=ReportOut)
async def generate_report(
    case_id: str,
    body: ReportGenerateRequest,
    current=Depends(get_current_shop_user),
    db: AsyncSession = Depends(get_db),
):
    """
    Generate an AI client report for the car owner (Prompt 4).
    Result is saved to case.client_report_text.
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

    ctx = B2BReportContext(
        vehicle_info=case.vehicle_info,
        confirmed_diagnosis=body.confirmed_diagnosis,
        repair_description=body.repair_description,
        parts_list=body.parts_list,
        shop_name=shop.name,
        language=body.language,
    )

    report_data = await generate_client_report(ctx)
    if report_data is None:
        logger.warning("generate_client_report returned None for case %s — using fallback", case_id)
        report_data = get_fallback_response("b2b_report")

    import json
    case.client_report_text = json.dumps(report_data)
    await db.commit()

    return ReportOut(**report_data)


@router.get("/{case_id}/report/pdf")
async def download_report_pdf(
    case_id: str,
    current=Depends(get_current_shop_user),
    db: AsyncSession = Depends(get_db),
):
    """Download the client report as a PDF file."""
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
    if not case.client_report_text:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="No report generated yet. Generate the report first.",
        )

    from app.services.pdf_service import generate_report_pdf  # lazy: weasyprint only needed here
    pdf_bytes = generate_report_pdf(case, shop)
    filename = f"report_{case_id[:8]}.pdf"
    return Response(
        content=pdf_bytes,
        media_type="application/pdf",
        headers={"Content-Disposition": f'attachment; filename="{filename}"'},
    )
