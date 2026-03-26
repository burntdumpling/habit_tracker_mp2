from __future__ import annotations

import calendar
from collections.abc import Iterator, Sequence
from datetime import date, timedelta
from enum import Enum
from typing import Protocol

from app.schemas.enums import DayOfWeek, FrequencyType

DAY_OF_WEEK_ORDER = {
    DayOfWeek.MONDAY: 0,
    DayOfWeek.TUESDAY: 1,
    DayOfWeek.WEDNESDAY: 2,
    DayOfWeek.THURSDAY: 3,
    DayOfWeek.FRIDAY: 4,
    DayOfWeek.SATURDAY: 5,
    DayOfWeek.SUNDAY: 6,
}


class SchedulePeriod(str, Enum):
    WEEK = "WEEK"
    MONTH = "MONTH"
    YEAR = "YEAR"


class HabitWeekdayLike(Protocol):
    weekday: DayOfWeek | int | str


class HabitScheduleLike(Protocol):
    start_date: date
    end_date: date | None
    frequency_type: FrequencyType
    every_n_days: int | None
    weekdays: Sequence[HabitWeekdayLike] | None


def is_habit_active_on_date(habit: HabitScheduleLike, current_date: date) -> bool:
    if current_date < habit.start_date:
        return False
    if habit.end_date is not None and current_date > habit.end_date:
        return False
    return True


def is_habit_scheduled_for_date(habit: HabitScheduleLike, current_date: date) -> bool:
    if not is_habit_active_on_date(habit, current_date):
        return False

    if habit.frequency_type == FrequencyType.DAILY:
        return True

    if habit.frequency_type == FrequencyType.WEEKLY:
        if not habit.weekdays:
            return False
        date_weekday = current_date.weekday()
        return any(
            _weekday_to_index(habit_weekday.weekday) == date_weekday
            for habit_weekday in habit.weekdays
        )

    if habit.frequency_type == FrequencyType.EVERY_N_DAYS:
        if habit.every_n_days is None or habit.every_n_days <= 0:
            return False
        days_since_start = (current_date - habit.start_date).days
        return days_since_start % habit.every_n_days == 0

    return False


def resolve_period_range(
    anchor_date: date,
    period: SchedulePeriod | str,
) -> tuple[date, date]:
    period_enum = period if isinstance(period, SchedulePeriod) else SchedulePeriod(period)

    if period_enum == SchedulePeriod.WEEK:
        start_date = anchor_date - timedelta(days=anchor_date.weekday())
        return start_date, start_date + timedelta(days=6)

    if period_enum == SchedulePeriod.MONTH:
        last_day = calendar.monthrange(anchor_date.year, anchor_date.month)[1]
        return anchor_date.replace(day=1), anchor_date.replace(day=last_day)

    return date(anchor_date.year, 1, 1), date(anchor_date.year, 12, 31)


def iter_dates(date_from: date, date_to: date) -> Iterator[date]:
    current_date = date_from
    while current_date <= date_to:
        yield current_date
        current_date += timedelta(days=1)


def iter_scheduled_dates(
    habit: HabitScheduleLike,
    date_from: date,
    date_to: date,
) -> Iterator[date]:
    if date_to < date_from:
        return

    start_date = max(date_from, habit.start_date)
    end_date = date_to if habit.end_date is None else min(date_to, habit.end_date)
    if end_date < start_date:
        return

    for current_date in iter_dates(start_date, end_date):
        if is_habit_scheduled_for_date(habit, current_date):
            yield current_date


def _weekday_to_index(weekday: DayOfWeek | int | str) -> int | None:
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
        except ValueError:
            return None
        return DAY_OF_WEEK_ORDER[weekday_enum]

    return None
