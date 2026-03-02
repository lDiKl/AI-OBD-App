from fastapi import APIRouter, Depends
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.database import get_db
from app.core.security import verify_firebase_token

router = APIRouter()


@router.post("/{case_id}/estimate/suggest")
async def suggest_estimate(
    case_id: str,
    token: dict = Depends(verify_firebase_token),
    db: AsyncSession = Depends(get_db),
):
    """AI suggests parts and labor hours for estimate (Prompt 5, local LLM)."""
    # TODO: call LLM with Prompt 5, return suggested_parts + labor_hours
    raise NotImplementedError


@router.put("/{case_id}/estimate")
async def save_estimate(
    case_id: str,
    token: dict = Depends(verify_firebase_token),
    db: AsyncSession = Depends(get_db),
):
    """Save mechanic-edited estimate to the case."""
    # TODO: save estimate_json to DiagnosticCase
    raise NotImplementedError
