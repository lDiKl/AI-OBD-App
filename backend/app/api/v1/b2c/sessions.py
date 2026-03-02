from fastapi import APIRouter, Depends
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.database import get_db
from app.core.security import verify_firebase_token

router = APIRouter()


@router.get("/")
async def list_sessions(token: dict = Depends(verify_firebase_token), db: AsyncSession = Depends(get_db)):
    # TODO: return scan history
    raise NotImplementedError


@router.get("/{session_id}")
async def get_session(session_id: str, token: dict = Depends(verify_firebase_token), db: AsyncSession = Depends(get_db)):
    # TODO: return single session with all codes
    raise NotImplementedError
