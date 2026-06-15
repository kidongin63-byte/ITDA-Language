package com.itda.language.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.itda.language.ui.theme.OutlineVariant
import com.itda.language.ui.theme.WarmOrangeCoral

@Composable
fun EqualizerVisualizer(
    amplitudeLevels: FloatArray,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 8,
    barWidth: Dp = 6.dp,
    maxBarHeight: Dp = 48.dp,
    minBarHeight: Dp = 4.dp,
    barColor: Color = WarmOrangeCoral,
    inactiveColor: Color = OutlineVariant,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        repeat(barCount) { index ->
            val targetLevel = if (isActive && index < amplitudeLevels.size) {
                amplitudeLevels[index]
            } else {
                0f
            }

            val animatedLevel by animateFloatAsState(
                targetValue = targetLevel,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
                label = "eq_bar_$index",
            )

            val barHeight = minBarHeight + (maxBarHeight - minBarHeight) * animatedLevel
            val color = if (isActive) barColor else inactiveColor

            // 바 + 글로우 효과
            Box(contentAlignment = Alignment.BottomCenter) {
                // 글로우 (바 아래 은은한 빛)
                if (isActive && animatedLevel > 0.1f) {
                    Box(
                        modifier = Modifier
                            .width(barWidth + 4.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(color.copy(alpha = 0.15f * animatedLevel)),
                    )
                }

                // 메인 바
                Box(
                    modifier = Modifier
                        .width(barWidth)
                        .height(barHeight)
                        .clip(RoundedCornerShape(barWidth / 2))
                        .background(color.copy(alpha = if (isActive) 0.6f + 0.4f * animatedLevel else 0.4f)),
                )
            }
        }
    }
}
