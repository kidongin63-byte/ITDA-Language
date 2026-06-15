package com.itda.language.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import com.itda.language.R
import com.itda.language.ui.theme.*

/**
 * 수어→음성 탭 (뼈대 UI)
 * 실제 카메라/수어 인식 기능은 미구현 — 디자인 목업의 레이아웃만 표시
 */
@Composable
fun SignToVoiceTab() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // ─── 카메라 뷰파인더 영역 (뼈대) ───
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF2A2A2A)),
            contentAlignment = Alignment.Center,
        ) {
            // 코너 브래킷 (뷰파인더 느낌)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
            ) {
                // 좌상단
                CornerBracket(
                    modifier = Modifier.align(Alignment.TopStart),
                )
                // 우상단
                CornerBracket(
                    modifier = Modifier.align(Alignment.TopEnd),
                    flipH = true,
                )
                // 좌하단
                CornerBracket(
                    modifier = Modifier.align(Alignment.BottomStart),
                    flipV = true,
                )
                // 우하단
                CornerBracket(
                    modifier = Modifier.align(Alignment.BottomEnd),
                    flipH = true,
                    flipV = true,
                )
            }

            // 안내 텍스트
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.sign_camera_guide),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.5f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }

            // "손동작 인식중" 배지
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color.White.copy(alpha = 0.2f),
            ) {
                Text(
                    text = stringResource(R.string.sign_detecting),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.8f),
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ─── 번역 결과 카드 (데모 텍스트) ───
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = stringResource(R.string.sign_result),
                    style = MaterialTheme.typography.labelMedium,
                    color = SecondaryText,
                )
                Spacer(modifier = Modifier.height(8.dp))

                // 데모 번역 텍스트 (하이라이트 포함)
                Text(
                    text = buildAnnotatedString {
                        append("\"안녕하세요, 만나서 반갑습니다. ")
                        withStyle(SpanStyle(color = WarmOrangeCoral, fontWeight = FontWeight.Bold)) {
                            append("오늘 날씨가 정말 좋네요.")
                        }
                        append("\"")
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = BaseText,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 28.sp,
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

/**
 * 뷰파인더 코너 브래킷 장식
 */
@Composable
private fun CornerBracket(
    modifier: Modifier = Modifier,
    flipH: Boolean = false,
    flipV: Boolean = false,
) {
    val size = 24.dp
    val thickness = 2.dp
    val bracketColor = Color.White.copy(alpha = 0.6f)

    Box(modifier = modifier.size(size)) {
        // 가로 선
        Box(
            modifier = Modifier
                .width(size)
                .height(thickness)
                .align(if (flipV) Alignment.BottomStart else Alignment.TopStart)
                .background(bracketColor),
        )
        // 세로 선
        Box(
            modifier = Modifier
                .width(thickness)
                .height(size)
                .align(
                    if (flipH) {
                        if (flipV) Alignment.BottomEnd else Alignment.TopEnd
                    } else {
                        if (flipV) Alignment.BottomStart else Alignment.TopStart
                    }
                )
                .background(bracketColor),
        )
    }
}
