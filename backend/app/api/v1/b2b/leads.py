from datetime import datetime

from fastapi import APIRouter, Depends, HTTPException, status
from pydantic import BaseModel
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.orm import selectinload

from app.core.database import get_db
from app.core.security import get_current_shop_user
from app.models.lead import ServiceLead, ShopQuote
from app.models.user import User

router = APIRouter()


# ── Schemas ───────────────────────────────────────────────────────────────────

class QuoteRequest(BaseModel):
    cost_min: float
    cost_max: float
    estimated_days: int
    notes: str | None = None


class QuoteOut(BaseModel):
    cost_min: float
    cost_max: float
    estimated_days: int
    notes: str | None = None

    model_config = {"from_attributes": True}


class LeadOut(BaseModel):
    lead_id: str
    user_email: str
    status: str
    dtc_codes: list
    vehicle_info: dict
    freeze_frame: dict | None = None
    created_at: datetime
    quote: QuoteOut | None = None

    model_config = {"from_attributes": True}


# ── Endpoints ─────────────────────────────────────────────────────────────────

@router.get("/", response_model=list[LeadOut])
async def get_leads(
    current=Depends(get_current_shop_user),
    db: AsyncSession = Depends(get_db),
):
    """Return all incoming leads for the current shop, newest first."""
    _, shop = current
    result = await db.execute(
        select(ServiceLead)
        .where(ServiceLead.shop_id == shop.id)
        .options(selectinload(ServiceLead.quote))
        .order_by(ServiceLead.created_at.desc())
    )
    leads = result.scalars().all()

    out = []
    for lead in leads:
        user_result = await db.execute(select(User).where(User.id == lead.user_id))
        user = user_result.scalar_one_or_none()
        user_email = user.email if user else "unknown"

        quote_out = None
        if lead.quote:
            quote_out = QuoteOut(
                cost_min=lead.quote.cost_min,
                cost_max=lead.quote.cost_max,
                estimated_days=lead.quote.estimated_days,
                notes=lead.quote.notes,
            )

        out.append(LeadOut(
            lead_id=lead.id,
            user_email=user_email,
            status=lead.status,
            dtc_codes=lead.dtc_codes,
            vehicle_info=lead.vehicle_info,
            freeze_frame=lead.freeze_frame,
            created_at=lead.created_at,
            quote=quote_out,
        ))

    return out


@router.get("/{lead_id}", response_model=LeadOut)
async def get_lead(
    lead_id: str,
    current=Depends(get_current_shop_user),
    db: AsyncSession = Depends(get_db),
):
    """Return a single lead with full details."""
    _, shop = current
    result = await db.execute(
        select(ServiceLead)
        .where(ServiceLead.id == lead_id, ServiceLead.shop_id == shop.id)
        .options(selectinload(ServiceLead.quote))
    )
    lead = result.scalar_one_or_none()
    if lead is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Lead not found")

    user_result = await db.execute(select(User).where(User.id == lead.user_id))
    user = user_result.scalar_one_or_none()

    quote_out = None
    if lead.quote:
        quote_out = QuoteOut(
            cost_min=lead.quote.cost_min,
            cost_max=lead.quote.cost_max,
            estimated_days=lead.quote.estimated_days,
            notes=lead.quote.notes,
        )

    return LeadOut(
        lead_id=lead.id,
        user_email=user.email if user else "unknown",
        status=lead.status,
        dtc_codes=lead.dtc_codes,
        vehicle_info=lead.vehicle_info,
        freeze_frame=lead.freeze_frame,
        created_at=lead.created_at,
        quote=quote_out,
    )


@router.put("/{lead_id}/quote", response_model=LeadOut)
async def send_quote(
    lead_id: str,
    body: QuoteRequest,
    current=Depends(get_current_shop_user),
    db: AsyncSession = Depends(get_db),
):
    """Send or update a quote for an incoming lead."""
    _, shop = current
    result = await db.execute(
        select(ServiceLead)
        .where(ServiceLead.id == lead_id, ServiceLead.shop_id == shop.id)
        .options(selectinload(ServiceLead.quote))
    )
    lead = result.scalar_one_or_none()
    if lead is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Lead not found")
    if lead.status == "closed":
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Lead is closed")

    if lead.quote:
        lead.quote.cost_min = body.cost_min
        lead.quote.cost_max = body.cost_max
        lead.quote.estimated_days = body.estimated_days
        lead.quote.notes = body.notes
    else:
        quote = ShopQuote(
            lead_id=lead.id,
            cost_min=body.cost_min,
            cost_max=body.cost_max,
            estimated_days=body.estimated_days,
            notes=body.notes,
        )
        db.add(quote)

    lead.status = "quoted"
    await db.commit()
    await db.refresh(lead)

    user_result = await db.execute(select(User).where(User.id == lead.user_id))
    user = user_result.scalar_one_or_none()

    # Re-fetch quote after commit
    result2 = await db.execute(
        select(ServiceLead)
        .where(ServiceLead.id == lead_id)
        .options(selectinload(ServiceLead.quote))
    )
    lead = result2.scalar_one()

    return LeadOut(
        lead_id=lead.id,
        user_email=user.email if user else "unknown",
        status=lead.status,
        dtc_codes=lead.dtc_codes,
        vehicle_info=lead.vehicle_info,
        freeze_frame=lead.freeze_frame,
        created_at=lead.created_at,
        quote=QuoteOut(
            cost_min=lead.quote.cost_min,
            cost_max=lead.quote.cost_max,
            estimated_days=lead.quote.estimated_days,
            notes=lead.quote.notes,
        ) if lead.quote else None,
    )


@router.put("/{lead_id}/close", response_model=LeadOut)
async def close_lead(
    lead_id: str,
    current=Depends(get_current_shop_user),
    db: AsyncSession = Depends(get_db),
):
    """Close a lead."""
    _, shop = current
    result = await db.execute(
        select(ServiceLead)
        .where(ServiceLead.id == lead_id, ServiceLead.shop_id == shop.id)
        .options(selectinload(ServiceLead.quote))
    )
    lead = result.scalar_one_or_none()
    if lead is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Lead not found")

    lead.status = "closed"
    await db.commit()
    await db.refresh(lead)

    user_result = await db.execute(select(User).where(User.id == lead.user_id))
    user = user_result.scalar_one_or_none()

    quote_out = None
    if lead.quote:
        quote_out = QuoteOut(
            cost_min=lead.quote.cost_min,
            cost_max=lead.quote.cost_max,
            estimated_days=lead.quote.estimated_days,
            notes=lead.quote.notes,
        )

    return LeadOut(
        lead_id=lead.id,
        user_email=user.email if user else "unknown",
        status=lead.status,
        dtc_codes=lead.dtc_codes,
        vehicle_info=lead.vehicle_info,
        freeze_frame=lead.freeze_frame,
        created_at=lead.created_at,
        quote=quote_out,
    )
