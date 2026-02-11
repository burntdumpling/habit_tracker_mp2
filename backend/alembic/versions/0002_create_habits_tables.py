"""create habits tables

Revision ID: 0002_create_habits_tables
Revises: 0001_create_users_table
Create Date: 2026-02-11 00:00:00.000000

"""

from typing import Sequence

from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql

# revision identifiers, used by Alembic.
revision: str = "0002_create_habits_tables"
down_revision: str | Sequence[str] | None = "0001_create_users_table"
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None


def upgrade() -> None:
    op.create_table(
        "habits",
        sa.Column("id", sa.BigInteger(), autoincrement=True, nullable=False),
        sa.Column("user_id", postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column("status", sa.String(), nullable=False),
        sa.Column("name", sa.String(length=255), nullable=False),
        sa.Column("icon_id", sa.Integer(), nullable=False),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.text("now()"), nullable=False),
        sa.Column("start_date", sa.Date(), nullable=False),
        sa.Column("end_date", sa.Date(), nullable=True),
        sa.Column("frequency_type", sa.String(), nullable=False),
        sa.Column("every_n_days", sa.Integer(), nullable=True),
        sa.ForeignKeyConstraint(["user_id"], ["users.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index("ix_habits_user_id", "habits", ["user_id"], unique=False)
    op.create_index("ix_habits_user_id_status", "habits", ["user_id", "status"], unique=False)

    op.create_table(
        "habit_weekdays",
        sa.Column("habit_id", sa.BigInteger(), nullable=False),
        sa.Column("weekday", sa.String(), nullable=False),
        sa.ForeignKeyConstraint(["habit_id"], ["habits.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("habit_id", "weekday"),
    )

    op.create_table(
        "habit_targets",
        sa.Column("habit_id", sa.BigInteger(), nullable=False),
        sa.Column("day_part", sa.String(), nullable=False),
        sa.Column("target_count", sa.Integer(), nullable=False),
        sa.CheckConstraint("target_count > 0", name="ck_habit_targets_target_count_positive"),
        sa.ForeignKeyConstraint(["habit_id"], ["habits.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("habit_id", "day_part"),
    )


def downgrade() -> None:
    op.drop_table("habit_targets")
    op.drop_table("habit_weekdays")
    op.drop_index("ix_habits_user_id_status", table_name="habits")
    op.drop_index("ix_habits_user_id", table_name="habits")
    op.drop_table("habits")
