from fastapi import APIRouter, Depends
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.database import get_db
from app.core.security import verify_firebase_token

router = APIRouter()


@router.get("/")
async def list_cases(token: dict = Depends(verify_firebase_token), db: AsyncSession = Depends(get_db)):
    # TODO: return shop's diagnostic cases
    raise NotImplementedError


@router.post("/")
async def create_case(token: dict = Depends(verify_firebase_token), db: AsyncSession = Depends(get_db)):
    # TODO: create new diagnostic case
    raise NotImplementedError


@router.get("/{case_id}")
async def get_case(case_id: str, token: dict = Depends(verify_firebase_token), db: AsyncSession = Depends(get_db)):
    # TODO: return case with AI analysis, report, estimate
    raise NotImplementedError


@router.put("/{case_id}")
async def update_case(case_id: str, token: dict = Depends(verify_firebase_token), db: AsyncSession = Depends(get_db)):
    # TODO: update case (add notes, photos, status)
    raise NotImplementedError
