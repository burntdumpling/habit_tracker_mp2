from datetime import date

from pydantic import BaseModel


class HabitDayCompletionOut(BaseModel):
    habitId: int
    date: date
    actualAmount: int
    targetCount: int
    isCompleted: bool
