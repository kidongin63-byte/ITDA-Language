import uuid
from datetime import datetime, timezone

from sqlalchemy import String, Integer, Text, DateTime, ForeignKey
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.core.database import Base


class FavoritePhrase(Base):
    __tablename__ = "favorite_phrases"

    id: Mapped[str] = mapped_column(
        String(36), primary_key=True, default=lambda: str(uuid.uuid4())
    )
    user_id: Mapped[str] = mapped_column(
        String(36), ForeignKey("users.id", ondelete="CASCADE"), index=True
    )

    phrase_text: Mapped[str] = mapped_column(Text)

    # 분류: greeting(인사), daily(일상), emergency(긴급), order(주문), medical(의료), custom(사용자정의)
    category: Mapped[str] = mapped_column(String(50), default="custom", index=True)

    usage_count: Mapped[int] = mapped_column(Integer, default=0)

    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), default=lambda: datetime.now(timezone.utc)
    )

    # Relationships
    user = relationship("User", back_populates="favorite_phrases")


class TTSUsageLog(Base):
    __tablename__ = "tts_usage_logs"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    user_id: Mapped[str] = mapped_column(
        String(36), ForeignKey("users.id"), index=True
    )
    char_count: Mapped[int] = mapped_column(Integer)
    voice_type: Mapped[str] = mapped_column(String(20))  # 'clova' or 'custom'
    speaker_code: Mapped[str | None] = mapped_column(String(50), nullable=True)
    latency_ms: Mapped[int | None] = mapped_column(Integer, nullable=True)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), default=lambda: datetime.now(timezone.utc)
    )
