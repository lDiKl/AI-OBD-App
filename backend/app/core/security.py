import json
import os

import firebase_admin
from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from firebase_admin import auth, credentials
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.config import settings
from app.core.database import get_db

_firebase_initialized = False


def _init_firebase() -> None:
    global _firebase_initialized
    if _firebase_initialized:
        return

    if settings.FIREBASE_CREDENTIALS_JSON:
        cred_dict = json.loads(settings.FIREBASE_CREDENTIALS_JSON)
        cred = credentials.Certificate(cred_dict)
    elif os.path.exists(settings.FIREBASE_CREDENTIALS_PATH):
        cred = credentials.Certificate(settings.FIREBASE_CREDENTIALS_PATH)
    else:
        raise RuntimeError("Firebase credentials not configured")

    firebase_admin.initialize_app(cred)
    _firebase_initialized = True


bearer_scheme = HTTPBearer()


async def verify_firebase_token(
    credentials: HTTPAuthorizationCredentials = Depends(bearer_scheme),
) -> dict:
    """Verify Firebase ID token and return decoded claims."""
    _init_firebase()
    try:
        decoded = auth.verify_id_token(credentials.credentials)
        return decoded
    except Exception:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid or expired token",
        )


async def get_current_shop_user(
    credentials: HTTPAuthorizationCredentials = Depends(bearer_scheme),
    db: AsyncSession = Depends(get_db),
):
    """
    Verify Firebase token → find ShopUser row.
    Returns (ShopUser, Shop). Raises 403 if not registered as a shop user.
    """
    from app.models.shop import Shop, ShopUser  # avoid circular import

    token_data = await verify_firebase_token(credentials)
    firebase_uid = token_data.get("uid")

    result = await db.execute(select(ShopUser).where(ShopUser.firebase_uid == firebase_uid))
    shop_user = result.scalar_one_or_none()

    if shop_user is None:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Not registered as a shop user. Use POST /api/v1/b2b/shop/setup to register.",
        )

    result = await db.execute(select(Shop).where(Shop.id == shop_user.shop_id))
    shop = result.scalar_one()

    return shop_user, shop


async def get_current_user(
    token_data: dict = Depends(verify_firebase_token),
    db: AsyncSession = Depends(get_db),
):
    """
    Verify Firebase token → look up (or create) User row in DB.
    Returns the User ORM object.
    """
    from app.models.user import User  # avoid circular import at module level

    firebase_uid = token_data.get("uid")
    if not firebase_uid:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid token claims")

    result = await db.execute(select(User).where(User.firebase_uid == firebase_uid))
    user = result.scalar_one_or_none()

    if user is None:
        # First sign-in — auto-provision the user row
        user = User(
            firebase_uid=firebase_uid,
            email=token_data.get("email", ""),
            subscription_status="free",
            region="EU",
            language="en",
        )
        db.add(user)
        await db.commit()
        await db.refresh(user)

    return user
