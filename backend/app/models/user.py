from sqlalchemy import String
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.models.base import Base, TimestampMixin, new_uuid


class User(Base, TimestampMixin):
    __tablename__ = "users"

    id: Mapped[str] = mapped_column(String, primary_key=True, default=new_uuid)
    firebase_uid: Mapped[str] = mapped_column(String, unique=True, index=True)
    email: Mapped[str] = mapped_column(String, unique=True)
    subscription_status: Mapped[str] = mapped_column(String, default="free")  # free | premium
    region: Mapped[str] = mapped_column(String, default="EU")
    language: Mapped[str] = mapped_column(String, default="en")

    vehicles: Mapped[list["Vehicle"]] = relationship(back_populates="user")  # noqa: F821
