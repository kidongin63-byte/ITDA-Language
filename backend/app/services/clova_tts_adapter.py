"""
Naver CLOVA Voice Premium API 어댑터

텍스트를 음성 바이너리(mp3/wav)로 변환하는 핵심 서비스.
문장 분할 + 병렬 합성으로 저지연 스트리밍을 지원한다.
"""

import re
import asyncio
import hashlib
import time

import httpx

from app.config import get_settings
from app.core.exceptions import TTSError, TextTooLongError
from app.schemas.tts import TTSSynthesizeRequest
from app.services.tts_base import TTSEngineBase

settings = get_settings()

# 문장 분할 패턴: 마침표, 물음표, 느낌표, 줄바꿈 등
_SENTENCE_SPLIT_RE = re.compile(r"(?<=[.!?~\n])\s+")


def _split_sentences(text: str) -> list[str]:
    """텍스트를 문장 단위로 분할"""
    sentences = _SENTENCE_SPLIT_RE.split(text.strip())
    return [s.strip() for s in sentences if s.strip()]


def _make_cache_key(speaker: str, text: str, speed: int, pitch: int, volume: int) -> str:
    """TTS 캐시 키 생성"""
    raw = f"{speaker}:{text}:{speed}:{pitch}:{volume}"
    return f"tts:cache:{hashlib.sha256(raw.encode()).hexdigest()}"


class ClovaTTSAdapter(TTSEngineBase):
    """CLOVA Voice Premium API와 통신하는 어댑터"""

    def __init__(self):
        self._client = httpx.AsyncClient(timeout=10.0)

    async def close(self):
        await self._client.aclose()

    async def synthesize(self, request: TTSSynthesizeRequest) -> tuple[bytes, int]:
        """
        텍스트를 음성 바이너리로 변환.
        Returns: (audio_bytes, latency_ms)
        """
        if len(request.text) > settings.MAX_TEXT_LENGTH:
            raise TextTooLongError(settings.MAX_TEXT_LENGTH)

        start = time.monotonic()

        data = {
            "speaker": request.speaker,
            "text": request.text,
            "volume": str(request.volume),
            "speed": str(request.speed),
            "pitch": str(request.pitch),
            "emotion": str(request.emotion),
            "emotion-strength": str(request.emotion_strength),
            "format": request.format,
            "alpha": str(request.alpha),
        }

        headers = {
            "X-NCP-APIGW-API-KEY-ID": settings.CLOVA_CLIENT_ID,
            "X-NCP-APIGW-API-KEY": settings.CLOVA_CLIENT_SECRET,
            "Content-Type": "application/x-www-form-urlencoded",
        }

        try:
            response = await self._client.post(
                settings.CLOVA_API_URL, data=data, headers=headers
            )
        except httpx.RequestError as e:
            raise TTSError(f"CLOVA API 연결 실패: {e}")

        if response.status_code != 200:
            raise TTSError(
                f"CLOVA API 오류 (HTTP {response.status_code}): {response.text[:200]}"
            )

        latency_ms = int((time.monotonic() - start) * 1000)
        return response.content, latency_ms

    async def synthesize_sentences(
        self, text: str, speaker: str, speed: int = 0, pitch: int = 0, volume: int = 0
    ) -> list[tuple[bytes, int]]:
        """
        텍스트를 문장 단위로 분할하여 병렬 합성.
        Returns: list of (audio_bytes, sequence_index)
        """
        sentences = _split_sentences(text)
        if not sentences:
            sentences = [text]

        async def _synth_one(idx: int, sentence: str) -> tuple[bytes, int]:
            req = TTSSynthesizeRequest(
                text=sentence,
                speaker=speaker,
                speed=speed,
                pitch=pitch,
                volume=volume,
            )
            audio, _ = await self.synthesize(req)
            return audio, idx

        tasks = [_synth_one(i, s) for i, s in enumerate(sentences)]
        results = await asyncio.gather(*tasks)
        # 순서대로 정렬
        results.sort(key=lambda x: x[1])
        return results

    def make_cache_key(self, speaker: str, text: str, speed: int, pitch: int, volume: int) -> str:
        return _make_cache_key(speaker, text, speed, pitch, volume)


# 싱글톤
clova_tts = ClovaTTSAdapter()
