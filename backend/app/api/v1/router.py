from fastapi import APIRouter

from app.api.v1.auth.verify import router as auth_router
from app.api.v1.b2b.router import router as b2b_router
from app.api.v1.b2c.router import router as b2c_router
from app.api.v1.shared.router import router as shared_router
from app.api.v1.webhooks import router as webhooks_router

api_router = APIRouter()

api_router.include_router(auth_router, prefix="/auth", tags=["Auth"])
api_router.include_router(b2c_router, prefix="/b2c", tags=["B2C"])
api_router.include_router(b2b_router, prefix="/b2b", tags=["B2B"])
api_router.include_router(shared_router, prefix="/shared", tags=["Shared"])
api_router.include_router(webhooks_router, prefix="/webhooks", tags=["Webhooks"])
