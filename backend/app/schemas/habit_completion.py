from __future__ import annotations

from datetime import date as date_type

from pydantic import BaseModel


class HabitDayCompletionOut(BaseModel):
    habitId: int
    date: date_type
    actualAmount: int
    targetCount: int
    isCompleted: bool
