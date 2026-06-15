"""
TTS 엔진 추상 베이스 클래스

CLOVA Voice, Edge TTS 등 다양한 TTS 엔진을 동일한 인터페이스로
교체할 수 있도록 추상화한다. config.py의 TTS_ENGINE 설정으로
사용할 엔진을 선택한다.
"""

from abc import ABC, abstractmethod

from app.schemas.tts import TTSSynthesizeRequest


class TTSEngineBase(ABC):
    """TTS 엔진 공통 인터페이스"""

    @abstractmethod
    async def synthesize(self, request: TTSSynthesizeRequest) -> tuple[bytes, int]:
        """
        텍스트를 음성 바이너리로 변환.
        Returns: (audio_bytes, latency_ms)
        """
        ...

    @abstractmethod
    async def synthesize_sentences(
        self, text: str, speaker: str, speed: int, pitch: int, volume: int
    ) -> list[tuple[bytes, int]]:
        """
        텍스트를 문장 단위로 분할하여 병렬 합성.
        Returns: list of (audio_bytes, sequence_index)
        """
        ...

    @abstractmethod
    def make_cache_key(self, speaker: str, text: str, speed: int, pitch: int, volume: int) -> str:
        """TTS 캐시 키 생성"""
        ...

    async def close(self):
        """리소스 정리 (선택적)"""
        pass
