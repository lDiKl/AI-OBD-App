from fastapi import APIRouter

from app.api.v1.b2b import diagnostic, cases, reports, estimates

router = APIRouter()

router.include_router(diagnostic.router, prefix="/diagnostic")
router.include_router(cases.router, prefix="/cases")
router.include_router(reports.router, prefix="/cases")
router.include_router(estimates.router, prefix="/cases")
