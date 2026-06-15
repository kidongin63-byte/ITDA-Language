package com.itda.language.data.model

// ─── Auth ────────────────────────────────────

data class RegisterRequest(
    val email: String,
    val nickname: String,
    val password: String,
    val phone: String = "",
    val gender: String = "",
    val birth_id: String = "",
)
data class LoginRequest(val email: String, val password: String)
data class RefreshRequest(val refresh_token: String)
data class TokenResponse(val access_token: String, val refresh_token: String, val token_type: String)
data class UserResponse(val id: String, val email: String, val nickname: String)

// ─── TTS ─────────────────────────────────────

data class TtsSynthesizeRequest(
    val text: String,
    val speaker: String = "nara",
    val speed: Int = 0,
    val pitch: Int = 0,
    val volume: Int = 0,
    val emotion: Int = 0,
    val emotion_strength: Int = 2,
    val alpha: Float = 0f,
    val format: String = "mp3",
)

// ─── Voice Persona ───────────────────────────

data class VoicePersona(
    val id: Int,
    val clova_speaker: String,
    val display_name: String,
    val gender: String,
    val age_group: String,
    val region: String,
    val tone: String,
    val emotion_support: Boolean,
    val preview_audio_url: String?,
)

data class VoiceCategoryList(
    val genders: Map<String, String>,
    val age_groups: Map<String, String>,
    val regions: Map<String, String>,
    val tones: Map<String, String>,
)

// ─── Phrases ─────────────────────────────────

data class PhraseResponse(
    val id: String,
    val phrase_text: String,
    val category: String,
    val usage_count: Int,
)

data class PhraseCreateRequest(val phrase_text: String, val category: String = "custom")
