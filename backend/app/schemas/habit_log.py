from __future__ import annotations

from datetime import date as date_type
from datetime import datetime

from pydantic import BaseModel, ConfigDict, Field

from app.schemas.enums import DayPart


class HabitLogUpsertIn(BaseModel):
    date: date_type
    dayPart: DayPart
    amount: int = Field(ge=0)


class HabitLogOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    habitId: int = Field(validation_alias="habit_id")
    date: date_type = Field(validation_alias="log_date")
    dayPart: DayPart = Field(validation_alias="day_part")
    amount: int = Field(ge=0)
    createdAt: datetime | None = Field(default=None, validation_alias="created_at")
    updatedAt: datetime | None = Field(default=None, validation_alias="updated_at")
