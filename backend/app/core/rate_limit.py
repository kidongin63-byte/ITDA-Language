"""Rate Limiting (no-op: Redis 없이 제한 없이 통과)"""


async def check_rate_limit(user_id: str) -> None:
    pass
