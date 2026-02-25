import uuid
from collections.abc import AsyncGenerator

import pytest_asyncio
from httpx import ASGITransport, AsyncClient
from sqlalchemy import text

from app.main import app
from app.db.session import engine


@pytest_asyncio.fixture(autouse=True)
async def clean_db() -> AsyncGenerator[None, None]:
    await engine.dispose()
    async with engine.begin() as conn:
        await conn.execute(
            text(
                """
                TRUNCATE TABLE habit_logs, habit_targets, habit_weekdays, habits, users
                RESTART IDENTITY CASCADE
                """
            )
        )
    yield
    await engine.dispose()


@pytest_asyncio.fixture
async def client() -> AsyncGenerator[AsyncClient, None]:
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as async_client:
        yield async_client


@pytest_asyncio.fixture
async def auth_headers(client: AsyncClient) -> dict[str, str]:
    email = f"user-{uuid.uuid4()}@example.com"
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


@pytest_asyncio.fixture
async def create_habit(client: AsyncClient, auth_headers: dict[str, str]):
    async def _create_habit(targets: list[dict[str, object]]) -> int:
        payload = {
            "name": "Read",
            "iconId": 1,
            "colorArgb": -16711936,
            "startDate": "2026-02-01",
            "frequency": {
                "type": "DAILY",
                "weekDays": None,
                "everyNDays": None,
            },
            "targets": targets,
        }
        response = await client.post("/habits", json=payload, headers=auth_headers)
        assert response.status_code == 201
        return response.json()["habitId"]

    return _create_habit


@pytest_asyncio.fixture
async def habit_id(create_habit) -> int:
    return await create_habit(
        [
            {"dayPart": "MORNING", "targetCount": 1},
            {"dayPart": "DAY", "targetCount": 1},
            {"dayPart": "EVENING", "targetCount": 1},
        ]
    )
