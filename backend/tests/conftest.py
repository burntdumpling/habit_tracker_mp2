import uuid
from collections.abc import AsyncGenerator
from datetime import date
from pathlib import Path
from typing import Any

import pytest
import pytest_asyncio
from httpx import ASGITransport, AsyncClient
from pydantic import Field, ValidationError, field_validator
from pydantic_settings import BaseSettings, SettingsConfigDict
from sqlalchemy import text
from sqlalchemy.ext.asyncio import AsyncEngine, AsyncSession, async_sessionmaker, create_async_engine
from sqlalchemy.pool import NullPool

from app.api.deps import get_db
from app.db.base import Base
from app.db import models as _models  # noqa: F401
from app.main import app

TEST_DB_TRUNCATE_SQL = """
TRUNCATE TABLE habit_logs, habit_targets, habit_weekdays, habits, users
RESTART IDENTITY CASCADE
"""


class TestSettings(BaseSettings):
    test_database_url: str = Field(validation_alias="TEST_DATABASE_URL")

    model_config = SettingsConfigDict(
        env_file=".env.test",
        env_file_encoding="utf-8",
        extra="ignore",
    )

    @field_validator("test_database_url")
    @classmethod
    def validate_test_database_url(cls, value: str) -> str:
        normalized = value.lower()
        if "test" not in normalized:
            raise ValueError(
                "TEST_DATABASE_URL must point to a dedicated test database and contain 'test' in the URL."
            )
        return value


def _load_test_settings() -> TestSettings:
    try:
        return TestSettings()
    except ValidationError as exc:
        env_file = Path(".env.test")
        if env_file.exists():
            raise RuntimeError(
                "Failed to load test database configuration from .env.test. "
                "Ensure TEST_DATABASE_URL points to a dedicated test database."
            ) from exc
        raise RuntimeError(
            "Missing test database configuration. Create .env.test with TEST_DATABASE_URL "
            "pointing to a dedicated test database, for example "
            "'postgresql+asyncpg://habit_user:habit_pass@localhost:5433/habit_test_db'."
        ) from exc


def _normalize_date(value: date | str | None) -> str | None:
    if value is None:
        return None
    if isinstance(value, date):
        return value.isoformat()
    return value


async def _build_auth_headers(client: AsyncClient, *, email_prefix: str = "user") -> dict[str, str]:
    email = f"{email_prefix}-{uuid.uuid4()}@example.com"
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


@pytest.fixture(scope="session")
def test_settings() -> TestSettings:
    return _load_test_settings()


@pytest_asyncio.fixture
async def test_engine(test_settings: TestSettings) -> AsyncGenerator[AsyncEngine, None]:
    engine = create_async_engine(
        test_settings.test_database_url,
        poolclass=NullPool,
    )
    try:
        yield engine
    finally:
        await engine.dispose()


@pytest.fixture
def test_session_maker(test_engine: AsyncEngine) -> async_sessionmaker[AsyncSession]:
    return async_sessionmaker(test_engine, expire_on_commit=False, class_=AsyncSession)


@pytest_asyncio.fixture
async def prepared_test_db(test_engine: AsyncEngine, test_settings: TestSettings) -> AsyncGenerator[None, None]:
    if "test" not in test_settings.test_database_url.lower():
        raise RuntimeError(
            "Refusing to prepare test schema because TEST_DATABASE_URL does not look like a test database."
        )

    async with test_engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)

    yield

    async with test_engine.begin() as conn:
        await conn.run_sync(Base.metadata.drop_all)


@pytest_asyncio.fixture(autouse=True)
async def clean_db(prepared_test_db: None, test_engine: AsyncEngine, test_settings: TestSettings) -> AsyncGenerator[None, None]:
    if "test" not in test_settings.test_database_url.lower():
        raise RuntimeError(
            "Refusing to run destructive test cleanup because TEST_DATABASE_URL does not look like a test database."
        )

    async with test_engine.begin() as conn:
        await conn.execute(text(TEST_DB_TRUNCATE_SQL))

    yield

    async with test_engine.begin() as conn:
        await conn.execute(text(TEST_DB_TRUNCATE_SQL))


@pytest_asyncio.fixture
async def client(test_session_maker: async_sessionmaker[AsyncSession]) -> AsyncGenerator[AsyncClient, None]:
    async def override_get_db() -> AsyncGenerator[AsyncSession, None]:
        async with test_session_maker() as session:
            yield session

    app.dependency_overrides[get_db] = override_get_db
    transport = ASGITransport(app=app)

    try:
        async with AsyncClient(transport=transport, base_url="http://test") as async_client:
            yield async_client
    finally:
        app.dependency_overrides.pop(get_db, None)


@pytest_asyncio.fixture
async def auth_headers(client: AsyncClient) -> dict[str, str]:
    return await _build_auth_headers(client, email_prefix="user")


@pytest_asyncio.fixture
async def second_user_auth_headers(client: AsyncClient) -> dict[str, str]:
    return await _build_auth_headers(client, email_prefix="second-user")


@pytest_asyncio.fixture
async def create_habit(client: AsyncClient, auth_headers: dict[str, str]):
    async def _create_habit(
        positional_targets: list[dict[str, object]] | None = None,
        /,
        *,
        auth: dict[str, str] | None = None,
        status: str = "ACTIVE",
        name: str = "Read",
        icon_id: int = 1,
        color_argb: int = -16711936,
        start_date: date | str = "2026-02-01",
        end_date: date | str | None = None,
        frequency: dict[str, Any] | None = None,
        targets: list[dict[str, object]] | None = None,
    ) -> int:
        if positional_targets is not None and targets is not None:
            raise TypeError("targets must be passed either positionally or as a keyword argument, not both")

        resolved_targets = positional_targets if positional_targets is not None else targets
        payload = {
            "status": status,
            "name": name,
            "iconId": icon_id,
            "colorArgb": color_argb,
            "startDate": _normalize_date(start_date),
            "endDate": _normalize_date(end_date),
            "frequency": frequency
            or {
                "type": "DAILY",
                "weekDays": None,
                "everyNDays": None,
            },
            "targets": resolved_targets
            or [
                {"dayPart": "FULL_DAY", "targetCount": 1},
            ],
        }
        response = await client.post("/habits", json=payload, headers=auth or auth_headers)
        assert response.status_code == 201
        return response.json()["habitId"]

    return _create_habit


@pytest_asyncio.fixture
async def log_factory(client: AsyncClient, auth_headers: dict[str, str]):
    async def _create_log(
        *,
        habit_id: int,
        auth: dict[str, str] | None = None,
        log_date: date | str = "2026-02-15",
        day_part: str = "FULL_DAY",
        amount: int = 1,
    ) -> dict[str, Any]:
        response = await client.put(
            f"/habits/{habit_id}/logs",
            json={
                "date": _normalize_date(log_date),
                "dayPart": day_part,
                "amount": amount,
            },
            headers=auth or auth_headers,
        )
        assert response.status_code == 200
        return response.json()

    return _create_log


@pytest_asyncio.fixture
async def habit_id(create_habit) -> int:
    return await create_habit(
        targets=[
            {"dayPart": "MORNING", "targetCount": 1},
            {"dayPart": "DAY", "targetCount": 1},
            {"dayPart": "EVENING", "targetCount": 1},
        ]
    )
