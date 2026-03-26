from __future__ import annotations

from datetime import date, timedelta
import uuid

import pytest
from httpx import AsyncClient


async def _create_auth_headers(client: AsyncClient) -> dict[str, str]:
    email = f"stats-user-{uuid.uuid4()}@example.com"
    password = "strongpass123"

    register_response = await client.post(
        "/auth/register",
        json={"email": email, "password": password},
    )
    assert register_response.status_code == 201

    token_response = await client.post(
        "/auth/token",
        data={"username": email, "password": password},
        headers={"Content-Type": "application/x-www-form-urlencoded"},
    )
    assert token_response.status_code == 200

    token = token_response.json()["access_token"]
    return {"Authorization": f"Bearer {token}"}


async def _create_habit(
    client: AsyncClient,
    auth_headers: dict[str, str],
    *,
    name: str,
    start_date: date,
    end_date: date | None = None,
    frequency_type: str = "DAILY",
    week_days: list[str] | None = None,
    every_n_days: int | None = None,
    targets: list[dict[str, object]],
) -> int:
    payload = {
        "name": name,
        "iconId": 1,
        "colorArgb": -16711936,
        "startDate": start_date.isoformat(),
        "endDate": end_date.isoformat() if end_date is not None else None,
        "frequency": {
            "type": frequency_type,
            "weekDays": week_days,
            "everyNDays": every_n_days,
        },
        "targets": targets,
    }
    response = await client.post("/habits", json=payload, headers=auth_headers)
    assert response.status_code == 201
    return response.json()["habitId"]


def _assert_stats_point_shape(point: dict[str, object]) -> None:
    assert set(point) == {"key", "label", "completed", "expected", "value", "is_future"}


@pytest.mark.asyncio
async def test_stats_overview_endpoint_returns_real_week_response(
    client: AsyncClient,
    auth_headers: dict[str, str],
) -> None:
    today = date.today()
    habit_id = await _create_habit(
        client,
        auth_headers,
        name="Overview Habit",
        start_date=today,
        targets=[{"dayPart": "FULL_DAY", "targetCount": 1}],
    )

    log_response = await client.put(
        f"/habits/{habit_id}/logs",
        json={"date": today.isoformat(), "dayPart": "FULL_DAY", "amount": 1},
        headers=auth_headers,
    )
    assert log_response.status_code == 200

    response = await client.get(
        "/stats/overview",
        params={"period": "WEEK", "anchor_date": today.isoformat()},
        headers=auth_headers,
    )

    assert response.status_code == 200
    body = response.json()
    assert set(body) == {"period", "anchor_date", "range_start", "range_end", "title", "subtitle", "cards"}
    assert body["period"] == "WEEK"
    assert body["anchor_date"] == today.isoformat()
    assert body["title"] == "THIS WEEK"
    assert len(body["cards"]) == 1

    card = body["cards"][0]
    assert set(card) == {"habit_id", "name", "icon", "color", "completion_rate", "points"}
    assert card["habit_id"] == habit_id
    assert card["name"] == "Overview Habit"
    assert card["icon"] is None
    assert card["color"] is None
    assert card["completion_rate"] == 100
    assert len(card["points"]) == 7
    for point in card["points"]:
        _assert_stats_point_shape(point)


@pytest.mark.asyncio
async def test_stats_overview_endpoint_requires_authentication(client: AsyncClient) -> None:
    response = await client.get(
        "/stats/overview",
        params={"period": "WEEK", "anchor_date": date.today().isoformat()},
    )

    assert response.status_code == 401


@pytest.mark.asyncio
async def test_habit_stats_details_endpoint_returns_real_response_for_owned_habit(
    client: AsyncClient,
    auth_headers: dict[str, str],
) -> None:
    today = date.today()
    partial_date = today - timedelta(days=1)
    habit_id = await _create_habit(
        client,
        auth_headers,
        name="Details Habit",
        start_date=partial_date,
        targets=[
            {"dayPart": "MORNING", "targetCount": 1},
            {"dayPart": "EVENING", "targetCount": 1},
        ],
    )

    partial_log = await client.put(
        f"/habits/{habit_id}/logs",
        json={"date": partial_date.isoformat(), "dayPart": "MORNING", "amount": 1},
        headers=auth_headers,
    )
    assert partial_log.status_code == 200

    morning_log = await client.put(
        f"/habits/{habit_id}/logs",
        json={"date": today.isoformat(), "dayPart": "MORNING", "amount": 1},
        headers=auth_headers,
    )
    evening_log = await client.put(
        f"/habits/{habit_id}/logs",
        json={"date": today.isoformat(), "dayPart": "EVENING", "amount": 1},
        headers=auth_headers,
    )
    assert morning_log.status_code == 200
    assert evening_log.status_code == 200

    response = await client.get(
        f"/stats/habits/{habit_id}",
        params={"period": "WEEK", "anchor_date": today.isoformat()},
        headers=auth_headers,
    )

    assert response.status_code == 200
    body = response.json()
    assert set(body) == {
        "habit_id",
        "name",
        "icon",
        "color",
        "frequency_label",
        "period",
        "anchor_date",
        "range_start",
        "range_end",
        "title",
        "subtitle",
        "completion_rate",
        "entries_points",
        "summary",
        "calendar",
        "time_of_day",
    }
    assert body["habit_id"] == habit_id
    assert body["name"] == "Details Habit"
    assert body["period"] == "WEEK"
    assert body["anchor_date"] == today.isoformat()
    assert body["title"] == "THIS WEEK"
    assert len(body["entries_points"]) == 7
    for point in body["entries_points"]:
        _assert_stats_point_shape(point)

    assert set(body["summary"]) == {"current_streak", "best_streak", "goals_met"}
    assert set(body["calendar"]) == {"month", "year", "days"}
    assert body["calendar"]["month"] == today.month
    assert body["calendar"]["year"] == today.year
    assert any(
        set(day) == {"date", "is_scheduled", "is_completed", "has_progress"}
        for day in body["calendar"]["days"]
    )
    partial_marker = next(day for day in body["calendar"]["days"] if day["date"] == partial_date.isoformat())
    assert partial_marker["is_scheduled"] is True
    assert partial_marker["is_completed"] is False
    assert partial_marker["has_progress"] is True

    assert set(body["time_of_day"]) == {"week_average_minutes", "week_average_label", "points"}
    assert len(body["time_of_day"]["points"]) == 7
    assert any(
        set(point) == {"day_label", "average_minutes"}
        for point in body["time_of_day"]["points"]
    )


@pytest.mark.asyncio
async def test_habit_stats_details_endpoint_returns_404_for_non_owned_habit(
    client: AsyncClient,
    auth_headers: dict[str, str],
) -> None:
    today = date.today()
    habit_id = await _create_habit(
        client,
        auth_headers,
        name="Private Habit",
        start_date=today,
        targets=[{"dayPart": "FULL_DAY", "targetCount": 1}],
    )
    other_auth_headers = await _create_auth_headers(client)

    response = await client.get(
        f"/stats/habits/{habit_id}",
        params={"period": "WEEK", "anchor_date": today.isoformat()},
        headers=other_auth_headers,
    )

    assert response.status_code == 404
    assert response.json() == {"detail": "Habit not found"}


@pytest.mark.asyncio
async def test_habit_stats_details_endpoint_requires_authentication(
    client: AsyncClient,
    auth_headers: dict[str, str],
) -> None:
    habit_id = await _create_habit(
        client,
        auth_headers,
        name="Auth Habit",
        start_date=date.today(),
        targets=[{"dayPart": "FULL_DAY", "targetCount": 1}],
    )

    response = await client.get(
        f"/stats/habits/{habit_id}",
        params={"period": "WEEK", "anchor_date": date.today().isoformat()},
    )

    assert response.status_code == 401
