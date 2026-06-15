package com.itda.language.data.api

import android.util.Base64
import android.util.Log
import com.itda.language.BuildConfig
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import okhttp3.*
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WebSocket 기반 실시간 TTS 스트리밍 클라이언트.
 *
 * 긴 텍스트를 문장 단위로 분할 합성하여 오디오 청크를
 * 순차적으로 수신한다. 첫 문장이 합성되는 즉시 재생을 시작할 수 있다.
 */
data class AudioChunk(
    val sequence: Int,
    val audioBytes: ByteArray,
    val isLast: Boolean,
)

@Singleton
class TtsWebSocketClient @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val tokenManager: TokenManager,
) {
    companion object {
        private const val TAG = "TtsWS"
    }

    private var webSocket: WebSocket? = null
    private val _audioChunks = Channel<AudioChunk>(Channel.BUFFERED)

    /** 수신된 오디오 청크를 순차적으로 받는 Flow */
    val audioChunks: Flow<AudioChunk> = _audioChunks.receiveAsFlow()

    fun connect() {
        val wsUrl = BuildConfig.BASE_URL
            .replace("http://", "ws://")
            .replace("https://", "wss://") + "/ws/v1/tts/stream"

        val request = Request.Builder()
            .url(wsUrl)
            .apply {
                tokenManager.getAccessTokenSync()?.let {
                    header("Authorization", "Bearer $it")
                }
            }
            .build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket connected")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val json = JSONObject(text)
                    when (json.getString("type")) {
                        "audio_chunk" -> {
                            val chunk = AudioChunk(
                                sequence = json.getInt("sequence"),
                                audioBytes = Base64.decode(json.getString("data"), Base64.DEFAULT),
                                isLast = json.getBoolean("is_last"),
                            )
                            _audioChunks.trySend(chunk)
                        }
                        "error" -> {
                            Log.e(TAG, "Server error: ${json.getString("detail")}")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Parse error", e)
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket failure", t)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed: $reason")
            }
        })
    }

    fun synthesize(text: String, speaker: String, speed: Int = 0, pitch: Int = 0, volume: Int = 0) {
        val msg = JSONObject().apply {
            put("type", "synthesize")
            put("text", text)
            put("speaker", speaker)
            put("speed", speed)
            put("pitch", pitch)
            put("volume", volume)
        }
        webSocket?.send(msg.toString())
    }

    fun disconnect() {
        webSocket?.close(1000, "User disconnect")
        webSocket = null
    }
}
