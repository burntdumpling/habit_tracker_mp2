import pytest
from httpx import AsyncClient


@pytest.mark.asyncio
async def test_habit_completion_basic(
    client: AsyncClient,
    auth_headers: dict[str, str],
    create_habit,
) -> None:
    habit_id = await create_habit([{"dayPart": "FULL_DAY", "targetCount": 5}])

    first_upsert = await client.put(
        f"/habits/{habit_id}/logs",
        json={"date": "2026-02-18", "dayPart": "FULL_DAY", "amount": 5},
        headers=auth_headers,
    )
    assert first_upsert.status_code == 200

    second_upsert = await client.put(
        f"/habits/{habit_id}/logs",
        json={"date": "2026-02-19", "dayPart": "FULL_DAY", "amount": 3},
        headers=auth_headers,
    )
    assert second_upsert.status_code == 200

    response = await client.get(
        f"/habits/{habit_id}/completion",
        params={"dateFrom": "2026-02-18", "dateTo": "2026-02-19"},
        headers=auth_headers,
    )
    assert response.status_code == 200
    body = response.json()
    assert len(body) == 2

    first_day, second_day = body
    assert first_day["date"] == "2026-02-18"
    assert first_day["actualAmount"] == 5
    assert first_day["targetCount"] == 5
    assert first_day["isCompleted"] is True

    assert second_day["date"] == "2026-02-19"
    assert second_day["actualAmount"] == 3
    assert second_day["targetCount"] == 5
    assert second_day["isCompleted"] is False


@pytest.mark.asyncio
async def test_habit_completion_no_logs_returns_zero(
    client: AsyncClient,
    auth_headers: dict[str, str],
    create_habit,
) -> None:
    habit_id = await create_habit(
        [
            {"dayPart": "MORNING", "targetCount": 1},
            {"dayPart": "EVENING", "targetCount": 2},
        ]
    )

    response = await client.get(
        f"/habits/{habit_id}/completion",
        params={"dateFrom": "2026-02-20", "dateTo": "2026-02-20"},
        headers=auth_headers,
    )
    assert response.status_code == 200
    body = response.json()
    assert len(body) == 1

    day = body[0]
    assert day["date"] == "2026-02-20"
    assert day["actualAmount"] == 0
    assert day["targetCount"] == 3
    assert day["isCompleted"] is False


@pytest.mark.asyncio
async def test_user_completion_multiple_habits(
    client: AsyncClient,
    auth_headers: dict[str, str],
    create_habit,
) -> None:
    first_habit_id = await create_habit([{"dayPart": "FULL_DAY", "targetCount": 2}])
    second_habit_id = await create_habit(
        [
            {"dayPart": "MORNING", "targetCount": 1},
            {"dayPart": "EVENING", "targetCount": 1},
        ]
    )

    first_log = await client.put(
        f"/habits/{first_habit_id}/logs",
        json={"date": "2026-02-21", "dayPart": "FULL_DAY", "amount": 2},
        headers=auth_headers,
    )
    assert first_log.status_code == 200

    second_log = await client.put(
        f"/habits/{second_habit_id}/logs",
        json={"date": "2026-02-21", "dayPart": "MORNING", "amount": 1},
        headers=auth_headers,
    )
    assert second_log.status_code == 200

    response = await client.get(
        "/habits/completion",
        params={"dateFrom": "2026-02-21", "dateTo": "2026-02-21"},
        headers=auth_headers,
    )
    assert response.status_code == 200
    body = response.json()
    assert len(body) == 2

    by_habit = {item["habitId"]: item for item in body}
    assert by_habit[first_habit_id]["actualAmount"] == 2
    assert by_habit[first_habit_id]["targetCount"] == 2
    assert by_habit[first_habit_id]["isCompleted"] is True

    assert by_habit[second_habit_id]["actualAmount"] == 1
    assert by_habit[second_habit_id]["targetCount"] == 2
    assert by_habit[second_habit_id]["isCompleted"] is False


@pytest.mark.asyncio
async def test_completion_invalid_date_range_returns_422(
    client: AsyncClient,
    auth_headers: dict[str, str],
    create_habit,
) -> None:
    habit_id = await create_habit([{"dayPart": "FULL_DAY", "targetCount": 1}])

    habit_response = await client.get(
        f"/habits/{habit_id}/completion",
        params={"dateFrom": "2026-02-25", "dateTo": "2026-02-20"},
        headers=auth_headers,
    )
    assert habit_response.status_code == 422
    assert habit_response.json() == {"detail": "dateFrom must be less than or equal to dateTo"}

    user_response = await client.get(
        "/habits/completion",
        params={"dateFrom": "2026-02-25", "dateTo": "2026-02-20"},
        headers=auth_headers,
    )
    assert user_response.status_code == 422
    assert user_response.json() == {"detail": "dateFrom must be less than or equal to dateTo"}
