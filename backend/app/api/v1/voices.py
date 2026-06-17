"""
음성 페르소나 및 커스텀 음성 API

카테고리 필터링: GET /personas?gender=female&age_group=young_adult&region=seoul&tone=bright
"""

from fastapi import APIRouter, Depends, HTTPException, Query, UploadFile, File, Form
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from pydantic import BaseModel

from app.core.auth import get_current_user
from app.core.database import get_db
from app.models.user import User
from app.models.voice_change_request import VoiceChangeRequest
from app.models.voice_persona import (
    VoicePersona,
    GENDERS,
    AGE_GROUPS,
    TONES,
    REGIONS,
)
from app.schemas.voice import (
    VoicePersonaResponse,
    VoiceCategoryListResponse,
    CustomVoiceUploadResponse,
    CustomVoiceResponse,
    CustomVoiceStatusResponse,
    UserPreferenceResponse,
    UserPreferenceUpdateRequest,
)
from app.services.custom_voice_service import custom_voice_service
from app.models.user_preference import UserPreference

router = APIRouter()


# ─── 카테고리 목록 ──────────────────────────────────

@router.get("/categories", response_model=VoiceCategoryListResponse)
async def get_voice_categories():
    """사용 가능한 모든 카테고리(성별, 연령, 지역, 톤) 목록 반환"""
    return VoiceCategoryListResponse(
        genders=GENDERS,
        age_groups=AGE_GROUPS,
        regions=REGIONS,
        tones=TONES,
    )


# ─── 프리셋 화자 목록 (카테고리 필터링) ─────────────

@router.get("/personas", response_model=list[VoicePersonaResponse])
async def list_personas(
    gender: str | None = Query(None, description="성별 필터 (male/female)"),
    age_group: str | None = Query(None, description="연령대 필터"),
    region: str | None = Query(None, description="지역 억양 필터"),
    tone: str | None = Query(None, description="톤 필터"),
    db: AsyncSession = Depends(get_db),
):
    """
    프리셋 화자 목록을 카테고리로 필터링하여 반환.
    필터를 지정하지 않으면 전체 활성 화자를 반환한다.
    """
    query = select(VoicePersona).where(VoicePersona.is_active == True)

    if gender:
        query = query.where(VoicePersona.gender == gender)
    if age_group:
        query = query.where(VoicePersona.age_group == age_group)
    if region:
        query = query.where(VoicePersona.region == region)
    if tone:
        query = query.where(VoicePersona.tone == tone)

    query = query.order_by(VoicePersona.sort_order, VoicePersona.id)

    result = await db.execute(query)
    return list(result.scalars().all())


@router.get("/personas/{persona_id}", response_model=VoicePersonaResponse)
async def get_persona(persona_id: int, db: AsyncSession = Depends(get_db)):
    result = await db.execute(
        select(VoicePersona).where(VoicePersona.id == persona_id)
    )
    persona = result.scalar_one_or_none()
    if persona is None:
        from app.core.exceptions import VoiceNotFoundError
        raise VoiceNotFoundError()
    return persona


# ─── 커스텀 음성 ────────────────────────────────────

@router.post("/custom/upload", response_model=CustomVoiceUploadResponse, status_code=201)
async def upload_custom_voice(
    voice_name: str = Form(..., description="음성 이름 (예: '엄마 목소리')"),
    reference_speaker: str | None = Form(None, description="기반 CLOVA 화자 코드"),
    file: UploadFile = File(..., description="녹음 파일 (WAV/MP3/M4A, 10초~5분)"),
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    """녹음 파일을 업로드하여 커스텀 음성을 등록"""
    audio_bytes = await file.read()
    voice = await custom_voice_service.upload_and_register(
        db=db,
        user_id=user.id,
        voice_name=voice_name,
        audio_bytes=audio_bytes,
        filename=file.filename or "audio.wav",
        reference_speaker=reference_speaker,
    )
    return voice


@router.get("/custom", response_model=list[CustomVoiceResponse])
async def list_custom_voices(
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    """내 커스텀 음성 목록"""
    return await custom_voice_service.get_user_voices(db, user.id)


@router.get("/custom/{voice_id}/status", response_model=CustomVoiceStatusResponse)
async def get_custom_voice_status(
    voice_id: str,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    """커스텀 음성 학습 상태 조회"""
    return await custom_voice_service.get_voice_status(db, voice_id, user.id)


@router.delete("/custom/{voice_id}", status_code=204)
async def delete_custom_voice(
    voice_id: str,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    """커스텀 음성 삭제"""
    await custom_voice_service.delete_voice(db, voice_id, user.id)


# ─── 사용자 설정 ────────────────────────────────────

@router.get("/preferences", response_model=UserPreferenceResponse)
async def get_preferences(
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    result = await db.execute(
        select(UserPreference).where(UserPreference.user_id == user.id)
    )
    prefs = result.scalar_one_or_none()
    if prefs is None:
        prefs = UserPreference(user_id=user.id)
        db.add(prefs)
        await db.flush()
    return prefs


@router.put("/preferences", response_model=UserPreferenceResponse)
async def update_preferences(
    body: UserPreferenceUpdateRequest,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    result = await db.execute(
        select(UserPreference).where(UserPreference.user_id == user.id)
    )
    prefs = result.scalar_one_or_none()
    if prefs is None:
        prefs = UserPreference(user_id=user.id)
        db.add(prefs)

    for field, value in body.model_dump(exclude_unset=True).items():
        setattr(prefs, field, value)

    await db.flush()
    return prefs


# ─── 음성 잠금 / 변경 요청 ─────────────────────────


class VoiceLockRequest(BaseModel):
    speaker: str
    voice_name: str


class VoiceChangeRequestBody(BaseModel):
    requested_speaker: str
    requested_voice_name: str
    reason: str = ""


@router.post("/lock")
async def lock_voice(
    body: VoiceLockRequest,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    """음성 선택 저장 (잠금 없이 자유 변경 가능)"""
    result = await db.execute(
        select(UserPreference).where(UserPreference.user_id == user.id)
    )
    prefs = result.scalar_one_or_none()
    if prefs is None:
        prefs = UserPreference(user_id=user.id)
        db.add(prefs)

    prefs.locked_speaker = body.speaker
    prefs.locked_voice_name = body.voice_name
    prefs.voice_locked = False
    await db.commit()
    return {"message": "음성이 저장되었습니다", "locked": False}


@router.get("/lock-status")
async def get_lock_status(
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    """음성 상태 확인 (항상 잠금 해제)"""
    result = await db.execute(
        select(UserPreference).where(UserPreference.user_id == user.id)
    )
    prefs = result.scalar_one_or_none()
    return {
        "locked": False,
        "speaker": prefs.locked_speaker if prefs else None,
        "voice_name": prefs.locked_voice_name if prefs else None,
    }


@router.post("/change-request")
async def request_voice_change(
    body: VoiceChangeRequestBody,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    """음성 변경 요청 제출"""
    req = VoiceChangeRequest(
        user_id=user.id,
        requested_speaker=body.requested_speaker,
        requested_voice_name=body.requested_voice_name,
        reason=body.reason,
    )
    db.add(req)
    await db.commit()
    return {"message": "변경 요청이 제출되었습니다", "request_id": req.id}
