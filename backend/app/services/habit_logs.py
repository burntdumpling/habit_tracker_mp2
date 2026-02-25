from __future__ import annotations

from datetime import date
from uuid import UUID

from fastapi import HTTPException, status
from sqlalchemy import case, func, select
from sqlalchemy.dialects.postgresql import insert
from sqlalchemy.exc import IntegrityError
from sqlalchemy.ext.asyncio import AsyncSession

from app.db.models import Habit, HabitLog, HabitTarget
from app.schemas.enums import DayPart
from app.schemas.habit_log import HabitLogOut, HabitLogUpsertIn


DAY_PART_ORDER = case(
    (HabitLog.day_part == DayPart.FULL_DAY, 0),
    (HabitLog.day_part == DayPart.MORNING, 1),
    (HabitLog.day_part == DayPart.DAY, 2),
    (HabitLog.day_part == DayPart.EVENING, 3),
    else_=4,
)


async def upsert_habit_log(
    db: AsyncSession,
    user_id: UUID,
    habit_id: int,
    payload: HabitLogUpsertIn,
) -> HabitLogOut:
    await _get_habit_or_404(db, user_id, habit_id)
    await _validate_habit_day_part_allowed(db, habit_id, payload.dayPart)

    stmt = (
        insert(HabitLog)
        .values(
            habit_id=habit_id,
            log_date=payload.date,
            day_part=payload.dayPart,
            amount=payload.amount,
        )
        .on_conflict_do_update(
            index_elements=[HabitLog.habit_id, HabitLog.log_date, HabitLog.day_part],
            set_={
                "amount": payload.amount,
                "updated_at": func.now(),
            },
        )
        .returning(HabitLog)
    )

    try:
        result = await db.execute(stmt)
        habit_log = result.scalar_one()
        await db.commit()
    except IntegrityError as exc:
        await db.rollback()
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="Integrity error while saving habit log",
        ) from exc

    return _map_habit_log_out(habit_log)


async def delete_habit_log(
    db: AsyncSession,
    user_id: UUID,
    habit_id: int,
    log_date: date,
    day_part: DayPart,
) -> None:
    await _get_habit_or_404(db, user_id, habit_id)
    await _validate_habit_day_part_allowed(db, habit_id, day_part)

    result = await db.execute(
        select(HabitLog).where(
            HabitLog.habit_id == habit_id,
            HabitLog.log_date == log_date,
            HabitLog.day_part == day_part,
        )
    )
    habit_log = result.scalar_one_or_none()
    if habit_log is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Habit log not found")

    await db.delete(habit_log)
    await db.commit()


async def list_habit_logs(
    db: AsyncSession,
    user_id: UUID,
    habit_id: int,
    date_from: date,
    date_to: date,
) -> list[HabitLogOut]:
    _validate_date_range(date_from, date_to)
    await _get_habit_or_404(db, user_id, habit_id)

    result = await db.execute(
        select(HabitLog)
        .where(
            HabitLog.habit_id == habit_id,
            HabitLog.log_date >= date_from,
            HabitLog.log_date <= date_to,
        )
        .order_by(HabitLog.log_date.asc(), DAY_PART_ORDER)
    )
    logs = result.scalars().all()
    return [_map_habit_log_out(log) for log in logs]


async def list_user_habit_logs(
    db: AsyncSession,
    user_id: UUID,
    date_from: date,
    date_to: date,
) -> list[HabitLogOut]:
    _validate_date_range(date_from, date_to)

    result = await db.execute(
        select(HabitLog)
        .join(Habit, Habit.id == HabitLog.habit_id)
        .where(
            Habit.user_id == user_id,
            HabitLog.log_date >= date_from,
            HabitLog.log_date <= date_to,
        )
        .order_by(HabitLog.log_date.asc(), DAY_PART_ORDER)
    )
    logs = result.scalars().all()
    return [_map_habit_log_out(log) for log in logs]


async def _get_habit_or_404(db: AsyncSession, user_id: UUID, habit_id: int) -> Habit:
    result = await db.execute(select(Habit).where(Habit.id == habit_id, Habit.user_id == user_id))
    habit = result.scalar_one_or_none()
    if habit is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Habit not found")
    return habit


async def _validate_habit_day_part_allowed(db: AsyncSession, habit_id: int, day_part: DayPart) -> None:
    result = await db.execute(select(HabitTarget.day_part).where(HabitTarget.habit_id == habit_id))
    allowed_day_parts = set(result.scalars().all())
    if day_part not in allowed_day_parts:
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_CONTENT,
            detail="dayPart not allowed for this habit",
        )


def _validate_date_range(date_from: date, date_to: date) -> None:
    if date_from > date_to:
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_CONTENT,
            detail="dateFrom must be less than or equal to dateTo",
        )


def _map_habit_log_out(habit_log: HabitLog) -> HabitLogOut:
    return HabitLogOut.model_validate(
        {
            "habit_id": habit_log.habit_id,
            "log_date": habit_log.log_date,
            "day_part": habit_log.day_part,
            "amount": habit_log.amount,
            "created_at": habit_log.created_at,
            "updated_at": habit_log.updated_at,
        }
    )
