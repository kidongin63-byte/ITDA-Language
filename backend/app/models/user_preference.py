import uuid
from datetime import datetime, timezone

from sqlalchemy import String, Integer, SmallInteger, Boolean, DateTime, ForeignKey
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.core.database import Base


class UserPreference(Base):
    __tablename__ = "user_preferences"

    id: Mapped[str] = mapped_column(
        String(36), primary_key=True, default=lambda: str(uuid.uuid4())
    )
    user_id: Mapped[str] = mapped_column(
        String(36), ForeignKey("users.id", ondelete="CASCADE"), unique=True
    )

    # 기본 음성 타입: 'preset' (CLOVA) 또는 'custom'
    default_voice_type: Mapped[str] = mapped_column(String(20), default="preset")

    # 기본 프리셋 화자 ID
    default_persona_id: Mapped[int | None] = mapped_column(
        Integer, ForeignKey("voice_personas.id"), nullable=True
    )

    # 기본 커스텀 음성 ID
    default_custom_voice_id: Mapped[str | None] = mapped_column(
        String(36), ForeignKey("custom_voices.id"), nullable=True
    )

    # 음성 조절 기본값 (-5 ~ 5)
    default_speed: Mapped[int] = mapped_column(SmallInteger, default=0)
    default_pitch: Mapped[int] = mapped_column(SmallInteger, default=0)
    default_volume: Mapped[int] = mapped_column(SmallInteger, default=0)

    # 음성 잠금 (최초 선택 후 잠금)
    voice_locked: Mapped[bool] = mapped_column(Boolean, default=False)
    locked_speaker: Mapped[str | None] = mapped_column(String(100), nullable=True)
    locked_voice_name: Mapped[str | None] = mapped_column(String(200), nullable=True)

    # 자동 재생 여부
    auto_play: Mapped[bool] = mapped_column(Boolean, default=True)

    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        default=lambda: datetime.now(timezone.utc),
        onupdate=lambda: datetime.now(timezone.utc),
    )

    # Relationships
    user = relationship("User", back_populates="preferences")
