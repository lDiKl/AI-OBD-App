"""initial schema

Revision ID: 0001
Revises:
Create Date: 2026-03-02

"""
from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op
from sqlalchemy.dialects.postgresql import JSONB

revision: str = "0001"
down_revision: Union[str, None] = None
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    # Enable pgvector (needed for future RAG support)
    op.execute("CREATE EXTENSION IF NOT EXISTS vector")

    # ── error_codes ──────────────────────────────────────────────────────────
    op.create_table(
        "error_codes",
        sa.Column("code", sa.String(), nullable=False),
        sa.Column("standard_description", sa.String(), nullable=False),
        sa.Column("category", sa.String(), nullable=True),
        sa.Column("system", sa.String(), nullable=True),
        sa.Column("severity_level", sa.String(), nullable=True),
        sa.Column("can_drive_flag", sa.String(), nullable=True),
        sa.Column("manufacturer_specific", sa.Boolean(), nullable=True, server_default=sa.text("false")),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.text("now()")),
        sa.Column("updated_at", sa.DateTime(timezone=True), server_default=sa.text("now()")),
        sa.PrimaryKeyConstraint("code"),
    )
    op.create_index("ix_error_codes_category", "error_codes", ["category"])
    op.create_index("ix_error_codes_system", "error_codes", ["system"])

    # ── repair_cost_estimates ────────────────────────────────────────────────
    op.create_table(
        "repair_cost_estimates",
        sa.Column("id", sa.String(), nullable=False),
        sa.Column("code", sa.String(), nullable=False),
        sa.Column("region", sa.String(10), nullable=False),
        sa.Column("currency", sa.String(3), nullable=False),
        sa.Column("cost_min", sa.Float(), nullable=False),
        sa.Column("cost_max", sa.Float(), nullable=False),
        sa.Column("labor_hours_min", sa.Float(), nullable=True, server_default=sa.text("0")),
        sa.Column("labor_hours_max", sa.Float(), nullable=True, server_default=sa.text("0")),
        sa.Column("source", sa.String(), nullable=True, server_default=sa.text("'estimated'")),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.text("now()")),
        sa.Column("updated_at", sa.DateTime(timezone=True), server_default=sa.text("now()")),
        sa.ForeignKeyConstraint(["code"], ["error_codes.code"]),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index("ix_repair_cost_estimates_code", "repair_cost_estimates", ["code"])
    op.create_index("ix_repair_cost_estimates_region", "repair_cost_estimates", ["region"])

    # ── users ────────────────────────────────────────────────────────────────
    op.create_table(
        "users",
        sa.Column("id", sa.String(), nullable=False),
        sa.Column("firebase_uid", sa.String(), nullable=False),
        sa.Column("email", sa.String(), nullable=True),
        sa.Column("subscription_status", sa.String(), nullable=True, server_default=sa.text("'free'")),
        sa.Column("region", sa.String(10), nullable=True, server_default=sa.text("'EU'")),
        sa.Column("language", sa.String(5), nullable=True, server_default=sa.text("'en'")),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.text("now()")),
        sa.Column("updated_at", sa.DateTime(timezone=True), server_default=sa.text("now()")),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index("ix_users_firebase_uid", "users", ["firebase_uid"], unique=True)
    op.create_index("ix_users_email", "users", ["email"])

    # ── vehicles ─────────────────────────────────────────────────────────────
    op.create_table(
        "vehicles",
        sa.Column("id", sa.String(), nullable=False),
        sa.Column("user_id", sa.String(), nullable=False),
        sa.Column("make", sa.String(), nullable=False),
        sa.Column("model", sa.String(), nullable=False),
        sa.Column("year", sa.Integer(), nullable=False),
        sa.Column("engine_type", sa.String(), nullable=True),
        sa.Column("vin", sa.String(17), nullable=True),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.text("now()")),
        sa.Column("updated_at", sa.DateTime(timezone=True), server_default=sa.text("now()")),
        sa.ForeignKeyConstraint(["user_id"], ["users.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index("ix_vehicles_user_id", "vehicles", ["user_id"])
    op.create_index("ix_vehicles_vin", "vehicles", ["vin"])

    # ── scan_sessions ────────────────────────────────────────────────────────
    op.create_table(
        "scan_sessions",
        sa.Column("id", sa.String(), nullable=False),
        sa.Column("vehicle_id", sa.String(), nullable=False),
        sa.Column("mileage", sa.Integer(), nullable=True, server_default=sa.text("0")),
        sa.Column("risk_score", sa.String(), nullable=True, server_default=sa.text("'low'")),
        sa.Column("raw_obd_data", JSONB(), nullable=True, server_default=sa.text("'{}'")),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.text("now()")),
        sa.Column("updated_at", sa.DateTime(timezone=True), server_default=sa.text("now()")),
        sa.ForeignKeyConstraint(["vehicle_id"], ["vehicles.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index("ix_scan_sessions_vehicle_id", "scan_sessions", ["vehicle_id"])

    # ── error_occurrences ────────────────────────────────────────────────────
    op.create_table(
        "error_occurrences",
        sa.Column("id", sa.String(), nullable=False),
        sa.Column("session_id", sa.String(), nullable=False),
        sa.Column("code", sa.String(), nullable=False),
        sa.Column("freeze_frame", JSONB(), nullable=True, server_default=sa.text("'{}'")),
        sa.Column("occurrence_count", sa.Integer(), nullable=True, server_default=sa.text("1")),
        sa.Column("ai_result", JSONB(), nullable=True, server_default=sa.text("'{}'")),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.text("now()")),
        sa.Column("updated_at", sa.DateTime(timezone=True), server_default=sa.text("now()")),
        sa.ForeignKeyConstraint(["code"], ["error_codes.code"]),
        sa.ForeignKeyConstraint(["session_id"], ["scan_sessions.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index("ix_error_occurrences_session_id", "error_occurrences", ["session_id"])
    op.create_index("ix_error_occurrences_code", "error_occurrences", ["code"])


def downgrade() -> None:
    op.drop_table("error_occurrences")
    op.drop_table("scan_sessions")
    op.drop_table("vehicles")
    op.drop_table("users")
    op.drop_table("repair_cost_estimates")
    op.drop_table("error_codes")
    op.execute("DROP EXTENSION IF EXISTS vector")
