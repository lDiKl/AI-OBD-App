from sqlalchemy import Boolean, Float, ForeignKey, String
from sqlalchemy.dialects.postgresql import ARRAY
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.models.base import Base, TimestampMixin, new_uuid


class Shop(Base, TimestampMixin):
    __tablename__ = "shops"

    id: Mapped[str] = mapped_column(String, primary_key=True, default=new_uuid)
    name: Mapped[str] = mapped_column(String)
    address: Mapped[str] = mapped_column(String, default="")
    lat: Mapped[float] = mapped_column(Float, default=0.0)
    lng: Mapped[float] = mapped_column(Float, default=0.0)
    phone: Mapped[str] = mapped_column(String, default="")
    email: Mapped[str] = mapped_column(String, default="")
    subscription_tier: Mapped[str] = mapped_column(String, default="basic")  # basic | pro
    rating: Mapped[float] = mapped_column(Float, default=0.0)
    verified: Mapped[bool] = mapped_column(Boolean, default=False)

    users: Mapped[list["ShopUser"]] = relationship(back_populates="shop")  # noqa: F821
    diagnostic_cases: Mapped[list["DiagnosticCase"]] = relationship(back_populates="shop")  # noqa: F821


class ShopUser(Base, TimestampMixin):
    __tablename__ = "shop_users"

    id: Mapped[str] = mapped_column(String, primary_key=True, default=new_uuid)
    shop_id: Mapped[str] = mapped_column(String, ForeignKey("shops.id"), index=True)
    firebase_uid: Mapped[str] = mapped_column(String, unique=True, index=True)
    email: Mapped[str] = mapped_column(String)
    role: Mapped[str] = mapped_column(String, default="mechanic")  # mechanic | senior | manager | owner

    shop: Mapped["Shop"] = relationship(back_populates="users")  # noqa: F821
