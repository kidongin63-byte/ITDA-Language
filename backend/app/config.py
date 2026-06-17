from pydantic_settings import BaseSettings
from functools import lru_cache


class Settings(BaseSettings):
    # App
    APP_NAME: str = "ITDA Language"
    APP_VERSION: str = "0.1.0"
    DEBUG: bool = False

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
    JWT_SECRET_KEY: str = "change-me-in-production"
    JWT_ALGORITHM: str = "HS256"
    JWT_ACCESS_TOKEN_EXPIRE_MINUTES: int = 10080  # 7일
    JWT_REFRESH_TOKEN_EXPIRE_DAYS: int = 90

    # CLOVA Voice API
    CLOVA_API_URL: str = "https://naveropenapi.apigw.ntruss.com/tts-premium/v1/tts"
    CLOVA_CLIENT_ID: str = ""
    CLOVA_CLIENT_SECRET: str = ""

    # TTS Engine: "edge" (무료, 기본) 또는 "clova" (유료, 고도화 시)
    TTS_ENGINE: str = "edge"

    # Audio
    TTS_CACHE_TTL_SECONDS: int = 86400  # 24시간
    MAX_TEXT_LENGTH: int = 1000

    model_config = {"env_file": ".env", "env_file_encoding": "utf-8"}


@lru_cache
def get_settings() -> Settings:
    return Settings()
