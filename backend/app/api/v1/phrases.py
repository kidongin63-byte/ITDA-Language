from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy import select, update
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.auth import get_current_user
from app.core.database import get_db
from app.models.user import User
from app.models.phrase import FavoritePhrase
from app.schemas.voice import PhraseCreateRequest, PhraseResponse

router = APIRouter()

# ─── 기본 프리셋 문구 (로그인 불필요) ───────────────

PRESET_PHRASES = {
    "greeting": [
        "안녕하세요.",
        "반갑습니다.",
        "안녕히 가세요.",
        "오랜만이에요.",
        "좋은 하루 되세요.",
        "처음 뵙겠습니다.",
        "만나서 반갑습니다.",
        "안녕히 계세요.",
        "좋은 아침입니다.",
        "수고하셨습니다.",
    ],
    "request": [
        "잠시만 기다려 주세요.",
        "다시 한번 말씀해 주세요.",
        "천천히 말씀해 주시겠어요?",
        "글로 써 주시겠어요?",
        "도움이 필요합니다.",
        "이쪽으로 와 주시겠어요?",
        "조금만 크게 말씀해 주세요.",
        "화면을 보여주시겠어요?",
        "한 번 더 확인해 주세요.",
        "자리를 안내해 주시겠어요?",
    ],
    "thanks": [
        "감사합니다.",
        "고맙습니다.",
        "덕분에 잘 됐어요.",
        "정말 감사드립니다.",
        "친절하게 대해 주셔서 감사합니다.",
        "도와주셔서 감사합니다.",
        "배려해 주셔서 고맙습니다.",
        "시간 내주셔서 감사합니다.",
        "수고해 주셔서 감사합니다.",
        "항상 감사하게 생각합니다.",
    ],
    "daily": [
        "네, 알겠습니다.",
        "아니요, 괜찮습니다.",
        "잠깐만요.",
        "저는 청각장애인입니다.",
        "문자로 대화할 수 있을까요?",
        "이 앱으로 대화할 수 있습니다.",
        "잠시 후에 다시 올게요.",
        "여기 앉아도 될까요?",
        "화장실이 어디인가요?",
        "시간이 얼마나 걸릴까요?",
    ],
    "emergency": [
        "도와주세요.",
        "119에 신고해 주세요.",
        "경찰을 불러주세요.",
        "아파요, 병원에 가야 해요.",
        "긴급 상황입니다.",
        "사고가 났습니다.",
        "구급차를 불러주세요.",
        "가족에게 연락해 주세요.",
        "위험합니다, 피해 주세요.",
        "여기 주소를 알려주세요.",
    ],
    "medical": [
        "진료 예약하고 싶습니다.",
        "여기가 아픕니다.",
        "약 처방전이 필요합니다.",
        "보험증 여기 있습니다.",
        "알레르기가 있습니다.",
        "현재 복용 중인 약이 있습니다.",
        "진료 기록을 보여드릴게요.",
        "검사 결과를 알고 싶습니다.",
        "다음 진료 예약을 잡아주세요.",
        "통증이 심합니다.",
    ],
    "order": [
        "주문하겠습니다.",
        "이것으로 주세요.",
        "포장해 주세요.",
        "카드로 결제할게요.",
        "영수증 주세요.",
        "메뉴판 좀 보여주세요.",
        "추천 메뉴가 무엇인가요?",
        "매운 것은 빼 주세요.",
        "물 한 잔 주세요.",
        "계산해 주세요.",
    ],
}


@router.get("/presets")
async def get_preset_phrases():
    """카테고리별 기본 프리셋 문구 목록 (로그인 불필요)"""
    return PRESET_PHRASES


@router.post("/presets/add-all", response_model=list[PhraseResponse], status_code=201)
async def add_all_presets(
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    """기본 프리셋 문구를 내 즐겨찾기에 일괄 추가"""
    added = []
    for category, phrases in PRESET_PHRASES.items():
        for text in phrases:
            # 중복 확인
            existing = await db.execute(
                select(FavoritePhrase).where(
                    FavoritePhrase.user_id == user.id,
                    FavoritePhrase.phrase_text == text,
                )
            )
            if existing.scalar_one_or_none() is None:
                phrase = FavoritePhrase(
                    user_id=user.id, phrase_text=text, category=category,
                )
                db.add(phrase)
                added.append(phrase)
    await db.flush()
    return added


@router.get("", response_model=list[PhraseResponse])
async def list_phrases(
    category: str | None = None,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    """즐겨찾기 문구 목록 (카테고리별 필터 가능)"""
    query = select(FavoritePhrase).where(FavoritePhrase.user_id == user.id)
    if category:
        query = query.where(FavoritePhrase.category == category)
    query = query.order_by(FavoritePhrase.usage_count.desc())

    result = await db.execute(query)
    return list(result.scalars().all())


@router.post("", response_model=PhraseResponse, status_code=201)
async def create_phrase(
    body: PhraseCreateRequest,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    """즐겨찾기 문구 추가"""
    phrase = FavoritePhrase(
        user_id=user.id,
        phrase_text=body.phrase_text,
        category=body.category,
    )
    db.add(phrase)
    await db.flush()
    return phrase


@router.post("/{phrase_id}/use")
async def increment_usage(
    phrase_id: str,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    """문구 사용 횟수 증가 (재생 시 호출)"""
    result = await db.execute(
        select(FavoritePhrase).where(
            FavoritePhrase.id == phrase_id, FavoritePhrase.user_id == user.id
        )
    )
    phrase = result.scalar_one_or_none()
    if phrase is None:
        raise HTTPException(status_code=404, detail="문구를 찾을 수 없습니다")

    phrase.usage_count += 1
    return {"usage_count": phrase.usage_count}


@router.delete("/{phrase_id}", status_code=204)
async def delete_phrase(
    phrase_id: str,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    """즐겨찾기 문구 삭제"""
    result = await db.execute(
        select(FavoritePhrase).where(
            FavoritePhrase.id == phrase_id, FavoritePhrase.user_id == user.id
        )
    )
    phrase = result.scalar_one_or_none()
    if phrase is None:
        raise HTTPException(status_code=404, detail="문구를 찾을 수 없습니다")
    await db.delete(phrase)
