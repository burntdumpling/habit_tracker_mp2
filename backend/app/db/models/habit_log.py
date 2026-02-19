from datetime import date, datetime

from sqlalchemy import (
    BigInteger,
    CheckConstraint,
    Date,
    DateTime,
    Enum,
    ForeignKey,
    Index,
    Integer,
    UniqueConstraint,
    func,
)
from sqlalchemy.orm import Mapped, mapped_column

from app.db.base import Base
from app.schemas.enums import DayPart


class HabitLog(Base):
    __tablename__ = "habit_logs"
    __table_args__ = (
        CheckConstraint("amount >= 0", name="ck_habit_logs_amount_non_negative"),
        UniqueConstraint(
            "habit_id",
            "log_date",
            "day_part",
            name="uq_habit_logs_habit_id_log_date_day_part",
        ),
        Index("ix_habit_logs_habit_id_log_date", "habit_id", "log_date"),
    )

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    habit_id: Mapped[int] = mapped_column(
        BigInteger,
        ForeignKey("habits.id", ondelete="CASCADE"),
        nullable=False,
        index=True,
    )
    log_date: Mapped[date] = mapped_column(Date, nullable=False)
    day_part: Mapped[DayPart] = mapped_column(Enum(DayPart, native_enum=False), nullable=False)
    amount: Mapped[int] = mapped_column(Integer, nullable=False)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        server_default=func.now(),
        nullable=False,
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        server_default=func.now(),
        nullable=False,
    )
