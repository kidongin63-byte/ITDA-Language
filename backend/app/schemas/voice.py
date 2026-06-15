from pydantic import BaseModel


# ─── 음성 페르소나 (프리셋) ─────────────────────────

class VoicePersonaResponse(BaseModel):
    id: int
    clova_speaker: str
    display_name: str
    gender: str
    age_group: str
    region: str
    tone: str
    emotion_support: bool
    preview_audio_url: str | None = None

    model_config = {"from_attributes": True}


class VoicePersonaFilter(BaseModel):
    """카테고리 필터링 쿼리"""
    gender: str | None = None       # male / female
    age_group: str | None = None    # child, teen, young_adult, middle_aged, senior
    region: str | None = None       # seoul, busan, jeju, ...
    tone: str | None = None         # bright, calm, warm, ...


class VoiceCategoryListResponse(BaseModel):
    """사용 가능한 카테고리 목록 반환"""
    genders: dict[str, str]
    age_groups: dict[str, str]
    regions: dict[str, str]
    tones: dict[str, str]


# ─── 커스텀 음성 ────────────────────────────────────

class CustomVoiceUploadResponse(BaseModel):
    id: str
    voice_name: str
    status: str

    model_config = {"from_attributes": True}


class CustomVoiceResponse(BaseModel):
    id: str
    voice_name: str
    reference_speaker: str | None
    sample_duration_sec: float | None
    status: str
    error_message: str | None = None

    model_config = {"from_attributes": True}


class CustomVoiceStatusResponse(BaseModel):
    id: str
    status: str  # processing / ready / failed
    error_message: str | None = None

    model_config = {"from_attributes": True}


# ─── 사용자 설정 ────────────────────────────────────

class UserPreferenceResponse(BaseModel):
    default_voice_type: str
    default_persona_id: int | None
    default_custom_voice_id: str | None
    default_speed: int
    default_pitch: int
    default_volume: int
    auto_play: bool

    model_config = {"from_attributes": True}


class UserPreferenceUpdateRequest(BaseModel):
    default_voice_type: str | None = None
    default_persona_id: int | None = None
    default_custom_voice_id: str | None = None
    default_speed: int | None = None
    default_pitch: int | None = None
    default_volume: int | None = None
    auto_play: bool | None = None


# ─── 즐겨찾기 문구 ──────────────────────────────────

class PhraseCreateRequest(BaseModel):
    phrase_text: str
    category: str = "custom"


class PhraseResponse(BaseModel):
    id: str
    phrase_text: str
    category: str
    usage_count: int

    model_config = {"from_attributes": True}
