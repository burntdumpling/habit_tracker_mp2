from __future__ import annotations

from datetime import date
from enum import Enum

from pydantic import BaseModel, ConfigDict, Field


class StatsPeriod(str, Enum):
    WEEK = "WEEK"
    MONTH = "MONTH"
    YEAR = "YEAR"


class StatsPointOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    key: str
    label: str
    completed: int = Field(ge=0)
    expected: int = Field(ge=0)
    value: float
    is_future: bool = False


class HabitStatsOverviewCardOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    habit_id: int
    name: str
    icon: str | None = None
    color: str | None = None
    completion_rate: int = Field(ge=0, le=100)
    points: list[StatsPointOut]


class StatsOverviewOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    period: StatsPeriod
    anchor_date: date
    range_start: date
    range_end: date
    title: str
    subtitle: str
    cards: list[HabitStatsOverviewCardOut]


class HabitStatsSummaryOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    current_streak: int = Field(ge=0)
    best_streak: int = Field(ge=0)
    goals_met: int = Field(ge=0)


class HabitCalendarDayMarkerOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    date: date
    is_scheduled: bool
    is_completed: bool
    has_progress: bool


class HabitMonthCalendarOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    month: int = Field(ge=1, le=12)
    year: int = Field(ge=1)
    days: list[HabitCalendarDayMarkerOut]


class TimeOfDayPointOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    day_label: str
    average_minutes: int | None = Field(default=None, ge=0)


class HabitTimeOfDayStatsOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    week_average_minutes: int | None = Field(default=None, ge=0)
    week_average_label: str | None = None
    points: list[TimeOfDayPointOut]


class HabitStatsDetailsOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    habit_id: int
    name: str
    icon: str | None = None
    color: str | None = None
    frequency_label: str
    period: StatsPeriod
    anchor_date: date
    range_start: date
    range_end: date
    title: str
    subtitle: str
    completion_rate: int = Field(ge=0, le=100)
    entries_points: list[StatsPointOut]
    summary: HabitStatsSummaryOut
    calendar: HabitMonthCalendarOut
    time_of_day: HabitTimeOfDayStatsOut
