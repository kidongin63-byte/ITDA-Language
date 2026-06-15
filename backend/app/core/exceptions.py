from fastapi import HTTPException, status


class TTSError(HTTPException):
    def __init__(self, detail: str = "TTS 합성 중 오류가 발생했습니다"):
        super().__init__(status_code=status.HTTP_502_BAD_GATEWAY, detail=detail)


class TextTooLongError(HTTPException):
    def __init__(self, max_length: int):
        super().__init__(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"텍스트가 너무 깁니다. 최대 {max_length}자까지 입력 가능합니다.",
        )


class VoiceNotFoundError(HTTPException):
    def __init__(self):
        super().__init__(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="요청한 음성을 찾을 수 없습니다",
        )


class CustomVoiceNotReadyError(HTTPException):
    def __init__(self):
        super().__init__(
            status_code=status.HTTP_409_CONFLICT,
            detail="커스텀 음성이 아직 준비되지 않았습니다. 학습 완료 후 사용 가능합니다.",
        )


class AudioUploadError(HTTPException):
    def __init__(self, detail: str = "음성 파일 업로드 중 오류가 발생했습니다"):
        super().__init__(status_code=status.HTTP_400_BAD_REQUEST, detail=detail)
