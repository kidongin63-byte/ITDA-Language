from pydantic import BaseModel, Field


class TTSSynthesizeRequest(BaseModel):
    text: str = Field(..., min_length=1, max_length=1000, description="합성할 텍스트")
    speaker: str = Field(default="ko-KR-SunHiNeural", description="Edge TTS 화자 코드")
    speed: int = Field(default=0, ge=-5, le=5, description="속도 (-5~5)")
    pitch: int = Field(default=0, ge=-5, le=5, description="피치 (-5~5)")
    volume: int = Field(default=0, ge=-5, le=5, description="볼륨 (-5~5)")
    emotion: int = Field(default=0, ge=0, le=2, description="감정 (0:기본, 1:슬픔, 2:기쁨)")
    emotion_strength: int = Field(default=2, ge=1, le=3, description="감정 강도 (1~3)")
    alpha: float = Field(default=0.0, ge=-1.0, le=1.0, description="음색 변조 (-1~1)")
    format: str = Field(default="mp3", pattern="^(mp3|wav)$")


class TTSCustomSynthesizeRequest(BaseModel):
    text: str = Field(..., min_length=1, max_length=1000)
    custom_voice_id: str
    speed: int = Field(default=0, ge=-5, le=5)
    pitch: int = Field(default=0, ge=-5, le=5)


class TTSStreamMessage(BaseModel):
    """WebSocket 메시지 포맷"""
    type: str  # 'synthesize'
    text: str
    speaker: str = "ko-KR-SunHiNeural"
    speed: int = 0
    pitch: int = 0
    volume: int = 0


class TTSAudioChunk(BaseModel):
    """WebSocket 응답 오디오 청크"""
    type: str = "audio_chunk"
    sequence: int
    data: str  # base64 인코딩된 오디오
    is_last: bool = False
