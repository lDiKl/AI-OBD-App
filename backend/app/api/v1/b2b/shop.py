from fastapi import APIRouter, Depends, HTTPException, status
from pydantic import BaseModel
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.database import get_db
from app.core.security import get_current_shop_user, verify_firebase_token
from app.models.shop import Shop, ShopUser

router = APIRouter()


# ── Schemas ───────────────────────────────────────────────────────────────────

class ShopSetupRequest(BaseModel):
    shop_name: str
    address: str = ""
    phone: str = ""
    email: str = ""


class ShopOut(BaseModel):
    id: str
    name: str
    address: str
    phone: str
    email: str
    subscription_tier: str
    verified: bool

    model_config = {"from_attributes": True}


class ShopUserOut(BaseModel):
    id: str
    shop_id: str
    email: str
    role: str

    model_config = {"from_attributes": True}


# ── Endpoints ─────────────────────────────────────────────────────────────────

@router.post("/setup", response_model=ShopOut, status_code=status.HTTP_201_CREATED)
async def setup_shop(
    body: ShopSetupRequest,
    token: dict = Depends(verify_firebase_token),
    db: AsyncSession = Depends(get_db),
):
    """
    Register a new shop and make the caller its owner.
    Called once when a new mechanic/manager first signs in to the B2B app.
    """
    firebase_uid = token.get("uid")
    email = token.get("email", "")

    # Check not already registered
    result = await db.execute(select(ShopUser).where(ShopUser.firebase_uid == firebase_uid))
    if result.scalar_one_or_none():
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="Already registered as a shop user.",
        )

    shop = Shop(
        name=body.shop_name,
        address=body.address,
        phone=body.phone,
        email=body.email or email,
    )
    db.add(shop)
    await db.flush()  # get shop.id without committing

    shop_user = ShopUser(
        shop_id=shop.id,
        firebase_uid=firebase_uid,
        email=email,
        role="owner",
    )
    db.add(shop_user)
    await db.commit()
    await db.refresh(shop)

    return ShopOut.model_validate(shop)


@router.get("/profile", response_model=ShopOut)
async def get_shop_profile(
    current=Depends(get_current_shop_user),
):
    """Return the current user's shop profile."""
    _, shop = current
    return ShopOut.model_validate(shop)


@router.put("/profile", response_model=ShopOut)
async def update_shop_profile(
    body: ShopSetupRequest,
    current=Depends(get_current_shop_user),
    db: AsyncSession = Depends(get_db),
):
    """Update shop profile. Any shop member can update."""
    _, shop = current
    shop.name = body.shop_name
    if body.address:
        shop.address = body.address
    if body.phone:
        shop.phone = body.phone
    if body.email:
        shop.email = body.email
    await db.commit()
    await db.refresh(shop)
    return ShopOut.model_validate(shop)
