package com.itda.language.data.repository

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.itda.language.data.api.TtsApi
import com.itda.language.data.model.TtsSynthesizeRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TtsRepository @Inject constructor(
    private val ttsApi: TtsApi,
    @ApplicationContext private val context: Context,
) {
    private var mediaPlayer: MediaPlayer? = null
    private var localTts: TextToSpeech? = null
    private var localTtsReady = false

    init {
        // Android 내장 TTS 초기화
        localTts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                localTts?.language = Locale.KOREAN
                localTtsReady = true
            }
        }
    }

    /**
     * 로컬 TTS로 즉시 재생 (실시간)
     */
    fun speakNow(
        text: String,
        speaker: String = "",
        speed: Int = 0,
        pitch: Int = 0,
        onStart: () -> Unit = {},
        onComplete: () -> Unit = {},
    ): Result<Unit> {
        if (!localTtsReady || localTts == null) {
            return Result.failure(Exception("TTS 엔진이 준비되지 않았습니다"))
        }

        val tts = localTts!!

        // 남성 화자면 피치를 낮춰서 남성 느낌 구현
        val isMale = speaker.contains("InJoon") || speaker.contains("Hyunsu") ||
            speaker.contains("Andrew") || speaker.contains("Brian") || speaker.contains("Remy")
        val basePitch = if (isMale) 0.75f else 1.1f

        // 속도/피치 변환 (-5~5 → 조정)
        tts.setSpeechRate(1.0f + speed * 0.1f)
        tts.setPitch(basePitch + pitch * 0.05f)

        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                onStart()
            }
            override fun onDone(utteranceId: String?) {
                onComplete()
            }
            @Deprecated("Deprecated")
            override fun onError(utteranceId: String?) {
                onComplete()
            }
        })

        val params = Bundle().apply {
            putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, android.media.AudioManager.STREAM_MUSIC)
        }

        tts.speak(text, TextToSpeech.QUEUE_FLUSH, params, "itda_tts")
        return Result.success(Unit)
    }

    /**
     * 로컬 TTS 중지
     */
    fun stopSpeaking() {
        localTts?.stop()
    }

    fun isLocalTtsPlaying(): Boolean {
        return localTts?.isSpeaking == true
    }

    /**
     * 서버 TTS로 고품질 합성 (기존 방식)
     */
    suspend fun synthesizeAndPlay(
        text: String,
        speaker: String,
        speed: Int = 0,
        pitch: Int = 0,
        volume: Int = 0,
        onPrepared: (audioSessionId: Int, durationMs: Int) -> Unit = { _, _ -> },
        onComplete: () -> Unit = {},
    ): Result<Unit> {
        return try {
            val request = TtsSynthesizeRequest(
                text = text,
                speaker = speaker,
                speed = speed,
                pitch = pitch,
                volume = volume,
            )
            val resp = ttsApi.synthesize(request)
            if (!resp.isSuccessful || resp.body() == null) {
                return Result.failure(Exception("TTS 합성 실패: ${resp.code()}"))
            }

            val tempFile = File(context.cacheDir, "tts_audio.mp3")
            resp.body()!!.byteStream().use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            stopPlaying()
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                setDataSource(tempFile.absolutePath)
                prepare()
                onPrepared(audioSessionId, duration)
                setOnCompletionListener {
                    onComplete()
                    release()
                    mediaPlayer = null
                }
                start()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun stopPlaying() {
        stopSpeaking()
        mediaPlayer?.let {
            if (it.isPlaying) it.stop()
            it.release()
        }
        mediaPlayer = null
    }

    fun getMediaPlayer(): MediaPlayer? = mediaPlayer

    fun isPlaying(): Boolean = mediaPlayer?.isPlaying == true || isLocalTtsPlaying()
}
