from __future__ import annotations

import calendar
from collections.abc import Set as AbstractSet
from datetime import date

from app.schemas.habit_stats import StatsPointOut
from app.services.helpers.habit_schedule import iter_dates

WEEKDAY_LABELS = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"]
MONTH_LABELS = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"]


def compute_completion_rate(expected_count: int, completed_count: int) -> int:
    if expected_count <= 0:
        return 0

    clamped_completed = max(completed_count, 0)
    raw_percent = round((clamped_completed / expected_count) * 100)
    return max(0, min(int(raw_percent), 100))


def build_week_points(
    *,
    range_start: date,
    range_end: date,
    today: date,
    expected_dates: AbstractSet[date],
    completed_dates: AbstractSet[date],
) -> list[StatsPointOut]:
    return [
        _build_point(
            current_date=current_date,
            label=WEEKDAY_LABELS[current_date.weekday()],
            today=today,
            expected_dates=expected_dates,
            completed_dates=completed_dates,
        )
        for current_date in iter_dates(range_start, range_end)
    ]


def build_month_points(
    *,
    range_start: date,
    range_end: date,
    today: date,
    expected_dates: AbstractSet[date],
    completed_dates: AbstractSet[date],
) -> list[StatsPointOut]:
    return [
        _build_point(
            current_date=current_date,
            label=str(current_date.day),
            today=today,
            expected_dates=expected_dates,
            completed_dates=completed_dates,
        )
        for current_date in iter_dates(range_start, range_end)
    ]


def build_year_points(
    *,
    range_start: date,
    range_end: date,
    today: date,
    expected_dates: AbstractSet[date],
    completed_dates: AbstractSet[date],
) -> list[StatsPointOut]:
    year = range_start.year
    points: list[StatsPointOut] = []

    for month in range(1, 13):
        month_start = date(year, month, 1)
        month_end = date(year, month, calendar.monthrange(year, month)[1])
        expected_count = _count_dates_in_range(expected_dates, month_start, month_end)
        completed_count = _count_dates_in_range(completed_dates, month_start, month_end)
        points.append(
            _normalize_point(
                key=f"{year}-{month:02d}",
                label=MONTH_LABELS[month - 1],
                completed=completed_count,
                expected=expected_count,
                is_future=month_start > today or month_end > range_end,
            )
        )

    return points


def _build_point(
    *,
    current_date: date,
    label: str,
    today: date,
    expected_dates: AbstractSet[date],
    completed_dates: AbstractSet[date],
) -> StatsPointOut:
    expected_count = 1 if current_date in expected_dates else 0
    completed_count = 1 if current_date in completed_dates else 0
    return _normalize_point(
        key=current_date.isoformat(),
        label=label,
        completed=completed_count,
        expected=expected_count,
        is_future=current_date > today,
    )


def _normalize_point(
    *,
    key: str,
    label: str,
    completed: int,
    expected: int,
    is_future: bool,
) -> StatsPointOut:
    normalized_completed = max(completed, 0)
    normalized_expected = max(expected, 0)
    return StatsPointOut(
        key=key,
        label=label,
        completed=normalized_completed,
        expected=normalized_expected,
        value=float(compute_completion_rate(normalized_expected, normalized_completed)),
        is_future=is_future,
    )


def _count_dates_in_range(
    values: AbstractSet[date],
    range_start: date,
    range_end: date,
) -> int:
    return sum(1 for current_date in values if range_start <= current_date <= range_end)
