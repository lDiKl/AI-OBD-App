from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.database import get_db
from app.core.security import verify_firebase_token

router = APIRouter()


@router.get("/{code}")
async def get_dtc(
    code: str,
    token: dict = Depends(verify_firebase_token),
    db: AsyncSession = Depends(get_db),
):
    """
    Lookup a single DTC code from shared database.
    Returns standard description, category, severity, can_drive flag.
    Used by both B2C and B2B clients.
    """
    # TODO: query ErrorCodes table, return structured data
    raise NotImplementedError


@router.get("/")
async def search_dtc(
    q: str,
    token: dict = Depends(verify_firebase_token),
    db: AsyncSession = Depends(get_db),
):
    """Search DTC codes by code prefix or keyword."""
    # TODO: implement search
    raise NotImplementedError
