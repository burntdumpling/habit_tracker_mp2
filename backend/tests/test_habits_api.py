import pytest
from httpx import AsyncClient


@pytest.mark.asyncio
async def test_create_habit_returns_color_argb(
    client: AsyncClient,
    auth_headers: dict[str, str],
) -> None:
    payload = {
        "status": "ACTIVE",
        "name": "Drink Water",
        "iconId": 7,
        "colorArgb": -65536,
        "startDate": "2026-02-20",
        "endDate": None,
        "frequency": {
            "type": "DAILY",
            "weekDays": None,
            "everyNDays": None,
        },
        "targets": [{"dayPart": "FULL_DAY", "targetCount": 8}],
    }

    response = await client.post("/habits", json=payload, headers=auth_headers)

    assert response.status_code == 201
    body = response.json()
    assert body["name"] == "Drink Water"
    assert body["iconId"] == 7
    assert body["colorArgb"] == -65536


@pytest.mark.asyncio
async def test_update_habit_patches_color_argb(
    client: AsyncClient,
    auth_headers: dict[str, str],
) -> None:
    create_payload = {
        "status": "ACTIVE",
        "name": "Meditate",
        "iconId": 3,
        "colorArgb": -16711936,
        "startDate": "2026-02-20",
        "endDate": None,
        "frequency": {
            "type": "DAILY",
            "weekDays": None,
            "everyNDays": None,
        },
        "targets": [{"dayPart": "FULL_DAY", "targetCount": 1}],
    }
    create_response = await client.post("/habits", json=create_payload, headers=auth_headers)
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
