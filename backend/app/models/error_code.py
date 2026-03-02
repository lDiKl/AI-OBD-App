from sqlalchemy import Boolean, Float, String
from sqlalchemy.dialects.postgresql import ARRAY
from sqlalchemy.orm import Mapped, mapped_column

from app.models.base import Base


class ErrorCode(Base):
    """
    Shared DTC database. Populated from docs/dtc_codes.csv + enrichment.
    Used by both B2C and B2B.
    """
    __tablename__ = "error_codes"

    code: Mapped[str] = mapped_column(String, primary_key=True)  # e.g. "P0420"
    standard_description: Mapped[str] = mapped_column(String)
    category: Mapped[str] = mapped_column(String)  # P | B | C | U
    system: Mapped[str] = mapped_column(String, default="unknown")  # engine | emissions | transmission | brakes | electrical
    severity_level: Mapped[str] = mapped_column(String, default="medium")  # low | medium | high | critical
    can_drive_flag: Mapped[str] = mapped_column(String, default="yes_with_caution")  # yes | yes_with_caution | yes_within_2_weeks | limited | no
    manufacturer_specific: Mapped[bool] = mapped_column(Boolean, default=False)
