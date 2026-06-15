from pydantic import BaseModel, EmailStr


class UserRegisterRequest(BaseModel):
    email: EmailStr
    nickname: str  # 실명
    phone: str = ""  # 전화번호
    gender: str = ""  # "male" or "female"
    birth_id: str = ""  # 주민번호 앞자리 (YYMMDD)
    password: str


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
