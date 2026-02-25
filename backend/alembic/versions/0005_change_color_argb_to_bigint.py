"""change color argb to bigint

Revision ID: 0005_change_color_argb_to_bigint
Revises: 0004_add_color_argb_to_habits
Create Date: 2026-02-25 00:00:00.000000

"""

from typing import Sequence

from alembic import op
import sqlalchemy as sa

# revision identifiers, used by Alembic.
revision: str = "0005_change_color_argb_to_bigint"
down_revision: str | Sequence[str] | None = "0004_add_color_argb_to_habits"
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None


def upgrade() -> None:
    op.alter_column(
        "habits",
        "color_argb",
        existing_type=sa.Integer(),
        type_=sa.BigInteger(),
        existing_nullable=False,
    )


def downgrade() -> None:
    op.alter_column(
        "habits",
        "color_argb",
        existing_type=sa.BigInteger(),
        type_=sa.Integer(),
        existing_nullable=False,
    )
