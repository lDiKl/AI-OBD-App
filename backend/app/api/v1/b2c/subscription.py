import logging

import stripe
from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.config import settings
from app.core.database import get_db
from app.core.security import get_current_user

router = APIRouter()
logger = logging.getLogger(__name__)

stripe.api_key = settings.STRIPE_SECRET_KEY


@router.post("/checkout")
async def create_checkout(user=Depends(get_current_user)):
    """Create Stripe Checkout session for B2C Premium. Returns URL to open in browser."""
    if not settings.STRIPE_SECRET_KEY:
        raise HTTPException(status_code=503, detail="Payment not configured")
    if not settings.STRIPE_PRICE_B2C_PREMIUM:
        raise HTTPException(status_code=503, detail="B2C price not configured")

    try:
        session = stripe.checkout.Session.create(
            mode="subscription",
            payment_method_types=["card"],
            line_items=[{"price": settings.STRIPE_PRICE_B2C_PREMIUM, "quantity": 1}],
            success_url=settings.B2C_SUCCESS_URL + "?session_id={CHECKOUT_SESSION_ID}",
            cancel_url=settings.B2C_CANCEL_URL,
            customer_email=user.email,
            metadata={"type": "b2c", "user_id": user.id},
        )
        return {"checkout_url": session.url}
    except stripe.StripeError as e:
        logger.error("Stripe checkout error (B2C): %s", e, exc_info=True)
        raise HTTPException(status_code=400, detail=str(e))


@router.get("/status")
async def get_status(user=Depends(get_current_user)):
    """Return current subscription status."""
    return {"subscription_status": user.subscription_status, "email": user.email}
