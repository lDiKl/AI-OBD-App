import json
import os

import firebase_admin
from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from firebase_admin import auth, credentials

from app.core.config import settings

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
