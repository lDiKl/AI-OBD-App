from datetime import datetime

from fastapi import APIRouter, Depends, HTTPException, status
from pydantic import BaseModel
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.orm import selectinload

from app.core.database import get_db
from app.core.security import get_current_user
from app.models.lead import ServiceLead, ShopQuote
from app.models.shop import Shop

router = APIRouter()


# ── Schemas ───────────────────────────────────────────────────────────────────

class SendLeadRequest(BaseModel):
    shop_id: str
    scan_session_id: str | None = None
    dtc_codes: list[str]
    vehicle_info: dict  # {make, model, year}
    freeze_frame: dict | None = None


class LeadOut(BaseModel):
    lead_id: str
    shop_name: str
    status: str
    dtc_codes: list
    vehicle_info: dict
    created_at: datetime
    quote: "QuoteOut | None" = None

    model_config = {"from_attributes": True}


class QuoteOut(BaseModel):
    cost_min: float
    cost_max: float
    estimated_days: int
    notes: str | None = None

    model_config = {"from_attributes": True}


LeadOut.model_rebuild()


class LeadCreatedOut(BaseModel):
    lead_id: str
    status: str


# ── Endpoints ─────────────────────────────────────────────────────────────────

@router.post("/", response_model=LeadCreatedOut, status_code=status.HTTP_201_CREATED)
async def send_lead(
    body: SendLeadRequest,
    current_user=Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    """Send a diagnostic lead to a specific shop."""
    # Verify shop exists
    shop_result = await db.execute(select(Shop).where(Shop.id == body.shop_id))
    shop = shop_result.scalar_one_or_none()
    if shop is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Shop not found")

    lead = ServiceLead(
        user_id=current_user.id,
        shop_id=body.shop_id,
        scan_session_id=body.scan_session_id,
        dtc_codes=body.dtc_codes,
        vehicle_info=body.vehicle_info,
        freeze_frame=body.freeze_frame,
        status="pending",
    )
    db.add(lead)
    await db.commit()
    await db.refresh(lead)
    return LeadCreatedOut(lead_id=lead.id, status=lead.status)


@router.get("/", response_model=list[LeadOut])
async def get_my_leads(
    current_user=Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    """Return all leads sent by the current user, newest first."""
    result = await db.execute(
        select(ServiceLead)
        .where(ServiceLead.user_id == current_user.id)
        .options(selectinload(ServiceLead.quote))
        .order_by(ServiceLead.created_at.desc())
    )
    leads = result.scalars().all()

    # Enrich with shop names
    out = []
    for lead in leads:
        shop_result = await db.execute(select(Shop).where(Shop.id == lead.shop_id))
        shop = shop_result.scalar_one_or_none()
        shop_name = shop.name if shop else "Unknown"

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
            shop_name=shop_name,
            status=lead.status,
            dtc_codes=lead.dtc_codes,
            vehicle_info=lead.vehicle_info,
            created_at=lead.created_at,
            quote=quote_out,
        ))

    return out
