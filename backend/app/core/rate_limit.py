"""Rate Limiting (인메모리 슬라이딩 윈도우)

주의: 프로세스 메모리 기반이라 단일 인스턴스에서만 정확합니다.
다중 워커/인스턴스로 확장할 경우 Redis 기반 구현으로 교체하세요.
"""

import time
from collections import defaultdict, deque

from fastapi import HTTPException, status

# key -> 최근 요청 타임스탬프(deque)
_hits: dict[str, deque] = defaultdict(deque)


def _check(key: str, limit: int, window_seconds: int) -> None:
    now = time.monotonic()
    bucket = _hits[key]
    # 윈도우를 벗어난 오래된 기록 제거
    while bucket and now - bucket[0] > window_seconds:
        bucket.popleft()
    if len(bucket) >= limit:
        retry_after = int(window_seconds - (now - bucket[0])) + 1
        raise HTTPException(
            status_code=status.HTTP_429_TOO_MANY_REQUESTS,
            detail="요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.",
            headers={"Retry-After": str(retry_after)},
        )
    bucket.append(now)


async def check_rate_limit(user_id: str) -> None:
    """TTS 합성 호출 제한: 사용자당 분당 30회"""
    _check(f"tts:{user_id}", limit=30, window_seconds=60)


def check_login_rate_limit(identifier: str) -> None:
    """로그인 브루트포스 방어: 식별자(IP+이메일)당 5분에 10회"""
    _check(f"login:{identifier}", limit=10, window_seconds=300)


def check_register_rate_limit(identifier: str) -> None:
    """회원가입 스팸 방어: IP당 1시간에 5회"""
    _check(f"register:{identifier}", limit=5, window_seconds=3600)
