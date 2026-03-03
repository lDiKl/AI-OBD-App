"""B2B tables: shops, shop_users, diagnostic_cases

Revision ID: 0002
Revises: 0001
Create Date: 2026-03-03

"""
from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op
from sqlalchemy.dialects.postgresql import JSONB

revision: str = "0002"
down_revision: Union[str, None] = "0001"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    # ── shops ─────────────────────────────────────────────────────────────────
    op.create_table(
        "shops",
        sa.Column("id", sa.String(), nullable=False),
        sa.Column("name", sa.String(), nullable=False),
        sa.Column("address", sa.String(), nullable=True, server_default=sa.text("''")),
        sa.Column("lat", sa.Float(), nullable=True, server_default=sa.text("0")),
        sa.Column("lng", sa.Float(), nullable=True, server_default=sa.text("0")),
        sa.Column("phone", sa.String(), nullable=True, server_default=sa.text("''")),
        sa.Column("email", sa.String(), nullable=True, server_default=sa.text("''")),
        sa.Column("subscription_tier", sa.String(), nullable=True, server_default=sa.text("'basic'")),
        sa.Column("rating", sa.Float(), nullable=True, server_default=sa.text("0")),
        sa.Column("verified", sa.Boolean(), nullable=True, server_default=sa.text("false")),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.text("now()")),
        sa.Column("updated_at", sa.DateTime(timezone=True), server_default=sa.text("now()")),
        sa.PrimaryKeyConstraint("id"),
    )

    # ── shop_users ────────────────────────────────────────────────────────────
    op.create_table(
        "shop_users",
        sa.Column("id", sa.String(), nullable=False),
        sa.Column("shop_id", sa.String(), nullable=False),
        sa.Column("firebase_uid", sa.String(), nullable=False),
        sa.Column("email", sa.String(), nullable=True, server_default=sa.text("''")),
        sa.Column("role", sa.String(), nullable=True, server_default=sa.text("'mechanic'")),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.text("now()")),
        sa.Column("updated_at", sa.DateTime(timezone=True), server_default=sa.text("now()")),
        sa.ForeignKeyConstraint(["shop_id"], ["shops.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index("ix_shop_users_shop_id", "shop_users", ["shop_id"])
    op.create_index("ix_shop_users_firebase_uid", "shop_users", ["firebase_uid"], unique=True)

    # ── diagnostic_cases ──────────────────────────────────────────────────────
    op.create_table(
        "diagnostic_cases",
        sa.Column("id", sa.String(), nullable=False),
        sa.Column("shop_id", sa.String(), nullable=False),
        sa.Column("vehicle_info", JSONB(), nullable=True, server_default=sa.text("'{}'")),
        sa.Column("input_codes", JSONB(), nullable=True, server_default=sa.text("'[]'")),
        sa.Column("symptoms_text", sa.String(), nullable=True, server_default=sa.text("''")),
        sa.Column("freeze_frame", JSONB(), nullable=True, server_default=sa.text("'{}'")),
        sa.Column("ai_result", JSONB(), nullable=True, server_default=sa.text("'{}'")),
        sa.Column("client_report_text", sa.String(), nullable=True, server_default=sa.text("''")),
        sa.Column("estimate", JSONB(), nullable=True, server_default=sa.text("'{}'")),
        sa.Column("status", sa.String(), nullable=True, server_default=sa.text("'open'")),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.text("now()")),
        sa.Column("updated_at", sa.DateTime(timezone=True), server_default=sa.text("now()")),
        sa.ForeignKeyConstraint(["shop_id"], ["shops.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index("ix_diagnostic_cases_shop_id", "diagnostic_cases", ["shop_id"])
    op.create_index("ix_diagnostic_cases_status", "diagnostic_cases", ["status"])


def downgrade() -> None:
    op.drop_table("diagnostic_cases")
    op.drop_table("shop_users")
    op.drop_table("shops")
