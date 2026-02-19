from __future__ import annotations

from collections import defaultdict
from datetime import date, timedelta
from uuid import UUID

from sqlalchemy import func, select
from sqlalchemy.ext.asyncio import AsyncSession

from app.db.models import Habit, HabitLog, HabitTarget
from app.schemas.enums import DayPart
from app.schemas.habit_completion import HabitDayCompletionOut
from app.services.habit_logs import _get_habit_or_404, _validate_date_range


async def list_habit_completion(
    db: AsyncSession,
    user_id: UUID,
    habit_id: int,
    date_from: date,
    date_to: date,
) -> list[HabitDayCompletionOut]:
    _validate_date_range(date_from, date_to)
    await _get_habit_or_404(db, user_id, habit_id)

    target_count = (await _get_target_counts_by_habit_ids(db, [habit_id])).get(habit_id, 0)

    actual_result = await db.execute(
        select(
            HabitLog.log_date,
            func.coalesce(func.sum(HabitLog.amount), 0).label("actual_amount"),
        )
        .where(
            HabitLog.habit_id == habit_id,
            HabitLog.log_date >= date_from,
            HabitLog.log_date <= date_to,
        )
        .group_by(HabitLog.log_date)
    )
    actual_by_date = {log_date: int(actual_amount) for log_date, actual_amount in actual_result.all()}

    result: list[HabitDayCompletionOut] = []
    for current_date in _iter_dates(date_from, date_to):
        actual_amount = actual_by_date.get(current_date, 0)
        result.append(
            HabitDayCompletionOut(
                habitId=habit_id,
                date=current_date,
                actualAmount=actual_amount,
                targetCount=target_count,
                isCompleted=actual_amount >= target_count,
            )
        )

    return result


async def list_user_habits_completion(
    db: AsyncSession,
    user_id: UUID,
    date_from: date,
    date_to: date,
) -> list[HabitDayCompletionOut]:
    _validate_date_range(date_from, date_to)

    habits_result = await db.execute(select(Habit.id).where(Habit.user_id == user_id).order_by(Habit.id.asc()))
    habit_ids = list(habits_result.scalars().all())
    if not habit_ids:
        return []

    target_counts = await _get_target_counts_by_habit_ids(db, habit_ids)

    actual_result = await db.execute(
        select(
            HabitLog.habit_id,
            HabitLog.log_date,
            func.coalesce(func.sum(HabitLog.amount), 0).label("actual_amount"),
        )
        .where(
            HabitLog.habit_id.in_(habit_ids),
            HabitLog.log_date >= date_from,
            HabitLog.log_date <= date_to,
        )
        .group_by(HabitLog.habit_id, HabitLog.log_date)
    )
    actual_by_habit_day = {
        (habit_id, log_date): int(actual_amount)
        for habit_id, log_date, actual_amount in actual_result.all()
    }

    dates = list(_iter_dates(date_from, date_to))
    result: list[HabitDayCompletionOut] = []
    for current_habit_id in habit_ids:
        target_count = target_counts.get(current_habit_id, 0)
        for current_date in dates:
            actual_amount = actual_by_habit_day.get((current_habit_id, current_date), 0)
            result.append(
                HabitDayCompletionOut(
                    habitId=current_habit_id,
                    date=current_date,
                    actualAmount=actual_amount,
                    targetCount=target_count,
                    isCompleted=actual_amount >= target_count,
                )
            )

    return result


async def _get_target_counts_by_habit_ids(db: AsyncSession, habit_ids: list[int]) -> dict[int, int]:
    if not habit_ids:
        return {}

    result = await db.execute(
        select(HabitTarget.habit_id, HabitTarget.day_part, HabitTarget.target_count).where(
            HabitTarget.habit_id.in_(habit_ids)
        )
    )
    raw_targets: dict[int, list[tuple[DayPart, int]]] = defaultdict(list)
    for habit_id, day_part, target_count in result.all():
        raw_targets[habit_id].append((day_part, target_count))

    target_counts: dict[int, int] = {}
    for habit_id in habit_ids:
        targets = raw_targets.get(habit_id, [])
        if len(targets) == 1 and targets[0][0] == DayPart.FULL_DAY:
            target_counts[habit_id] = targets[0][1]
        else:
            target_counts[habit_id] = sum(target_count for _, target_count in targets)

    return target_counts


def _iter_dates(date_from: date, date_to: date):
    current_date = date_from
    while current_date <= date_to:
        yield current_date
        current_date += timedelta(days=1)
