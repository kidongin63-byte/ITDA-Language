package com.itda.language.audio

import android.media.MediaPlayer
import android.media.audiofx.Visualizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.min

@Singleton
class PlaybackProgressTracker @Inject constructor() {

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private val _amplitudeLevels = MutableStateFlow(FloatArray(BAND_COUNT))
    val amplitudeLevels: StateFlow<FloatArray> = _amplitudeLevels.asStateFlow()

    private var trackingJob: Job? = null
    private var visualizer: Visualizer? = null
    private var previousProgress = 0f

    /**
     * MediaPlayer 재생 추적 시작
     */
    fun startTracking(
        mediaPlayer: MediaPlayer,
        audioSessionId: Int,
        scope: CoroutineScope,
        hasAudioPermission: Boolean,
    ) {
        stopTracking()
        previousProgress = 0f
        _progress.value = 0f
        _amplitudeLevels.value = FloatArray(BAND_COUNT)

        // Visualizer 연결 (권한 있을 때만)
        if (hasAudioPermission) {
            tryAttachVisualizer(audioSessionId)
        }

        // 진행률 폴링
        trackingJob = scope.launch {
            while (isActive) {
                try {
                    val duration = mediaPlayer.duration
                    val position = mediaPlayer.currentPosition
                    if (duration > 0) {
                        val raw = (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                        // 스무딩
                        val smoothed = lerp(previousProgress, raw, 0.3f)
                        previousProgress = smoothed
                        _progress.value = smoothed
                    }

                    // Visualizer 없으면 시뮬레이션
                    if (visualizer == null) {
                        simulateAmplitudes()
                    }
                } catch (_: IllegalStateException) {
                    // MediaPlayer가 해제된 경우
                    break
                }
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    /**
     * 추적 중지
     */
    fun stopTracking() {
        trackingJob?.cancel()
        trackingJob = null
        releaseVisualizer()
        _progress.value = 0f
        _amplitudeLevels.value = FloatArray(BAND_COUNT)
        previousProgress = 0f
    }

    private fun tryAttachVisualizer(audioSessionId: Int) {
        try {
            visualizer = Visualizer(audioSessionId).apply {
                captureSize = Visualizer.getCaptureSizeRange()[0]
                setDataCaptureListener(
                    object : Visualizer.OnDataCaptureListener {
                        override fun onWaveFormDataCapture(
                            vis: Visualizer?,
                            waveform: ByteArray?,
                            samplingRate: Int,
                        ) {
                            // 사용 안 함
                        }

                        override fun onFftDataCapture(
                            vis: Visualizer?,
                            fft: ByteArray?,
                            samplingRate: Int,
                        ) {
                            fft ?: return
                            _amplitudeLevels.value = extractBands(fft)
                        }
                    },
                    Visualizer.getMaxCaptureRate() / 2,
                    false,
                    true,
                )
                enabled = true
            }
        } catch (_: Exception) {
            visualizer = null
        }
    }

    private fun releaseVisualizer() {
        try {
            visualizer?.enabled = false
            visualizer?.release()
        } catch (_: Exception) {
        }
        visualizer = null
    }

    /**
     * FFT 데이터에서 8밴드 추출
     */
    private fun extractBands(fft: ByteArray): FloatArray {
        val bands = FloatArray(BAND_COUNT)
        val bandSize = (fft.size / 2) / BAND_COUNT

        for (i in 0 until BAND_COUNT) {
            var sum = 0f
            for (j in 0 until bandSize) {
                val index = (i * bandSize + j) * 2
                if (index + 1 < fft.size) {
                    val real = fft[index].toFloat()
                    val imag = fft[index + 1].toFloat()
                    sum += hypot(real, imag)
                }
            }
            val avg = sum / bandSize
            // 정규화 (0~1)
            bands[i] = (avg / 128f).coerceIn(0f, 1f)
        }
        return bands
    }

    /**
     * Visualizer 없을 때 시뮬레이션
     */
    private fun simulateAmplitudes() {
        val progress = _progress.value
        if (progress <= 0f) {
            _amplitudeLevels.value = FloatArray(BAND_COUNT)
            return
        }
        val time = System.currentTimeMillis()
        val bands = FloatArray(BAND_COUNT) { i ->
            val phase = (time / (200L + i * 50L)).toFloat()
            val base = 0.3f + 0.4f * abs(kotlin.math.sin(phase))
            (base * progress.coerceAtMost(1f)).coerceIn(0f, 1f)
        }
        _amplitudeLevels.value = bands
    }

    private fun lerp(start: Float, end: Float, fraction: Float): Float {
        return start + (end - start) * fraction
    }

    companion object {
        private const val BAND_COUNT = 8
        private const val POLL_INTERVAL_MS = 50L
    }
}
