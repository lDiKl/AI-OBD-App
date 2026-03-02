from fastapi import APIRouter, Depends, HTTPException, Query, status
from pydantic import BaseModel
from sqlalchemy import or_, select
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.database import get_db
from app.core.security import verify_firebase_token
from app.models.error_code import ErrorCode

router = APIRouter()


class DtcOut(BaseModel):
    code: str
    standard_description: str
    category: str
    system: str
    severity_level: str
    can_drive_flag: str
    manufacturer_specific: bool

    model_config = {"from_attributes": True}


@router.get("/search", response_model=list[DtcOut])
async def search_dtc(
    q: str = Query(..., min_length=1, description="Code prefix or keyword"),
    limit: int = Query(20, le=100),
    token: dict = Depends(verify_firebase_token),
    db: AsyncSession = Depends(get_db),
):
    """Search DTC codes by code prefix or description keyword."""
    q_upper = q.upper()
    result = await db.execute(
        select(ErrorCode)
        .where(
            or_(
                ErrorCode.code.startswith(q_upper),
                ErrorCode.standard_description.ilike(f"%{q}%"),
            )
        )
        .limit(limit)
    )
    return result.scalars().all()


@router.get("/{code}", response_model=DtcOut)
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
    result = await db.execute(select(ErrorCode).where(ErrorCode.code == code.upper()))
    dtc = result.scalar_one_or_none()
    if dtc is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=f"DTC code {code} not found")
    return dtc
