from __future__ import annotations

from datetime import date as date_type

from fastapi import APIRouter, Depends, status
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.deps import get_current_user, get_db
from app.db.models.user import User
from app.schemas.habit_stats import HabitStatsDetailsOut, StatsOverviewOut, StatsPeriod
from app.services.habit_stats import get_habit_stats_details, get_stats_overview

router = APIRouter(prefix="/stats", tags=["stats"])


@router.get(
    "/overview",
    response_model=StatsOverviewOut,
    status_code=status.HTTP_200_OK,
    description="Aggregated overview statistics for the selected period.",
)
async def get_stats_overview_endpoint(
    period: StatsPeriod = StatsPeriod.MONTH,
    anchor_date: date_type | None = None,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
) -> StatsOverviewOut:
    resolved_anchor_date = anchor_date or date_type.today()
    return await get_stats_overview(
        db=db,
        user_id=current_user.id,
        period=period,
        anchor_date=resolved_anchor_date,
    )


@router.get(
    "/habits/{habit_id}",
    response_model=HabitStatsDetailsOut,
    status_code=status.HTTP_200_OK,
    description="Aggregated statistics details for a single habit.",
)
async def get_habit_stats_details_endpoint(
    habit_id: int,
    period: StatsPeriod = StatsPeriod.MONTH,
    anchor_date: date_type | None = None,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
) -> HabitStatsDetailsOut:
    resolved_anchor_date = anchor_date or date_type.today()
    return await get_habit_stats_details(
        db=db,
        user_id=current_user.id,
        habit_id=habit_id,
        period=period,
        anchor_date=resolved_anchor_date,
    )
