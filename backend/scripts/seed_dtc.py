"""
Seed DTC codes from docs/dtc_codes.csv into the error_codes table.

Usage (from backend/ directory):
    python scripts/seed_dtc.py

Requires DATABASE_URL env var or defaults to local dev DB.
"""

import asyncio
import csv
import os
import sys
from pathlib import Path

# Add the backend root to PYTHONPATH so we can import app.*
sys.path.insert(0, str(Path(__file__).parent.parent))

from sqlalchemy import text
from sqlalchemy.ext.asyncio import async_sessionmaker, create_async_engine

DATABASE_URL = os.environ.get(
    "DATABASE_URL",
    "postgresql+asyncpg://obd_user:obd_pass@localhost:5432/obd_db",
)

# Search in multiple locations: Docker mount first, then relative paths
_candidates = [
    Path("/docs/dtc_codes.csv"),                                        # Docker: ../docs mounted at /docs
    Path(__file__).parent.parent.parent / "docs" / "dtc_codes.csv",    # local: monorepo root/docs
    Path(__file__).parent.parent / "docs" / "dtc_codes.csv",           # local: backend/docs
]
CSV_PATH = next((p for p in _candidates if p.exists()), _candidates[0])

# Map DTC code first letter to category name
CATEGORY_MAP = {
    "P": "P",  # Powertrain
    "B": "B",  # Body
    "C": "C",  # Chassis
    "U": "U",  # Network
}

# Heuristic system detection from code range
def detect_system(code: str) -> str:
    if not code or len(code) < 4:
        return "unknown"
    prefix = code[:3].upper()
    try:
        num = int(code[1:4])
    except ValueError:
        return "unknown"

    if code[0] == "C":
        return "brakes"
    if code[0] == "B":
        return "body"
    if code[0] == "U":
        return "network"

    # Powertrain ranges
    if 100 <= num <= 199:
        return "fuel_air"
    if 200 <= num <= 299:
        return "fuel_air"
    if 300 <= num <= 399:
        return "ignition"
    if 400 <= num <= 499:
        return "emissions"
    if 500 <= num <= 599:
        return "speed_idle"
    if 600 <= num <= 699:
        return "computer"
    if 700 <= num <= 799:
        return "transmission"
    if 800 <= num <= 899:
        return "transmission"
    if 1000 <= num <= 1999:
        return "manufacturer"
    return "engine"


def detect_severity(code: str, system: str) -> str:
    """Conservative default severity based on system."""
    system_severity = {
        "brakes": "high",
        "ignition": "high",
        "fuel_air": "medium",
        "emissions": "medium",
        "transmission": "medium",
        "speed_idle": "medium",
        "computer": "medium",
        "fuel": "medium",
        "body": "low",
        "network": "low",
        "manufacturer": "medium",
        "engine": "medium",
    }
    return system_severity.get(system, "medium")


def detect_can_drive(severity: str) -> str:
    return {
        "critical": "no",
        "high": "limited",
        "medium": "yes_within_2_weeks",
        "low": "yes",
    }.get(severity, "yes_with_caution")


async def seed() -> None:
    if not CSV_PATH.exists():
        print(f"ERROR: CSV not found at {CSV_PATH}")
        sys.exit(1)

    engine = create_async_engine(DATABASE_URL, echo=False)
    Session = async_sessionmaker(engine, expire_on_commit=False)

    rows = []
    with open(CSV_PATH, newline="", encoding="utf-8") as f:
        reader = csv.reader(f)
        for row in reader:
            if len(row) < 2:
                continue
            code = row[0].strip().strip('"').upper()
            description = row[1].strip().strip('"')
            if not code:
                continue

            system = detect_system(code)
            severity = detect_severity(code, system)
            can_drive = detect_can_drive(severity)
            category = CATEGORY_MAP.get(code[0], "P") if code else "P"

            rows.append({
                "code": code,
                "standard_description": description,
                "category": category,
                "system": system,
                "severity_level": severity,
                "can_drive_flag": can_drive,
                "manufacturer_specific": False,
            })

    print(f"Seeding {len(rows)} DTC codes...")

    async with Session() as session:
        # Upsert using PostgreSQL ON CONFLICT DO NOTHING
        for batch_start in range(0, len(rows), 500):
            batch = rows[batch_start : batch_start + 500]
            await session.execute(
                text("""
                    INSERT INTO error_codes
                        (code, standard_description, category, system,
                         severity_level, can_drive_flag, manufacturer_specific)
                    VALUES
                        (:code, :standard_description, :category, :system,
                         :severity_level, :can_drive_flag, :manufacturer_specific)
                    ON CONFLICT (code) DO NOTHING
                """),
                batch,
            )
        await session.commit()

    await engine.dispose()
    print(f"Done. {len(rows)} codes processed.")


if __name__ == "__main__":
    asyncio.run(seed())
