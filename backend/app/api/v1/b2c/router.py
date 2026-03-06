from fastapi import APIRouter

from app.api.v1.b2c import scan, vehicles, sessions, subscription, services, leads

router = APIRouter()

router.include_router(scan.router, prefix="/scan")
router.include_router(vehicles.router, prefix="/vehicles")
router.include_router(sessions.router, prefix="/sessions")
router.include_router(subscription.router, prefix="/subscription")
router.include_router(services.router, prefix="/services", tags=["B2C Services"])
router.include_router(leads.router, prefix="/leads", tags=["B2C Leads"])
