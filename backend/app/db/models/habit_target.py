from __future__ import annotations

from typing import TYPE_CHECKING

from sqlalchemy import BigInteger, CheckConstraint, Enum, ForeignKey, Integer
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.db.base import Base
from app.schemas.enums import DayPart

if TYPE_CHECKING:
    from app.db.models.habit import Habit


class HabitTarget(Base):
    __tablename__ = "habit_targets"
    __table_args__ = (CheckConstraint("target_count > 0", name="ck_habit_targets_target_count_positive"),)

    habit_id: Mapped[int] = mapped_column(
        BigInteger,
        ForeignKey("habits.id", ondelete="CASCADE"),
        primary_key=True,
    )
    day_part: Mapped[DayPart] = mapped_column(
        Enum(DayPart, native_enum=False),
        primary_key=True,
    )
    target_count: Mapped[int] = mapped_column(Integer, nullable=False)

    habit: Mapped[Habit] = relationship(
        back_populates="targets",
        lazy="selectin",
    )
