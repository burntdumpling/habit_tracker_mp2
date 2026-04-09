from datetime import date

import pytest
from httpx import AsyncClient


def _habit_payload(
    *,
    status: str = "ACTIVE",
    name: str = "Drink Water",
    icon_id: int = 7,
    color_argb: int = -65536,
    start_date: str = "2026-02-20",
    end_date: str | None = None,
    frequency: dict[str, object] | None = None,
    targets: list[dict[str, object]] | None = None,
) -> dict[str, object]:
    resolved_frequency = (
        frequency
        if frequency is not None
        else {
            "type": "DAILY",
            "weekDays": None,
            "everyNDays": None,
        }
    )
    resolved_targets = (
        targets
        if targets is not None
        else [
            {"dayPart": "FULL_DAY", "targetCount": 1},
        ]
    )

    return {
        "status": status,
        "name": name,
        "iconId": icon_id,
        "colorArgb": color_argb,
        "startDate": start_date,
        "endDate": end_date,
        "frequency": resolved_frequency,
        "targets": resolved_targets,
    }


@pytest.mark.asyncio
async def test_create_habit_returns_color_argb(
    client: AsyncClient,
    auth_headers: dict[str, str],
) -> None:
    response = await client.post(
        "/habits",
        json=_habit_payload(color_argb=-65536, targets=[{"dayPart": "FULL_DAY", "targetCount": 8}]),
        headers=auth_headers,
    )

    assert response.status_code == 201
    body = response.json()
    assert body["name"] == "Drink Water"
    assert body["iconId"] == 7
    assert body["colorArgb"] == -65536


@pytest.mark.asyncio
async def test_list_habits_returns_empty_list(
    client: AsyncClient,
    auth_headers: dict[str, str],
) -> None:
    response = await client.get("/habits", headers=auth_headers)

    assert response.status_code == 200
    assert response.json() == []


@pytest.mark.asyncio
async def test_list_habits_returns_created_habits(
    client: AsyncClient,
    auth_headers: dict[str, str],
    create_habit,
) -> None:
    first_habit_id = await create_habit(
        name="Read",
        targets=[{"dayPart": "FULL_DAY", "targetCount": 1}],
    )
    second_habit_id = await create_habit(
        name="Run",
        targets=[{"dayPart": "MORNING", "targetCount": 2}],
    )

    response = await client.get("/habits", headers=auth_headers)

    assert response.status_code == 200
    body = response.json()
    assert len(body) == 2
    assert {habit["habitId"] for habit in body} == {first_habit_id, second_habit_id}
    assert {habit["name"] for habit in body} == {"Read", "Run"}


@pytest.mark.asyncio
async def test_list_habits_supports_status_filter(
    client: AsyncClient,
    auth_headers: dict[str, str],
    create_habit,
) -> None:
    active_habit_id = await create_habit(name="Active Habit", status="ACTIVE")
    finished_habit_id = await create_habit(name="Finished Habit", status="FINISHED")

    active_response = await client.get("/habits", params={"status": "ACTIVE"}, headers=auth_headers)
    finished_response = await client.get("/habits", params={"status": "FINISHED"}, headers=auth_headers)

    assert active_response.status_code == 200
    assert {habit["habitId"] for habit in active_response.json()} == {active_habit_id}

    assert finished_response.status_code == 200
    assert {habit["habitId"] for habit in finished_response.json()} == {finished_habit_id}


@pytest.mark.asyncio
async def test_get_habit_returns_happy_path(
    client: AsyncClient,
    auth_headers: dict[str, str],
    create_habit,
) -> None:
    habit_id = await create_habit(
        name="Read Book",
        icon_id=3,
        color_argb=-256,
        start_date=date(2026, 2, 1),
        end_date=date(2026, 2, 28),
        frequency={
            "type": "WEEKLY",
            "weekDays": ["MONDAY", "WEDNESDAY"],
            "everyNDays": None,
        },
        targets=[
            {"dayPart": "MORNING", "targetCount": 1},
            {"dayPart": "EVENING", "targetCount": 2},
        ],
    )

    response = await client.get(f"/habits/{habit_id}", headers=auth_headers)

    assert response.status_code == 200
    body = response.json()
    assert body["habitId"] == habit_id
    assert body["name"] == "Read Book"
    assert body["iconId"] == 3
    assert body["colorArgb"] == -256
    assert body["startDate"] == "2026-02-01"
    assert body["endDate"] == "2026-02-28"
    assert body["frequency"] == {
        "type": "WEEKLY",
        "weekDays": ["MONDAY", "WEDNESDAY"],
        "everyNDays": None,
    }
    assert body["targets"] == [
        {"dayPart": "MORNING", "targetCount": 1},
        {"dayPart": "EVENING", "targetCount": 2},
    ]


@pytest.mark.asyncio
async def test_get_habit_returns_404_for_other_users_habit(
    client: AsyncClient,
    second_user_auth_headers: dict[str, str],
    create_habit,
) -> None:
    habit_id = await create_habit(name="Private Habit")

    response = await client.get(f"/habits/{habit_id}", headers=second_user_auth_headers)

    assert response.status_code == 404
    assert response.json() == {"detail": "Habit not found"}


@pytest.mark.asyncio
async def test_update_habit_patches_color_argb(
    client: AsyncClient,
    auth_headers: dict[str, str],
) -> None:
    create_response = await client.post(
        "/habits",
        json=_habit_payload(
            name="Meditate",
            icon_id=3,
            color_argb=-16711936,
        ),
        headers=auth_headers,
    )
    assert create_response.status_code == 201
    habit_id = create_response.json()["habitId"]

    patch_response = await client.patch(
        f"/habits/{habit_id}",
        json={"colorArgb": -256},
        headers=auth_headers,
    )

    assert patch_response.status_code == 200
    updated = patch_response.json()
    assert updated["habitId"] == habit_id
    assert updated["colorArgb"] == -256


@pytest.mark.asyncio
async def test_patch_habit_replaces_frequency_and_targets(
    client: AsyncClient,
    auth_headers: dict[str, str],
    create_habit,
) -> None:
    habit_id = await create_habit(
        name="Flexible Habit",
        frequency={
            "type": "WEEKLY",
            "weekDays": ["MONDAY", "FRIDAY"],
            "everyNDays": None,
        },
        targets=[
            {"dayPart": "MORNING", "targetCount": 1},
            {"dayPart": "EVENING", "targetCount": 1},
        ],
    )

    patch_response = await client.patch(
        f"/habits/{habit_id}",
        json={
            "frequency": {
                "type": "EVERY_N_DAYS",
                "weekDays": None,
                "everyNDays": 3,
            },
            "targets": [{"dayPart": "FULL_DAY", "targetCount": 4}],
        },
        headers=auth_headers,
    )
    assert patch_response.status_code == 200

    get_response = await client.get(f"/habits/{habit_id}", headers=auth_headers)

    assert get_response.status_code == 200
    body = get_response.json()
    assert body["frequency"] == {
        "type": "EVERY_N_DAYS",
        "weekDays": None,
        "everyNDays": 3,
    }
    assert body["targets"] == [
        {"dayPart": "FULL_DAY", "targetCount": 4},
    ]


@pytest.mark.asyncio
@pytest.mark.parametrize(
    ("patch", "expected_detail"),
    [
        (
            {"startDate": "2026-02-20", "endDate": "2026-02-19"},
            "startDate must be less than or equal to endDate",
        ),
        (
            {"targets": []},
            "targets must not be empty",
        ),
        (
            {
                "frequency": {
                    "type": "DAILY",
                    "weekDays": ["MONDAY"],
                    "everyNDays": None,
                }
            },
            "DAILY requires empty weekDays and null everyNDays",
        ),
    ],
)
async def test_patch_habit_rejects_invalid_payloads(
    client: AsyncClient,
    auth_headers: dict[str, str],
    create_habit,
    patch: dict[str, object],
    expected_detail: str,
) -> None:
    habit_id = await create_habit()

    response = await client.patch(
        f"/habits/{habit_id}",
        json=patch,
        headers=auth_headers,
    )

    assert response.status_code == 422
    assert response.json() == {"detail": expected_detail}


@pytest.mark.asyncio
async def test_second_user_cannot_patch_other_users_habit(
    client: AsyncClient,
    second_user_auth_headers: dict[str, str],
    create_habit,
) -> None:
    habit_id = await create_habit(name="Private Habit")

    response = await client.patch(
        f"/habits/{habit_id}",
        json={"name": "Hacked Habit"},
        headers=second_user_auth_headers,
    )

    assert response.status_code == 404
    assert response.json() == {"detail": "Habit not found"}


@pytest.mark.asyncio
async def test_delete_habit_returns_204_and_removes_habit(
    client: AsyncClient,
    auth_headers: dict[str, str],
    create_habit,
) -> None:
    habit_id = await create_habit(name="Temporary Habit")

    delete_response = await client.delete(f"/habits/{habit_id}", headers=auth_headers)
    get_response = await client.get(f"/habits/{habit_id}", headers=auth_headers)

    assert delete_response.status_code == 204
    assert get_response.status_code == 404
    assert get_response.json() == {"detail": "Habit not found"}


@pytest.mark.asyncio
async def test_delete_habit_returns_404_for_missing_habit(
    client: AsyncClient,
    auth_headers: dict[str, str],
) -> None:
    response = await client.delete("/habits/999999", headers=auth_headers)

    assert response.status_code == 404
    assert response.json() == {"detail": "Habit not found"}


@pytest.mark.asyncio
async def test_second_user_cannot_delete_other_users_habit(
    client: AsyncClient,
    second_user_auth_headers: dict[str, str],
    create_habit,
) -> None:
    habit_id = await create_habit(name="Private Habit")

    response = await client.delete(f"/habits/{habit_id}", headers=second_user_auth_headers)

    assert response.status_code == 404
    assert response.json() == {"detail": "Habit not found"}


@pytest.mark.asyncio
@pytest.mark.parametrize(
    ("payload", "expected_detail"),
    [
        (
            _habit_payload(start_date="2026-02-20", end_date="2026-02-19"),
            "startDate must be less than or equal to endDate",
        ),
        (
            _habit_payload(targets=[]),
            "targets must not be empty",
        ),
        (
            _habit_payload(
                targets=[
                    {"dayPart": "MORNING", "targetCount": 1},
                    {"dayPart": "MORNING", "targetCount": 2},
                ]
            ),
            "targets must not contain duplicate dayPart values",
        ),
        (
            _habit_payload(
                targets=[
                    {"dayPart": "FULL_DAY", "targetCount": 1},
                    {"dayPart": "MORNING", "targetCount": 1},
                ]
            ),
            "FULL_DAY must be the only target dayPart",
        ),
        (
            _habit_payload(
                frequency={
                    "type": "DAILY",
                    "weekDays": ["MONDAY"],
                    "everyNDays": None,
                }
            ),
            "DAILY requires empty weekDays and null everyNDays",
        ),
        (
            _habit_payload(
                frequency={
                    "type": "WEEKLY",
                    "weekDays": None,
                    "everyNDays": None,
                }
            ),
            "WEEKLY requires non-empty weekDays and null everyNDays",
        ),
        (
            _habit_payload(
                frequency={
                    "type": "EVERY_N_DAYS",
                    "weekDays": ["MONDAY"],
                    "everyNDays": 2,
                }
            ),
            "EVERY_N_DAYS requires everyNDays >= 1 and empty weekDays",
        ),
    ],
)
async def test_create_habit_rejects_invalid_payloads(
    client: AsyncClient,
    auth_headers: dict[str, str],
    payload: dict[str, object],
    expected_detail: str,
) -> None:
    response = await client.post("/habits", json=payload, headers=auth_headers)

    assert response.status_code == 422
    assert response.json() == {"detail": expected_detail}
