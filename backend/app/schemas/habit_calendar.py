from __future__ import annotations

from datetime import date as date_type

from pydantic import BaseModel, ConfigDict, Field

from app.schemas.enums import DayPart, HabitStatus


class CalendarDayPartTargetOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    dayPart: DayPart = Field(validation_alias="day_part")
    targetCount: int = Field(validation_alias="target_count", ge=0)


class CalendarDayPartActualOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    dayPart: DayPart = Field(validation_alias="day_part")
    amount: int = Field(ge=0)


class CalendarHabitSummaryOut(BaseModel):
    targetTotal: int = Field(ge=0)
    actualTotal: int = Field(ge=0)
    isCompleted: bool


class CalendarHabitOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    habitId: int = Field(validation_alias="habit_id")
    name: str
    iconId: int = Field(validation_alias="icon_id")
    status: HabitStatus
    targets: list[CalendarDayPartTargetOut]
    actual: list[CalendarDayPartActualOut]
    summary: CalendarHabitSummaryOut


class CalendarDaySummaryOut(BaseModel):
    plannedCount: int = Field(ge=0)
    completedCount: int = Field(ge=0)
    hasAnyCompleted: bool


class CalendarDayOut(BaseModel):
    date: date_type
    summary: CalendarDaySummaryOut
    habits: list[CalendarHabitOut]


class CalendarRangeOut(BaseModel):
    dateFrom: date_type = Field(validation_alias="date_from")
    dateTo: date_type = Field(validation_alias="date_to")
    days: list[CalendarDayOut]
