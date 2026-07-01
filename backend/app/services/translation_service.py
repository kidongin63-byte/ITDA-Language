"""
번역 서비스 — 텍스트 언어와 음성 언어가 다를 때 자동 번역.
농인과 외국인 간 다국어 대화를 지원한다.
Google Translate (무료, API 키 불필요)를 사용한다.
"""

import asyncio
import logging
from deep_translator import GoogleTranslator
from langdetect import detect, DetectorFactory, LangDetectException

logger = logging.getLogger(__name__)

# langdetect는 기본적으로 비결정적 — 시드를 고정해 같은 입력에 항상 같은 결과를 내게 함
# (이 값을 안 박으면 짧은 문장에서 "가끔 번역되고 가끔 안 됨" 증상 발생)
DetectorFactory.seed = 0

# 번역 외부호출 재시도 횟수 (간헐적 네트워크 실패 대응)
_TRANSLATE_ATTEMPTS = 3

# Edge TTS 음성 코드 → deep_translator 언어 코드 매핑
VOICE_LANG_MAP = {
    "ko": "ko",
    "en": "en",
    "ja": "ja",
    "zh": "zh-CN",
    "vi": "vi",
    "es": "es",
    "fr": "fr",
    "de": "de",
    "hi": "hi",
    "pt": "pt",
    "it": "it",
    "mn": "mn",
    "ru": "ru",
    "th": "th",
    "ar": "ar",
}


def extract_voice_language(speaker: str) -> str:
    """음성 코드에서 언어 코드 추출. 예: 'ko-KR-SunHiNeural:+30' → 'ko'"""
    voice = speaker.split(":")[0]
    return voice.split("-")[0].lower()


def detect_text_language(text: str) -> str:
    """텍스트 언어 감지. 실패 시 'ko' 반환 (주 사용자가 한국 농인)"""
    try:
        lang = detect(text)
        return lang.split("-")[0].lower()
    except LangDetectException:
        return "ko"


def is_multilingual_voice(speaker: str) -> bool:
    """다국어 음성(Multilingual)은 번역 없이 여러 언어를 읽을 수 있다."""
    return "Multilingual" in speaker.split(":")[0]


def _translate_sync(text: str, source_lang: str, target_lang: str) -> str:
    """Google Translate 동기 호출"""
    src = VOICE_LANG_MAP.get(source_lang, source_lang)
    tgt = VOICE_LANG_MAP.get(target_lang, target_lang)
    translator = GoogleTranslator(source=src, target=tgt)
    return translator.translate(text)


async def translate_text(text: str, source_lang: str, target_lang: str) -> str:
    """Google Translate를 비동기로 호출 (스레드풀 + 재시도)"""
    loop = asyncio.get_event_loop()
    last_err: Exception | None = None
    for attempt in range(1, _TRANSLATE_ATTEMPTS + 1):
        try:
            result = await loop.run_in_executor(
                None, _translate_sync, text, source_lang, target_lang
            )
            # 빈 결과는 실패로 간주하고 재시도
            if result and result.strip():
                return result
            last_err = ValueError("빈 번역 결과")
        except Exception as e:  # noqa: BLE001 - 네트워크 등 모든 실패를 재시도
            last_err = e
            logger.warning("번역 시도 %d/%d 실패: %s", attempt, _TRANSLATE_ATTEMPTS, e)
        if attempt < _TRANSLATE_ATTEMPTS:
            await asyncio.sleep(0.4 * attempt)  # 점증 백오프
    raise last_err if last_err else RuntimeError("번역 실패")


async def auto_translate_if_needed(text: str, speaker: str) -> tuple[str, bool]:
    """
    텍스트 언어 ≠ 음성 언어일 때 자동 번역.
    Returns: (번역된 텍스트 또는 원본, 번역 여부)
    """
    try:
        # 다국어 음성은 번역 불필요
        if is_multilingual_voice(speaker):
            return text, False

        text_lang = detect_text_language(text)
        voice_lang = extract_voice_language(speaker)

        if text_lang == voice_lang:
            return text, False

        logger.info("번역: %s → %s (speaker=%s)", text_lang, voice_lang, speaker)
        translated = await translate_text(text, text_lang, voice_lang)
        return translated, True

    except Exception as e:
        logger.warning("번역 실패, 원본 텍스트 사용: %s", e)
        return text, False
