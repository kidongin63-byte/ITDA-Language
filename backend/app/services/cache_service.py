"""Redis 기반 TTS 오디오 캐싱 서비스

Redis 연결 실패 시에도 TTS 합성 자체는 계속되도록
모든 캐시 연산에 방어 코드를 적용한다.
"""

import logging

import redis.asyncio as redis

from app.config import get_settings

settings = get_settings()
logger = logging.getLogger(__name__)

_redis: redis.Redis | None = None


async def get_redis() -> redis.Redis | None:
    global _redis
    if _redis is None:
        try:
            _redis = redis.from_url(settings.REDIS_URL, decode_responses=False)
            await _redis.ping()
        except Exception:
            logger.warning("Redis 연결 실패 — 캐시 없이 동작합니다")
            _redis = None
    return _redis


async def get_cached_audio(cache_key: str) -> bytes | None:
    try:
        r = await get_redis()
        if r is None:
            return None
        return await r.get(cache_key)
    except Exception:
        logger.warning("Redis 캐시 조회 실패 (key=%s)", cache_key)
        return None


async def set_cached_audio(cache_key: str, audio: bytes) -> None:
    try:
        r = await get_redis()
        if r is None:
            return
        await r.set(cache_key, audio, ex=settings.TTS_CACHE_TTL_SECONDS)
    except Exception:
        logger.warning("Redis 캐시 저장 실패 (key=%s)", cache_key)


async def close_redis() -> None:
    global _redis
    if _redis:
        try:
            await _redis.aclose()
        except Exception:
            pass
        _redis = None
