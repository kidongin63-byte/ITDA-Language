from fastapi import APIRouter, Depends
from fastapi.responses import Response
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.auth import get_current_user
from app.core.database import get_db
from app.core.rate_limit import check_rate_limit
from app.models.user import User
from app.schemas.tts import TTSSynthesizeRequest
from app.services.tts_orchestrator import tts_orchestrator

router = APIRouter()


@router.post("/synthesize")
async def synthesize(
    body: TTSSynthesizeRequest,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    """텍스트를 음성으로 합성하여 오디오 바이너리를 반환"""
    await check_rate_limit(user.id)
    audio = await tts_orchestrator.synthesize_with_cache(body, user.id, db)

    content_type = "audio/mpeg" if body.format == "mp3" else "audio/wav"
    return Response(content=audio, media_type=content_type)
