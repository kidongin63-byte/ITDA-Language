"""민감 개인정보(PII) 대칭 암호화 헬퍼.

birth_id(생년월일) 등 향후 활용이 필요하지만 평문 저장은 피해야 하는 값을
Fernet(AES128-CBC + HMAC)으로 암호화/복호화한다.

키 우선순위:
  1) 환경변수 BIRTH_ID_ENC_KEY (Fernet.generate_key() 로 생성한 44자 키)
  2) 미설정 시 JWT_SECRET_KEY에서 결정적으로 파생 (별도 env 없이 동작)
※ 키가 바뀌면 기존 암호문은 복호화되지 않으므로 운영에서는 전용 키 지정을 권장.
"""

import base64
import hashlib
from functools import lru_cache

from cryptography.fernet import Fernet, InvalidToken

from app.config import get_settings


@lru_cache
def _fernet() -> Fernet:
    settings = get_settings()
    key = settings.BIRTH_ID_ENC_KEY.strip()
    if key:
        fernet_key = key.encode()
    else:
        # 전용 키 미설정 시 JWT_SECRET_KEY에서 32바이트 키를 결정적으로 파생
        digest = hashlib.sha256(settings.JWT_SECRET_KEY.encode()).digest()
        fernet_key = base64.urlsafe_b64encode(digest)
    return Fernet(fernet_key)


def encrypt_pii(value: str) -> str:
    """평문 → 암호문(문자열)."""
    return _fernet().encrypt(value.encode()).decode()


def decrypt_pii(token: str | None) -> str | None:
    """암호문 → 평문. 복호화 실패 시 None."""
    if not token:
        return None
    try:
        return _fernet().decrypt(token.encode()).decode()
    except InvalidToken:
        return None
