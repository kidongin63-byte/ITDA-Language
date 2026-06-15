import uuid
from datetime import datetime, timezone

from sqlalchemy import String, Float, DateTime, ForeignKey
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.core.database import Base


class CustomVoice(Base):
    __tablename__ = "custom_voices"

    id: Mapped[str] = mapped_column(
        String(36), primary_key=True, default=lambda: str(uuid.uuid4())
    )
    user_id: Mapped[str] = mapped_column(
        String(36), ForeignKey("users.id", ondelete="CASCADE"), index=True
    )

    # 사용자가 지정한 이름 (예: '엄마 목소리', '아빠 목소리')
    voice_name: Mapped[str] = mapped_column(String(100))

    # OpenVoice v2 음색 임베딩 파일 경로
    embedding_path: Mapped[str] = mapped_column(String(500))

    # 기반이 되는 CLOVA 참조 화자 코드
    reference_speaker: Mapped[str | None] = mapped_column(String(50), nullable=True)

    # 원본 녹음 길이 (초)
    sample_duration_sec: Mapped[float | None] = mapped_column(Float, nullable=True)

    # 학습 상태: processing / ready / failed
    status: Mapped[str] = mapped_column(String(20), default="processing", index=True)

    # 실패 시 사유
    error_message: Mapped[str | None] = mapped_column(String(500), nullable=True)

    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), default=lambda: datetime.now(timezone.utc)
    )

    # Relationships
    user = relationship("User", back_populates="custom_voices")
