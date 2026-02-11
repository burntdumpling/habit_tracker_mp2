from fastapi import APIRouter, Depends, Response, status
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.deps import get_current_user, get_db
from app.db.models.user import User
from app.schemas.enums import HabitStatus
from app.schemas.habit import HabitCreate, HabitOut, HabitUpdate
from app.services.habits import create_habit, delete_habit, get_habit, list_habits, update_habit

router = APIRouter(prefix="/habits", tags=["habits"])


@router.get("", response_model=list[HabitOut], status_code=status.HTTP_200_OK)
async def list_habits_endpoint(
    status: HabitStatus | None = None,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
) -> list[HabitOut]:
    return await list_habits(db=db, user_id=current_user.id, status=status)


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
