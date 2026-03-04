from datetime import datetime

from sqlalchemy import DateTime, Float, ForeignKey, Integer, String, Text, func
from sqlalchemy.dialects.postgresql import JSONB
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.models.base import Base, new_uuid


class ServiceLead(Base):
    __tablename__ = "service_leads"

    id: Mapped[str] = mapped_column(String, primary_key=True, default=new_uuid)
    user_id: Mapped[str] = mapped_column(String, ForeignKey("users.id", ondelete="CASCADE"), index=True)
    shop_id: Mapped[str] = mapped_column(String, ForeignKey("shops.id", ondelete="CASCADE"), index=True)
    scan_session_id: Mapped[str | None] = mapped_column(
        String, ForeignKey("scan_sessions.id", ondelete="SET NULL"), nullable=True
    )
    dtc_codes: Mapped[list] = mapped_column(JSONB, default=list)
    vehicle_info: Mapped[dict] = mapped_column(JSONB, default=dict)
    freeze_frame: Mapped[dict | None] = mapped_column(JSONB, nullable=True)
    status: Mapped[str] = mapped_column(String, default="pending")  # pending | quoted | closed
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), onupdate=func.now()
    )

    quote: Mapped["ShopQuote | None"] = relationship(back_populates="lead", uselist=False)


class ShopQuote(Base):
    __tablename__ = "shop_quotes"

    id: Mapped[str] = mapped_column(String, primary_key=True, default=new_uuid)
    lead_id: Mapped[str] = mapped_column(
        String, ForeignKey("service_leads.id", ondelete="CASCADE"), unique=True, index=True
    )
    cost_min: Mapped[float] = mapped_column(Float)
    cost_max: Mapped[float] = mapped_column(Float)
    estimated_days: Mapped[int] = mapped_column(Integer)
    notes: Mapped[str | None] = mapped_column(Text, nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())

    lead: Mapped["ServiceLead"] = relationship(back_populates="quote")
