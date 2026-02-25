from datetime import date, datetime

from pydantic import BaseModel, ConfigDict, Field

from app.schemas.enums import DayOfWeek, DayPart, FrequencyType, HabitStatus


class FrequencyIn(BaseModel):
    type: FrequencyType
    weekDays: list[DayOfWeek] | None = None
    everyNDays: int | None = Field(default=None, gt=0)


class FrequencyOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    type: FrequencyType
    weekDays: list[DayOfWeek] | None = None
    everyNDays: int | None = None


class HabitTargetIn(BaseModel):
    dayPart: DayPart
    targetCount: int = Field(gt=0)


class HabitTargetOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    dayPart: DayPart = Field(validation_alias="day_part")
    targetCount: int = Field(validation_alias="target_count", gt=0)


class HabitCreate(BaseModel):
    status: HabitStatus = HabitStatus.ACTIVE
    name: str
    iconId: int
    colorArgb: int = Field(ge=-2147483648, le=2147483647)
    startDate: date
    endDate: date | None = None
    frequency: FrequencyIn
    targets: list[HabitTargetIn]


class HabitUpdate(BaseModel):
    status: HabitStatus | None = None
    name: str | None = None
    iconId: int | None = None
    colorArgb: int | None = Field(default=None, ge=-2147483648, le=2147483647)
    startDate: date | None = None
    endDate: date | None = None
    frequency: FrequencyIn | None = None
    targets: list[HabitTargetIn] | None = None


class HabitOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    habitId: int = Field(validation_alias="id")
    status: HabitStatus
    name: str
    iconId: int = Field(validation_alias="icon_id")
    colorArgb: int = Field(validation_alias="color_argb", ge=-2147483648, le=2147483647)
    createdAt: datetime = Field(validation_alias="created_at")
    startDate: date = Field(validation_alias="start_date")
    endDate: date | None = Field(default=None, validation_alias="end_date")
    frequency: FrequencyOut
    targets: list[HabitTargetOut]
