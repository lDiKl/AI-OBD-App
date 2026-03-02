from sqlalchemy import Float, ForeignKey, String
from sqlalchemy.orm import Mapped, mapped_column

from app.models.base import Base, TimestampMixin, new_uuid


class RepairCostEstimate(Base, TimestampMixin):
    """
    Repair cost ranges per DTC code per region.
    Region codes: EU, US, UA (Ukraine), RU (Russia), etc.
    Costs in EUR, USD, or local currency depending on region.
    """

    __tablename__ = "repair_cost_estimates"

    id: Mapped[str] = mapped_column(String, primary_key=True, default=new_uuid)
    code: Mapped[str] = mapped_column(String, ForeignKey("error_codes.code"), index=True)
    region: Mapped[str] = mapped_column(String(10), index=True)  # EU, US, UA, ...
    currency: Mapped[str] = mapped_column(String(3))             # EUR, USD, UAH, ...
    cost_min: Mapped[float] = mapped_column(Float)
    cost_max: Mapped[float] = mapped_column(Float)
    labor_hours_min: Mapped[float] = mapped_column(Float, default=0.0)
    labor_hours_max: Mapped[float] = mapped_column(Float, default=0.0)
    source: Mapped[str] = mapped_column(String, default="estimated")  # estimated | verified
