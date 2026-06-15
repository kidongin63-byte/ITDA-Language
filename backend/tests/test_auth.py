import pytest
from httpx import AsyncClient


@pytest.mark.asyncio
async def test_register(client: AsyncClient):
    resp = await client.post("/api/v1/auth/register", json={
        "email": "new@itda.kr",
        "nickname": "새사용자",
        "password": "pass1234",
    })
    assert resp.status_code == 201
    data = resp.json()
    assert "access_token" in data
    assert "refresh_token" in data
    assert data["token_type"] == "bearer"


@pytest.mark.asyncio
async def test_register_duplicate_email(client: AsyncClient):
    body = {"email": "dup@itda.kr", "nickname": "유저", "password": "pass1234"}
    await client.post("/api/v1/auth/register", json=body)
    resp = await client.post("/api/v1/auth/register", json=body)
    assert resp.status_code == 409


@pytest.mark.asyncio
async def test_login_success(client: AsyncClient):
    await client.post("/api/v1/auth/register", json={
        "email": "login@itda.kr",
        "nickname": "로그인유저",
        "password": "pass1234",
    })
    resp = await client.post("/api/v1/auth/login", json={
        "email": "login@itda.kr",
        "password": "pass1234",
    })
    assert resp.status_code == 200
    assert "access_token" in resp.json()


@pytest.mark.asyncio
async def test_login_wrong_password(client: AsyncClient):
    await client.post("/api/v1/auth/register", json={
        "email": "wrong@itda.kr",
        "nickname": "유저",
        "password": "pass1234",
    })
    resp = await client.post("/api/v1/auth/login", json={
        "email": "wrong@itda.kr",
        "password": "wrongpassword",
    })
    assert resp.status_code == 401


@pytest.mark.asyncio
async def test_refresh_token(client: AsyncClient):
    reg = await client.post("/api/v1/auth/register", json={
        "email": "refresh@itda.kr",
        "nickname": "유저",
        "password": "pass1234",
    })
    refresh_token = reg.json()["refresh_token"]

    resp = await client.post("/api/v1/auth/refresh", json={
        "refresh_token": refresh_token,
    })
    assert resp.status_code == 200
    assert "access_token" in resp.json()


@pytest.mark.asyncio
async def test_get_me(client: AsyncClient, auth_headers: dict):
    resp = await client.get("/api/v1/auth/me", headers=auth_headers)
    assert resp.status_code == 200
    data = resp.json()
    assert data["email"] == "test@itda.kr"
    assert data["nickname"] == "테스트유저"


@pytest.mark.asyncio
async def test_unauthorized_without_token(client: AsyncClient):
    resp = await client.get("/api/v1/auth/me")
    assert resp.status_code == 403
