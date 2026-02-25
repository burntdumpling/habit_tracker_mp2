"""add color argb to habits

Revision ID: 0004_add_color_argb_to_habits
Revises: 0003_create_habit_logs_table
Create Date: 2026-02-25 00:00:00.000000

"""

from typing import Sequence

from alembic import op
import sqlalchemy as sa

# revision identifiers, used by Alembic.
revision: str = "0004_add_color_argb_to_habits"
down_revision: str | Sequence[str] | None = "0003_create_habit_logs_table"
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None


def upgrade() -> None:
    op.add_column(
        "habits",
        sa.Column("color_argb", sa.Integer(), nullable=False, server_default=sa.text("0")),
    )
    op.alter_column("habits", "color_argb", server_default=None)


def downgrade() -> None:
    op.drop_column("habits", "color_argb")
