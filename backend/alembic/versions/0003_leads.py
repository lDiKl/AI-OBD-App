"""Lead system: service_leads + shop_quotes

Revision ID: 0003
Revises: 0002
Create Date: 2026-03-04

"""
from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op
from sqlalchemy.dialects.postgresql import JSONB

revision: str = "0003"
down_revision: Union[str, None] = "0002"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    # ── service_leads ─────────────────────────────────────────────────────────
    op.create_table(
        "service_leads",
        sa.Column("id", sa.String(), nullable=False),
        sa.Column("user_id", sa.String(), nullable=False),
        sa.Column("shop_id", sa.String(), nullable=False),
        sa.Column("scan_session_id", sa.String(), nullable=True),
        sa.Column("dtc_codes", JSONB(), nullable=True, server_default=sa.text("'[]'")),
        sa.Column("vehicle_info", JSONB(), nullable=True, server_default=sa.text("'{}'")),
        sa.Column("freeze_frame", JSONB(), nullable=True),
        sa.Column("status", sa.String(), nullable=True, server_default=sa.text("'pending'")),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.text("now()")),
        sa.Column("updated_at", sa.DateTime(timezone=True), server_default=sa.text("now()")),
        sa.ForeignKeyConstraint(["user_id"], ["users.id"], ondelete="CASCADE"),
        sa.ForeignKeyConstraint(["shop_id"], ["shops.id"], ondelete="CASCADE"),
        sa.ForeignKeyConstraint(["scan_session_id"], ["scan_sessions.id"], ondelete="SET NULL"),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index("ix_service_leads_user_id", "service_leads", ["user_id"])
    op.create_index("ix_service_leads_shop_id", "service_leads", ["shop_id"])
    op.create_index("ix_service_leads_status", "service_leads", ["status"])

    # ── shop_quotes ───────────────────────────────────────────────────────────
    op.create_table(
        "shop_quotes",
        sa.Column("id", sa.String(), nullable=False),
        sa.Column("lead_id", sa.String(), nullable=False),
        sa.Column("cost_min", sa.Float(), nullable=False),
        sa.Column("cost_max", sa.Float(), nullable=False),
        sa.Column("estimated_days", sa.Integer(), nullable=False),
        sa.Column("notes", sa.Text(), nullable=True),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.text("now()")),
        sa.ForeignKeyConstraint(["lead_id"], ["service_leads.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("lead_id"),
    )
    op.create_index("ix_shop_quotes_lead_id", "shop_quotes", ["lead_id"])


def downgrade() -> None:
    op.drop_table("shop_quotes")
    op.drop_table("service_leads")
