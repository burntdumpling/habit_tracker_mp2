from __future__ import annotations

from collections import defaultdict
from datetime import date, datetime
from uuid import UUID

from fastapi import HTTPException, status
from sqlalchemy import func, select
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.orm import selectinload

from app.db.models import Habit, HabitLog
from app.schemas.enums import DayOfWeek, DayPart, FrequencyType
from app.schemas.habit_stats import (
    HabitCalendarDayMarkerOut,
    HabitStatsDetailsOut,
    HabitStatsOverviewCardOut,
    HabitStatsSummaryOut,
    HabitMonthCalendarOut,
    HabitTimeOfDayStatsOut,
    StatsOverviewOut,
    StatsPeriod,
    StatsPointOut,
    TimeOfDayPointOut,
)
from app.services.helpers.habit_schedule import iter_dates, iter_scheduled_dates, resolve_period_range
from app.services.helpers.habit_stats_common import (
    build_month_points,
    build_week_points,
    build_year_points,
    compute_completion_rate,
)

DAY_OF_WEEK_ORDER = {
    DayOfWeek.MONDAY: 0,
    DayOfWeek.TUESDAY: 1,
    DayOfWeek.WEDNESDAY: 2,
    DayOfWeek.THURSDAY: 3,
    DayOfWeek.FRIDAY: 4,
    DayOfWeek.SATURDAY: 5,
    DayOfWeek.SUNDAY: 6,
}

WEEKDAY_LABELS = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"]


async def get_stats_overview(
    db: AsyncSession,
    user_id: UUID,
    period: StatsPeriod,
    anchor_date: date,
) -> StatsOverviewOut:
    range_start, range_end = resolve_period_range(anchor_date, period.value)
    habits = await _get_user_habits(db=db, user_id=user_id)
    habit_ids = [habit.id for habit in habits]
    actual_amounts = await _get_actual_amounts_by_habit_day_part(
        db=db,
        habit_ids=habit_ids,
        date_from=range_start,
        date_to=range_end,
    )
    today = _get_today()

    cards = [
        card
        for habit in habits
        if (
            card := _build_overview_card(
                habit=habit,
                period=period,
                range_start=range_start,
                range_end=range_end,
                today=today,
                actual_amounts=actual_amounts,
            )
        )
        is not None
    ]

    title, subtitle = _build_period_labels(
        period=period,
        anchor_date=anchor_date,
        range_start=range_start,
        range_end=range_end,
    )
    return StatsOverviewOut(
        period=period,
        anchor_date=anchor_date,
        range_start=range_start,
        range_end=range_end,
        title=title,
        subtitle=subtitle,
        cards=cards,
    )


async def get_habit_stats_details(
    db: AsyncSession,
    user_id: UUID,
    habit_id: int,
    period: StatsPeriod,
    anchor_date: date,
) -> HabitStatsDetailsOut:
    habit = await _get_habit_or_404(db=db, user_id=user_id, habit_id=habit_id)
    range_start, range_end = resolve_period_range(anchor_date, period.value)
    calendar_start, calendar_end = resolve_period_range(anchor_date, StatsPeriod.MONTH.value)
    today = _get_today()
    logs_end = max(range_end, calendar_end, today)
    logs = await _get_habit_logs(
        db=db,
        habit_id=habit.id,
        date_from=habit.start_date,
        date_to=logs_end,
    )
    actual_amounts = _build_actual_amounts_by_habit_day_part(logs)
    progress_dates = _build_progress_dates(logs)

    expected_dates = set(iter_scheduled_dates(habit, range_start, range_end))
    completed_dates = _get_completed_dates(habit=habit, candidate_dates=expected_dates, actual_amounts=actual_amounts)
    expected_up_to_today = {scheduled_date for scheduled_date in expected_dates if scheduled_date <= today}
    completed_up_to_today = {completed_date for completed_date in completed_dates if completed_date <= today}
    entries_points = _build_points_for_period(
        period=period,
        range_start=range_start,
        range_end=range_end,
        today=today,
        expected_dates=expected_dates,
        completed_dates=completed_dates,
    )
    title, subtitle = _build_period_labels(
        period=period,
        anchor_date=anchor_date,
        range_start=range_start,
        range_end=range_end,
    )

    summary = _build_habit_summary(habit=habit, actual_amounts=actual_amounts, today=today)
    calendar = _build_habit_calendar(
        habit=habit,
        month_start=calendar_start,
        month_end=calendar_end,
        actual_amounts=actual_amounts,
        progress_dates=progress_dates,
    )
    time_of_day = _build_time_of_day_stats(logs)

    return HabitStatsDetailsOut(
        habit_id=habit.id,
        name=habit.name,
        icon=None,
        color=None,
        frequency_label=_build_frequency_label(habit),
        period=period,
        anchor_date=anchor_date,
        range_start=range_start,
        range_end=range_end,
        title=title,
        subtitle=subtitle,
        completion_rate=compute_completion_rate(len(expected_up_to_today), len(completed_up_to_today)),
        entries_points=entries_points,
        summary=summary,
        calendar=calendar,
        time_of_day=time_of_day,
    )


async def _get_user_habits(db: AsyncSession, user_id: UUID) -> list[Habit]:
    result = await db.execute(
        select(Habit)
        .options(selectinload(Habit.weekdays), selectinload(Habit.targets))
        .where(Habit.user_id == user_id)
        .order_by(Habit.id.asc())
    )
    return list(result.scalars().all())


async def _get_habit_or_404(db: AsyncSession, user_id: UUID, habit_id: int) -> Habit:
    result = await db.execute(
        select(Habit)
        .options(selectinload(Habit.weekdays), selectinload(Habit.targets))
        .where(Habit.id == habit_id, Habit.user_id == user_id)
    )
    habit = result.scalar_one_or_none()
    if habit is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Habit not found")
    return habit


async def _get_habit_logs(
    db: AsyncSession,
    habit_id: int,
    date_from: date,
    date_to: date,
) -> list[HabitLog]:
    result = await db.execute(
        select(HabitLog)
        .where(
            HabitLog.habit_id == habit_id,
            HabitLog.log_date >= date_from,
            HabitLog.log_date <= date_to,
        )
        .order_by(HabitLog.log_date.asc(), HabitLog.created_at.asc(), HabitLog.id.asc())
    )
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


def _build_actual_amounts_by_habit_day_part(logs: list[HabitLog]) -> dict[tuple[int, date], dict[DayPart, int]]:
    by_habit_day: dict[tuple[int, date], dict[DayPart, int]] = defaultdict(dict)
    for log in logs:
        by_habit_day[(log.habit_id, log.log_date)][log.day_part] = (
            by_habit_day[(log.habit_id, log.log_date)].get(log.day_part, 0) + int(log.amount)
        )
    return dict(by_habit_day)


def _build_progress_dates(logs: list[HabitLog]) -> set[date]:
    return {log.log_date for log in logs if log.amount > 0}


def _build_overview_card(
    *,
    habit: Habit,
    period: StatsPeriod,
    range_start: date,
    range_end: date,
    today: date,
    actual_amounts: dict[tuple[int, date], dict[DayPart, int]],
) -> HabitStatsOverviewCardOut | None:
    expected_dates = set(iter_scheduled_dates(habit, range_start, range_end))
    completed_dates = {
        scheduled_date
        for scheduled_date in expected_dates
        if _is_habit_completed_for_date(
            habit=habit,
            actual_by_day_part=actual_amounts.get((habit.id, scheduled_date), {}),
        )
    }

    if not expected_dates and not completed_dates:
        return None

    points = _build_points_for_period(
        period=period,
        range_start=range_start,
        range_end=range_end,
        today=today,
        expected_dates=expected_dates,
        completed_dates=completed_dates,
    )
    expected_up_to_today = {scheduled_date for scheduled_date in expected_dates if scheduled_date <= today}
    completed_up_to_today = {completed_date for completed_date in completed_dates if completed_date <= today}

    return HabitStatsOverviewCardOut(
        habit_id=habit.id,
        name=habit.name,
        icon=None,
        color=None,
        completion_rate=compute_completion_rate(len(expected_up_to_today), len(completed_up_to_today)),
        points=points,
    )


def _is_habit_completed_for_date(
    *,
    habit: Habit,
    actual_by_day_part: dict[DayPart, int],
) -> bool:
    if not habit.targets:
        return False
    return all(actual_by_day_part.get(target.day_part, 0) >= target.target_count for target in habit.targets)


def _get_completed_dates(
    *,
    habit: Habit,
    candidate_dates: set[date],
    actual_amounts: dict[tuple[int, date], dict[DayPart, int]],
) -> set[date]:
    return {
        scheduled_date
        for scheduled_date in candidate_dates
        if _is_habit_completed_for_date(
            habit=habit,
            actual_by_day_part=actual_amounts.get((habit.id, scheduled_date), {}),
        )
    }


def _build_points_for_period(
    *,
    period: StatsPeriod,
    range_start: date,
    range_end: date,
    today: date,
    expected_dates: set[date],
    completed_dates: set[date],
) -> list[StatsPointOut]:
    if period == StatsPeriod.WEEK:
        return build_week_points(
            range_start=range_start,
            range_end=range_end,
            today=today,
            expected_dates=expected_dates,
            completed_dates=completed_dates,
        )
    if period == StatsPeriod.MONTH:
        return build_month_points(
            range_start=range_start,
            range_end=range_end,
            today=today,
            expected_dates=expected_dates,
            completed_dates=completed_dates,
        )
    return build_year_points(
        range_start=range_start,
        range_end=range_end,
        today=today,
        expected_dates=expected_dates,
        completed_dates=completed_dates,
    )


def _build_period_labels(
    *,
    period: StatsPeriod,
    anchor_date: date,
    range_start: date,
    range_end: date,
) -> tuple[str, str]:
    if period == StatsPeriod.WEEK:
        return "THIS WEEK", _format_week_subtitle(range_start, range_end)
    if period == StatsPeriod.MONTH:
        return "THIS MONTH", anchor_date.strftime("%B %Y")
    return "THIS YEAR", str(anchor_date.year)


def _format_week_subtitle(range_start: date, range_end: date) -> str:
    if range_start.year != range_end.year:
        return f"{_format_full_date(range_start)} - {_format_full_date(range_end)}"
    if range_start.month != range_end.month:
        return f"{range_start.strftime('%b')} {range_start.day} - {range_end.strftime('%b')} {range_end.day}, {range_end.year}"
    return f"{range_start.strftime('%b')} {range_start.day}-{range_end.day}, {range_end.year}"


def _format_full_date(value: date) -> str:
    return f"{value.strftime('%b')} {value.day}, {value.year}"


def _get_today() -> date:
    return date.today()


def _build_habit_summary(
    *,
    habit: Habit,
    actual_amounts: dict[tuple[int, date], dict[DayPart, int]],
    today: date,
) -> HabitStatsSummaryOut:
    lifetime_end = today if habit.end_date is None else min(today, habit.end_date)
    if lifetime_end < habit.start_date:
        return HabitStatsSummaryOut(current_streak=0, best_streak=0, goals_met=0)

    scheduled_dates = list(iter_scheduled_dates(habit, habit.start_date, lifetime_end))
    completed_dates = _get_completed_dates(
        habit=habit,
        candidate_dates=set(scheduled_dates),
        actual_amounts=actual_amounts,
    )
    current_streak, best_streak = _compute_streaks(scheduled_dates, completed_dates)
    return HabitStatsSummaryOut(
        current_streak=current_streak,
        best_streak=best_streak,
        goals_met=len(completed_dates),
    )


def _compute_streaks(scheduled_dates: list[date], completed_dates: set[date]) -> tuple[int, int]:
    current_streak = 0
    best_streak = 0
    running_streak = 0

    for scheduled_date in scheduled_dates:
        if scheduled_date in completed_dates:
            running_streak += 1
            best_streak = max(best_streak, running_streak)
        else:
            running_streak = 0

    current_streak = running_streak
    return current_streak, best_streak


def _build_habit_calendar(
    *,
    habit: Habit,
    month_start: date,
    month_end: date,
    actual_amounts: dict[tuple[int, date], dict[DayPart, int]],
    progress_dates: set[date],
) -> HabitMonthCalendarOut:
    scheduled_dates = set(iter_scheduled_dates(habit, month_start, month_end))
    completed_dates = _get_completed_dates(
        habit=habit,
        candidate_dates=scheduled_dates,
        actual_amounts=actual_amounts,
    )
    return HabitMonthCalendarOut(
        month=month_start.month,
        year=month_start.year,
        days=[
            HabitCalendarDayMarkerOut(
                date=current_date,
                is_scheduled=current_date in scheduled_dates,
                is_completed=current_date in completed_dates,
                has_progress=current_date in progress_dates,
            )
            for current_date in iter_dates(month_start, month_end)
        ],
    )


def _build_time_of_day_stats(logs: list[HabitLog]) -> HabitTimeOfDayStatsOut:
    minutes_by_weekday: dict[int, list[int]] = defaultdict(list)
    all_minutes: list[int] = []

    for log in logs:
        if log.amount <= 0:
            continue
        timestamp = _get_log_timestamp(log)
        if timestamp is None:
            continue
        minute_of_day = timestamp.hour * 60 + timestamp.minute
        weekday_index = log.log_date.weekday()
        minutes_by_weekday[weekday_index].append(minute_of_day)
        all_minutes.append(minute_of_day)

    points = [
        TimeOfDayPointOut(
            day_label=WEEKDAY_LABELS[weekday_index],
            average_minutes=_average_minutes(minutes_by_weekday.get(weekday_index, [])),
        )
        for weekday_index in range(7)
    ]
    week_average_minutes = _average_minutes(all_minutes)
    return HabitTimeOfDayStatsOut(
        week_average_minutes=week_average_minutes,
        week_average_label=_format_minutes_label(week_average_minutes),
        points=points,
    )


def _get_log_timestamp(log: HabitLog) -> datetime | None:
    return log.updated_at or log.created_at


def _average_minutes(values: list[int]) -> int | None:
    if not values:
        return None
    return round(sum(values) / len(values))


def _format_minutes_label(value: int | None) -> str | None:
    if value is None:
        return None
    hours = value // 60
    minutes = value % 60
    return f"{hours:02d}:{minutes:02d}"


def _build_frequency_label(habit: Habit) -> str:
    if habit.frequency_type == FrequencyType.DAILY:
        return "Every day"

    if habit.frequency_type == FrequencyType.WEEKLY:
        ordered_days = sorted((item.weekday for item in habit.weekdays), key=lambda item: DAY_OF_WEEK_ORDER[item])
        if not ordered_days:
            return "Weekly"
        labels = ", ".join(WEEKDAY_LABELS[DAY_OF_WEEK_ORDER[weekday]] for weekday in ordered_days)
        return f"Weekly: {labels}"

    if habit.frequency_type == FrequencyType.EVERY_N_DAYS and habit.every_n_days is not None:
        return f"Every {habit.every_n_days} days"

    return "Custom schedule"
