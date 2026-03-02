from fastapi import APIRouter

from app.api.v1.shared import dtc

router = APIRouter()

router.include_router(dtc.router, prefix="/dtc")
