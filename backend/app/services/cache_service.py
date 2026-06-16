"""TTS 오디오 캐싱 서비스 (no-op: 캐시 없이 동작)"""


async def get_cached_audio(cache_key: str) -> bytes | None:
    return None


async def set_cached_audio(cache_key: str, audio: bytes) -> None:
    pass


async def close_redis() -> None:
    pass
