from fastapi import APIRouter

from app.api.v1.b2b import diagnostic, cases, reports, estimates, shop

router = APIRouter()

router.include_router(shop.router, prefix="/shop", tags=["B2B Shop"])
router.include_router(diagnostic.router, prefix="/diagnostic", tags=["B2B Diagnostic"])
router.include_router(cases.router, prefix="/cases", tags=["B2B Cases"])
router.include_router(reports.router, prefix="/cases", tags=["B2B Reports"])
router.include_router(estimates.router, prefix="/cases", tags=["B2B Estimates"])
