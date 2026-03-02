from fastapi import APIRouter

from app.api.v1.b2c.router import router as b2c_router
from app.api.v1.b2b.router import router as b2b_router
from app.api.v1.shared.router import router as shared_router

api_router = APIRouter()

api_router.include_router(b2c_router, prefix="/b2c", tags=["B2C"])
api_router.include_router(b2b_router, prefix="/b2b", tags=["B2B"])
api_router.include_router(shared_router, prefix="/shared", tags=["Shared"])
