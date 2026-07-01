import re

from pydantic import BaseModel, EmailStr, Field, field_validator


class UserRegisterRequest(BaseModel):
    email: EmailStr
    nickname: str  # 실명
    phone: str = ""  # 전화번호
    gender: str = ""  # "male" or "female"
    birth_id: str = ""  # 주민번호 앞자리 (YYMMDD) — 향후 활용 대비 수집 유지
    password: str = Field(min_length=8, max_length=128)

    @field_validator("password")
    @classmethod
    def validate_password_strength(cls, v: str) -> str:
        # 최소 8자 + 영문/숫자 조합 요구
        if not re.search(r"[A-Za-z]", v) or not re.search(r"\d", v):
            raise ValueError("비밀번호는 영문과 숫자를 모두 포함해야 합니다")
        return v


class UserLoginRequest(BaseModel):
    email: EmailStr
    password: str


class TokenResponse(BaseModel):
    access_token: str
    refresh_token: str
    token_type: str = "bearer"


class RefreshRequest(BaseModel):
    refresh_token: str


class UserResponse(BaseModel):
    id: str
    email: str
    nickname: str
    phone: str | None = None
    gender: str | None = None

    model_config = {"from_attributes": True}
