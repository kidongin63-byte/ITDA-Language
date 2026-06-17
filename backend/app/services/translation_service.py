"""
번역 서비스 — 텍스트 언어와 음성 언어가 다를 때 GPT-4o-mini로 자동 번역.
농인과 외국인 간 다국어 대화를 지원한다.
"""

import logging
from openai import AsyncOpenAI
from langdetect import detect, LangDetectException

from app.config import get_settings

logger = logging.getLogger(__name__)

# Edge TTS 음성 코드 → 언어 이름 매핑
VOICE_LANG_MAP = {
    "ko": "Korean",
    "en": "English",
    "ja": "Japanese",
    "zh": "Chinese",
    "vi": "Vietnamese",
    "es": "Spanish",
    "fr": "French",
    "de": "German",
    "hi": "Hindi",
    "pt": "Portuguese",
    "it": "Italian",
    "mn": "Mongolian",
    "ru": "Russian",
    "th": "Thai",
    "ar": "Arabic",
}

_client: AsyncOpenAI | None = None


def _get_client() -> AsyncOpenAI:
    global _client
    if _client is None:
        _client = AsyncOpenAI(api_key=get_settings().OPENAI_API_KEY)
    return _client


def extract_voice_language(speaker: str) -> str:
    """음성 코드에서 언어 코드 추출. 예: 'ko-KR-SunHiNeural:+30' → 'ko'"""
    # 피치 오프셋 제거
    voice = speaker.split(":")[0]
    # 첫 번째 세그먼트가 언어 코드
    return voice.split("-")[0].lower()


def detect_text_language(text: str) -> str:
    """텍스트 언어 감지. 실패 시 'ko' 반환 (주 사용자가 한국 농인)"""
    try:
        lang = detect(text)
        # langdetect은 'zh-cn', 'zh-tw' 등을 반환할 수 있음
        return lang.split("-")[0].lower()
    except LangDetectException:
        return "ko"


def is_multilingual_voice(speaker: str) -> bool:
    """다국어 음성(Multilingual)은 번역 없이 여러 언어를 읽을 수 있다."""
    return "Multilingual" in speaker.split(":")[0]


async def translate_text(text: str, source_lang: str, target_lang: str) -> str:
    """GPT-4o-mini로 번역. 간결하고 자연스러운 번역을 반환."""
    client = _get_client()
    source_name = VOICE_LANG_MAP.get(source_lang, source_lang)
    target_name = VOICE_LANG_MAP.get(target_lang, target_lang)

    response = await client.chat.completions.create(
        model="gpt-4o-mini",
        messages=[
            {
                "role": "system",
                "content": (
                    f"Translate the following {source_name} text to {target_name}. "
                    "This is for a deaf communication app, so keep the translation "
                    "natural and easy to understand. "
                    "Return ONLY the translated text, nothing else."
                ),
            },
            {"role": "user", "content": text},
        ],
        temperature=0.3,
        max_tokens=1500,
    )
    return response.choices[0].message.content.strip()


async def auto_translate_if_needed(text: str, speaker: str) -> tuple[str, bool]:
    """
    텍스트 언어 ≠ 음성 언어일 때 자동 번역.
    Returns: (번역된 텍스트 또는 원본, 번역 여부)
    """
    try:
        settings = get_settings()
        if not settings.OPENAI_API_KEY:
            return text, False

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
        # 번역 실패해도 TTS는 계속 시도
        logger.warning("번역 실패, 원본 텍스트 사용: %s", e)
        return text, False
