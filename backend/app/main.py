from contextlib import asynccontextmanager

import os

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import HTMLResponse
from fastapi.staticfiles import StaticFiles

from sqlalchemy import select, text

from app.config import get_settings
from app.api.v1 import admin, auth, tts, voices, phrases, websocket
from app.core.database import engine, Base, async_session
from app.models.voice_persona import VoicePersona


VOICE_SEEDS = [
    # ─── 🇰🇷 한국어 ───
    {"clova_speaker": "ko-KR-SunHiNeural", "display_name": "선히 (여성)", "gender": "female", "age_group": "young_adult", "region": "seoul", "tone": "bright", "sort_order": 1},
    {"clova_speaker": "ko-KR-InJoonNeural", "display_name": "인준 (남성)", "gender": "male", "age_group": "middle_aged", "region": "seoul", "tone": "calm", "sort_order": 2},
    {"clova_speaker": "ko-KR-HyunsuMultilingualNeural", "display_name": "현수 (남성)", "gender": "male", "age_group": "young_adult", "region": "seoul", "tone": "warm", "sort_order": 3},
    # ─── 🇰🇷 한국어 (다국어 음성) ───
    {"clova_speaker": "en-US-AvaMultilingualNeural", "display_name": "아바 (여성, 밝은)", "gender": "female", "age_group": "young_adult", "region": "seoul", "tone": "bright", "sort_order": 4},
    {"clova_speaker": "en-US-EmmaMultilingualNeural", "display_name": "엠마 (여성, 따뜻한)", "gender": "female", "age_group": "young_adult", "region": "seoul", "tone": "warm", "sort_order": 5},
    {"clova_speaker": "fr-FR-VivienneMultilingualNeural", "display_name": "비비안 (여성, 우아한)", "gender": "female", "age_group": "young_adult", "region": "seoul", "tone": "elegant", "sort_order": 6},
    {"clova_speaker": "de-DE-SeraphinaMultilingualNeural", "display_name": "세라피나 (여성, 차분한)", "gender": "female", "age_group": "middle_aged", "region": "seoul", "tone": "calm", "sort_order": 7},
    {"clova_speaker": "pt-BR-ThalitaMultilingualNeural", "display_name": "탈리타 (여성, 부드러운)", "gender": "female", "age_group": "young_adult", "region": "seoul", "tone": "gentle", "sort_order": 8},
    {"clova_speaker": "en-US-AndrewMultilingualNeural", "display_name": "앤드류 (남성, 차분한)", "gender": "male", "age_group": "young_adult", "region": "seoul", "tone": "calm", "sort_order": 9},
    {"clova_speaker": "en-US-BrianMultilingualNeural", "display_name": "브라이언 (남성, 진지한)", "gender": "male", "age_group": "middle_aged", "region": "seoul", "tone": "serious", "sort_order": 10},
    {"clova_speaker": "fr-FR-RemyMultilingualNeural", "display_name": "레미 (남성, 부드러운)", "gender": "male", "age_group": "young_adult", "region": "seoul", "tone": "gentle", "sort_order": 11},
    {"clova_speaker": "de-DE-FlorianMultilingualNeural", "display_name": "플로리안 (남성, 활기찬)", "gender": "male", "age_group": "young_adult", "region": "seoul", "tone": "energetic", "sort_order": 12},
    {"clova_speaker": "en-AU-WilliamMultilingualNeural", "display_name": "윌리엄 (남성, 따뜻한)", "gender": "male", "age_group": "middle_aged", "region": "seoul", "tone": "warm", "sort_order": 13},
    {"clova_speaker": "it-IT-GiuseppeMultilingualNeural", "display_name": "주세페 (남성, 친근한)", "gender": "male", "age_group": "young_adult", "region": "seoul", "tone": "friendly", "sort_order": 14},
    # ─── 🇺🇸 영어 ───
    {"clova_speaker": "en-US-JennyNeural", "display_name": "Jenny (여성, English)", "gender": "female", "age_group": "young_adult", "region": "seoul", "tone": "friendly", "sort_order": 15},
    {"clova_speaker": "en-US-GuyNeural", "display_name": "Guy (남성, English)", "gender": "male", "age_group": "middle_aged", "region": "seoul", "tone": "calm", "sort_order": 16},
    # ─── 🇯🇵 일본어 ───
    {"clova_speaker": "ja-JP-NanamiNeural", "display_name": "나나미 (여성, 日本語)", "gender": "female", "age_group": "young_adult", "region": "seoul", "tone": "gentle", "sort_order": 17},
    {"clova_speaker": "ja-JP-KeitaNeural", "display_name": "케이타 (남성, 日本語)", "gender": "male", "age_group": "young_adult", "region": "seoul", "tone": "calm", "sort_order": 18},
    # ─── 🇨🇳 중국어 ───
    {"clova_speaker": "zh-CN-XiaoxiaoNeural", "display_name": "샤오샤오 (여성, 中文)", "gender": "female", "age_group": "young_adult", "region": "seoul", "tone": "bright", "sort_order": 19},
    {"clova_speaker": "zh-CN-YunjianNeural", "display_name": "윈지엔 (남성, 中文)", "gender": "male", "age_group": "middle_aged", "region": "seoul", "tone": "serious", "sort_order": 20},
    # ─── 🇻🇳 베트남어 ───
    {"clova_speaker": "vi-VN-HoaiMyNeural", "display_name": "호아이미 (여성, Tiếng Việt)", "gender": "female", "age_group": "young_adult", "region": "seoul", "tone": "warm", "sort_order": 21},
    {"clova_speaker": "vi-VN-NamMinhNeural", "display_name": "남민 (남성, Tiếng Việt)", "gender": "male", "age_group": "young_adult", "region": "seoul", "tone": "calm", "sort_order": 22},
    # ─── 🇪🇸 스페인어 ───
    {"clova_speaker": "es-ES-ElviraNeural", "display_name": "엘비라 (여성, Español)", "gender": "female", "age_group": "young_adult", "region": "seoul", "tone": "elegant", "sort_order": 23},
    {"clova_speaker": "es-ES-AlvaroNeural", "display_name": "알바로 (남성, Español)", "gender": "male", "age_group": "young_adult", "region": "seoul", "tone": "energetic", "sort_order": 24},
    # ─── 🇲🇳 몽골어 ───
    {"clova_speaker": "mn-MN-YesuiNeural", "display_name": "예수이 (여성, Монгол)", "gender": "female", "age_group": "young_adult", "region": "seoul", "tone": "gentle", "sort_order": 25},
    {"clova_speaker": "mn-MN-BataaNeural", "display_name": "바타 (남성, Монгол)", "gender": "male", "age_group": "young_adult", "region": "seoul", "tone": "serious", "sort_order": 26},
    # ─── 🇮🇳 힌디어 ───
    {"clova_speaker": "hi-IN-SwaraNeural", "display_name": "스와라 (여성, हिन्दी)", "gender": "female", "age_group": "young_adult", "region": "seoul", "tone": "warm", "sort_order": 27},
    {"clova_speaker": "hi-IN-MadhurNeural", "display_name": "마두르 (남성, हिन्दी)", "gender": "male", "age_group": "young_adult", "region": "seoul", "tone": "friendly", "sort_order": 28},
    # ─── 🇫🇷 프랑스어 ───
    {"clova_speaker": "fr-FR-DeniseNeural", "display_name": "드니즈 (여성, Français)", "gender": "female", "age_group": "young_adult", "region": "seoul", "tone": "elegant", "sort_order": 29},
    {"clova_speaker": "fr-FR-HenriNeural", "display_name": "앙리 (남성, Français)", "gender": "male", "age_group": "middle_aged", "region": "seoul", "tone": "calm", "sort_order": 30},
]


@asynccontextmanager
async def lifespan(app: FastAPI):
    # Startup: 테이블 자동 생성
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)
        # 자가 마이그레이션: 기존 birth_id VARCHAR(6) → VARCHAR(255)
        # (암호화 저장으로 암호문 길이가 늘어남. PostgreSQL만 대상, 반복 실행 안전)
        if conn.dialect.name == "postgresql":
            try:
                await conn.execute(
                    text("ALTER TABLE users ALTER COLUMN birth_id TYPE VARCHAR(255)")
                )
            except Exception:
                pass
    # 음성 시드 데이터 삽입 (새 음성 추가 시 자동 반영)
    async with async_session() as session:
        from sqlalchemy import func
        count = (await session.execute(select(func.count()).select_from(VoicePersona))).scalar()
        if count < len(VOICE_SEEDS):
            await session.execute(VoicePersona.__table__.delete())
            for seed in VOICE_SEEDS:
                session.add(VoicePersona(**seed))
            await session.commit()
    yield
    # Shutdown
    await engine.dispose()


settings = get_settings()

app = FastAPI(
    title=settings.APP_NAME,
    version=settings.APP_VERSION,
    description="소리로 이어주는 따뜻한 다리",
    lifespan=lifespan,
    # 운영 환경에서는 API 문서(/docs, /redoc)를 노출하지 않음
    docs_url=None if settings.is_production else "/docs",
    redoc_url=None if settings.is_production else "/redoc",
    openapi_url=None if settings.is_production else "/openapi.json",
)

# CORS: 운영은 지정된 도메인만 허용. Bearer 토큰 인증이라 쿠키 자격증명은 불필요
_cors_origins = settings.cors_origin_list
app.add_middleware(
    CORSMiddleware,
    allow_origins=_cors_origins,
    allow_credentials=False,
    allow_methods=["*"],
    allow_headers=["*"],
)

# API 라우터 등록
app.include_router(auth.router, prefix="/api/v1/auth", tags=["인증"])
app.include_router(tts.router, prefix="/api/v1/tts", tags=["TTS"])
app.include_router(voices.router, prefix="/api/v1/voices", tags=["음성"])
app.include_router(phrases.router, prefix="/api/v1/phrases", tags=["즐겨찾기"])
app.include_router(websocket.router, prefix="/ws/v1", tags=["WebSocket"])
app.include_router(admin.router, prefix="/api/v1/admin", tags=["관리자"])


static_dir = os.path.join(os.path.dirname(__file__), "static")
app.mount("/static", StaticFiles(directory=static_dir), name="static")


@app.get("/", include_in_schema=False, response_class=HTMLResponse)
async def root():
    index_path = os.path.join(static_dir, "index.html")
    with open(index_path, "r", encoding="utf-8") as f:
        return f.read()


@app.get("/app", include_in_schema=False, response_class=HTMLResponse)
async def pwa_app():
    app_path = os.path.join(static_dir, "app.html")
    with open(app_path, "r", encoding="utf-8") as f:
        return f.read()


@app.get("/health")
async def health_check():
    return {"status": "ok", "version": settings.APP_VERSION}
