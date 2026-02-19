from datetime import date as dt_date, datetime

from pydantic import BaseModel, ConfigDict, Field

from app.schemas.enums import DayPart


class HabitLogUpsertIn(BaseModel):
    date: dt_date
    dayPart: DayPart
    amount: int = Field(ge=0)


class HabitLogOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    habitId: int = Field(validation_alias="habit_id")
    date: dt_date = Field(validation_alias="log_date")
    dayPart: DayPart = Field(validation_alias="day_part")
    amount: int = Field(ge=0)
    createdAt: datetime | None = Field(default=None, validation_alias="created_at")
    updatedAt: datetime | None = Field(default=None, validation_alias="updated_at")
