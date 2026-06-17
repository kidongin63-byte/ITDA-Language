from contextlib import asynccontextmanager

import os

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import HTMLResponse
from fastapi.staticfiles import StaticFiles

from app.config import get_settings
from app.api.v1 import admin, auth, tts, voices, phrases, websocket
from app.core.database import engine, Base


@asynccontextmanager
async def lifespan(app: FastAPI):
    # Startup: 테이블 자동 생성
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)
    yield
    # Shutdown
    await engine.dispose()


settings = get_settings()

app = FastAPI(
    title=settings.APP_NAME,
    version=settings.APP_VERSION,
    description="농인을 위한 텍스트→음성 변환 서비스",
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
