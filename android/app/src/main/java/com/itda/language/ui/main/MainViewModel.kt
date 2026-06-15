package com.itda.language.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.itda.language.audio.PlaybackProgressTracker
import com.itda.language.audio.SpeakerManager
import com.itda.language.audio.SpeakerWarning
import com.itda.language.data.api.VoiceApi
import com.itda.language.data.api.VoiceChangeRequestBody
import com.itda.language.data.api.VoiceLockRequest
import com.itda.language.data.repository.TtsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MainUiState(
    val inputText: String = "",
    val selectedSpeaker: String = "ko-KR-SunHiNeural",
    val selectedVoiceName: String = "선희 - 여성 (밝은, 서울)",
    val speed: Int = 0,
    val pitch: Int = 0,
    val volume: Int = 0,
    val showSliders: Boolean = false,
    val isSynthesizing: Boolean = false,
    val isPlaying: Boolean = false,
    val playbackComplete: Boolean = false,
    val playbackProgress: Float = 0f,
    val amplitudeLevels: FloatArray = FloatArray(8),
    val speakerWarning: SpeakerWarning? = null,
    val voiceLocked: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val ttsRepository: TtsRepository,
    private val voiceApi: VoiceApi,
    private val speakerManager: SpeakerManager,
    private val progressTracker: PlaybackProgressTracker,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    // RECORD_AUDIO 권한 상태 (UI에서 설정)
    var hasAudioPermission: Boolean = false

    init {
        // 진행률/이퀄라이저 데이터를 UI 상태로 포워딩
        viewModelScope.launch {
            progressTracker.progress.collect { progress ->
                _uiState.value = _uiState.value.copy(playbackProgress = progress)
            }
        }
        viewModelScope.launch {
            progressTracker.amplitudeLevels.collect { levels ->
                _uiState.value = _uiState.value.copy(amplitudeLevels = levels)
            }
        }
    }

    fun updateText(text: String) {
        _uiState.value = _uiState.value.copy(inputText = text, error = null)
    }

    fun selectVoice(speaker: String, displayName: String) {
        _uiState.value = _uiState.value.copy(
            selectedSpeaker = speaker,
            selectedVoiceName = displayName,
        )
    }

    /**
     * 최초 음성 선택 + 잠금 (서버에 잠금 요청)
     */
    fun selectVoiceAndLock(speaker: String, displayName: String) {
        selectVoice(speaker, displayName)
        _uiState.value = _uiState.value.copy(voiceLocked = true)
        viewModelScope.launch {
            try {
                voiceApi.lockVoice(VoiceLockRequest(speaker, displayName))
            } catch (_: Exception) {
                // 서버 실패해도 로컬에서는 잠금 유지
            }
        }
    }

    /**
     * 음성 변경 요청 제출 (관리자 승인 필요)
     */
    fun requestVoiceChange(speaker: String, displayName: String, reason: String) {
        viewModelScope.launch {
            try {
                voiceApi.requestVoiceChange(
                    VoiceChangeRequestBody(speaker, displayName, reason)
                )
                _uiState.value = _uiState.value.copy(
                    error = null,
                )
            } catch (_: Exception) { }
        }
    }

    fun synthesizeAndPlay() {
        val text = _uiState.value.inputText.trim()
        if (text.isBlank()) return

        // 스피커 강제 활성화 + 볼륨 확인
        speakerManager.ensureSpeakerOutput()

        val warning = if (!speakerManager.isSpeakerAvailable()) {
            SpeakerWarning.SpeakerUnavailable
        } else {
            speakerManager.checkVolumeLevel()
        }

        if (warning is SpeakerWarning.VolumeMuted || warning is SpeakerWarning.VolumeLow) {
            speakerManager.boostVolumeIfNeeded(50)
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSynthesizing = true,
                isPlaying = false,
                error = null,
                playbackComplete = false,
                playbackProgress = 0f,
                speakerWarning = warning,
            )

            // 서버 TTS 시도 (선택한 화자 목소리 반영)
            ttsRepository.synthesizeAndPlay(
                text = text,
                speaker = _uiState.value.selectedSpeaker,
                speed = _uiState.value.speed,
                pitch = _uiState.value.pitch,
                volume = _uiState.value.volume,
                onPrepared = { audioSessionId, _ ->
                    val player = ttsRepository.getMediaPlayer() ?: return@synthesizeAndPlay
                    progressTracker.startTracking(
                        mediaPlayer = player,
                        audioSessionId = audioSessionId,
                        scope = viewModelScope,
                        hasAudioPermission = hasAudioPermission,
                    )
                },
                onComplete = {
                    progressTracker.stopTracking()
                    speakerManager.releaseSpeakerControl()
                    _uiState.value = _uiState.value.copy(
                        isPlaying = false,
                        playbackComplete = true,
                        playbackProgress = 0f,
                        amplitudeLevels = FloatArray(8),
                    )
                },
            ).onSuccess {
                _uiState.value = _uiState.value.copy(isSynthesizing = false, isPlaying = true)
            }.onFailure {
                // 서버 실패 → 로컬 TTS 폴백 (즉시 재생)
                _uiState.value = _uiState.value.copy(
                    isSynthesizing = false,
                    isPlaying = true,
                )
                startProgressSimulation(text)
                ttsRepository.speakNow(
                    text = text,
                    speaker = _uiState.value.selectedSpeaker,
                    speed = _uiState.value.speed,
                    pitch = _uiState.value.pitch,
                    onStart = { },
                    onComplete = {
                        viewModelScope.launch {
                            _uiState.value = _uiState.value.copy(playbackProgress = 1f)
                            kotlinx.coroutines.delay(300)
                            speakerManager.releaseSpeakerControl()
                            _uiState.value = _uiState.value.copy(
                                isPlaying = false,
                                playbackComplete = true,
                                playbackProgress = 0f,
                                amplitudeLevels = FloatArray(8),
                            )
                        }
                    },
                )
            }
        }
    }

    /**
     * 텍스트 길이 기반 진행률 시뮬레이션 (로컬 TTS용)
     * 한국어 TTS: 약 5~7글자/초, 줄바꿈/공백 제외
     */
    private fun startProgressSimulation(text: String) {
        viewModelScope.launch {
            val speakableLength = text.replace("\n", "").replace(" ", "").length
            // 글자당 약 150ms (한국어 TTS 평균 속도에 맞춤)
            val estimatedDurationMs = (speakableLength * 150L).coerceAtLeast(800L)
            val startTime = System.currentTimeMillis()

            while (_uiState.value.isPlaying) {
                val elapsed = System.currentTimeMillis() - startTime
                val progress = (elapsed.toFloat() / estimatedDurationMs).coerceIn(0f, 0.95f)

                // 이퀄라이저 시뮬레이션
                val time = System.currentTimeMillis()
                val bands = FloatArray(8) { i ->
                    val phase = (time / (180L + i * 40L)).toFloat()
                    (0.3f + 0.5f * kotlin.math.abs(kotlin.math.sin(phase))).coerceIn(0f, 1f)
                }

                _uiState.value = _uiState.value.copy(
                    playbackProgress = progress,
                    amplitudeLevels = bands,
                )

                kotlinx.coroutines.delay(40)
            }
        }
    }

    fun stopPlaying() {
        progressTracker.stopTracking()
        ttsRepository.stopPlaying()
        speakerManager.releaseSpeakerControl()
        _uiState.value = _uiState.value.copy(
            isPlaying = false,
            playbackProgress = 0f,
            amplitudeLevels = FloatArray(8),
        )
    }

    fun boostVolume() {
        speakerManager.boostVolumeIfNeeded(50)
        _uiState.value = _uiState.value.copy(speakerWarning = null)
    }

    fun dismissSpeakerWarning() {
        _uiState.value = _uiState.value.copy(speakerWarning = null)
    }

    fun clearPlaybackComplete() {
        _uiState.value = _uiState.value.copy(playbackComplete = false)
    }

    fun toggleSliders() {
        _uiState.value = _uiState.value.copy(showSliders = !_uiState.value.showSliders)
    }

    fun updateSpeed(value: Int) {
        _uiState.value = _uiState.value.copy(speed = value)
    }

    fun updatePitch(value: Int) {
        _uiState.value = _uiState.value.copy(pitch = value)
    }

    fun updateVolume(value: Int) {
        _uiState.value = _uiState.value.copy(volume = value)
    }
}
