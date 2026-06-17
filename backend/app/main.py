from contextlib import asynccontextmanager

import os

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import HTMLResponse
from fastapi.staticfiles import StaticFiles

from sqlalchemy import select

from app.config import get_settings
from app.api.v1 import admin, auth, tts, voices, phrases, websocket
from app.core.database import engine, Base, async_session
from app.models.voice_persona import VoicePersona


VOICE_SEEDS = [
    {
        "clova_speaker": "ko-KR-SunHiNeural",
        "display_name": "선히 (여성, 밝은 톤)",
        "gender": "female",
        "age_group": "young_adult",
        "region": "seoul",
        "tone": "bright",
        "emotion_support": False,
        "sort_order": 1,
    },
    {
        "clova_speaker": "ko-KR-InJoonNeural",
        "display_name": "인준 (남성, 차분한 톤)",
        "gender": "male",
        "age_group": "middle_aged",
        "region": "seoul",
        "tone": "calm",
        "emotion_support": False,
        "sort_order": 2,
    },
    {
        "clova_speaker": "ko-KR-HyunsuMultilingualNeural",
        "display_name": "현수 (남성, 따뜻한 톤)",
        "gender": "male",
        "age_group": "young_adult",
        "region": "seoul",
        "tone": "warm",
        "emotion_support": False,
        "sort_order": 3,
    },
]


@asynccontextmanager
async def lifespan(app: FastAPI):
    # Startup: 테이블 자동 생성
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)
    # 음성 시드 데이터 삽입
    async with async_session() as session:
        result = await session.execute(select(VoicePersona).limit(1))
        if result.scalar_one_or_none() is None:
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
)

# CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
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
