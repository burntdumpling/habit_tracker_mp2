from enum import Enum


class HabitStatus(str, Enum):
    ACTIVE = "ACTIVE"
    PAUSED = "PAUSED"
    FINISHED = "FINISHED"


class FrequencyType(str, Enum):
    DAILY = "DAILY"
    WEEKLY = "WEEKLY"
    EVERY_N_DAYS = "EVERY_N_DAYS"


class DayPart(str, Enum):
    MORNING = "MORNING"
    DAY = "DAY"
    EVENING = "EVENING"
    FULL_DAY = "FULL_DAY"


class DayOfWeek(str, Enum):
    MONDAY = "MONDAY"
    TUESDAY = "TUESDAY"
    WEDNESDAY = "WEDNESDAY"
    THURSDAY = "THURSDAY"
    FRIDAY = "FRIDAY"
    SATURDAY = "SATURDAY"
    SUNDAY = "SUNDAY"
