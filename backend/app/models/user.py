import uuid
from datetime import datetime, timezone

from sqlalchemy import String, DateTime
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.core.database import Base


class User(Base):
    __tablename__ = "users"

    id: Mapped[str] = mapped_column(
        String(36), primary_key=True, default=lambda: str(uuid.uuid4())
    )
    email: Mapped[str] = mapped_column(String(255), unique=True, index=True)
    nickname: Mapped[str] = mapped_column(String(100))  # 실명
    phone: Mapped[str | None] = mapped_column(String(20), unique=True, nullable=True, index=True)
    gender: Mapped[str | None] = mapped_column(String(10), nullable=True)  # "male" or "female"
    birth_id: Mapped[str | None] = mapped_column(String(255), nullable=True)  # 생년월일(YYMMDD) 암호화 저장 — app.core.crypto
    hashed_password: Mapped[str] = mapped_column(String(255))
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), default=lambda: datetime.now(timezone.utc)
    )
    role: Mapped[str] = mapped_column(String(20), default="user")  # "user" or "admin"
    admin_pin_hash: Mapped[str | None] = mapped_column(String(255), nullable=True)

    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        default=lambda: datetime.now(timezone.utc),
        onupdate=lambda: datetime.now(timezone.utc),
    )

    # Relationships
    custom_voices = relationship("CustomVoice", back_populates="user", cascade="all, delete-orphan")
    preferences = relationship("UserPreference", back_populates="user", uselist=False, cascade="all, delete-orphan")
    favorite_phrases = relationship("FavoritePhrase", back_populates="user", cascade="all, delete-orphan")
