"""
Stripe webhook handler — single endpoint for both B2C and B2B events.
Stripe routes by metadata.type: "b2c" | "b2b".
No Firebase auth — uses Stripe signature verification.
"""
import logging

import stripe
from fastapi import APIRouter, Depends, HTTPException, Request
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.config import settings
from app.core.database import get_db

router = APIRouter()
logger = logging.getLogger(__name__)


@router.post("/stripe", include_in_schema=False)
async def stripe_webhook(request: Request, db: AsyncSession = Depends(get_db)):
    """
    Receive Stripe events and update subscription status in DB.
    Register this URL in Stripe Dashboard or use 'stripe listen --forward-to ...' in dev.
    """
    payload = await request.body()
    sig = request.headers.get("stripe-signature", "")

    try:
        event = stripe.Webhook.construct_event(payload, sig, settings.STRIPE_WEBHOOK_SECRET)
    except stripe.SignatureVerificationError:
        logger.warning("Stripe webhook: invalid signature")
        raise HTTPException(status_code=400, detail="Invalid signature")
    except Exception as e:
        logger.error("Stripe webhook: failed to parse event: %s", e, exc_info=True)
        raise HTTPException(status_code=400, detail="Malformed event")

    logger.info("Stripe event: %s", event["type"])

    if event["type"] == "checkout.session.completed":
        session = event["data"]["object"]
        meta = session.get("metadata") or {}
        event_type = meta.get("type")

        if event_type == "b2c":
            await _handle_b2c_checkout(meta, db)
        elif event_type == "b2b":
            await _handle_b2b_checkout(meta, db)
        else:
            logger.warning("Stripe webhook: unknown metadata.type=%s", event_type)

    elif event["type"] in ("customer.subscription.deleted", "customer.subscription.paused"):
        # Downgrade on cancellation — check metadata if available
        subscription = event["data"]["object"]
        meta = subscription.get("metadata") or {}
        event_type = meta.get("type")
        if event_type == "b2c":
            await _downgrade_b2c(meta, db)
        elif event_type == "b2b":
            await _downgrade_b2b(meta, db)

    return {"received": True}


async def _handle_b2c_checkout(meta: dict, db: AsyncSession) -> None:
    from app.models.user import User

    user_id = meta.get("user_id")
    if not user_id:
        logger.error("B2C checkout.session.completed: missing user_id in metadata")
        return

    result = await db.execute(select(User).where(User.id == user_id))
    user = result.scalar_one_or_none()
    if user is None:
        logger.error("B2C checkout: user %s not found", user_id)
        return

    user.subscription_status = "premium"
    await db.commit()
    logger.info("B2C user %s upgraded to premium", user_id)


async def _handle_b2b_checkout(meta: dict, db: AsyncSession) -> None:
    from app.models.shop import Shop

    shop_id = meta.get("shop_id")
    tier = meta.get("tier", "pro")
    if not shop_id:
        logger.error("B2B checkout.session.completed: missing shop_id in metadata")
        return

    result = await db.execute(select(Shop).where(Shop.id == shop_id))
    shop = result.scalar_one_or_none()
    if shop is None:
        logger.error("B2B checkout: shop %s not found", shop_id)
        return

    shop.subscription_tier = tier
    await db.commit()
    logger.info("B2B shop %s upgraded to %s", shop_id, tier)


async def _downgrade_b2c(meta: dict, db: AsyncSession) -> None:
    from app.models.user import User

    user_id = meta.get("user_id")
    if not user_id:
        return
    result = await db.execute(select(User).where(User.id == user_id))
    user = result.scalar_one_or_none()
    if user:
        user.subscription_status = "free"
        await db.commit()
        logger.info("B2C user %s downgraded to free", user_id)


async def _downgrade_b2b(meta: dict, db: AsyncSession) -> None:
    from app.models.shop import Shop

    shop_id = meta.get("shop_id")
    if not shop_id:
        return
    result = await db.execute(select(Shop).where(Shop.id == shop_id))
    shop = result.scalar_one_or_none()
    if shop:
        shop.subscription_tier = "basic"
        await db.commit()
        logger.info("B2B shop %s downgraded to basic", shop_id)
