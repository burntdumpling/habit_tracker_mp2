from __future__ import annotations

from datetime import date
from uuid import UUID

from fastapi import HTTPException, status
from sqlalchemy import delete, select
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.orm import selectinload

from app.db.models import Habit, HabitTarget, HabitWeekday
from app.schemas.enums import DayOfWeek, DayPart, FrequencyType, HabitStatus
from app.schemas.habit import (
    FrequencyIn,
    HabitCreate,
    HabitOut,
    HabitTargetIn,
    HabitUpdate,
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

DAY_PART_ORDER = {
    DayPart.FULL_DAY: 0,
    DayPart.MORNING: 1,
    DayPart.DAY: 2,
    DayPart.EVENING: 3,
}


async def create_habit(db: AsyncSession, user_id: UUID, payload: HabitCreate) -> HabitOut:
    _validate_dates(payload.startDate, payload.endDate)
    _validate_frequency(payload.frequency)
    _validate_targets(payload.targets)

    habit = Habit(
        user_id=user_id,
        status=payload.status,
        name=payload.name,
        icon_id=payload.iconId,
        start_date=payload.startDate,
        end_date=payload.endDate,
        frequency_type=payload.frequency.type,
        every_n_days=payload.frequency.everyNDays,
    )
    db.add(habit)
    await db.flush()

    weekdays = _build_weekdays(habit.id, payload.frequency)
    if weekdays:
        db.add_all(weekdays)

    db.add_all(_build_targets(habit.id, payload.targets))

    await db.commit()
    created = await _get_habit_or_404(db, user_id, habit.id)
    return _map_habit_out(created)


async def update_habit(db: AsyncSession, user_id: UUID, habit_id: int, patch: HabitUpdate) -> HabitOut:
    habit = await _get_habit_or_404(db, user_id, habit_id)

    start_date = patch.startDate if patch.startDate is not None else habit.start_date
    end_date = patch.endDate if patch.endDate is not None else habit.end_date
    _validate_dates(start_date, end_date)

    if patch.status is not None:
        habit.status = patch.status
    if patch.name is not None:
        habit.name = patch.name
    if patch.iconId is not None:
        habit.icon_id = patch.iconId
    if patch.startDate is not None:
        habit.start_date = patch.startDate
    if patch.endDate is not None:
        habit.end_date = patch.endDate

    if patch.frequency is not None:
        _validate_frequency(patch.frequency)
        habit.frequency_type = patch.frequency.type
        habit.every_n_days = patch.frequency.everyNDays

        await db.execute(delete(HabitWeekday).where(HabitWeekday.habit_id == habit.id))
        new_weekdays = _build_weekdays(habit.id, patch.frequency)
        if new_weekdays:
            db.add_all(new_weekdays)

    if patch.targets is not None:
        _validate_targets(patch.targets)
        await db.execute(delete(HabitTarget).where(HabitTarget.habit_id == habit.id))
        db.add_all(_build_targets(habit.id, patch.targets))

    await db.commit()
    updated = await _get_habit_or_404(db, user_id, habit_id)
    return _map_habit_out(updated)


async def delete_habit(db: AsyncSession, user_id: UUID, habit_id: int) -> None:
    habit = await _get_habit_or_404(db, user_id, habit_id)
    await db.delete(habit)
    await db.commit()


async def list_habits(db: AsyncSession, user_id: UUID, status: HabitStatus | None) -> list[HabitOut]:
    query = (
        select(Habit)
        .options(selectinload(Habit.weekdays), selectinload(Habit.targets))
        .where(Habit.user_id == user_id)
    )
    if status is not None:
        query = query.where(Habit.status == status)

    query = query.order_by(Habit.created_at.desc(), Habit.id.desc())
    result = await db.execute(query)
    habits = result.scalars().all()
    return [_map_habit_out(habit) for habit in habits]


async def get_habit(db: AsyncSession, user_id: UUID, habit_id: int) -> HabitOut:
    habit = await _get_habit_or_404(db, user_id, habit_id)
    return _map_habit_out(habit)


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


def _validate_dates(start_date: date, end_date: date | None) -> None:
    if end_date is not None and start_date > end_date:
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail="startDate must be less than or equal to endDate",
        )


def _validate_frequency(frequency: FrequencyIn) -> None:
    week_days = frequency.weekDays or []

    if len(set(week_days)) != len(week_days):
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail="weekDays must not contain duplicates",
        )

    if frequency.type == FrequencyType.DAILY:
        if week_days or frequency.everyNDays is not None:
            raise HTTPException(
                status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
                detail="DAILY requires empty weekDays and null everyNDays",
            )
        return

    if frequency.type == FrequencyType.WEEKLY:
        if not week_days or frequency.everyNDays is not None:
            raise HTTPException(
                status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
                detail="WEEKLY requires non-empty weekDays and null everyNDays",
            )
        return

    if frequency.type == FrequencyType.EVERY_N_DAYS:
        if week_days or frequency.everyNDays is None or frequency.everyNDays < 1:
            raise HTTPException(
                status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
                detail="EVERY_N_DAYS requires everyNDays >= 1 and empty weekDays",
            )
        return

    raise HTTPException(
        status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
        detail="Invalid frequency type",
    )


def _validate_targets(targets: list[HabitTargetIn]) -> None:
    if not targets:
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail="targets must not be empty",
        )

    seen: set[DayPart] = set()
    for target in targets:
        if target.targetCount < 1:
            raise HTTPException(
                status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
                detail="targetCount must be greater than 0",
            )
        if target.dayPart in seen:
            raise HTTPException(
                status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
                detail="targets must not contain duplicate dayPart values",
            )
        seen.add(target.dayPart)

    if DayPart.FULL_DAY in seen:
        if len(targets) != 1 or targets[0].dayPart != DayPart.FULL_DAY:
            raise HTTPException(
                status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
                detail="FULL_DAY must be the only target dayPart",
            )
        return

    allowed_parts = {DayPart.MORNING, DayPart.DAY, DayPart.EVENING}
    if not seen.issubset(allowed_parts):
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail="targets dayPart values must be MORNING, DAY, EVENING, or FULL_DAY",
        )


def _build_weekdays(habit_id: int, frequency: FrequencyIn) -> list[HabitWeekday]:
    if frequency.type != FrequencyType.WEEKLY:
        return []
    return [HabitWeekday(habit_id=habit_id, weekday=weekday) for weekday in (frequency.weekDays or [])]


def _build_targets(habit_id: int, targets: list[HabitTargetIn]) -> list[HabitTarget]:
    return [
        HabitTarget(
            habit_id=habit_id,
            day_part=target.dayPart,
            target_count=target.targetCount,
        )
        for target in targets
    ]


def _map_habit_out(habit: Habit) -> HabitOut:
    week_days: list[DayOfWeek] | None = None
    if habit.frequency_type == FrequencyType.WEEKLY:
        week_days = sorted((weekday.weekday for weekday in habit.weekdays), key=lambda day: DAY_OF_WEEK_ORDER[day])

    targets = sorted(habit.targets, key=lambda item: DAY_PART_ORDER[item.day_part])

    data = {
        "id": habit.id,
        "status": habit.status,
        "name": habit.name,
        "icon_id": habit.icon_id,
        "created_at": habit.created_at,
        "start_date": habit.start_date,
        "end_date": habit.end_date,
        "frequency": {
            "type": habit.frequency_type,
            "weekDays": week_days,
            "everyNDays": habit.every_n_days,
        },
        "targets": [
            {
                "day_part": target.day_part,
                "target_count": target.target_count,
            }
            for target in targets
        ],
    }
    return HabitOut.model_validate(data)
