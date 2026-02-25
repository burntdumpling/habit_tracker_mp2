from __future__ import annotations

import uuid
from datetime import date, datetime
from typing import TYPE_CHECKING

from sqlalchemy import BigInteger, Date, DateTime, Enum, ForeignKey, Integer, String, func
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.db.base import Base
from app.schemas.enums import FrequencyType, HabitStatus

if TYPE_CHECKING:
    from app.db.models.habit_target import HabitTarget
    from app.db.models.habit_weekday import HabitWeekday


class Habit(Base):
    __tablename__ = "habits"

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    user_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("users.id", ondelete="CASCADE"),
        nullable=False,
        index=True,
    )
    status: Mapped[HabitStatus] = mapped_column(
        Enum(HabitStatus, native_enum=False),
        nullable=False,
        default=HabitStatus.ACTIVE,
    )
    name: Mapped[str] = mapped_column(String(255), nullable=False)
    icon_id: Mapped[int] = mapped_column(Integer, nullable=False)
    color_argb: Mapped[int] = mapped_column(BigInteger, nullable=False, default=0)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        server_default=func.now(),
        nullable=False,
    )
    start_date: Mapped[date] = mapped_column(Date, nullable=False)
    end_date: Mapped[date | None] = mapped_column(Date, nullable=True)
    frequency_type: Mapped[FrequencyType] = mapped_column(
        Enum(FrequencyType, native_enum=False),
        nullable=False,
    )
    every_n_days: Mapped[int | None] = mapped_column(Integer, nullable=True)

    weekdays: Mapped[list[HabitWeekday]] = relationship(
        back_populates="habit",
        cascade="all, delete-orphan",
        lazy="selectin",
    )
    targets: Mapped[list[HabitTarget]] = relationship(
        back_populates="habit",
        cascade="all, delete-orphan",
        lazy="selectin",
    )

    @property
    def frequency(self) -> dict[str, object]:
        return {
            "type": self.frequency_type,
            "weekDays": [item.weekday for item in self.weekdays] if self.weekdays else None,
            "everyNDays": self.every_n_days,
        }
