import secrets
from datetime import datetime, timezone

from fastapi import APIRouter, Depends, HTTPException
from pydantic import BaseModel
from sqlalchemy import select, update
from sqlalchemy.ext.asyncio import AsyncSession

from app.config import get_settings
from app.core.auth import get_current_user
from app.core.database import get_db
from app.models.user import User
from app.models.user_preference import UserPreference
from app.models.voice_change_request import VoiceChangeRequest

router = APIRouter()

settings = get_settings()


class PinVerifyRequest(BaseModel):
    pin: str


class PinVerifyResponse(BaseModel):
    valid: bool
    is_admin: bool


class VoiceChangeRequestResponse(BaseModel):
    id: str
    user_id: str
    user_email: str
    user_nickname: str
    current_voice: str | None
    requested_speaker: str
    requested_voice_name: str
    reason: str
    status: str
    admin_notes: str | None
    created_at: datetime


class AdminActionRequest(BaseModel):
    notes: str = ""


# ─── PIN 검증 ───

@router.post("/verify-pin", response_model=PinVerifyResponse)
async def verify_admin_pin(
    body: PinVerifyRequest,
):
    # ADMIN_PIN 환경변수와 대조. 미설정 시 항상 실패(fail-closed)
    configured_pin = settings.ADMIN_PIN
    valid = bool(configured_pin) and secrets.compare_digest(body.pin, configured_pin)
    return PinVerifyResponse(valid=valid, is_admin=valid)


# ─── 변경 요청 목록 (관리자) ───

@router.get("/requests", response_model=list[VoiceChangeRequestResponse])
async def get_change_requests(
    status: str = "pending",
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    if user.role != "admin":
        raise HTTPException(403, "관리자 권한이 필요합니다")

    query = (
        select(VoiceChangeRequest, User, UserPreference)
        .join(User, VoiceChangeRequest.user_id == User.id)
        .outerjoin(UserPreference, UserPreference.user_id == User.id)
        .where(VoiceChangeRequest.status == status)
        .order_by(VoiceChangeRequest.created_at.desc())
    )
    result = await db.execute(query)
    rows = result.all()

    return [
        VoiceChangeRequestResponse(
            id=req.id,
            user_id=req.user_id,
            user_email=u.email,
            user_nickname=u.nickname,
            current_voice=pref.locked_voice_name if pref else None,
            requested_speaker=req.requested_speaker,
            requested_voice_name=req.requested_voice_name,
            reason=req.reason,
            status=req.status,
            admin_notes=req.admin_notes,
            created_at=req.created_at,
        )
        for req, u, pref in rows
    ]


# ─── 승인 ───

@router.post("/requests/{request_id}/approve")
async def approve_request(
    request_id: str,
    body: AdminActionRequest,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    if user.role != "admin":
        raise HTTPException(403, "관리자 권한이 필요합니다")

    result = await db.execute(
        select(VoiceChangeRequest).where(VoiceChangeRequest.id == request_id)
    )
    req = result.scalar_one_or_none()
    if not req:
        raise HTTPException(404, "요청을 찾을 수 없습니다")

    # 요청 승인
    req.status = "approved"
    req.admin_notes = body.notes
    req.resolved_at = datetime.now(timezone.utc)

    # 사용자 음성 변경
    await db.execute(
        update(UserPreference)
        .where(UserPreference.user_id == req.user_id)
        .values(
            locked_speaker=req.requested_speaker,
            locked_voice_name=req.requested_voice_name,
        )
    )

    await db.commit()
    return {"message": "승인 완료"}


# ─── 거부 ───

@router.post("/requests/{request_id}/reject")
async def reject_request(
    request_id: str,
    body: AdminActionRequest,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    if user.role != "admin":
        raise HTTPException(403, "관리자 권한이 필요합니다")

    result = await db.execute(
        select(VoiceChangeRequest).where(VoiceChangeRequest.id == request_id)
    )
    req = result.scalar_one_or_none()
    if not req:
        raise HTTPException(404, "요청을 찾을 수 없습니다")

    req.status = "rejected"
    req.admin_notes = body.notes
    req.resolved_at = datetime.now(timezone.utc)

    await db.commit()
    return {"message": "거부 완료"}
