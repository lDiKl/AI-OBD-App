from fastapi import APIRouter, Depends, HTTPException, status
from pydantic import BaseModel
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.database import get_db
from app.core.security import get_current_user
from app.models.user import User
from app.models.vehicle import Vehicle

router = APIRouter()


# ── Schemas ──────────────────────────────────────────────────────────────────

class VehicleCreate(BaseModel):
    make: str
    model: str
    year: int
    engine_type: str = ""
    vin: str = ""


class VehicleUpdate(BaseModel):
    make: str | None = None
    model: str | None = None
    year: int | None = None
    engine_type: str | None = None
    vin: str | None = None


class VehicleOut(BaseModel):
    id: str
    make: str
    model: str
    year: int
    engine_type: str
    vin: str

    model_config = {"from_attributes": True}


# ── Endpoints ─────────────────────────────────────────────────────────────────

@router.get("/", response_model=list[VehicleOut])
async def list_vehicles(
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    result = await db.execute(select(Vehicle).where(Vehicle.user_id == current_user.id))
    return result.scalars().all()


@router.post("/", response_model=VehicleOut, status_code=status.HTTP_201_CREATED)
async def create_vehicle(
    body: VehicleCreate,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    vehicle = Vehicle(
        user_id=current_user.id,
        make=body.make,
        model=body.model,
        year=body.year,
        engine_type=body.engine_type,
        vin=body.vin,
    )
    db.add(vehicle)
    await db.commit()
    await db.refresh(vehicle)
    return vehicle


@router.put("/{vehicle_id}", response_model=VehicleOut)
async def update_vehicle(
    vehicle_id: str,
    body: VehicleUpdate,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    result = await db.execute(
        select(Vehicle).where(Vehicle.id == vehicle_id, Vehicle.user_id == current_user.id)
    )
    vehicle = result.scalar_one_or_none()
    if vehicle is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Vehicle not found")

    for field, value in body.model_dump(exclude_none=True).items():
        setattr(vehicle, field, value)

    await db.commit()
    await db.refresh(vehicle)
    return vehicle


@router.delete("/{vehicle_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_vehicle(
    vehicle_id: str,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    result = await db.execute(
        select(Vehicle).where(Vehicle.id == vehicle_id, Vehicle.user_id == current_user.id)
    )
    vehicle = result.scalar_one_or_none()
    if vehicle is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Vehicle not found")

    await db.delete(vehicle)
    await db.commit()
