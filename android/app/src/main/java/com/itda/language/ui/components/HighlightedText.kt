package com.itda.language.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import com.itda.language.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun HighlightedText(
    text: String,
    progress: Float,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = TextStyle(fontSize = 22.sp, lineHeight = 36.sp),
    highlightColor: Color = WarmOrangeCoral.copy(alpha = 0.2f),
    spokenColor: Color = BaseText,
    unspokenColor: Color = SecondaryText.copy(alpha = 0.4f),
) {
    if (text.isEmpty()) return

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 80),
        label = "textProgress",
    )

    val charOffset = (animatedProgress * text.length).toInt().coerceIn(0, text.length)
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    // 현재 읽히는 줄로 자동 스크롤
    LaunchedEffect(charOffset) {
        val layout = textLayoutResult ?: return@LaunchedEffect
        if (charOffset > 0 && charOffset < text.length) {
            val lineIndex = layout.getLineForOffset(charOffset)
            val lineTop = layout.getLineTop(lineIndex).toInt()
            scope.launch {
                scrollState.animateScrollTo(
                    (lineTop - 100).coerceAtLeast(0),
                )
            }
        }
    }

    // AnnotatedString: 읽힌 부분 진하게, 안 읽힌 부분 연하게
    val annotatedText = buildAnnotatedString {
        if (charOffset > 0) {
            withStyle(SpanStyle(color = spokenColor)) {
                append(text.substring(0, charOffset))
            }
        }
        if (charOffset < text.length) {
            withStyle(SpanStyle(color = unspokenColor)) {
                append(text.substring(charOffset))
            }
        }
    }

    Text(
        text = annotatedText,
        style = textStyle,
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .drawBehind {
                // 현재 위치에 하이라이트 바 그리기
                val layout = textLayoutResult ?: return@drawBehind
                if (charOffset <= 0 || charOffset >= text.length) return@drawBehind

                try {
                    val rect = layout.getBoundingBox(charOffset.coerceIn(0, text.length - 1))
                    val lineIndex = layout.getLineForOffset(charOffset)
                    val lineTop = layout.getLineTop(lineIndex)
                    val lineBottom = layout.getLineBottom(lineIndex)

                    // 코랄 색상 커서 바
                    drawRoundRect(
                        color = WarmOrangeCoral.copy(alpha = 0.6f),
                        topLeft = Offset(rect.left - 1f, lineTop),
                        size = Size(3f, lineBottom - lineTop),
                        cornerRadius = CornerRadius(2f, 2f),
                    )

                    // 현재 줄 전체 배경 하이라이트
                    drawRoundRect(
                        color = highlightColor,
                        topLeft = Offset(0f, lineTop),
                        size = Size(size.width, lineBottom - lineTop),
                        cornerRadius = CornerRadius(4f, 4f),
                    )
                } catch (_: Exception) {
                    // 범위 초과 시 무시
                }
            },
        onTextLayout = { textLayoutResult = it },
    )
}
