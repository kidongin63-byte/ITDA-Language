"""
TTS 화자 시드 데이터 — 엔진별 자동 선택

TTS_ENGINE=edge  → Edge TTS 한국어 음성 (무료)
TTS_ENGINE=clova → CLOVA Voice Premium 화자 (유료, 고도화 시)

실행: python -m app.seed_data
"""

import asyncio

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.config import get_settings
from app.core.database import async_session, engine, Base
from app.models.voice_persona import VoicePersona

settings = get_settings()

# ─── Edge TTS 화자 ────────────────────────────────────

EDGE_PERSONAS = [
    # 한국어 전용 음성
    {
        "clova_speaker": "ko-KR-SunHiNeural",
        "display_name": "선희 - 여성 (밝은, 서울)",
        "gender": "female",
        "age_group": "young_adult",
        "region": "seoul",
        "tone": "bright",
        "emotion_support": False,
        "sort_order": 1,
    },
    {
        "clova_speaker": "ko-KR-InJoonNeural",
        "display_name": "인준 - 남성 (차분한, 서울)",
        "gender": "male",
        "age_group": "young_adult",
        "region": "seoul",
        "tone": "calm",
        "emotion_support": False,
        "sort_order": 2,
    },
    {
        "clova_speaker": "ko-KR-HyunsuMultilingualNeural",
        "display_name": "현수 - 남성 (따뜻한, 서울)",
        "gender": "male",
        "age_group": "young_adult",
        "region": "seoul",
        "tone": "warm",
        "emotion_support": False,
        "sort_order": 3,
    },
    # 어린이 음성 (피치 +30Hz 변형)
    {
        "clova_speaker": "ko-KR-SunHiNeural:+30",
        "display_name": "선희 (어린이) - 여자 아이 (밝은, 서울)",
        "gender": "female",
        "age_group": "child",
        "region": "seoul",
        "tone": "bright",
        "emotion_support": False,
        "sort_order": 4,
    },
    {
        "clova_speaker": "ko-KR-InJoonNeural:+30",
        "display_name": "인준 (소년) - 남자 아이 (활기찬, 서울)",
        "gender": "male",
        "age_group": "child",
        "region": "seoul",
        "tone": "energetic",
        "emotion_support": False,
        "sort_order": 5,
    },
    # 시니어 음성 (피치 -20Hz 변형)
    {
        "clova_speaker": "ko-KR-SunHiNeural:-20",
        "display_name": "선희 (시니어) - 여성 (따뜻한, 서울)",
        "gender": "female",
        "age_group": "senior",
        "region": "seoul",
        "tone": "warm",
        "emotion_support": False,
        "sort_order": 6,
    },
    {
        "clova_speaker": "ko-KR-InJoonNeural:-20",
        "display_name": "인준 (시니어) - 남성 (진지한, 서울)",
        "gender": "male",
        "age_group": "senior",
        "region": "seoul",
        "tone": "serious",
        "emotion_support": False,
        "sort_order": 7,
    },
    # Multilingual 음성 (한국어 지원)
    {
        "clova_speaker": "en-US-AvaMultilingualNeural",
        "display_name": "아바 - 여성 (부드러운, 다국어)",
        "gender": "female",
        "age_group": "young_adult",
        "region": "seoul",
        "tone": "gentle",
        "emotion_support": False,
        "sort_order": 10,
    },
    {
        "clova_speaker": "en-US-EmmaMultilingualNeural",
        "display_name": "엠마 - 여성 (우아한, 다국어)",
        "gender": "female",
        "age_group": "young_adult",
        "region": "seoul",
        "tone": "elegant",
        "emotion_support": False,
        "sort_order": 11,
    },
    {
        "clova_speaker": "en-US-AndrewMultilingualNeural",
        "display_name": "앤드류 - 남성 (진지한, 다국어)",
        "gender": "male",
        "age_group": "middle_aged",
        "region": "seoul",
        "tone": "serious",
        "emotion_support": False,
        "sort_order": 12,
    },
    {
        "clova_speaker": "en-US-BrianMultilingualNeural",
        "display_name": "브라이언 - 남성 (시원한, 다국어)",
        "gender": "male",
        "age_group": "young_adult",
        "region": "seoul",
        "tone": "cool",
        "emotion_support": False,
        "sort_order": 13,
    },
    {
        "clova_speaker": "fr-FR-VivienneMultilingualNeural",
        "display_name": "비비엔 - 여성 (다정한, 다국어)",
        "gender": "female",
        "age_group": "young_adult",
        "region": "seoul",
        "tone": "friendly",
        "emotion_support": False,
        "sort_order": 14,
    },
    {
        "clova_speaker": "fr-FR-RemyMultilingualNeural",
        "display_name": "레미 - 남성 (활기찬, 다국어)",
        "gender": "male",
        "age_group": "young_adult",
        "region": "seoul",
        "tone": "energetic",
        "emotion_support": False,
        "sort_order": 15,
    },
    {
        "clova_speaker": "de-DE-SeraphinaMultilingualNeural",
        "display_name": "세라피나 - 여성 (밝은, 다국어)",
        "gender": "female",
        "age_group": "middle_aged",
        "region": "seoul",
        "tone": "bright",
        "emotion_support": False,
        "sort_order": 16,
    },
]

# ─── CLOVA Voice Premium 화자 (고도화 시 사용) ────────

CLOVA_PERSONAS = [
    {
        "clova_speaker": "nara",
        "display_name": "나라 - 20대 여성 (밝은, 서울)",
        "gender": "female",
        "age_group": "young_adult",
        "region": "seoul",
        "tone": "bright",
        "emotion_support": True,
        "sort_order": 1,
    },
    {
        "clova_speaker": "nara_call",
        "display_name": "나라 (전화) - 20대 여성 (다정한, 서울)",
        "gender": "female",
        "age_group": "young_adult",
        "region": "seoul",
        "tone": "friendly",
        "emotion_support": False,
        "sort_order": 2,
    },
    {
        "clova_speaker": "mijin",
        "display_name": "미진 - 30대 여성 (차분한, 서울)",
        "gender": "female",
        "age_group": "young_adult",
        "region": "seoul",
        "tone": "calm",
        "emotion_support": False,
        "sort_order": 3,
    },
    {
        "clova_speaker": "jinho",
        "display_name": "진호 - 30대 남성 (차분한, 서울)",
        "gender": "male",
        "age_group": "young_adult",
        "region": "seoul",
        "tone": "calm",
        "emotion_support": True,
        "sort_order": 20,
    },
    {
        "clova_speaker": "minho",
        "display_name": "민호 - 20대 남성 (밝은, 서울)",
        "gender": "male",
        "age_group": "young_adult",
        "region": "seoul",
        "tone": "bright",
        "emotion_support": True,
        "sort_order": 21,
    },
]


def _get_personas() -> list[dict]:
    """설정에 따라 시드 데이터 선택"""
    if settings.TTS_ENGINE == "clova":
        return CLOVA_PERSONAS
    return EDGE_PERSONAS


async def seed_personas():
    """음성 페르소나 시드 데이터 삽입 (중복 시 스킵)"""
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)

    personas = _get_personas()
    print(f"TTS 엔진: {settings.TTS_ENGINE}")
    print(f"시드 화자: {len(personas)}종")
    print()

    async with async_session() as session:
        for persona_data in personas:
            existing = await session.execute(
                select(VoicePersona).where(
                    VoicePersona.clova_speaker == persona_data["clova_speaker"]
                )
            )
            if existing.scalar_one_or_none() is None:
                session.add(VoicePersona(**persona_data))
                print(f"  + {persona_data['display_name']}")
            else:
                print(f"  = {persona_data['display_name']} (이미 존재)")

        await session.commit()
    print(f"\n시드 완료 ({len(personas)}개 화자)")


if __name__ == "__main__":
    asyncio.run(seed_personas())
