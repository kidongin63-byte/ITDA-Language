"""
Redis 기반 Rate Limiting

CLOVA API 과도 호출을 방지하고, 사용자별 TTS 요청 횟수를 제한한다.
Redis가 없으면 제한 없이 통과시킨다 (cache_service와 동일 원칙).
"""

import logging
import time

from fastapi import HTTPException, status

from app.services.cache_service import get_redis

logger = logging.getLogger(__name__)

# 기본 제한: 사용자당 분당 30회, 일당 1000회
RATE_LIMIT_PER_MINUTE = 30
RATE_LIMIT_PER_DAY = 1000


async def check_rate_limit(user_id: str) -> None:
    """
    사용자별 TTS 요청 횟수를 확인하고 제한을 초과하면 429를 반환한다.
    Redis 연결 실패 시에는 제한 없이 통과시킨다.
    """
    try:
        r = await get_redis()
        if r is None:
            return

        now = int(time.time())
        minute_key = f"rate:{user_id}:min:{now // 60}"
        day_key = f"rate:{user_id}:day:{now // 86400}"

        # 분당 체크
        minute_count = await r.incr(minute_key)
        if minute_count == 1:
            await r.expire(minute_key, 60)
        if minute_count > RATE_LIMIT_PER_MINUTE:
            raise HTTPException(
                status_code=status.HTTP_429_TOO_MANY_REQUESTS,
                detail=f"요청이 너무 많습니다. 분당 {RATE_LIMIT_PER_MINUTE}회까지 가능합니다.",
            )

        # 일당 체크
        day_count = await r.incr(day_key)
        if day_count == 1:
            await r.expire(day_key, 86400)
        if day_count > RATE_LIMIT_PER_DAY:
            raise HTTPException(
                status_code=status.HTTP_429_TOO_MANY_REQUESTS,
                detail=f"일일 사용량을 초과했습니다. 하루 {RATE_LIMIT_PER_DAY}회까지 가능합니다.",
            )

    except HTTPException:
        raise
    except Exception:
        logger.warning("Rate limit 확인 실패 — 제한 없이 통과")
