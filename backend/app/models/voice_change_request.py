import uuid
from datetime import datetime, timezone

from sqlalchemy import String, DateTime, ForeignKey, Text
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.core.database import Base


class VoiceChangeRequest(Base):
    __tablename__ = "voice_change_requests"

    id: Mapped[str] = mapped_column(
        String(36), primary_key=True, default=lambda: str(uuid.uuid4())
    )
    user_id: Mapped[str] = mapped_column(
        String(36), ForeignKey("users.id", ondelete="CASCADE"), index=True
    )
    requested_speaker: Mapped[str] = mapped_column(String(100))
    requested_voice_name: Mapped[str] = mapped_column(String(200))
    reason: Mapped[str] = mapped_column(Text, default="")
    status: Mapped[str] = mapped_column(String(20), default="pending")  # pending/approved/rejected
    admin_notes: Mapped[str | None] = mapped_column(Text, nullable=True)

    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), default=lambda: datetime.now(timezone.utc)
    )
    resolved_at: Mapped[datetime | None] = mapped_column(
        DateTime(timezone=True), nullable=True
    )

    user = relationship("User")
