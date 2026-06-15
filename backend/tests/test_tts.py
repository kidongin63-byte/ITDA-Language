"""TTS 합성 테스트 — TTS 엔진을 mock하여 빠른 단위 테스트"""

import pytest
from httpx import AsyncClient
from unittest.mock import AsyncMock, patch


FAKE_MP3 = b"\xff\xfb\x90\x00" + b"\x00" * 100  # 가짜 mp3 바이너리


@pytest.mark.asyncio
async def test_synthesize_success(client: AsyncClient, auth_headers: dict):
    with patch(
        "app.services.tts_orchestrator.tts_orchestrator._engine.synthesize",
        new_callable=AsyncMock,
        return_value=(FAKE_MP3, 150),
    ):
        resp = await client.post(
            "/api/v1/tts/synthesize",
            json={
                "text": "안녕하세요, 반갑습니다.",
                "speaker": "ko-KR-SunHiNeural",
                "speed": 0,
                "pitch": 0,
                "volume": 0,
                "emotion": 0,
                "format": "mp3",
            },
            headers=auth_headers,
        )
    assert resp.status_code == 200
    assert resp.headers["content-type"] == "audio/mpeg"
    assert len(resp.content) > 0


@pytest.mark.asyncio
async def test_synthesize_wav_format(client: AsyncClient, auth_headers: dict):
    fake_wav = b"RIFF" + b"\x00" * 100
    with patch(
        "app.services.tts_orchestrator.tts_orchestrator._engine.synthesize",
        new_callable=AsyncMock,
        return_value=(fake_wav, 200),
    ):
        resp = await client.post(
            "/api/v1/tts/synthesize",
            json={"text": "테스트", "speaker": "ko-KR-InJoonNeural", "format": "wav"},
            headers=auth_headers,
        )
    assert resp.status_code == 200
    assert resp.headers["content-type"] == "audio/wav"


@pytest.mark.asyncio
async def test_synthesize_empty_text(client: AsyncClient, auth_headers: dict):
    resp = await client.post(
        "/api/v1/tts/synthesize",
        json={"text": "", "speaker": "ko-KR-SunHiNeural"},
        headers=auth_headers,
    )
    assert resp.status_code == 422  # Pydantic validation error


@pytest.mark.asyncio
async def test_synthesize_unauthorized(client: AsyncClient):
    resp = await client.post(
        "/api/v1/tts/synthesize",
        json={"text": "테스트", "speaker": "ko-KR-SunHiNeural"},
    )
    assert resp.status_code == 403


@pytest.mark.asyncio
async def test_synthesize_invalid_speed(client: AsyncClient, auth_headers: dict):
    resp = await client.post(
        "/api/v1/tts/synthesize",
        json={"text": "테스트", "speaker": "ko-KR-SunHiNeural", "speed": 10},
        headers=auth_headers,
    )
    assert resp.status_code == 422
