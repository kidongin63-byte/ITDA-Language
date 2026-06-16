from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

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


@app.get("/health")
async def health_check():
    return {"status": "ok", "version": settings.APP_VERSION}
