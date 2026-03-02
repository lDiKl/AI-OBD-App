from fastapi import APIRouter, Depends
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.database import get_db
from app.core.security import verify_firebase_token

router = APIRouter()


@router.get("/")
async def list_vehicles(token: dict = Depends(verify_firebase_token), db: AsyncSession = Depends(get_db)):
    # TODO: return user's vehicles
    raise NotImplementedError


@router.post("/")
async def create_vehicle(token: dict = Depends(verify_firebase_token), db: AsyncSession = Depends(get_db)):
    # TODO: create vehicle
    raise NotImplementedError


@router.put("/{vehicle_id}")
async def update_vehicle(vehicle_id: str, token: dict = Depends(verify_firebase_token), db: AsyncSession = Depends(get_db)):
    # TODO: update vehicle
    raise NotImplementedError


@router.delete("/{vehicle_id}")
async def delete_vehicle(vehicle_id: str, token: dict = Depends(verify_firebase_token), db: AsyncSession = Depends(get_db)):
    # TODO: delete vehicle
    raise NotImplementedError
