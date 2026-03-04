import math
from datetime import datetime

from fastapi import APIRouter, Depends, Query
from sqlalchemy import select, text
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.database import get_db
from app.core.security import get_current_user
from app.models.shop import Shop
from pydantic import BaseModel

router = APIRouter()


class ShopNearbyOut(BaseModel):
    id: str
    name: str
    address: str
    phone: str
    rating: float
    distance_km: float

    model_config = {"from_attributes": True}


@router.get("/nearby", response_model=list[ShopNearbyOut])
async def get_nearby_shops(
    lat: float = Query(..., description="Latitude"),
    lng: float = Query(..., description="Longitude"),
    radius_km: float = Query(50.0, description="Search radius in km"),
    _=Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    """Return shops within radius_km sorted by distance. Uses Haversine formula."""
    result = await db.execute(select(Shop))
    shops = result.scalars().all()

    nearby = []
    for shop in shops:
        dist = _haversine(lat, lng, shop.lat, shop.lng)
        if dist <= radius_km:
            nearby.append(ShopNearbyOut(
                id=shop.id,
                name=shop.name,
                address=shop.address,
                phone=shop.phone,
                rating=shop.rating,
                distance_km=round(dist, 1),
            ))

    nearby.sort(key=lambda s: s.distance_km)
    return nearby


def _haversine(lat1: float, lon1: float, lat2: float, lon2: float) -> float:
    """Return distance in km between two lat/lng points."""
    R = 6371.0
    phi1, phi2 = math.radians(lat1), math.radians(lat2)
    dphi = math.radians(lat2 - lat1)
    dlambda = math.radians(lon2 - lon1)
    a = math.sin(dphi / 2) ** 2 + math.cos(phi1) * math.cos(phi2) * math.sin(dlambda / 2) ** 2
    return R * 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))
