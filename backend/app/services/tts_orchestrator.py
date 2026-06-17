"""
TTS 오케스트레이터 — TTS 엔진(Edge/CLOVA)을 자동 선택하고
캐싱, 로깅, WebSocket 스트리밍 흐름을 조율한다.

엔진 전환: .env의 TTS_ENGINE=edge 또는 TTS_ENGINE=clova
"""

from sqlalchemy.ext.asyncio import AsyncSession

from app.config import get_settings
from app.models.phrase import TTSUsageLog
from app.schemas.tts import TTSSynthesizeRequest
from app.services import cache_service
from app.services.tts_base import TTSEngineBase
from app.services.translation_service import auto_translate_if_needed

settings = get_settings()


def _get_engine() -> TTSEngineBase:
    """설정에 따라 TTS 엔진 인스턴스를 반환"""
    if settings.TTS_ENGINE == "clova":
        from app.services.clova_tts_adapter import clova_tts
        return clova_tts
    else:
        from app.services.edge_tts_adapter import edge_tts_engine
        return edge_tts_engine


class TTSOrchestrator:

    def __init__(self):
        self._engine: TTSEngineBase = _get_engine()

    @property
    def engine_name(self) -> str:
        return settings.TTS_ENGINE

    async def synthesize_with_cache(
        self, request: TTSSynthesizeRequest, user_id: str, db: AsyncSession
    ) -> bytes:
        """자동 번역 → 캐시 확인 → 합성 → 캐시 저장 → 사용 로그 기록"""
        # 텍스트 언어 ≠ 음성 언어일 때 자동 번역
        translated_text, was_translated = await auto_translate_if_needed(
            request.text, request.speaker
        )
        if was_translated:
            request = TTSSynthesizeRequest(
                text=translated_text,
                speaker=request.speaker,
                speed=request.speed,
                pitch=request.pitch,
                volume=request.volume,
                emotion=request.emotion,
                emotion_strength=request.emotion_strength,
                alpha=request.alpha,
                format=request.format,
            )

        cache_key = self._engine.make_cache_key(
            request.speaker, request.text, request.speed, request.pitch, request.volume
        )

        # 1. 캐시 히트 확인
        cached = await cache_service.get_cached_audio(cache_key)
        if cached:
            await self._log_usage(db, user_id, request, latency_ms=0)
            return cached

        # 2. 합성
        audio, latency_ms = await self._engine.synthesize(request)

        # 3. 캐시 저장
        await cache_service.set_cached_audio(cache_key, audio)

        # 4. 사용 로그
        await self._log_usage(db, user_id, request, latency_ms)

        return audio

    async def synthesize_stream_chunks(
        self, text: str, speaker: str, speed: int = 0, pitch: int = 0, volume: int = 0
    ) -> list[tuple[bytes, int]]:
        """자동 번역 → 문장 단위 분할 합성 (WebSocket 스트리밍용)"""
        translated_text, _ = await auto_translate_if_needed(text, speaker)
        return await self._engine.synthesize_sentences(
            translated_text, speaker, speed, pitch, volume
        )

    async def _log_usage(
        self,
        db: AsyncSession,
        user_id: str,
        request: TTSSynthesizeRequest,
        latency_ms: int,
    ) -> None:
        log = TTSUsageLog(
            user_id=user_id,
            char_count=len(request.text),
            voice_type=self.engine_name,
            speaker_code=request.speaker,
            latency_ms=latency_ms,
        )
        db.add(log)


# 싱글톤
tts_orchestrator = TTSOrchestrator()
