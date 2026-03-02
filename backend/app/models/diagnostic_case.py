from sqlalchemy import ForeignKey, String
from sqlalchemy.dialects.postgresql import JSONB
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.models.base import Base, TimestampMixin, new_uuid


class DiagnosticCase(Base, TimestampMixin):
    __tablename__ = "diagnostic_cases"

    id: Mapped[str] = mapped_column(String, primary_key=True, default=new_uuid)
    shop_id: Mapped[str] = mapped_column(String, ForeignKey("shops.id"), index=True)
    vehicle_info: Mapped[dict] = mapped_column(JSONB)  # {make, model, year, engine, vin, mileage}
    input_codes: Mapped[list] = mapped_column(JSONB, default=list)  # ["P0420", "P0171"]
    symptoms_text: Mapped[str] = mapped_column(String, default="")
    freeze_frame: Mapped[dict] = mapped_column(JSONB, default=dict)
    ai_result: Mapped[dict] = mapped_column(JSONB, default=dict)  # Prompt 3 output
    client_report_text: Mapped[str] = mapped_column(String, default="")  # Prompt 4 output
    estimate: Mapped[dict] = mapped_column(JSONB, default=dict)  # {parts[], labor_hours, total}
    status: Mapped[str] = mapped_column(String, default="open")  # open | in_progress | completed

    shop: Mapped["Shop"] = relationship(back_populates="diagnostic_cases")  # noqa: F821
