from __future__ import annotations

from dataclasses import dataclass, field
from datetime import date, datetime, timezone

import pytest
from fastapi import HTTPException, status

from app.schemas.enums import DayOfWeek, DayPart, FrequencyType
from app.schemas.habit_stats import StatsPeriod
from app.services import habit_stats


@dataclass
class StubHabitWeekday:
    weekday: DayOfWeek | int | str


@dataclass
class StubHabitTarget:
    day_part: DayPart
    target_count: int


@dataclass
class StubHabit:
    id: int
    name: str
    start_date: date
    end_date: date | None
    frequency_type: FrequencyType
    targets: list[StubHabitTarget]
    every_n_days: int | None = None
    weekdays: list[StubHabitWeekday] = field(default_factory=list)


@dataclass
class StubHabitLog:
    id: int
    habit_id: int
    log_date: date
    day_part: DayPart
    amount: int
    created_at: datetime
    updated_at: datetime


@pytest.fixture(autouse=True)
def clean_db() -> None:
    return None


@pytest.mark.asyncio
async def test_get_stats_overview_week_daily_habit_counts_only_past_expected_days_for_rate(monkeypatch: pytest.MonkeyPatch) -> None:
    habit = StubHabit(
        id=1,
        name="Morning Run",
        start_date=date(2026, 3, 1),
        end_date=None,
        frequency_type=FrequencyType.DAILY,
        targets=[StubHabitTarget(day_part=DayPart.MORNING, target_count=1)],
    )

    async def fake_get_user_habits(**kwargs) -> list[StubHabit]:
        return [habit]

    async def fake_get_actual_amounts(**kwargs) -> dict[tuple[int, date], dict[DayPart, int]]:
        return {
            (1, date(2026, 3, 16)): {DayPart.MORNING: 1},
            (1, date(2026, 3, 18)): {DayPart.MORNING: 1},
        }

    monkeypatch.setattr(habit_stats, "_get_user_habits", fake_get_user_habits)
    monkeypatch.setattr(habit_stats, "_get_actual_amounts_by_habit_day_part", fake_get_actual_amounts)
    monkeypatch.setattr(habit_stats, "_get_today", lambda: date(2026, 3, 18))

    overview = await habit_stats.get_stats_overview(
        db=None,
        user_id=None,
        period=StatsPeriod.WEEK,
        anchor_date=date(2026, 3, 18),
    )

    assert overview.title == "THIS WEEK"
    assert overview.subtitle == "Mar 16-22, 2026"
    assert len(overview.cards) == 1
    card = overview.cards[0]
    assert card.habit_id == 1
    assert card.name == "Morning Run"
    assert card.completion_rate == 67
    assert len(card.points) == 7
    assert [point.completed for point in card.points[:3]] == [1, 0, 1]
    assert [point.expected for point in card.points[:3]] == [1, 1, 1]
    assert [point.is_future for point in card.points] == [False, False, False, True, True, True, True]


@pytest.mark.asyncio
async def test_get_stats_overview_returns_multiple_cards_in_habit_order(monkeypatch: pytest.MonkeyPatch) -> None:
    first_habit = StubHabit(
        id=1,
        name="Reading",
        start_date=date(2026, 3, 1),
        end_date=None,
        frequency_type=FrequencyType.DAILY,
        targets=[StubHabitTarget(day_part=DayPart.FULL_DAY, target_count=1)],
    )
    second_habit = StubHabit(
        id=2,
        name="Yoga",
        start_date=date(2026, 3, 1),
        end_date=None,
        frequency_type=FrequencyType.WEEKLY,
        weekdays=[StubHabitWeekday(weekday=DayOfWeek.MONDAY)],
        targets=[StubHabitTarget(day_part=DayPart.MORNING, target_count=1)],
    )

    async def fake_get_user_habits(**kwargs) -> list[StubHabit]:
        return [first_habit, second_habit]

    async def fake_get_actual_amounts(**kwargs) -> dict[tuple[int, date], dict[DayPart, int]]:
        return {
            (1, date(2026, 3, 16)): {DayPart.FULL_DAY: 1},
            (2, date(2026, 3, 16)): {DayPart.MORNING: 1},
        }

    monkeypatch.setattr(habit_stats, "_get_user_habits", fake_get_user_habits)
    monkeypatch.setattr(habit_stats, "_get_actual_amounts_by_habit_day_part", fake_get_actual_amounts)
    monkeypatch.setattr(habit_stats, "_get_today", lambda: date(2026, 3, 18))

    overview = await habit_stats.get_stats_overview(
        db=None,
        user_id=None,
        period=StatsPeriod.WEEK,
        anchor_date=date(2026, 3, 18),
    )

    assert [card.habit_id for card in overview.cards] == [1, 2]
    assert [card.name for card in overview.cards] == ["Reading", "Yoga"]


@pytest.mark.asyncio
async def test_get_stats_overview_builds_month_points_for_full_month(monkeypatch: pytest.MonkeyPatch) -> None:
    habit = StubHabit(
        id=1,
        name="Walk",
        start_date=date(2026, 3, 1),
        end_date=None,
        frequency_type=FrequencyType.DAILY,
        targets=[StubHabitTarget(day_part=DayPart.FULL_DAY, target_count=1)],
    )

    async def fake_get_user_habits(**kwargs) -> list[StubHabit]:
        return [habit]

    async def fake_get_actual_amounts(**kwargs) -> dict[tuple[int, date], dict[DayPart, int]]:
        return {(1, date(2026, 3, 1)): {DayPart.FULL_DAY: 1}}

    monkeypatch.setattr(habit_stats, "_get_user_habits", fake_get_user_habits)
    monkeypatch.setattr(habit_stats, "_get_actual_amounts_by_habit_day_part", fake_get_actual_amounts)
    monkeypatch.setattr(habit_stats, "_get_today", lambda: date(2026, 3, 15))

    overview = await habit_stats.get_stats_overview(
        db=None,
        user_id=None,
        period=StatsPeriod.MONTH,
        anchor_date=date(2026, 3, 18),
    )

    assert overview.title == "THIS MONTH"
    assert overview.subtitle == "March 2026"
    assert len(overview.cards[0].points) == 31


@pytest.mark.asyncio
async def test_get_stats_overview_builds_year_points_for_twelve_months(monkeypatch: pytest.MonkeyPatch) -> None:
    habit = StubHabit(
        id=1,
        name="Meditate",
        start_date=date(2026, 1, 1),
        end_date=None,
        frequency_type=FrequencyType.DAILY,
        targets=[StubHabitTarget(day_part=DayPart.MORNING, target_count=1)],
    )

    async def fake_get_user_habits(**kwargs) -> list[StubHabit]:
        return [habit]

    async def fake_get_actual_amounts(**kwargs) -> dict[tuple[int, date], dict[DayPart, int]]:
        return {
            (1, date(2026, 1, 2)): {DayPart.MORNING: 1},
            (1, date(2026, 1, 3)): {DayPart.MORNING: 1},
        }

    monkeypatch.setattr(habit_stats, "_get_user_habits", fake_get_user_habits)
    monkeypatch.setattr(habit_stats, "_get_actual_amounts_by_habit_day_part", fake_get_actual_amounts)
    monkeypatch.setattr(habit_stats, "_get_today", lambda: date(2026, 6, 15))

    overview = await habit_stats.get_stats_overview(
        db=None,
        user_id=None,
        period=StatsPeriod.YEAR,
        anchor_date=date(2026, 6, 15),
    )

    assert overview.title == "THIS YEAR"
    assert overview.subtitle == "2026"
    assert len(overview.cards) == 1
    assert len(overview.cards[0].points) == 12


@pytest.mark.asyncio
async def test_get_stats_overview_returns_empty_cards_when_user_has_no_relevant_habits(
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    async def fake_get_user_habits(**kwargs) -> list[StubHabit]:
        return []

    async def fake_get_actual_amounts(**kwargs) -> dict[tuple[int, date], dict[DayPart, int]]:
        return {}

    monkeypatch.setattr(habit_stats, "_get_user_habits", fake_get_user_habits)
    monkeypatch.setattr(habit_stats, "_get_actual_amounts_by_habit_day_part", fake_get_actual_amounts)
    monkeypatch.setattr(habit_stats, "_get_today", lambda: date(2026, 3, 18))

    overview = await habit_stats.get_stats_overview(
        db=None,
        user_id=None,
        period=StatsPeriod.WEEK,
        anchor_date=date(2026, 3, 18),
    )

    assert overview.cards == []


@pytest.mark.asyncio
async def test_get_stats_overview_zero_target_habit_is_not_completed_by_default(
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    habit = StubHabit(
        id=1,
        name="Targetless Habit",
        start_date=date(2026, 3, 1),
        end_date=None,
        frequency_type=FrequencyType.DAILY,
        targets=[],
    )

    async def fake_get_user_habits(**kwargs) -> list[StubHabit]:
        return [habit]

    async def fake_get_actual_amounts(**kwargs) -> dict[tuple[int, date], dict[DayPart, int]]:
        return {}

    monkeypatch.setattr(habit_stats, "_get_user_habits", fake_get_user_habits)
    monkeypatch.setattr(habit_stats, "_get_actual_amounts_by_habit_day_part", fake_get_actual_amounts)
    monkeypatch.setattr(habit_stats, "_get_today", lambda: date(2026, 3, 18))

    overview = await habit_stats.get_stats_overview(
        db=None,
        user_id=None,
        period=StatsPeriod.WEEK,
        anchor_date=date(2026, 3, 18),
    )

    assert len(overview.cards) == 1
    card = overview.cards[0]
    assert card.completion_rate == 0
    assert [point.expected for point in card.points[:3]] == [1, 1, 1]
    assert [point.completed for point in card.points[:3]] == [0, 0, 0]


@pytest.mark.asyncio
async def test_get_stats_overview_partial_logs_do_not_complete_multi_target_habit(
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    habit = StubHabit(
        id=1,
        name="Two Part Habit",
        start_date=date(2026, 3, 1),
        end_date=None,
        frequency_type=FrequencyType.DAILY,
        targets=[
            StubHabitTarget(day_part=DayPart.MORNING, target_count=1),
            StubHabitTarget(day_part=DayPart.EVENING, target_count=1),
        ],
    )

    async def fake_get_user_habits(**kwargs) -> list[StubHabit]:
        return [habit]

    async def fake_get_actual_amounts(**kwargs) -> dict[tuple[int, date], dict[DayPart, int]]:
        return {
            (1, date(2026, 3, 16)): {DayPart.MORNING: 1},
        }

    monkeypatch.setattr(habit_stats, "_get_user_habits", fake_get_user_habits)
    monkeypatch.setattr(habit_stats, "_get_actual_amounts_by_habit_day_part", fake_get_actual_amounts)
    monkeypatch.setattr(habit_stats, "_get_today", lambda: date(2026, 3, 18))

    overview = await habit_stats.get_stats_overview(
        db=None,
        user_id=None,
        period=StatsPeriod.WEEK,
        anchor_date=date(2026, 3, 18),
    )

    assert len(overview.cards) == 1
    card = overview.cards[0]
    assert card.completion_rate == 0
    assert card.points[0].expected == 1
    assert card.points[0].completed == 0
    assert card.points[0].value == 0.0


@pytest.mark.asyncio
async def test_get_habit_stats_details_returns_404_when_habit_is_missing(
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    async def fake_get_habit_or_404(**kwargs):
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Habit not found")

    monkeypatch.setattr(habit_stats, "_get_habit_or_404", fake_get_habit_or_404)

    with pytest.raises(HTTPException) as exc_info:
        await habit_stats.get_habit_stats_details(
            db=None,
            user_id=None,
            habit_id=999,
            period=StatsPeriod.WEEK,
            anchor_date=date(2026, 3, 18),
        )

    assert exc_info.value.status_code == 404
    assert exc_info.value.detail == "Habit not found"


@pytest.mark.asyncio
async def test_get_habit_stats_details_week_aggregates_points_summary_calendar_and_time_of_day(
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    habit = StubHabit(
        id=1,
        name="Practice Piano",
        start_date=date(2026, 3, 1),
        end_date=None,
        frequency_type=FrequencyType.DAILY,
        targets=[
            StubHabitTarget(day_part=DayPart.MORNING, target_count=1),
            StubHabitTarget(day_part=DayPart.EVENING, target_count=1),
        ],
    )
    logs = [
        StubHabitLog(
            id=1,
            habit_id=1,
            log_date=date(2026, 3, 16),
            day_part=DayPart.MORNING,
            amount=1,
            created_at=datetime(2026, 3, 16, 8, 0, tzinfo=timezone.utc),
            updated_at=datetime(2026, 3, 16, 8, 15, tzinfo=timezone.utc),
        ),
        StubHabitLog(
            id=2,
            habit_id=1,
            log_date=date(2026, 3, 16),
            day_part=DayPart.EVENING,
            amount=1,
            created_at=datetime(2026, 3, 16, 20, 0, tzinfo=timezone.utc),
            updated_at=datetime(2026, 3, 16, 20, 15, tzinfo=timezone.utc),
        ),
        StubHabitLog(
            id=3,
            habit_id=1,
            log_date=date(2026, 3, 17),
            day_part=DayPart.MORNING,
            amount=1,
            created_at=datetime(2026, 3, 17, 8, 30, tzinfo=timezone.utc),
            updated_at=datetime(2026, 3, 17, 8, 45, tzinfo=timezone.utc),
        ),
        StubHabitLog(
            id=4,
            habit_id=1,
            log_date=date(2026, 3, 17),
            day_part=DayPart.EVENING,
            amount=1,
            created_at=datetime(2026, 3, 17, 20, 0, tzinfo=timezone.utc),
            updated_at=datetime(2026, 3, 17, 20, 5, tzinfo=timezone.utc),
        ),
        StubHabitLog(
            id=5,
            habit_id=1,
            log_date=date(2026, 3, 18),
            day_part=DayPart.MORNING,
            amount=1,
            created_at=datetime(2026, 3, 18, 9, 0, tzinfo=timezone.utc),
            updated_at=datetime(2026, 3, 18, 9, 0, tzinfo=timezone.utc),
        ),
        StubHabitLog(
            id=6,
            habit_id=1,
            log_date=date(2026, 3, 20),
            day_part=DayPart.MORNING,
            amount=1,
            created_at=datetime(2026, 3, 20, 7, 50, tzinfo=timezone.utc),
            updated_at=datetime(2026, 3, 20, 8, 0, tzinfo=timezone.utc),
        ),
        StubHabitLog(
            id=7,
            habit_id=1,
            log_date=date(2026, 3, 20),
            day_part=DayPart.EVENING,
            amount=1,
            created_at=datetime(2026, 3, 20, 19, 45, tzinfo=timezone.utc),
            updated_at=datetime(2026, 3, 20, 20, 0, tzinfo=timezone.utc),
        ),
    ]

    async def fake_get_habit_or_404(**kwargs) -> StubHabit:
        return habit

    async def fake_get_habit_logs(**kwargs) -> list[StubHabitLog]:
        return logs

    monkeypatch.setattr(habit_stats, "_get_habit_or_404", fake_get_habit_or_404)
    monkeypatch.setattr(habit_stats, "_get_habit_logs", fake_get_habit_logs)
    monkeypatch.setattr(habit_stats, "_get_today", lambda: date(2026, 3, 20))

    details = await habit_stats.get_habit_stats_details(
        db=None,
        user_id=None,
        habit_id=1,
        period=StatsPeriod.WEEK,
        anchor_date=date(2026, 3, 18),
    )

    assert details.habit_id == 1
    assert details.name == "Practice Piano"
    assert details.frequency_label == "Every day"
    assert details.title == "THIS WEEK"
    assert details.subtitle == "Mar 16-22, 2026"
    assert len(details.entries_points) == 7
    assert details.completion_rate == 60
    assert details.summary.current_streak == 1
    assert details.summary.best_streak == 2
    assert details.summary.goals_met == 3

    day_16 = next(day for day in details.calendar.days if day.date == date(2026, 3, 16))
    day_18 = next(day for day in details.calendar.days if day.date == date(2026, 3, 18))
    day_19 = next(day for day in details.calendar.days if day.date == date(2026, 3, 19))
    assert day_16.is_scheduled is True
    assert day_16.is_completed is True
    assert day_16.has_progress is True
    assert day_18.is_scheduled is True
    assert day_18.is_completed is False
    assert day_18.has_progress is True
    assert day_19.is_scheduled is True
    assert day_19.is_completed is False
    assert day_19.has_progress is False

    monday_point = next(point for point in details.time_of_day.points if point.day_label == "Mon")
    tuesday_point = next(point for point in details.time_of_day.points if point.day_label == "Tue")
    friday_point = next(point for point in details.time_of_day.points if point.day_label == "Fri")
    assert monday_point.average_minutes == 855
    assert tuesday_point.average_minutes == 865
    assert friday_point.average_minutes == 840
    assert details.time_of_day.week_average_minutes == 809
    assert details.time_of_day.week_average_label == "13:29"


@pytest.mark.asyncio
@pytest.mark.parametrize(
    ("period", "anchor_date", "expected_points"),
    [
        (StatsPeriod.MONTH, date(2026, 3, 18), 31),
        (StatsPeriod.YEAR, date(2026, 6, 15), 12),
    ],
)
async def test_get_habit_stats_details_builds_expected_point_counts_for_month_and_year(
    monkeypatch: pytest.MonkeyPatch,
    period: StatsPeriod,
    anchor_date: date,
    expected_points: int,
) -> None:
    habit = StubHabit(
        id=1,
        name="Stretch",
        start_date=date(2026, 1, 1),
        end_date=None,
        frequency_type=FrequencyType.DAILY,
        targets=[StubHabitTarget(day_part=DayPart.FULL_DAY, target_count=1)],
    )

    async def fake_get_habit_or_404(**kwargs) -> StubHabit:
        return habit

    async def fake_get_habit_logs(**kwargs) -> list[StubHabitLog]:
        return []

    monkeypatch.setattr(habit_stats, "_get_habit_or_404", fake_get_habit_or_404)
    monkeypatch.setattr(habit_stats, "_get_habit_logs", fake_get_habit_logs)
    monkeypatch.setattr(habit_stats, "_get_today", lambda: date(2026, 3, 18))

    details = await habit_stats.get_habit_stats_details(
        db=None,
        user_id=None,
        habit_id=1,
        period=period,
        anchor_date=anchor_date,
    )

    assert len(details.entries_points) == expected_points
    assert details.time_of_day.week_average_minutes is None
    assert details.time_of_day.week_average_label is None
    assert all(point.average_minutes is None for point in details.time_of_day.points)
