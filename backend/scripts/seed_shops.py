"""
Seed script: creates 4 test shops near Wrocław + Firebase Auth accounts for B2B login.

Run inside the api container:
    docker compose exec api python scripts/seed_shops.py

Output: prints shop IDs and B2B login credentials.
"""

import asyncio
import json
import os
import sys
import uuid

# ── Bootstrap path & Firebase ──────────────────────────────────────────────
sys.path.insert(0, "/app")

import firebase_admin
from firebase_admin import auth, credentials
from sqlalchemy import select
from sqlalchemy.ext.asyncio import create_async_engine, AsyncSession
from sqlalchemy.orm import sessionmaker

from app.models.shop import Shop, ShopUser
from app.models.base import Base

DATABASE_URL = os.environ["DATABASE_URL"]
FIREBASE_CREDENTIALS_PATH = os.environ.get("FIREBASE_CREDENTIALS_PATH", "/app/firebase-credentials.json")
FIREBASE_CREDENTIALS_JSON = os.environ.get("FIREBASE_CREDENTIALS_JSON", "")

# ── Shop definitions (Wrocław area, user at 51.0611, 17.0530) ───────────────
SHOPS = [
    {
        "name": "AutoSerwis Południe",
        "address": "ul. Borowska 15, 50-528 Wrocław",
        "phone": "+48 71 123 45 67",
        "email": "mechanic1@shopai.test",
        "password": "Test1234!",
        "lat": 51.065,
        "lng": 17.058,
        "rating": 4.7,
        "subscription_tier": "pro",
    },
    {
        "name": "Mechanika Precyzyjna",
        "address": "ul. Świdnicka 42, 50-027 Wrocław",
        "phone": "+48 71 234 56 78",
        "email": "mechanic2@shopai.test",
        "password": "Test1234!",
        "lat": 51.075,
        "lng": 17.040,
        "rating": 4.5,
        "subscription_tier": "basic",
    },
    {
        "name": "CarFix Centrum",
        "address": "pl. Grunwaldzki 8, 50-384 Wrocław",
        "phone": "+48 71 345 67 89",
        "email": "mechanic3@shopai.test",
        "password": "Test1234!",
        "lat": 51.085,
        "lng": 17.020,
        "rating": 4.2,
        "subscription_tier": "pro",
    },
    {
        "name": "OBD Expert Wrocław",
        "address": "ul. Legnicka 55, 54-204 Wrocław",
        "phone": "+48 71 456 78 90",
        "email": "mechanic4@shopai.test",
        "password": "Test1234!",
        "lat": 51.110,
        "lng": 17.040,
        "rating": 4.9,
        "subscription_tier": "pro",
    },
]


def init_firebase():
    if not firebase_admin._apps:
        if FIREBASE_CREDENTIALS_JSON:
            cred = credentials.Certificate(json.loads(FIREBASE_CREDENTIALS_JSON))
        else:
            cred = credentials.Certificate(FIREBASE_CREDENTIALS_PATH)
        firebase_admin.initialize_app(cred)


def get_or_create_firebase_user(email: str, password: str) -> str:
    """Return existing Firebase UID or create a new user."""
    try:
        user = auth.get_user_by_email(email)
        print(f"  Firebase user already exists: {email} → {user.uid}")
        return user.uid
    except auth.UserNotFoundError:
        user = auth.create_user(email=email, password=password, email_verified=True)
        print(f"  Firebase user created: {email} → {user.uid}")
        return user.uid


async def seed(session: AsyncSession):
    results = []

    for shop_data in SHOPS:
        email = shop_data["email"]
        password = shop_data["password"]

        # 1. Firebase account
        firebase_uid = get_or_create_firebase_user(email, password)

        # 2. Check if shop already exists (by email)
        existing = await session.execute(select(Shop).where(Shop.email == email))
        shop = existing.scalar_one_or_none()

        if shop is None:
            shop = Shop(
                id=str(uuid.uuid4()),
                name=shop_data["name"],
                address=shop_data["address"],
                phone=shop_data["phone"],
                email=email,
                lat=shop_data["lat"],
                lng=shop_data["lng"],
                rating=shop_data["rating"],
                subscription_tier=shop_data["subscription_tier"],
                verified=True,
            )
            session.add(shop)
            await session.flush()
            print(f"  Shop created: {shop.name} (id={shop.id})")
        else:
            # Update lat/lng if shop exists but coordinates are 0
            if shop.lat == 0.0:
                shop.lat = shop_data["lat"]
                shop.lng = shop_data["lng"]
            print(f"  Shop already exists: {shop.name} (id={shop.id})")

        # 3. Check if ShopUser exists
        existing_user = await session.execute(
            select(ShopUser).where(ShopUser.firebase_uid == firebase_uid)
        )
        shop_user = existing_user.scalar_one_or_none()

        if shop_user is None:
            shop_user = ShopUser(
                id=str(uuid.uuid4()),
                shop_id=shop.id,
                firebase_uid=firebase_uid,
                email=email,
                role="owner",
            )
            session.add(shop_user)
            print(f"  ShopUser created for {email}")
        else:
            print(f"  ShopUser already exists for {email}")

        results.append({
            "shop_name": shop_data["name"],
            "shop_id": shop.id,
            "email": email,
            "password": password,
            "address": shop_data["address"],
            "lat": shop_data["lat"],
            "lng": shop_data["lng"],
            "distance_hint": shop_data.get("_note", ""),
        })

    await session.commit()
    return results


async def main():
    print("=== Seed: test shops near Wrocław ===\n")

    init_firebase()

    engine = create_async_engine(DATABASE_URL, echo=False)
    async_session = sessionmaker(engine, class_=AsyncSession, expire_on_commit=False)

    async with async_session() as session:
        results = await seed(session)

    await engine.dispose()

    print("\n=== DONE — B2B Login Credentials ===")
    print(f"{'Shop':<28} {'Email':<30} {'Password':<14} {'Coords'}")
    print("-" * 95)
    for r in results:
        print(f"{r['shop_name']:<28} {r['email']:<30} {r['password']:<14} {r['lat']}, {r['lng']}")

    print("\nB2B App: use any email/password above to log in as a shop mechanic.")
    print("B2C App: scan → Analyze with AI → Find Nearby Service → shops should appear.\n")


if __name__ == "__main__":
    asyncio.run(main())
