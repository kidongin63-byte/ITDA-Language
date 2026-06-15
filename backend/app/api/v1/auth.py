from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.auth import (
    hash_password,
    verify_password,
    create_access_token,
    create_refresh_token,
    decode_token,
    get_current_user,
)
from app.core.database import get_db
from app.models.user import User
from app.models.user_preference import UserPreference
from app.schemas.user import (
    UserRegisterRequest,
    UserLoginRequest,
    TokenResponse,
    RefreshRequest,
    UserResponse,
)

router = APIRouter()


@router.post("/register", response_model=TokenResponse, status_code=201)
async def register(body: UserRegisterRequest, db: AsyncSession = Depends(get_db)):
    # 이메일 중복 확인
    existing = await db.execute(select(User).where(User.email == body.email))
    if existing.scalar_one_or_none():
        raise HTTPException(status_code=409, detail="이미 등록된 이메일입니다")

    # 전화번호 중복 확인
    if body.phone:
        existing_phone = await db.execute(select(User).where(User.phone == body.phone))
        if existing_phone.scalar_one_or_none():
            raise HTTPException(status_code=409, detail="이미 등록된 전화번호입니다")

    # 주민번호 앞자리 + 이름 조합 중복 확인 (동일인 재가입 방지)
    if body.birth_id and body.nickname:
        from sqlalchemy import and_
        existing_person = await db.execute(
            select(User).where(
                and_(User.birth_id == body.birth_id, User.nickname == body.nickname)
            )
        )
        if existing_person.scalar_one_or_none():
            raise HTTPException(status_code=409, detail="이미 가입된 회원입니다")

    user = User(
        email=body.email,
        nickname=body.nickname,
        phone=body.phone or None,
        gender=body.gender or None,
        birth_id=body.birth_id or None,
        hashed_password=hash_password(body.password),
    )
    db.add(user)
    await db.flush()

    # 기본 설정 생성
    prefs = UserPreference(user_id=user.id)
    db.add(prefs)

    return TokenResponse(
        access_token=create_access_token(user.id),
        refresh_token=create_refresh_token(user.id),
    )


@router.post("/login", response_model=TokenResponse)
async def login(body: UserLoginRequest, db: AsyncSession = Depends(get_db)):
    result = await db.execute(select(User).where(User.email == body.email))
    user = result.scalar_one_or_none()
    if user is None or not verify_password(body.password, user.hashed_password):
        raise HTTPException(status_code=401, detail="이메일 또는 비밀번호가 올바르지 않습니다")

    return TokenResponse(
        access_token=create_access_token(user.id),
        refresh_token=create_refresh_token(user.id),
    )


@router.post("/refresh", response_model=TokenResponse)
async def refresh(body: RefreshRequest, db: AsyncSession = Depends(get_db)):
    payload = decode_token(body.refresh_token)
    if payload.get("type") != "refresh":
        raise HTTPException(status_code=401, detail="리프레시 토큰이 아닙니다")

    user_id = payload.get("sub")
    result = await db.execute(select(User).where(User.id == user_id))
    if result.scalar_one_or_none() is None:
        raise HTTPException(status_code=401, detail="사용자를 찾을 수 없습니다")

    return TokenResponse(
        access_token=create_access_token(user_id),
        refresh_token=create_refresh_token(user_id),
    )


@router.get("/me", response_model=UserResponse)
async def get_me(user: User = Depends(get_current_user)):
    return user
