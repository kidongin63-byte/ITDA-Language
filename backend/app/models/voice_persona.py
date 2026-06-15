"""
음성 페르소나 모델 — 카테고리 기반 분류 시스템

카테고리 구조 (선택 순서):
  1. 성별 (gender): male / female
  2. 연령대 (age_group): child, teen, young_adult, middle_aged, senior
  3. 지역 억양 (region): 17개 시도 — 서울, 경기, 강원, 충북, 충남, 전북, 전남, 경북, 경남,
                                      제주, 부산, 대구, 인천, 광주, 대전, 울산, 세종
  4. 톤 (tone): 10가지 — bright, calm, warm, cool, energetic, gentle, serious, friendly, elegant, husky
"""

from sqlalchemy import String, Integer, Boolean
from sqlalchemy.orm import Mapped, mapped_column

from app.core.database import Base


class VoicePersona(Base):
    __tablename__ = "voice_personas"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)

    # CLOVA 화자 코드 (예: 'nara', 'jinho', 'mijin')
    clova_speaker: Mapped[str] = mapped_column(String(50), unique=True)

    # 표시 이름 (예: '20대 여성 - 밝은 톤 (서울)')
    display_name: Mapped[str] = mapped_column(String(200))

    # === 카테고리 1: 성별 ===
    gender: Mapped[str] = mapped_column(
        String(10), index=True  # 'male', 'female'
    )

    # === 카테고리 2: 연령대 ===
    age_group: Mapped[str] = mapped_column(
        String(20), index=True
        # 'child'        : 어린이 (~ 12세)
        # 'teen'         : 청소년 (13 ~ 18세)
        # 'young_adult'  : 청년 (19 ~ 35세)
        # 'middle_aged'  : 중년 (36 ~ 55세)
        # 'senior'       : 노년 (56세 ~)
    )

    # === 카테고리 3: 시도별 억양 ===
    region: Mapped[str] = mapped_column(
        String(20), index=True
        # 'seoul'      : 서울 (표준어)
        # 'gyeonggi'   : 경기
        # 'gangwon'    : 강원
        # 'chungbuk'   : 충북
        # 'chungnam'   : 충남
        # 'jeonbuk'    : 전북
        # 'jeonnam'    : 전남
        # 'gyeongbuk'  : 경북
        # 'gyeongnam'  : 경남
        # 'jeju'       : 제주
        # 'busan'      : 부산
        # 'daegu'      : 대구
        # 'incheon'    : 인천
        # 'gwangju'    : 광주
        # 'daejeon'    : 대전
        # 'ulsan'      : 울산
        # 'sejong'     : 세종
    )

    # === 카테고리 4: 톤 (10종) ===
    tone: Mapped[str] = mapped_column(
        String(30), index=True
        # 'bright'     : 밝은
        # 'calm'       : 차분한
        # 'warm'       : 따뜻한
        # 'cool'       : 시원한/쿨한
        # 'energetic'  : 활기찬
        # 'gentle'     : 부드러운
        # 'serious'    : 진지한
        # 'friendly'   : 다정한
        # 'elegant'    : 우아한
        # 'husky'      : 허스키한
    )

    # 감정 표현 지원 여부
    emotion_support: Mapped[bool] = mapped_column(Boolean, default=False)

    # 미리듣기 샘플 오디오 경로
    preview_audio_url: Mapped[str | None] = mapped_column(String(500), nullable=True)

    # 활성 상태
    is_active: Mapped[bool] = mapped_column(Boolean, default=True, index=True)

    # 정렬 순서
    sort_order: Mapped[int] = mapped_column(Integer, default=0)


# ─── 카테고리 상수 (코드 전체에서 재사용) ─────────────────────────

GENDERS = {"male": "남성", "female": "여성"}

AGE_GROUPS = {
    "child": "어린이",
    "teen": "청소년",
    "young_adult": "청년",
    "middle_aged": "중년",
    "senior": "시니어",
}

TONES = {
    "bright": "밝은",
    "calm": "차분한",
    "warm": "따뜻한",
    "cool": "시원한",
    "energetic": "활기찬",
    "gentle": "부드러운",
    "serious": "진지한",
    "friendly": "다정한",
    "elegant": "우아한",
    "husky": "허스키한",
}

REGIONS = {
    "seoul": "서울 (표준어)",
    "gyeonggi": "경기",
    "gangwon": "강원",
    "chungbuk": "충북",
    "chungnam": "충남",
    "jeonbuk": "전북",
    "jeonnam": "전남",
    "gyeongbuk": "경북",
    "gyeongnam": "경남",
    "jeju": "제주",
    "busan": "부산",
    "daegu": "대구",
    "incheon": "인천",
    "gwangju": "광주",
    "daejeon": "대전",
    "ulsan": "울산",
    "sejong": "세종",
}
