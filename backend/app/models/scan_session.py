from sqlalchemy import Float, ForeignKey, Integer, String
from sqlalchemy.dialects.postgresql import JSONB
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.models.base import Base, TimestampMixin, new_uuid


class ScanSession(Base, TimestampMixin):
    __tablename__ = "scan_sessions"

    id: Mapped[str] = mapped_column(String, primary_key=True, default=new_uuid)
    vehicle_id: Mapped[str] = mapped_column(String, ForeignKey("vehicles.id"), index=True)
    mileage: Mapped[int] = mapped_column(Integer, default=0)
    risk_score: Mapped[str] = mapped_column(String, default="low")  # low | medium | high
    raw_obd_data: Mapped[dict] = mapped_column(JSONB, default=dict)  # raw ELM327 output

    vehicle: Mapped["Vehicle"] = relationship(back_populates="scan_sessions")  # noqa: F821
    error_occurrences: Mapped[list["ErrorOccurrence"]] = relationship(back_populates="session")  # noqa: F821


class ErrorOccurrence(Base, TimestampMixin):
    __tablename__ = "error_occurrences"

    id: Mapped[str] = mapped_column(String, primary_key=True, default=new_uuid)
    session_id: Mapped[str] = mapped_column(String, ForeignKey("scan_sessions.id"), index=True)
    code: Mapped[str] = mapped_column(String, ForeignKey("error_codes.code"))
    freeze_frame: Mapped[dict] = mapped_column(JSONB, default=dict)
    occurrence_count: Mapped[int] = mapped_column(Integer, default=1)
    ai_result: Mapped[dict] = mapped_column(JSONB, default=dict)  # LLM output cached here

    session: Mapped["ScanSession"] = relationship(back_populates="error_occurrences")  # noqa: F821
