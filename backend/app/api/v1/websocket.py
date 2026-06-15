"""
WebSocket 실시간 TTS 스트리밍 엔드포인트

클라이언트가 텍스트를 전송하면 문장 단위로 분할 합성하여
오디오 청크를 순차적으로 스트리밍한다.
"""

import base64
import json

from fastapi import APIRouter, WebSocket, WebSocketDisconnect

from app.services.tts_orchestrator import tts_orchestrator

router = APIRouter()


@router.websocket("/tts/stream")
async def tts_stream(ws: WebSocket):
    await ws.accept()

    try:
        while True:
            raw = await ws.receive_text()
            msg = json.loads(raw)

            if msg.get("type") != "synthesize":
                await ws.send_json({"type": "error", "detail": "지원하지 않는 메시지 타입"})
                continue

            text = msg.get("text", "").strip()
            if not text:
                await ws.send_json({"type": "error", "detail": "텍스트가 비어있습니다"})
                continue

            speaker = msg.get("speaker", "nara")
            speed = msg.get("speed", 0)
            pitch = msg.get("pitch", 0)
            volume = msg.get("volume", 0)

            # 문장 분할 병렬 합성
            chunks = await tts_orchestrator.synthesize_stream_chunks(
                text, speaker, speed, pitch, volume
            )

            # 순차 전송
            for i, (audio_bytes, seq) in enumerate(chunks):
                is_last = i == len(chunks) - 1
                await ws.send_json(
                    {
                        "type": "audio_chunk",
                        "sequence": seq,
                        "data": base64.b64encode(audio_bytes).decode("ascii"),
                        "is_last": is_last,
                    }
                )

    except WebSocketDisconnect:
        pass
    except json.JSONDecodeError:
        await ws.close(code=1003, reason="잘못된 JSON 형식")
    except Exception as e:
        await ws.send_json({"type": "error", "detail": str(e)})
        await ws.close(code=1011)
