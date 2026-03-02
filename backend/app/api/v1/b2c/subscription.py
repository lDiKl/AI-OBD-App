import stripe
from fastapi import APIRouter, Depends, HTTPException, Request

from app.core.config import settings
from app.core.security import verify_firebase_token

router = APIRouter()

stripe.api_key = settings.STRIPE_SECRET_KEY


@router.post("/checkout")
async def create_checkout(token: dict = Depends(verify_firebase_token)):
    """Create Stripe Checkout session. Opens in browser — no in-app payment UI."""
    try:
        session = stripe.checkout.Session.create(
            mode="subscription",
            payment_method_types=["card"],
            line_items=[{"price": settings.STRIPE_PRICE_B2C_PREMIUM, "quantity": 1}],
            success_url=settings.B2C_SUCCESS_URL + "?session_id={CHECKOUT_SESSION_ID}",
            cancel_url=settings.B2C_CANCEL_URL,
            metadata={"firebase_uid": token["uid"]},
        )
        return {"checkout_url": session.url}
    except stripe.StripeError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.post("/webhook")
async def stripe_webhook(request: Request):
    """Stripe webhook — updates subscription_status in DB."""
    payload = await request.body()
    sig = request.headers.get("stripe-signature", "")

    try:
        event = stripe.Webhook.construct_event(payload, sig, settings.STRIPE_WEBHOOK_SECRET)
    except stripe.SignatureVerificationError:
        raise HTTPException(status_code=400, detail="Invalid signature")

    if event.type == "customer.subscription.created":
        # TODO: update user.subscription_status = "premium"
        pass
    elif event.type == "customer.subscription.deleted":
        # TODO: update user.subscription_status = "free"
        pass

    return {"received": True}
