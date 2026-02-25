from __future__ import annotations

from collections import defaultdict
from datetime import date, timedelta
from uuid import UUID

from fastapi import HTTPException, status
from sqlalchemy import func, select
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.orm import selectinload

from app.db.models import Habit, HabitLog
from app.schemas.enums import DayOfWeek, DayPart, FrequencyType, HabitStatus
from app.schemas.habit_calendar import (
    CalendarDayOut,
    CalendarDayPartActualOut,
    CalendarDayPartTargetOut,
    CalendarHabitOut,
    CalendarHabitSummaryOut,
    CalendarRangeOut,
)
from app.services.habit_logs import _validate_date_range

DAY_OF_WEEK_ORDER = {
    DayOfWeek.MONDAY: 0,
    DayOfWeek.TUESDAY: 1,
    DayOfWeek.WEDNESDAY: 2,
    DayOfWeek.THURSDAY: 3,
    DayOfWeek.FRIDAY: 4,
    DayOfWeek.SATURDAY: 5,
    DayOfWeek.SUNDAY: 6,
}

DAY_PART_ORDER = {
    DayPart.FULL_DAY: 0,
    DayPart.MORNING: 1,
    DayPart.DAY: 2,
    DayPart.EVENING: 3,
}


async def get_calendar_day(
    db: AsyncSession,
    user_id: UUID,
    date_: date,
    status: HabitStatus | None = None,
) -> CalendarDayOut:
    range_result = await get_calendar_range(
        db=db,
        user_id=user_id,
        date_from=date_,
        date_to=date_,
        status=status,
        max_days=1,
    )
    return range_result.days[0]


async def get_calendar_range(
    db: AsyncSession,
    user_id: UUID,
    date_from: date,
    date_to: date,
    status: HabitStatus | None = None,
    max_days: int = 31,
) -> CalendarRangeOut:
    _validate_date_range(date_from, date_to)
    _validate_max_days(date_from, date_to, max_days)

    habits = await _get_user_habits(db, user_id, status)
    habit_ids = [habit.id for habit in habits]
    actual_amounts = await _get_actual_amounts_by_habit_day_part(db, habit_ids, date_from, date_to)

    days = [
        _build_calendar_day(
            current_date=current_date,
            habits=habits,
            actual_amounts=actual_amounts,
        )
        for current_date in _iter_dates(date_from, date_to)
    ]

    return CalendarRangeOut.model_validate(
        {
            "date_from": date_from,
            "date_to": date_to,
            "days": days,
        }
    )


async def _get_user_habits(
    db: AsyncSession,
    user_id: UUID,
    status: HabitStatus | None,
) -> list[Habit]:
    query = (
        select(Habit)
        .options(selectinload(Habit.weekdays), selectinload(Habit.targets))
        .where(Habit.user_id == user_id)
        .order_by(Habit.id.asc())
    )
    if status is not None:
        query = query.where(Habit.status == status)

    result = await db.execute(query)
    return list(result.scalars().all())


async def _get_actual_amounts_by_habit_day_part(
    db: AsyncSession,
    habit_ids: list[int],
    date_from: date,
    date_to: date,
) -> dict[tuple[int, date], dict[DayPart, int]]:
    if not habit_ids:
        return {}

    result = await db.execute(
        select(
            HabitLog.habit_id,
            HabitLog.log_date,
            HabitLog.day_part,
            func.coalesce(func.sum(HabitLog.amount), 0).label("amount"),
        )
        .where(
            HabitLog.habit_id.in_(habit_ids),
            HabitLog.log_date >= date_from,
            HabitLog.log_date <= date_to,
        )
        .group_by(HabitLog.habit_id, HabitLog.log_date, HabitLog.day_part)
    )

    by_habit_day: dict[tuple[int, date], dict[DayPart, int]] = defaultdict(dict)
    for habit_id, log_date, day_part, amount in result.all():
        by_habit_day[(habit_id, log_date)][day_part] = int(amount)
    return dict(by_habit_day)


def _build_calendar_day(
    current_date: date,
    habits: list[Habit],
    actual_amounts: dict[tuple[int, date], dict[DayPart, int]],
) -> CalendarDayOut:
    scheduled_habits = [
        _build_calendar_habit(
            habit=habit,
            actual_by_day_part=actual_amounts.get((habit.id, current_date), {}),
        )
        for habit in habits
        if _is_habit_scheduled_for_date(habit, current_date)
    ]

    return CalendarDayOut(date=current_date, habits=scheduled_habits)


def _build_calendar_habit(
    habit: Habit,
    actual_by_day_part: dict[DayPart, int],
) -> CalendarHabitOut:
    targets = sorted(habit.targets, key=lambda item: DAY_PART_ORDER[item.day_part])
    target_items = [
        CalendarDayPartTargetOut.model_validate(
            {
                "day_part": target.day_part,
                "target_count": target.target_count,
            }
        )
        for target in targets
    ]

    actual_items = [
        CalendarDayPartActualOut.model_validate(
            {
                "day_part": target.dayPart,
                "amount": actual_by_day_part.get(target.dayPart, 0),
            }
        )
        for target in target_items
    ]

    target_total = sum(target.targetCount for target in target_items)
    actual_total = sum(actual.amount for actual in actual_items)
    summary = CalendarHabitSummaryOut(
        targetTotal=target_total,
        actualTotal=actual_total,
        isCompleted=all(actual.amount >= target.targetCount for target, actual in zip(target_items, actual_items)),
    )

    return CalendarHabitOut.model_validate(
        {
            "habit_id": habit.id,
            "name": habit.name,
            "icon_id": habit.icon_id,
            "status": habit.status,
            "targets": target_items,
            "actual": actual_items,
            "summary": summary,
        }
    )


def _is_habit_scheduled_for_date(habit: Habit, current_date: date) -> bool:
    """
    Determine if a habit should appear on a given date based on date bounds
    and frequency settings (DAILY, WEEKLY, EVERY_N_DAYS).
    """
    if current_date < habit.start_date:
        return False
    if habit.end_date is not None and current_date > habit.end_date:
        return False

    if habit.frequency_type == FrequencyType.DAILY:
        return True

    if habit.frequency_type == FrequencyType.WEEKLY:
        if not habit.weekdays:
            return False
        date_weekday = current_date.weekday()
        for habit_weekday in habit.weekdays:
            weekday_index = _weekday_to_index(habit_weekday.weekday)
            if weekday_index is not None and weekday_index == date_weekday:
                return True
        return False

    if habit.frequency_type == FrequencyType.EVERY_N_DAYS:
        if habit.every_n_days is None or habit.every_n_days <= 0:
            return False
        days_since_start = (current_date - habit.start_date).days
        return days_since_start % habit.every_n_days == 0

    return False


def _weekday_to_index(weekday: object) -> int | None:
    if isinstance(weekday, DayOfWeek):
        return DAY_OF_WEEK_ORDER[weekday]

    if isinstance(weekday, int):
        return weekday if 0 <= weekday <= 6 else None

    if isinstance(weekday, str):
        if weekday.isdigit():
            parsed = int(weekday)
            return parsed if 0 <= parsed <= 6 else None
        try:
            weekday_enum = DayOfWeek(weekday)
            return DAY_OF_WEEK_ORDER[weekday_enum]
        except ValueError:
            return None

    return None


def _validate_max_days(date_from: date, date_to: date, max_days: int) -> None:
    if max_days < 1:
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_CONTENT,
            detail="maxDays must be greater than or equal to 1",
        )

    requested_days = (date_to - date_from).days + 1
    if requested_days > max_days:
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_CONTENT,
            detail=f"Requested range exceeds maxDays={max_days}",
        )


def _iter_dates(date_from: date, date_to: date):
    current_date = date_from
    while current_date <= date_to:
        yield current_date
        current_date += timedelta(days=1)
