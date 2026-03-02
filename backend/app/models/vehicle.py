from sqlalchemy import ForeignKey, String
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.models.base import Base, TimestampMixin, new_uuid


class Vehicle(Base, TimestampMixin):
    __tablename__ = "vehicles"

    id: Mapped[str] = mapped_column(String, primary_key=True, default=new_uuid)
    user_id: Mapped[str] = mapped_column(String, ForeignKey("users.id"), index=True)
    make: Mapped[str] = mapped_column(String)
    model: Mapped[str] = mapped_column(String)
    year: Mapped[int]
    engine_type: Mapped[str] = mapped_column(String, default="")
    vin: Mapped[str] = mapped_column(String, default="")

    user: Mapped["User"] = relationship(back_populates="vehicles")  # noqa: F821
    scan_sessions: Mapped[list["ScanSession"]] = relationship(back_populates="vehicle")  # noqa: F821
