import logging

import stripe
from fastapi import APIRouter, Depends, HTTPException
from pydantic import BaseModel

from app.core.config import settings
from app.core.security import get_current_shop_user

router = APIRouter()
logger = logging.getLogger(__name__)

stripe.api_key = settings.STRIPE_SECRET_KEY


class B2BCheckoutRequest(BaseModel):
    tier: str = "pro"  # "basic" | "pro"
    success_url: str | None = None  # override for mobile deep links
    cancel_url: str | None = None


@router.post("/checkout")
async def create_checkout(body: B2BCheckoutRequest, current=Depends(get_current_shop_user)):
    """Create Stripe Checkout session for B2B Basic or Pro. Returns URL to redirect to."""
    if not settings.STRIPE_SECRET_KEY:
        raise HTTPException(status_code=503, detail="Payment not configured")

    _, shop = current

    if body.tier == "pro":
        price_id = settings.STRIPE_PRICE_B2B_PRO
    else:
        price_id = settings.STRIPE_PRICE_B2B_BASIC

    if not price_id:
        raise HTTPException(status_code=503, detail=f"B2B {body.tier} price not configured")

    try:
        resolved_success = body.success_url or (settings.B2B_SUCCESS_URL + "?session_id={CHECKOUT_SESSION_ID}")
        resolved_cancel = body.cancel_url or settings.B2B_CANCEL_URL
        session = stripe.checkout.Session.create(
            mode="subscription",
            payment_method_types=["card"],
            line_items=[{"price": price_id, "quantity": 1}],
            success_url=resolved_success,
            cancel_url=resolved_cancel,
            customer_email=shop.email or None,
            metadata={"type": "b2b", "shop_id": shop.id, "tier": body.tier},
        )
        return {"checkout_url": session.url}
    except stripe.StripeError as e:
        logger.error("Stripe checkout error (B2B): %s", e, exc_info=True)
        raise HTTPException(status_code=400, detail=str(e))


@router.get("/status")
async def get_status(current=Depends(get_current_shop_user)):
    """Return current shop subscription tier."""
    _, shop = current
    return {"subscription_tier": shop.subscription_tier}
