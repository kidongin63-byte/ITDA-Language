"""커스텀 음성 관리 서비스 (placeholder - S3/학습 미구현)"""


class CustomVoiceService:
    async def upload_and_register(self, *args, **kwargs):
        raise NotImplementedError("커스텀 음성 기능은 아직 준비 중입니다")

    async def get_voice_status(self, *args, **kwargs):
        raise NotImplementedError("커스텀 음성 기능은 아직 준비 중입니다")

    async def delete_voice(self, *args, **kwargs):
        raise NotImplementedError("커스텀 음성 기능은 아직 준비 중입니다")


custom_voice_service = CustomVoiceService()
