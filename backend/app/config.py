from pydantic_settings import BaseSettings
from functools import lru_cache


DEFAULT_JWT_SECRET = "change-me-in-production"


class Settings(BaseSettings):
    # App
    APP_NAME: str = "ITDA Language"
    APP_VERSION: str = "0.1.0"
    DEBUG: bool = False
    # "development" 또는 "production" (Render 배포 시 ENVIRONMENT=production 지정)
    ENVIRONMENT: str = "development"

    @property
    def is_production(self) -> bool:
        return self.ENVIRONMENT.lower() == "production"

    # CORS 허용 오리진 (쉼표로 구분). 개발은 전체 허용, 운영은 실제 도메인만 지정
    CORS_ORIGINS: str = "*"

    @property
    def cors_origin_list(self) -> list[str]:
        return [o.strip() for o in self.CORS_ORIGINS.split(",") if o.strip()]

    # 관리자 PIN (환경변수 ADMIN_PIN으로 주입, 비어 있으면 관리자 인증 비활성화)
    ADMIN_PIN: str = ""

    # PII(birth_id) 암호화 키. 미설정 시 JWT_SECRET_KEY에서 파생 (app.core.crypto 참고)
    # 운영에서는 `python -c "from cryptography.fernet import Fernet; print(Fernet.generate_key().decode())"` 로 생성한 값 권장
    BIRTH_ID_ENC_KEY: str = ""

    # Database (SQLite by default)
    DATABASE_URL: str = "sqlite+aiosqlite:///./itda_dev.db"

    @property
    def effective_database_url(self) -> str:
        """Render 환경에서는 PostgreSQL, 로컬은 SQLite"""
        import os
        # Render PostgreSQL (환경변수 DATABASE_URL이 postgres://로 시작)
        db_url = os.environ.get("DATABASE_URL", self.DATABASE_URL)
        if db_url.startswith("postgres://"):
            db_url = db_url.replace("postgres://", "postgresql+asyncpg://", 1)
        elif db_url.startswith("postgresql://"):
            db_url = db_url.replace("postgresql://", "postgresql+asyncpg://", 1)
        return db_url

    # JWT Auth
    JWT_SECRET_KEY: str = DEFAULT_JWT_SECRET
    JWT_ALGORITHM: str = "HS256"
    JWT_ACCESS_TOKEN_EXPIRE_MINUTES: int = 10080  # 7일
    JWT_REFRESH_TOKEN_EXPIRE_DAYS: int = 90

    # CLOVA Voice API
    CLOVA_API_URL: str = "https://naveropenapi.apigw.ntruss.com/tts-premium/v1/tts"
    CLOVA_CLIENT_ID: str = ""
    CLOVA_CLIENT_SECRET: str = ""

    # OpenAI API (번역용)
    OPENAI_API_KEY: str = ""

    # TTS Engine: "edge" (무료, 기본) 또는 "clova" (유료, 고도화 시)
    TTS_ENGINE: str = "edge"

    # Audio
    TTS_CACHE_TTL_SECONDS: int = 86400  # 24시간
    MAX_TEXT_LENGTH: int = 1000

    model_config = {"env_file": ".env", "env_file_encoding": "utf-8"}


@lru_cache
def get_settings() -> Settings:
    settings = Settings()
    # 운영 환경에서 알려진 약한 비밀키를 그대로 쓰면 토큰 위조가 가능하므로 기동을 차단
    weak_secrets = {DEFAULT_JWT_SECRET, "production-secret-change-me", "secret", "changeme"}
    if settings.is_production and settings.JWT_SECRET_KEY in weak_secrets:
        raise RuntimeError(
            "보안 오류: 운영 환경에서는 JWT_SECRET_KEY 환경변수를 반드시 설정해야 합니다. "
            "예) `python -c \"import secrets; print(secrets.token_urlsafe(64))\"` 로 생성한 값을 지정하세요."
        )
    if settings.is_production and settings.cors_origin_list == ["*"]:
        raise RuntimeError(
            "보안 오류: 운영 환경에서는 CORS_ORIGINS를 실제 도메인으로 지정해야 합니다. "
            "예) CORS_ORIGINS=https://itda-language.onrender.com"
        )
    return settings
