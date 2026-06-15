"""
커스텀 음성 관리 서비스

녹음 업로드 → 전처리 → Object Storage 저장 → Celery 학습 태스크 트리거
"""

import uuid

import boto3
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.config import get_settings
from app.core.exceptions import VoiceNotFoundError, CustomVoiceNotReadyError
from app.models.custom_voice import CustomVoice
from app.services.audio_preprocessor import validate_and_preprocess

settings = get_settings()


def _get_s3_client():
    return boto3.client(
        "s3",
        endpoint_url=settings.S3_ENDPOINT_URL or None,
        aws_access_key_id=settings.S3_ACCESS_KEY,
        aws_secret_access_key=settings.S3_SECRET_KEY,
    )


class CustomVoiceService:

    async def upload_and_register(
        self,
        db: AsyncSession,
        user_id: str,
        voice_name: str,
        audio_bytes: bytes,
        filename: str,
        reference_speaker: str | None = None,
    ) -> CustomVoice:
        """녹음 업로드 → 전처리 → 저장 → DB 등록"""
        # 1. 전처리
        processed_bytes, duration = validate_and_preprocess(audio_bytes, filename)

        # 2. Object Storage 업로드
        voice_id = str(uuid.uuid4())
        raw_key = f"custom_voices/{user_id}/{voice_id}/raw.wav"
        processed_key = f"custom_voices/{user_id}/{voice_id}/processed.wav"
        embedding_key = f"custom_voices/{user_id}/{voice_id}/embedding.pt"

        s3 = _get_s3_client()
        s3.put_object(
            Bucket=settings.S3_BUCKET_NAME, Key=raw_key, Body=audio_bytes
        )
        s3.put_object(
            Bucket=settings.S3_BUCKET_NAME, Key=processed_key, Body=processed_bytes
        )

        # 3. DB 등록
        custom_voice = CustomVoice(
            id=voice_id,
            user_id=user_id,
            voice_name=voice_name,
            embedding_path=embedding_key,
            reference_speaker=reference_speaker,
            sample_duration_sec=duration,
            status="processing",
        )
        db.add(custom_voice)
        await db.flush()

        # 4. Celery 학습 태스크 트리거
        from app.tasks.voice_training import train_voice_embedding

        train_voice_embedding.delay(voice_id, processed_key, embedding_key)

        return custom_voice

    async def get_user_voices(
        self, db: AsyncSession, user_id: str
    ) -> list[CustomVoice]:
        result = await db.execute(
            select(CustomVoice)
            .where(CustomVoice.user_id == user_id)
            .order_by(CustomVoice.created_at.desc())
        )
        return list(result.scalars().all())

    async def get_voice_status(
        self, db: AsyncSession, voice_id: str, user_id: str
    ) -> CustomVoice:
        result = await db.execute(
            select(CustomVoice).where(
                CustomVoice.id == voice_id, CustomVoice.user_id == user_id
            )
        )
        voice = result.scalar_one_or_none()
        if voice is None:
            raise VoiceNotFoundError()
        return voice

    async def delete_voice(
        self, db: AsyncSession, voice_id: str, user_id: str
    ) -> None:
        voice = await self.get_voice_status(db, voice_id, user_id)

        # Object Storage 파일 삭제
        s3 = _get_s3_client()
        prefix = f"custom_voices/{user_id}/{voice_id}/"
        response = s3.list_objects_v2(
            Bucket=settings.S3_BUCKET_NAME, Prefix=prefix
        )
        if "Contents" in response:
            objects = [{"Key": obj["Key"]} for obj in response["Contents"]]
            s3.delete_objects(
                Bucket=settings.S3_BUCKET_NAME, Delete={"Objects": objects}
            )

        await db.delete(voice)


custom_voice_service = CustomVoiceService()
