import pytest
from httpx import AsyncClient
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.voice_persona import VoicePersona


async def _seed_personas(db: AsyncSession):
    """테스트용 화자 시드"""
    personas = [
        VoicePersona(
            clova_speaker="nara", display_name="나라 - 20대 여성 (밝은, 서울)",
            gender="female", age_group="young_adult", region="seoul", tone="bright",
            emotion_support=True, sort_order=1,
        ),
        VoicePersona(
            clova_speaker="jinho", display_name="진호 - 30대 남성 (차분한, 서울)",
            gender="male", age_group="young_adult", region="seoul", tone="calm",
            emotion_support=True, sort_order=2,
        ),
        VoicePersona(
            clova_speaker="dahyun", display_name="다현 - 20대 여성 (다정한, 부산)",
            gender="female", age_group="young_adult", region="busan", tone="friendly",
            emotion_support=False, sort_order=3,
        ),
        VoicePersona(
            clova_speaker="youngil", display_name="영일 - 60대 남성 (따뜻한, 서울)",
            gender="male", age_group="senior", region="seoul", tone="warm",
            emotion_support=False, sort_order=4,
        ),
    ]
    for p in personas:
        db.add(p)
    await db.flush()


@pytest.mark.asyncio
async def test_get_categories(client: AsyncClient):
    resp = await client.get("/api/v1/voices/categories")
    assert resp.status_code == 200
    data = resp.json()
    assert "genders" in data
    assert "age_groups" in data
    assert "regions" in data
    assert "tones" in data
    assert data["genders"]["male"] == "남성"
    assert data["regions"]["busan"] == "부산"
    assert data["tones"]["bright"] == "밝은"


@pytest.mark.asyncio
async def test_list_all_personas(client: AsyncClient, db_session: AsyncSession):
    await _seed_personas(db_session)
    await db_session.commit()

    resp = await client.get("/api/v1/voices/personas")
    assert resp.status_code == 200
    data = resp.json()
    assert len(data) == 4


@pytest.mark.asyncio
async def test_filter_by_gender(client: AsyncClient, db_session: AsyncSession):
    await _seed_personas(db_session)
    await db_session.commit()

    resp = await client.get("/api/v1/voices/personas?gender=female")
    assert resp.status_code == 200
    data = resp.json()
    assert len(data) == 2
    assert all(p["gender"] == "female" for p in data)


@pytest.mark.asyncio
async def test_filter_by_region(client: AsyncClient, db_session: AsyncSession):
    await _seed_personas(db_session)
    await db_session.commit()

    resp = await client.get("/api/v1/voices/personas?region=busan")
    assert resp.status_code == 200
    data = resp.json()
    assert len(data) == 1
    assert data[0]["clova_speaker"] == "dahyun"


@pytest.mark.asyncio
async def test_filter_by_tone(client: AsyncClient, db_session: AsyncSession):
    await _seed_personas(db_session)
    await db_session.commit()

    resp = await client.get("/api/v1/voices/personas?tone=calm")
    assert resp.status_code == 200
    data = resp.json()
    assert len(data) == 1
    assert data[0]["clova_speaker"] == "jinho"


@pytest.mark.asyncio
async def test_filter_combined(client: AsyncClient, db_session: AsyncSession):
    await _seed_personas(db_session)
    await db_session.commit()

    resp = await client.get("/api/v1/voices/personas?gender=male&age_group=senior")
    assert resp.status_code == 200
    data = resp.json()
    assert len(data) == 1
    assert data[0]["clova_speaker"] == "youngil"


@pytest.mark.asyncio
async def test_get_single_persona(client: AsyncClient, db_session: AsyncSession):
    await _seed_personas(db_session)
    await db_session.commit()

    resp = await client.get("/api/v1/voices/personas/1")
    assert resp.status_code == 200
    assert resp.json()["clova_speaker"] == "nara"


@pytest.mark.asyncio
async def test_get_nonexistent_persona(client: AsyncClient):
    resp = await client.get("/api/v1/voices/personas/9999")
    assert resp.status_code == 404
