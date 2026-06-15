package com.itda.language.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    // Primary — Warm Coral
    primary = CoralDark,
    onPrimary = Color.White,
    primaryContainer = WarmOrangeCoral,
    onPrimaryContainer = Color(0xFF590F00),
    inversePrimary = CoralLight,

    // Secondary — Stone
    secondary = Secondary,
    onSecondary = Color.White,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,

    // Tertiary — Teal
    tertiary = Tertiary,
    onTertiary = Color.White,
    tertiaryContainer = TertiaryContainer,
    onTertiaryContainer = OnTertiaryContainer,

    // Background & Surface — Warm Ivory
    background = WarmIvory,
    onBackground = BaseText,
    surface = SurfaceWhite,
    onSurface = OnSurface,
    onSurfaceVariant = OnSurfaceVariant,
    surfaceVariant = SurfaceContainerHighest,
    inverseSurface = InverseSurface,
    inverseOnSurface = InverseOnSurface,
    surfaceTint = CoralDark,

    // Outline
    outline = Outline,
    outlineVariant = OutlineVariant,

    // Error
    error = Error,
    onError = Color.White,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,
)

val ITDAShapes = Shapes(
    small = RoundedCornerShape(12.dp),   // 버튼, 입력 필드
    medium = RoundedCornerShape(16.dp),  // 카드
    large = RoundedCornerShape(24.dp),   // 바텀시트
)

@Composable
fun ITDATheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = ITDATypography,
        shapes = ITDAShapes,
        content = content,
    )
}
