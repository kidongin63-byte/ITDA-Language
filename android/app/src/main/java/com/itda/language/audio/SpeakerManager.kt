package com.itda.language.audio

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

sealed class SpeakerWarning {
    object VolumeMuted : SpeakerWarning()
    object VolumeLow : SpeakerWarning()
    object SpeakerUnavailable : SpeakerWarning()
}

@Singleton
class SpeakerManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    /**
     * 스피커폰 강제 활성화
     */
    fun ensureSpeakerOutput() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // API 31+: setCommunicationDevice
            val speaker = audioManager.availableCommunicationDevices
                .firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
            if (speaker != null) {
                audioManager.setCommunicationDevice(speaker)
            }
        } else {
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            @Suppress("DEPRECATION")
            audioManager.isSpeakerphoneOn = true
        }
    }

    /**
     * 볼륨 상태 확인
     */
    fun checkVolumeLevel(): SpeakerWarning? {
        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

        return when {
            current == 0 -> SpeakerWarning.VolumeMuted
            current < (max * 0.15f).toInt() -> SpeakerWarning.VolumeLow
            else -> null
        }
    }

    /**
     * 볼륨이 낮으면 최소 50%까지 올림
     */
    fun boostVolumeIfNeeded(minPercent: Int = 50) {
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val minLevel = (max * minPercent / 100).coerceAtLeast(1)
        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

        if (current < minLevel) {
            audioManager.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                minLevel,
                AudioManager.FLAG_SHOW_UI,
            )
        }
    }

    /**
     * 스피커 제어 해제
     */
    fun releaseSpeakerControl() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            audioManager.clearCommunicationDevice()
        } else {
            @Suppress("DEPRECATION")
            audioManager.isSpeakerphoneOn = false
            audioManager.mode = AudioManager.MODE_NORMAL
        }
    }

    /**
     * 스피커 사용 가능 여부 확인
     */
    fun isSpeakerAvailable(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            audioManager.availableCommunicationDevices
                .any { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
        } else {
            true // 대부분의 기기에 스피커 있음
        }
    }
}
