from datetime import date

from fastapi import APIRouter, Depends, Response, status
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.deps import get_current_user, get_db
from app.db.models.user import User
from app.schemas.enums import DayPart, HabitStatus
from app.schemas.habit_calendar import CalendarDayOut, CalendarRangeOut
from app.schemas.habit import HabitCreate, HabitOut, HabitUpdate
from app.schemas.habit_log import HabitLogOut, HabitLogUpsertIn
from app.services.habit_calendar import get_calendar_day, get_calendar_range
from app.services.habit_logs import (
    delete_habit_log,
    list_habit_logs,
    list_user_habit_logs,
    upsert_habit_log,
)
from app.services.habits import create_habit, delete_habit, get_habit, list_habits, update_habit

router = APIRouter(prefix="/habits", tags=["habits"])


@router.get("", response_model=list[HabitOut], status_code=status.HTTP_200_OK)
async def list_habits_endpoint(
    status: HabitStatus | None = None,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
) -> list[HabitOut]:
    return await list_habits(db=db, user_id=current_user.id, status=status)


@router.get("/logs", response_model=list[HabitLogOut], status_code=status.HTTP_200_OK)
async def list_user_habit_logs_endpoint(
    dateFrom: date,
    dateTo: date,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
) -> list[HabitLogOut]:
    return await list_user_habit_logs(
        db=db,
        user_id=current_user.id,
        date_from=dateFrom,
        date_to=dateTo,
    )


@router.get(
    "/calendar/day",
    response_model=CalendarDayOut,
    status_code=status.HTTP_200_OK,
    description="Calendar view for a single date.",
)
async def get_calendar_day_endpoint(
    date: date,
    status: HabitStatus | None = None,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
) -> CalendarDayOut:
    return await get_calendar_day(
        db=db,
        user_id=current_user.id,
        date_=date,
        status=status,
    )


@router.get(
    "/calendar/range",
    response_model=CalendarRangeOut,
    status_code=status.HTTP_200_OK,
    description="Calendar view for a date range (inclusive). Range limited to 31 days.",
)
async def get_calendar_range_endpoint(
    dateFrom: date,
    dateTo: date,
    status: HabitStatus | None = None,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
) -> CalendarRangeOut:
    return await get_calendar_range(
        db=db,
        user_id=current_user.id,
        date_from=dateFrom,
        date_to=dateTo,
        status=status,
        max_days=31,
    )


@router.get("/{habit_id}", response_model=HabitOut, status_code=status.HTTP_200_OK)
async def get_habit_endpoint(
    habit_id: int,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
) -> HabitOut:
    return await get_habit(db=db, user_id=current_user.id, habit_id=habit_id)


@router.post("", response_model=HabitOut, status_code=status.HTTP_201_CREATED)
async def create_habit_endpoint(
    payload: HabitCreate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
) -> HabitOut:
    return await create_habit(db=db, user_id=current_user.id, payload=payload)


@router.patch("/{habit_id}", response_model=HabitOut, status_code=status.HTTP_200_OK)
async def update_habit_endpoint(
    habit_id: int,
    patch: HabitUpdate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
) -> HabitOut:
    return await update_habit(db=db, user_id=current_user.id, habit_id=habit_id, patch=patch)


@router.delete("/{habit_id}", status_code=status.HTTP_204_NO_CONTENT, response_class=Response)
async def delete_habit_endpoint(
    habit_id: int,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
) -> Response:
    await delete_habit(db=db, user_id=current_user.id, habit_id=habit_id)
    return Response(status_code=status.HTTP_204_NO_CONTENT)


@router.put(
    "/{habit_id}/logs",
    response_model=HabitLogOut,
    status_code=status.HTTP_200_OK,
    description=(
        "Create or update a habit log. Logs are allowed only for dayPart values defined in the habit targets; "
        "otherwise returns 422."
    ),
)
async def upsert_habit_log_endpoint(
    habit_id: int,
    payload: HabitLogUpsertIn,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
) -> HabitLogOut:
    return await upsert_habit_log(db=db, user_id=current_user.id, habit_id=habit_id, payload=payload)


@router.get("/{habit_id}/logs", response_model=list[HabitLogOut], status_code=status.HTTP_200_OK)
async def list_habit_logs_endpoint(
    habit_id: int,
    dateFrom: date,
    dateTo: date,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
) -> list[HabitLogOut]:
    return await list_habit_logs(
        db=db,
        user_id=current_user.id,
        habit_id=habit_id,
        date_from=dateFrom,
        date_to=dateTo,
    )


@router.delete(
    "/{habit_id}/logs",
    status_code=status.HTTP_204_NO_CONTENT,
    response_class=Response,
    description=(
        "Delete a habit log. dayPart must be one of the habit target dayPart values; otherwise returns 422."
    ),
)
async def delete_habit_log_endpoint(
    habit_id: int,
    date: date,
    dayPart: DayPart,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
) -> Response:
    await delete_habit_log(
        db=db,
        user_id=current_user.id,
        habit_id=habit_id,
        log_date=date,
        day_part=dayPart,
    )
    return Response(status_code=status.HTTP_204_NO_CONTENT)
