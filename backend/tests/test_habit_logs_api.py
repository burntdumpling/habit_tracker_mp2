import asyncio
from datetime import datetime

import pytest
from httpx import AsyncClient


@pytest.mark.asyncio
async def test_upsert_insert(client: AsyncClient, auth_headers: dict[str, str], habit_id: int) -> None:
    response = await client.put(
        f"/habits/{habit_id}/logs",
        json={"date": "2026-02-15", "dayPart": "MORNING", "amount": 1},
        headers=auth_headers,
    )

    assert response.status_code == 200
    body = response.json()
    assert set(body.keys()) == {"habitId", "date", "dayPart", "amount", "createdAt", "updatedAt"}
    assert body["habitId"] == habit_id
    assert body["date"] == "2026-02-15"
    assert body["dayPart"] == "MORNING"
    assert body["amount"] == 1
    assert body["createdAt"] is not None
    assert body["updatedAt"] is not None


@pytest.mark.asyncio
async def test_upsert_update_same_key_updates_amount_and_updated_at(
    client: AsyncClient,
    auth_headers: dict[str, str],
    habit_id: int,
) -> None:
    first_response = await client.put(
        f"/habits/{habit_id}/logs",
        json={"date": "2026-02-16", "dayPart": "DAY", "amount": 1},
        headers=auth_headers,
    )
    assert first_response.status_code == 200
    first = first_response.json()

    await asyncio.sleep(0.02)

    second_response = await client.put(
        f"/habits/{habit_id}/logs",
        json={"date": "2026-02-16", "dayPart": "DAY", "amount": 5},
        headers=auth_headers,
    )
    assert second_response.status_code == 200
    second = second_response.json()

    assert second["amount"] == 5
    assert second["createdAt"] == first["createdAt"]
    assert datetime.fromisoformat(second["updatedAt"]) >= datetime.fromisoformat(first["updatedAt"])


@pytest.mark.asyncio
async def test_list_ordering_by_date_and_day_part(
    client: AsyncClient,
    auth_headers: dict[str, str],
    habit_id: int,
) -> None:
    logs_to_create = [
        {"date": "2026-02-11", "dayPart": "EVENING", "amount": 1},
        {"date": "2026-02-10", "dayPart": "MORNING", "amount": 1},
        {"date": "2026-02-10", "dayPart": "DAY", "amount": 1},
    ]
    for payload in logs_to_create:
        response = await client.put(f"/habits/{habit_id}/logs", json=payload, headers=auth_headers)
        assert response.status_code == 200

    response = await client.get(
        f"/habits/{habit_id}/logs",
        params={"dateFrom": "2026-02-10", "dateTo": "2026-02-11"},
        headers=auth_headers,
    )

    assert response.status_code == 200
    body = response.json()
    ordered_pairs = [(item["date"], item["dayPart"]) for item in body]
    assert ordered_pairs == [
        ("2026-02-10", "MORNING"),
        ("2026-02-10", "DAY"),
        ("2026-02-11", "EVENING"),
    ]


@pytest.mark.asyncio
async def test_delete_habit_log_ok(client: AsyncClient, auth_headers: dict[str, str], habit_id: int) -> None:
    create_response = await client.put(
        f"/habits/{habit_id}/logs",
        json={"date": "2026-02-18", "dayPart": "MORNING", "amount": 2},
        headers=auth_headers,
    )
    assert create_response.status_code == 200

    delete_response = await client.delete(
        f"/habits/{habit_id}/logs",
        params={"date": "2026-02-18", "dayPart": "MORNING"},
        headers=auth_headers,
    )
    assert delete_response.status_code == 204


@pytest.mark.asyncio
async def test_delete_habit_log_missing_returns_404(
    client: AsyncClient,
    auth_headers: dict[str, str],
    habit_id: int,
) -> None:
    response = await client.delete(
        f"/habits/{habit_id}/logs",
        params={"date": "2026-02-18", "dayPart": "MORNING"},
        headers=auth_headers,
    )

    assert response.status_code == 404
    assert response.json() == {"detail": "Habit log not found"}


@pytest.mark.asyncio
async def test_list_habit_logs_invalid_date_range_returns_422(
    client: AsyncClient,
    auth_headers: dict[str, str],
    habit_id: int,
) -> None:
    response = await client.get(
        f"/habits/{habit_id}/logs",
        params={"dateFrom": "2026-02-20", "dateTo": "2026-02-10"},
        headers=auth_headers,
    )

    assert response.status_code == 422
    assert response.json() == {"detail": "dateFrom must be less than or equal to dateTo"}


@pytest.mark.asyncio
async def test_upsert_rejects_day_part_not_allowed_for_full_day_target(
    client: AsyncClient,
    auth_headers: dict[str, str],
    create_habit,
) -> None:
    full_day_habit_id = await create_habit([{"dayPart": "FULL_DAY", "targetCount": 1}])

    response = await client.put(
        f"/habits/{full_day_habit_id}/logs",
        json={"date": "2026-02-19", "dayPart": "MORNING", "amount": 1},
        headers=auth_headers,
    )

    assert response.status_code == 422
    assert response.json() == {"detail": "dayPart not allowed for this habit"}


@pytest.mark.asyncio
async def test_upsert_rejects_day_parts_not_in_partial_targets(
    client: AsyncClient,
    auth_headers: dict[str, str],
    create_habit,
) -> None:
    partial_habit_id = await create_habit(
        [
            {"dayPart": "MORNING", "targetCount": 1},
            {"dayPart": "EVENING", "targetCount": 1},
        ]
    )

    day_response = await client.put(
        f"/habits/{partial_habit_id}/logs",
        json={"date": "2026-02-19", "dayPart": "DAY", "amount": 1},
        headers=auth_headers,
    )
    assert day_response.status_code == 422
    assert day_response.json() == {"detail": "dayPart not allowed for this habit"}

    full_day_response = await client.put(
        f"/habits/{partial_habit_id}/logs",
        json={"date": "2026-02-19", "dayPart": "FULL_DAY", "amount": 1},
        headers=auth_headers,
    )
    assert full_day_response.status_code == 422
    assert full_day_response.json() == {"detail": "dayPart not allowed for this habit"}


@pytest.mark.asyncio
async def test_second_user_cannot_upsert_log_for_other_users_habit(
    client: AsyncClient,
    second_user_auth_headers: dict[str, str],
    create_habit,
) -> None:
    habit_id = await create_habit(
        targets=[{"dayPart": "FULL_DAY", "targetCount": 1}],
    )

    response = await client.put(
        f"/habits/{habit_id}/logs",
        json={"date": "2026-02-21", "dayPart": "FULL_DAY", "amount": 1},
        headers=second_user_auth_headers,
    )

    assert response.status_code == 404
    assert response.json() == {"detail": "Habit not found"}


@pytest.mark.asyncio
async def test_second_user_cannot_list_logs_for_other_users_habit(
    client: AsyncClient,
    second_user_auth_headers: dict[str, str],
    create_habit,
    log_factory,
) -> None:
    habit_id = await create_habit(
        targets=[{"dayPart": "FULL_DAY", "targetCount": 1}],
    )
    await log_factory(habit_id=habit_id, log_date="2026-02-21", day_part="FULL_DAY", amount=1)

    response = await client.get(
        f"/habits/{habit_id}/logs",
        params={"dateFrom": "2026-02-21", "dateTo": "2026-02-21"},
        headers=second_user_auth_headers,
    )

    assert response.status_code == 404
    assert response.json() == {"detail": "Habit not found"}


@pytest.mark.asyncio
async def test_second_user_cannot_delete_log_for_other_users_habit(
    client: AsyncClient,
    second_user_auth_headers: dict[str, str],
    create_habit,
    log_factory,
) -> None:
    habit_id = await create_habit(
        targets=[{"dayPart": "FULL_DAY", "targetCount": 1}],
    )
    await log_factory(habit_id=habit_id, log_date="2026-02-21", day_part="FULL_DAY", amount=1)

    response = await client.delete(
        f"/habits/{habit_id}/logs",
        params={"date": "2026-02-21", "dayPart": "FULL_DAY"},
        headers=second_user_auth_headers,
    )

    assert response.status_code == 404
    assert response.json() == {"detail": "Habit not found"}


@pytest.mark.asyncio
async def test_list_user_habit_logs_returns_empty_list(
    client: AsyncClient,
    auth_headers: dict[str, str],
) -> None:
    response = await client.get(
        "/habits/logs",
        params={"dateFrom": "2026-02-10", "dateTo": "2026-02-12"},
        headers=auth_headers,
    )

    assert response.status_code == 200
    assert response.json() == []


@pytest.mark.asyncio
async def test_list_user_habit_logs_returns_logs_across_multiple_habits_in_sorted_order(
    client: AsyncClient,
    auth_headers: dict[str, str],
    create_habit,
    log_factory,
) -> None:
    morning_habit_id = await create_habit(
        name="Morning Habit",
        targets=[{"dayPart": "MORNING", "targetCount": 1}],
    )
    full_day_habit_id = await create_habit(
        name="Full Day Habit",
        targets=[{"dayPart": "FULL_DAY", "targetCount": 1}],
    )

    await log_factory(habit_id=full_day_habit_id, log_date="2026-02-11", day_part="FULL_DAY", amount=1)
    await log_factory(habit_id=morning_habit_id, log_date="2026-02-10", day_part="MORNING", amount=1)

    response = await client.get(
        "/habits/logs",
        params={"dateFrom": "2026-02-10", "dateTo": "2026-02-11"},
        headers=auth_headers,
    )

    assert response.status_code == 200
    body = response.json()
    assert [(item["habitId"], item["date"], item["dayPart"]) for item in body] == [
        (morning_habit_id, "2026-02-10", "MORNING"),
        (full_day_habit_id, "2026-02-11", "FULL_DAY"),
    ]


@pytest.mark.asyncio
async def test_list_user_habit_logs_sorts_by_date_and_day_part(
    client: AsyncClient,
    auth_headers: dict[str, str],
    create_habit,
    log_factory,
) -> None:
    habit_id = await create_habit(
        targets=[
            {"dayPart": "MORNING", "targetCount": 1},
            {"dayPart": "DAY", "targetCount": 1},
            {"dayPart": "EVENING", "targetCount": 1},
        ]
    )

    await log_factory(habit_id=habit_id, log_date="2026-02-11", day_part="EVENING", amount=1)
    await log_factory(habit_id=habit_id, log_date="2026-02-10", day_part="DAY", amount=1)
    await log_factory(habit_id=habit_id, log_date="2026-02-10", day_part="MORNING", amount=1)

    response = await client.get(
        "/habits/logs",
        params={"dateFrom": "2026-02-10", "dateTo": "2026-02-11"},
        headers=auth_headers,
    )

    assert response.status_code == 200
    assert [(item["date"], item["dayPart"]) for item in response.json()] == [
        ("2026-02-10", "MORNING"),
        ("2026-02-10", "DAY"),
        ("2026-02-11", "EVENING"),
    ]


@pytest.mark.asyncio
async def test_list_user_habit_logs_does_not_include_other_users_logs(
    client: AsyncClient,
    auth_headers: dict[str, str],
    second_user_auth_headers: dict[str, str],
    create_habit,
    log_factory,
) -> None:
    own_habit_id = await create_habit(
        name="Own Habit",
        targets=[{"dayPart": "FULL_DAY", "targetCount": 1}],
    )
    other_habit_id = await create_habit(
        auth=second_user_auth_headers,
        name="Other Habit",
        targets=[{"dayPart": "FULL_DAY", "targetCount": 1}],
    )

    await log_factory(habit_id=own_habit_id, log_date="2026-02-12", day_part="FULL_DAY", amount=1)
    await log_factory(
        habit_id=other_habit_id,
        auth=second_user_auth_headers,
        log_date="2026-02-12",
        day_part="FULL_DAY",
        amount=1,
    )

    response = await client.get(
        "/habits/logs",
        params={"dateFrom": "2026-02-12", "dateTo": "2026-02-12"},
        headers=auth_headers,
    )

    assert response.status_code == 200
    body = response.json()
    assert len(body) == 1
    assert body[0]["habitId"] == own_habit_id


@pytest.mark.asyncio
async def test_list_user_habit_logs_invalid_date_range_returns_422(
    client: AsyncClient,
    auth_headers: dict[str, str],
) -> None:
    response = await client.get(
        "/habits/logs",
        params={"dateFrom": "2026-02-20", "dateTo": "2026-02-10"},
        headers=auth_headers,
    )

    assert response.status_code == 422
    assert response.json() == {"detail": "dateFrom must be less than or equal to dateTo"}
