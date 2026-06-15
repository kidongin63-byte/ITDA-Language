"""
Celery 태스크: 커스텀 음성 학습 (OpenVoice v2 음색 임베딩 추출)

업로드된 오디오에서 화자 음색 임베딩을 추출하여 Object Storage에 저장한다.
실제 OpenVoice v2 모델은 GPU 서버에서 실행되며,
여기서는 태스크 프레임워크와 상태 업데이트 로직을 구현한다.
"""

from celery import Celery

from app.config import get_settings

settings = get_settings()

celery_app = Celery(
    "itda_tasks",
    broker=settings.CELERY_BROKER_URL,
    backend=settings.CELERY_RESULT_BACKEND,
)

celery_app.conf.update(
    task_serializer="json",
    result_serializer="json",
    accept_content=["json"],
    timezone="Asia/Seoul",
    enable_utc=True,
)


@celery_app.task(name="train_voice_embedding", bind=True, max_retries=2)
def train_voice_embedding(self, voice_id: str, audio_key: str, embedding_key: str):
    """
    커스텀 음성 학습 태스크

    1. Object Storage에서 전처리된 오디오 다운로드
    2. OpenVoice v2로 음색 임베딩 추출
    3. 임베딩을 Object Storage에 업로드
    4. DB 상태를 'ready'로 업데이트

    NOTE: 실제 OpenVoice v2 추론 코드는 GPU 환경 구성 후 추가 구현.
          현재는 태스크 골격과 상태 관리 로직만 포함.
    """
    import boto3
    from sqlalchemy import create_engine, update
    from sqlalchemy.orm import Session

    from app.models.custom_voice import CustomVoice

    # 동기 DB 엔진 (Celery 워커에서는 async 불가)
    sync_db_url = settings.DATABASE_URL.replace("+asyncpg", "+psycopg2")
    sync_engine = create_engine(sync_db_url)

    try:
        # 1. 오디오 다운로드
        s3 = boto3.client(
            "s3",
            endpoint_url=settings.S3_ENDPOINT_URL or None,
            aws_access_key_id=settings.S3_ACCESS_KEY,
            aws_secret_access_key=settings.S3_SECRET_KEY,
        )
        response = s3.get_object(Bucket=settings.S3_BUCKET_NAME, Key=audio_key)
        audio_bytes = response["Body"].read()

        # 2. OpenVoice v2 음색 임베딩 추출
        # TODO: GPU 환경 구성 후 실제 추론 코드 구현
        # from openvoice import se_extractor
        # embedding = se_extractor.get_se(audio_bytes, ...)
        # torch.save(embedding, embedding_path)

        # 임시: 빈 임베딩 파일 생성 (개발용 플레이스홀더)
        placeholder_embedding = b"PLACEHOLDER_EMBEDDING"
        s3.put_object(
            Bucket=settings.S3_BUCKET_NAME,
            Key=embedding_key,
            Body=placeholder_embedding,
        )

        # 3. DB 상태 업데이트 → ready
        with Session(sync_engine) as session:
            session.execute(
                update(CustomVoice)
                .where(CustomVoice.id == voice_id)
                .values(status="ready")
            )
            session.commit()

    except Exception as exc:
        # 실패 상태 기록
        with Session(sync_engine) as session:
            session.execute(
                update(CustomVoice)
                .where(CustomVoice.id == voice_id)
                .values(status="failed", error_message=str(exc)[:500])
            )
            session.commit()

        # 재시도
        raise self.retry(exc=exc, countdown=60)

    finally:
        sync_engine.dispose()
