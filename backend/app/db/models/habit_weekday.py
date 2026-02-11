from __future__ import annotations

from typing import TYPE_CHECKING

from sqlalchemy import BigInteger, Enum, ForeignKey
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.db.base import Base
from app.schemas.enums import DayOfWeek

if TYPE_CHECKING:
    from app.db.models.habit import Habit


class HabitWeekday(Base):
    __tablename__ = "habit_weekdays"

    habit_id: Mapped[int] = mapped_column(
        BigInteger,
        ForeignKey("habits.id", ondelete="CASCADE"),
        primary_key=True,
    )
    weekday: Mapped[DayOfWeek] = mapped_column(
        Enum(DayOfWeek, native_enum=False),
        primary_key=True,
    )

    habit: Mapped[Habit] = relationship(
        back_populates="weekdays",
        lazy="selectin",
    )
