from fastapi import APIRouter, Depends
from fastapi.responses import Response
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.database import get_db
from app.core.security import verify_firebase_token

router = APIRouter()


@router.post("/{case_id}/report/generate")
async def generate_report(
    case_id: str,
    token: dict = Depends(verify_firebase_token),
    db: AsyncSession = Depends(get_db),
):
    """Generate AI client report text for a case."""
    # TODO: call LLM with Prompt 4, save to case.client_report_text
    raise NotImplementedError


@router.get("/{case_id}/report/pdf")
async def download_report_pdf(
    case_id: str,
    token: dict = Depends(verify_firebase_token),
    db: AsyncSession = Depends(get_db),
):
    """Generate and return PDF of client report."""
    # TODO: render HTML → PDF via weasyprint, return as application/pdf
    raise NotImplementedError
