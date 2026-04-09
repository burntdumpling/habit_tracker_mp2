import uuid

import pytest
from httpx import AsyncClient


async def _register_user(
    client: AsyncClient,
    *,
    email: str | None = None,
    password: str = "strongpass123",
) -> tuple[str, str]:
    resolved_email = email or f"auth-user-{uuid.uuid4()}@example.com"
    response = await client.post(
        "/auth/register",
        json={"email": resolved_email, "password": password},
    )
    assert response.status_code == 201
    return resolved_email, password


@pytest.mark.asyncio
async def test_register_duplicate_email_returns_409(client: AsyncClient) -> None:
    email = f"duplicate-{uuid.uuid4()}@example.com"
    password = "strongpass123"

    first_response = await client.post(
        "/auth/register",
        json={"email": email, "password": password},
    )
    assert first_response.status_code == 201

    second_response = await client.post(
        "/auth/register",
        json={"email": email, "password": password},
    )

    assert second_response.status_code == 409
    assert second_response.json() == {"detail": "Email already registered"}


@pytest.mark.asyncio
async def test_token_wrong_password_returns_401(client: AsyncClient) -> None:
    email, _password = await _register_user(client)

    response = await client.post(
        "/auth/token",
        data={"username": email, "password": "wrongpass123"},
        headers={"Content-Type": "application/x-www-form-urlencoded"},
    )

    assert response.status_code == 401
    assert response.json() == {"detail": "Incorrect email or password"}


@pytest.mark.asyncio
async def test_token_unknown_email_returns_401(client: AsyncClient) -> None:
    response = await client.post(
        "/auth/token",
        data={"username": f"missing-{uuid.uuid4()}@example.com", "password": "strongpass123"},
        headers={"Content-Type": "application/x-www-form-urlencoded"},
    )

    assert response.status_code == 401
    assert response.json() == {"detail": "Incorrect email or password"}


@pytest.mark.asyncio
async def test_read_current_user_returns_user_for_valid_token(
    client: AsyncClient,
    auth_headers: dict[str, str],
) -> None:
    response = await client.get("/auth/me", headers=auth_headers)

    assert response.status_code == 200
    body = response.json()
    assert set(body) == {"id", "email"}
    assert body["email"].endswith("@example.com")


@pytest.mark.asyncio
async def test_read_current_user_rejects_invalid_token(client: AsyncClient) -> None:
    response = await client.get(
        "/auth/me",
        headers={"Authorization": "Bearer malformed.token.value"},
    )

    assert response.status_code == 401
    assert response.json() == {"detail": "Invalid authentication credentials"}


@pytest.mark.asyncio
async def test_read_current_user_requires_token(client: AsyncClient) -> None:
    response = await client.get("/auth/me")

    assert response.status_code == 401
    assert response.json() == {"detail": "Not authenticated"}
