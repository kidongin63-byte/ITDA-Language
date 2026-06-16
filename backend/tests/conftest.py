"""
pytest 공통 fixtures

- 인메모리 SQLite async DB로 격리된 테스트 환경 제공
- FastAPI TestClient (httpx AsyncClient)
- Cache mock
- 인증된 사용자 + 토큰 fixture
"""

import asyncio

import pytest
import pytest_asyncio
from httpx import ASGITransport, AsyncClient
from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker, create_async_engine

from app.core.database import Base, get_db
from app.main import app
from app.services import cache_service

# ─── 인메모리 SQLite 엔진 ─────────────────────────

TEST_DB_URL = "sqlite+aiosqlite:///:memory:"


@pytest.fixture(scope="session")
def event_loop():
    loop = asyncio.new_event_loop()
    yield loop
    loop.close()


@pytest_asyncio.fixture(scope="function")
async def db_session():
    engine = create_async_engine(TEST_DB_URL, echo=False)
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)

    session_factory = async_sessionmaker(engine, class_=AsyncSession, expire_on_commit=False)
    async with session_factory() as session:
        yield session

    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.drop_all)
    await engine.dispose()


# ─── Cache mock ───────────────────────────────────

@pytest_asyncio.fixture(autouse=True)
async def mock_cache(monkeypatch):
    """모든 테스트에서 Redis를 무시 (캐시 비활성화)"""

    async def _noop_get(_key):
        return None

    async def _noop_set(_key, _val):
        pass

    monkeypatch.setattr(cache_service, "get_cached_audio", _noop_get)
    monkeypatch.setattr(cache_service, "set_cached_audio", _noop_set)


# ─── FastAPI TestClient ──────────────────────────

@pytest_asyncio.fixture(scope="function")
async def client(db_session: AsyncSession):
    async def _override_get_db():
        yield db_session

    app.dependency_overrides[get_db] = _override_get_db

    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as ac:
        yield ac

    app.dependency_overrides.clear()


# ─── 인증 헬퍼 ───────────────────────────────────

@pytest_asyncio.fixture
async def auth_token(client: AsyncClient) -> str:
    """회원가입 후 access_token 반환"""
    resp = await client.post("/api/v1/auth/register", json={
        "email": "test@itda.kr",
        "nickname": "테스트유저",
        "password": "test1234",
    })
    assert resp.status_code == 201
    return resp.json()["access_token"]


@pytest_asyncio.fixture
async def auth_headers(auth_token: str) -> dict:
    """Authorization 헤더 딕셔너리"""
    return {"Authorization": f"Bearer {auth_token}"}
