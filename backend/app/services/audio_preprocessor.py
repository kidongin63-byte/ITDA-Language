"""
커스텀 음성 녹음 전처리 서비스

업로드된 WAV 파일의 노이즈 제거, 묵음 구간 제거, 볼륨 정규화,
품질 검증(SNR) 등을 수행한다.
"""

import io
import tempfile
from pathlib import Path

import numpy as np
from pydub import AudioSegment
from pydub.silence import detect_nonsilent

from app.core.exceptions import AudioUploadError


# 최소 녹음 길이 (초)
MIN_DURATION_SEC = 10
# 최대 녹음 길이 (초)
MAX_DURATION_SEC = 300
# 최소 SNR (dB)
MIN_SNR_DB = 15
# 타겟 샘플레이트
TARGET_SAMPLE_RATE = 16000


def validate_and_preprocess(audio_bytes: bytes, filename: str) -> tuple[bytes, float]:
    """
    오디오 파일을 검증하고 전처리한다.

    Returns:
        (전처리된 WAV 바이트, 녹음 길이(초))

    Raises:
        AudioUploadError: 파일이 유효하지 않거나 품질이 낮은 경우
    """
    # 1. 오디오 로드
    try:
        audio = AudioSegment.from_file(io.BytesIO(audio_bytes))
    except Exception:
        raise AudioUploadError("지원하지 않는 오디오 형식입니다. WAV, MP3, M4A를 사용해주세요.")

    # 2. 길이 검증
    duration_sec = len(audio) / 1000.0
    if duration_sec < MIN_DURATION_SEC:
        raise AudioUploadError(f"녹음이 너무 짧습니다. 최소 {MIN_DURATION_SEC}초 이상 녹음해주세요.")
    if duration_sec > MAX_DURATION_SEC:
        raise AudioUploadError(f"녹음이 너무 깁니다. 최대 {MAX_DURATION_SEC // 60}분까지 가능합니다.")

    # 3. 모노 변환 + 리샘플링
    audio = audio.set_channels(1).set_frame_rate(TARGET_SAMPLE_RATE)

    # 4. 묵음 구간 제거
    nonsilent_ranges = detect_nonsilent(audio, min_silence_len=500, silence_thresh=-40)
    if nonsilent_ranges:
        trimmed = AudioSegment.empty()
        for start, end in nonsilent_ranges:
            trimmed += audio[start:end]
        audio = trimmed

    # 5. 볼륨 정규화 (-20 dBFS 타겟)
    target_dbfs = -20.0
    change_in_dbfs = target_dbfs - audio.dBFS
    audio = audio.apply_gain(change_in_dbfs)

    # 6. SNR 검증 (간이)
    samples = np.array(audio.get_array_of_samples(), dtype=np.float32)
    if len(samples) > 0:
        signal_power = np.mean(samples**2)
        # 하위 10% 구간을 노이즈로 추정
        sorted_power = np.sort(samples**2)
        noise_power = np.mean(sorted_power[: len(sorted_power) // 10]) + 1e-10
        snr = 10 * np.log10(signal_power / noise_power)
        if snr < MIN_SNR_DB:
            raise AudioUploadError(
                f"녹음 품질이 낮습니다 (SNR: {snr:.1f}dB). 조용한 환경에서 다시 녹음해주세요."
            )

    # 7. WAV로 내보내기
    buffer = io.BytesIO()
    audio.export(buffer, format="wav")
    processed_bytes = buffer.getvalue()

    final_duration = len(audio) / 1000.0
    return processed_bytes, final_duration
