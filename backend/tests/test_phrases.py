"""프리셋 문구 및 즐겨찾기 API 테스트"""

import pytest
from httpx import AsyncClient


@pytest.mark.asyncio
async def test_get_presets(client: AsyncClient):
    resp = await client.get("/api/v1/phrases/presets")
    assert resp.status_code == 200
    data = resp.json()
    assert "greeting" in data
    assert "emergency" in data
    assert "medical" in data
    assert "order" in data
    assert len(data["greeting"]) >= 4
    total = sum(len(v) for v in data.values())
    assert total >= 30


@pytest.mark.asyncio
async def test_add_all_presets(client: AsyncClient, auth_headers: dict):
    resp = await client.post("/api/v1/phrases/presets/add-all", headers=auth_headers)
    assert resp.status_code == 201
    added = resp.json()
    assert len(added) >= 30


@pytest.mark.asyncio
async def test_presets_no_duplicate(client: AsyncClient, auth_headers: dict):
    # 첫 번째 추가
    await client.post("/api/v1/phrases/presets/add-all", headers=auth_headers)
    # 두 번째 추가 — 중복 없어야 함
    resp = await client.post("/api/v1/phrases/presets/add-all", headers=auth_headers)
    assert resp.status_code == 201
    assert len(resp.json()) == 0


@pytest.mark.asyncio
async def test_list_phrases_by_category(client: AsyncClient, auth_headers: dict):
    await client.post("/api/v1/phrases/presets/add-all", headers=auth_headers)

    resp = await client.get("/api/v1/phrases?category=emergency", headers=auth_headers)
    assert resp.status_code == 200
    phrases = resp.json()
    assert len(phrases) >= 3
    assert all(p["category"] == "emergency" for p in phrases)


@pytest.mark.asyncio
async def test_create_custom_phrase(client: AsyncClient, auth_headers: dict):
    resp = await client.post(
        "/api/v1/phrases",
        json={"phrase_text": "커피 한 잔 주세요", "category": "order"},
        headers=auth_headers,
    )
    assert resp.status_code == 201
    assert resp.json()["phrase_text"] == "커피 한 잔 주세요"


@pytest.mark.asyncio
async def test_phrase_usage_count(client: AsyncClient, auth_headers: dict):
    # 문구 생성
    resp = await client.post(
        "/api/v1/phrases",
        json={"phrase_text": "감사합니다"},
        headers=auth_headers,
    )
    pid = resp.json()["id"]

    # 사용 횟수 증가
    resp = await client.post(f"/api/v1/phrases/{pid}/use", headers=auth_headers)
    assert resp.json()["usage_count"] == 1

    resp = await client.post(f"/api/v1/phrases/{pid}/use", headers=auth_headers)
    assert resp.json()["usage_count"] == 2


@pytest.mark.asyncio
async def test_delete_phrase(client: AsyncClient, auth_headers: dict):
    resp = await client.post(
        "/api/v1/phrases",
        json={"phrase_text": "삭제할 문구"},
        headers=auth_headers,
    )
    pid = resp.json()["id"]

    resp = await client.delete(f"/api/v1/phrases/{pid}", headers=auth_headers)
    assert resp.status_code == 204

    resp = await client.get("/api/v1/phrases", headers=auth_headers)
    ids = [p["id"] for p in resp.json()]
    assert pid not in ids
