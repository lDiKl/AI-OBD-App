from fastapi import APIRouter, Depends

from app.core.security import get_current_user
from app.models.user import User

router = APIRouter()


@router.post("/verify")
async def verify_token(current_user: User = Depends(get_current_user)):
    """
    Verify Firebase token and return the authenticated user profile.
    Creates the user row on first sign-in (auto-provisioning).
    """
    return {
        "id": current_user.id,
        "email": current_user.email,
        "subscription_status": current_user.subscription_status,
        "region": current_user.region,
        "language": current_user.language,
    }
