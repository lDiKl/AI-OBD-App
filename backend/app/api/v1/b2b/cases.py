from datetime import datetime

from fastapi import APIRouter, Depends, HTTPException, status
from pydantic import BaseModel
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.database import get_db
from app.core.security import get_current_shop_user
from app.models.diagnostic_case import DiagnosticCase

router = APIRouter()


# ── Schemas ───────────────────────────────────────────────────────────────────

class CaseCreate(BaseModel):
    vehicle_info: dict
    input_codes: list[str]
    symptoms_text: str = ""


class CaseUpdate(BaseModel):
    status: str | None = None
    symptoms_text: str | None = None


class CaseOut(BaseModel):
    id: str
    shop_id: str
    vehicle_info: dict
    input_codes: list
    symptoms_text: str
    ai_result: dict
    client_report_text: str
    estimate: dict
    status: str
    created_at: datetime

    model_config = {"from_attributes": True}


# ── Endpoints ─────────────────────────────────────────────────────────────────

@router.get("/", response_model=list[CaseOut])
async def list_cases(
    current=Depends(get_current_shop_user),
    db: AsyncSession = Depends(get_db),
):
    """Return all diagnostic cases for the current shop, newest first."""
    _, shop = current
    result = await db.execute(
        select(DiagnosticCase)
        .where(DiagnosticCase.shop_id == shop.id)
        .order_by(DiagnosticCase.created_at.desc())
    )
    return result.scalars().all()


@router.post("/", response_model=CaseOut, status_code=status.HTTP_201_CREATED)
async def create_case(
    body: CaseCreate,
    current=Depends(get_current_shop_user),
    db: AsyncSession = Depends(get_db),
):
    """Manually create a diagnostic case (without AI analysis)."""
    _, shop = current
    case = DiagnosticCase(
        shop_id=shop.id,
        vehicle_info=body.vehicle_info,
        input_codes=body.input_codes,
        symptoms_text=body.symptoms_text,
    )
    db.add(case)
    await db.commit()
    await db.refresh(case)
    return case


@router.get("/{case_id}", response_model=CaseOut)
async def get_case(
    case_id: str,
    current=Depends(get_current_shop_user),
    db: AsyncSession = Depends(get_db),
):
    """Return a single diagnostic case with full AI analysis, report, and estimate."""
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
    return case


@router.put("/{case_id}", response_model=CaseOut)
async def update_case(
    case_id: str,
    body: CaseUpdate,
    current=Depends(get_current_shop_user),
    db: AsyncSession = Depends(get_db),
):
    """Update case status or symptoms."""
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

    if body.status is not None:
        case.status = body.status
    if body.symptoms_text is not None:
        case.symptoms_text = body.symptoms_text

    await db.commit()
    await db.refresh(case)
    return case


@router.delete("/{case_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_case(
    case_id: str,
    current=Depends(get_current_shop_user),
    db: AsyncSession = Depends(get_db),
):
    """Delete a diagnostic case."""
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
    await db.delete(case)
    await db.commit()
