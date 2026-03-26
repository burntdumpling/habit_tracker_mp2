from __future__ import annotations

from dataclasses import dataclass, field
from datetime import date

import pytest

from app.schemas.enums import DayOfWeek, FrequencyType
from app.services.helpers.habit_schedule import (
    SchedulePeriod,
    is_habit_active_on_date,
    is_habit_scheduled_for_date,
    iter_scheduled_dates,
    resolve_period_range,
)


@dataclass
class StubHabitWeekday:
    weekday: DayOfWeek | int | str


@dataclass
class StubHabit:
    start_date: date
    end_date: date | None
    frequency_type: FrequencyType
    every_n_days: int | None = None
    weekdays: list[StubHabitWeekday] = field(default_factory=list)


@pytest.fixture(autouse=True)
def clean_db() -> None:
    return None


def test_is_habit_active_on_date_respects_start_and_end_bounds() -> None:
    habit = StubHabit(
        start_date=date(2026, 2, 10),
        end_date=date(2026, 2, 20),
        frequency_type=FrequencyType.DAILY,
    )

    assert is_habit_active_on_date(habit, date(2026, 2, 9)) is False
    assert is_habit_active_on_date(habit, date(2026, 2, 10)) is True
    assert is_habit_active_on_date(habit, date(2026, 2, 20)) is True
    assert is_habit_active_on_date(habit, date(2026, 2, 21)) is False


def test_is_habit_scheduled_for_weekly_habit_supports_existing_weekday_values() -> None:
    habit = StubHabit(
        start_date=date(2026, 2, 1),
        end_date=None,
        frequency_type=FrequencyType.WEEKLY,
        weekdays=[
            StubHabitWeekday(weekday=DayOfWeek.MONDAY),
            StubHabitWeekday(weekday="3"),
        ],
    )

    assert is_habit_scheduled_for_date(habit, date(2026, 2, 2)) is True
    assert is_habit_scheduled_for_date(habit, date(2026, 2, 5)) is True
    assert is_habit_scheduled_for_date(habit, date(2026, 2, 4)) is False


def test_is_habit_scheduled_for_every_n_days_uses_start_date_as_anchor() -> None:
    habit = StubHabit(
        start_date=date(2026, 2, 10),
        end_date=None,
        frequency_type=FrequencyType.EVERY_N_DAYS,
        every_n_days=3,
    )

    assert is_habit_scheduled_for_date(habit, date(2026, 2, 10)) is True
    assert is_habit_scheduled_for_date(habit, date(2026, 2, 12)) is False
    assert is_habit_scheduled_for_date(habit, date(2026, 2, 13)) is True


def test_iter_scheduled_dates_filters_to_active_scheduled_days() -> None:
    habit = StubHabit(
        start_date=date(2026, 2, 3),
        end_date=date(2026, 2, 14),
        frequency_type=FrequencyType.WEEKLY,
        weekdays=[StubHabitWeekday(weekday="MONDAY"), StubHabitWeekday(weekday="FRIDAY")],
    )

    scheduled_dates = list(iter_scheduled_dates(habit, date(2026, 2, 1), date(2026, 2, 20)))

    assert scheduled_dates == [
        date(2026, 2, 6),
        date(2026, 2, 9),
        date(2026, 2, 13),
    ]


def test_resolve_period_range_returns_calendar_boundaries() -> None:
    assert resolve_period_range(date(2026, 3, 18), SchedulePeriod.WEEK) == (
        date(2026, 3, 16),
        date(2026, 3, 22),
    )
    assert resolve_period_range(date(2026, 2, 18), SchedulePeriod.MONTH) == (
        date(2026, 2, 1),
        date(2026, 2, 28),
    )
    assert resolve_period_range(date(2026, 2, 18), SchedulePeriod.YEAR) == (
        date(2026, 1, 1),
        date(2026, 12, 31),
    )
