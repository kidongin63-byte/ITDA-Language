package com.itda.language.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.itda.language.ui.theme.WarmOrangeCoral

@Composable
fun WaveformIndicator(
    isActive: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 5,
    barWidth: Dp = 4.dp,
    maxBarHeight: Dp = 28.dp,
    minBarHeight: Dp = 6.dp,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(barCount) { index ->
            val animatedHeight by infiniteTransition.animateFloat(
                initialValue = if (isActive) minBarHeight.value else minBarHeight.value,
                targetValue = if (isActive) maxBarHeight.value else minBarHeight.value,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 400 + index * 80,
                        easing = FastOutSlowInEasing,
                    ),
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(index * 100),
                ),
                label = "bar_$index",
            )

            Box(
                modifier = Modifier
                    .width(barWidth)
                    .height(animatedHeight.dp)
                    .clip(RoundedCornerShape(barWidth / 2))
                    .background(
                        if (isActive) WarmOrangeCoral
                        else MaterialTheme.colorScheme.outlineVariant
                    ),
            )
        }
    }
}
