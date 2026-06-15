"""
Microsoft Edge TTS 어댑터

무료, API 키 불필요, 한국어 음성 지원.
고도화 시 CLOVA Voice로 전환 가능하도록 TTSEngineBase를 구현한다.
"""

import asyncio
import hashlib
import io
import re
import time

import edge_tts

from app.core.exceptions import TTSError, TextTooLongError
from app.config import get_settings
from app.schemas.tts import TTSSynthesizeRequest
from app.services.tts_base import TTSEngineBase

settings = get_settings()

_SENTENCE_SPLIT_RE = re.compile(r"(?<=[.!?~\n])\s+")


def _split_sentences(text: str) -> list[str]:
    sentences = _SENTENCE_SPLIT_RE.split(text.strip())
    return [s.strip() for s in sentences if s.strip()]


class EdgeTTSAdapter(TTSEngineBase):
    """Microsoft Edge TTS 어댑터 — 무료, API 키 불필요"""

    async def synthesize(self, request: TTSSynthesizeRequest) -> tuple[bytes, int]:
        if len(request.text) > settings.MAX_TEXT_LENGTH:
            raise TextTooLongError(settings.MAX_TEXT_LENGTH)

        start = time.monotonic()

        # 화자 코드에 피치 오프셋이 포함될 수 있음
        # 예: "ko-KR-SunHiNeural:+30" → voice="ko-KR-SunHiNeural", base_pitch=+30Hz
        voice = request.speaker
        base_pitch_offset = 0
        if ":" in voice:
            voice, offset_str = voice.rsplit(":", 1)
            try:
                base_pitch_offset = int(offset_str)
            except ValueError:
                pass

        # Edge TTS 속도/피치 변환: CLOVA (-5~5) → Edge TTS 형식
        rate = f"{request.speed * 10:+d}%"
        total_pitch = request.pitch * 10 + base_pitch_offset
        pitch_hz = f"{total_pitch:+d}Hz"
        vol = f"{request.volume * 10:+d}%"

        try:
            communicate = edge_tts.Communicate(
                text=request.text,
                voice=voice,
                rate=rate,
                pitch=pitch_hz,
                volume=vol,
            )

            audio_data = io.BytesIO()
            async for chunk in communicate.stream():
                if chunk["type"] == "audio":
                    audio_data.write(chunk["data"])

            audio_bytes = audio_data.getvalue()
            if not audio_bytes:
                raise TTSError("Edge TTS: 음성 합성 결과가 비어있습니다")

        except TTSError:
            raise
        except Exception as e:
            raise TTSError(f"Edge TTS 합성 실패: {e}")

        latency_ms = int((time.monotonic() - start) * 1000)
        return audio_bytes, latency_ms

    async def synthesize_sentences(
        self, text: str, speaker: str, speed: int = 0, pitch: int = 0, volume: int = 0
    ) -> list[tuple[bytes, int]]:
        sentences = _split_sentences(text)
        if not sentences:
            sentences = [text]

        async def _synth_one(idx: int, sentence: str) -> tuple[bytes, int]:
            req = TTSSynthesizeRequest(
                text=sentence, speaker=speaker, speed=speed, pitch=pitch, volume=volume,
            )
            audio, _ = await self.synthesize(req)
            return audio, idx

        tasks = [_synth_one(i, s) for i, s in enumerate(sentences)]
        results = await asyncio.gather(*tasks)
        results.sort(key=lambda x: x[1])
        return results

    def make_cache_key(self, speaker: str, text: str, speed: int, pitch: int, volume: int) -> str:
        raw = f"edge:{speaker}:{text}:{speed}:{pitch}:{volume}"
        return f"tts:cache:{hashlib.sha256(raw.encode()).hexdigest()}"


# 싱글톤
edge_tts_engine = EdgeTTSAdapter()
