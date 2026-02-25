from datetime import date, timedelta

import pytest
from httpx import AsyncClient

DAY_OF_WEEK_NAMES = [
    "MONDAY",
    "TUESDAY",
    "WEDNESDAY",
    "THURSDAY",
    "FRIDAY",
    "SATURDAY",
    "SUNDAY",
]


async def _create_habit(
    client: AsyncClient,
    auth_headers: dict[str, str],
    *,
    name: str,
    status: str = "ACTIVE",
    icon_id: int = 1,
    color_argb: int = -16711936,
    start_date: date,
    end_date: date | None = None,
    frequency_type: str = "DAILY",
    week_days: list[str] | None = None,
    every_n_days: int | None = None,
    targets: list[dict[str, object]],
) -> int:
    payload = {
        "status": status,
        "name": name,
        "iconId": icon_id,
        "colorArgb": color_argb,
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


def _day_name_for(d: date) -> str:
    return DAY_OF_WEEK_NAMES[d.weekday()]


@pytest.mark.asyncio
async def test_calendar_day_returns_scheduled_habits_only(
    client: AsyncClient,
    auth_headers: dict[str, str],
) -> None:
    test_date = date(2026, 2, 18)

    daily_habit_id = await _create_habit(
        client,
        auth_headers,
        name="Daily Habit",
        start_date=test_date - timedelta(days=10),
        frequency_type="DAILY",
        targets=[
            {"dayPart": "MORNING", "targetCount": 1},
            {"dayPart": "EVENING", "targetCount": 1},
        ],
    )
    weekly_habit_id = await _create_habit(
        client,
        auth_headers,
        name="Weekly Habit",
        start_date=test_date - timedelta(days=10),
        frequency_type="WEEKLY",
        week_days=[_day_name_for(test_date)],
        targets=[{"dayPart": "FULL_DAY", "targetCount": 2}],
    )
    every_n_days_habit_id = await _create_habit(
        client,
        auth_headers,
        name="Every N Days Habit",
        start_date=test_date - timedelta(days=1),
        frequency_type="EVERY_N_DAYS",
        every_n_days=2,
        targets=[{"dayPart": "MORNING", "targetCount": 1}],
    )

    response = await client.get(
        "/habits/calendar/day",
        params={"date": test_date.isoformat()},
        headers=auth_headers,
    )

    assert response.status_code == 200
    body = response.json()
    returned_ids = {habit["habitId"] for habit in body["habits"]}
    assert daily_habit_id in returned_ids
    assert weekly_habit_id in returned_ids
    assert every_n_days_habit_id not in returned_ids


@pytest.mark.asyncio
async def test_calendar_day_actual_is_filled_with_zeros_for_missing_parts(
    client: AsyncClient,
    auth_headers: dict[str, str],
) -> None:
    test_date = date(2026, 2, 19)
    habit_id = await _create_habit(
        client,
        auth_headers,
        name="Partial Logs Habit",
        start_date=test_date - timedelta(days=3),
        frequency_type="DAILY",
        targets=[
            {"dayPart": "MORNING", "targetCount": 1},
            {"dayPart": "EVENING", "targetCount": 1},
        ],
    )

    upsert_response = await client.put(
        f"/habits/{habit_id}/logs",
        json={"date": test_date.isoformat(), "dayPart": "MORNING", "amount": 1},
        headers=auth_headers,
    )
    assert upsert_response.status_code == 200

    response = await client.get(
        "/habits/calendar/day",
        params={"date": test_date.isoformat()},
        headers=auth_headers,
    )
    assert response.status_code == 200

    body = response.json()
    habit = next(item for item in body["habits"] if item["habitId"] == habit_id)
    assert habit["actual"] == [
        {"dayPart": "MORNING", "amount": 1},
        {"dayPart": "EVENING", "amount": 0},
    ]
    assert habit["summary"]["targetTotal"] == 2
    assert habit["summary"]["actualTotal"] == 1
    assert habit["summary"]["isCompleted"] is False


@pytest.mark.asyncio
async def test_calendar_day_is_completed_requires_all_target_parts(
    client: AsyncClient,
    auth_headers: dict[str, str],
) -> None:
    test_date = date(2026, 2, 22)
    habit_id = await _create_habit(
        client,
        auth_headers,
        name="Strict Completion Habit",
        start_date=test_date - timedelta(days=5),
        frequency_type="DAILY",
        targets=[
            {"dayPart": "MORNING", "targetCount": 1},
            {"dayPart": "EVENING", "targetCount": 1},
        ],
    )

    upsert_response = await client.put(
        f"/habits/{habit_id}/logs",
        json={"date": test_date.isoformat(), "dayPart": "MORNING", "amount": 2},
        headers=auth_headers,
    )
    assert upsert_response.status_code == 200

    response = await client.get(
        "/habits/calendar/day",
        params={"date": test_date.isoformat()},
        headers=auth_headers,
    )
    assert response.status_code == 200

    body = response.json()
    habit = next(item for item in body["habits"] if item["habitId"] == habit_id)
    assert habit["summary"]["targetTotal"] == 2
    assert habit["summary"]["actualTotal"] == 2
    assert habit["summary"]["isCompleted"] is False


@pytest.mark.asyncio
async def test_calendar_range_max_days_31_enforced(
    client: AsyncClient,
    auth_headers: dict[str, str],
) -> None:
    response = await client.get(
        "/habits/calendar/range",
        params={"dateFrom": "2026-01-01", "dateTo": "2026-02-01"},
        headers=auth_headers,
    )

    assert response.status_code == 422


@pytest.mark.asyncio
async def test_calendar_range_returns_days_inclusive(
    client: AsyncClient,
    auth_headers: dict[str, str],
) -> None:
    response = await client.get(
        "/habits/calendar/range",
        params={"dateFrom": "2026-02-10", "dateTo": "2026-02-12"},
        headers=auth_headers,
    )

    assert response.status_code == 200
    body = response.json()
    assert body["dateFrom"] == "2026-02-10"
    assert body["dateTo"] == "2026-02-12"
    assert [day["date"] for day in body["days"]] == [
        "2026-02-10",
        "2026-02-11",
        "2026-02-12",
    ]
    assert len(body["days"]) == 3


@pytest.mark.asyncio
async def test_calendar_filters_by_status(
    client: AsyncClient,
    auth_headers: dict[str, str],
) -> None:
    test_date = date(2026, 2, 20)
    active_habit_id = await _create_habit(
        client,
        auth_headers,
        name="Active Habit",
        status="ACTIVE",
        start_date=test_date - timedelta(days=7),
        frequency_type="DAILY",
        targets=[{"dayPart": "MORNING", "targetCount": 1}],
    )
    finished_habit_id = await _create_habit(
        client,
        auth_headers,
        name="Finished Habit",
        status="FINISHED",
        start_date=test_date - timedelta(days=7),
        frequency_type="DAILY",
        targets=[{"dayPart": "MORNING", "targetCount": 1}],
    )

    active_response = await client.get(
        "/habits/calendar/day",
        params={"date": test_date.isoformat(), "status": "ACTIVE"},
        headers=auth_headers,
    )
    assert active_response.status_code == 200
    active_ids = {habit["habitId"] for habit in active_response.json()["habits"]}
    assert active_ids == {active_habit_id}

    finished_response = await client.get(
        "/habits/calendar/day",
        params={"date": test_date.isoformat(), "status": "FINISHED"},
        headers=auth_headers,
    )
    assert finished_response.status_code == 200
    finished_ids = {habit["habitId"] for habit in finished_response.json()["habits"]}
    assert finished_ids == {finished_habit_id}
