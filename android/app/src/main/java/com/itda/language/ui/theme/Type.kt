package com.itda.language.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val ITDATypography = Typography(
    headlineLarge = TextStyle(
        fontSize = 32.sp,
        fontWeight = FontWeight.Black,
        lineHeight = 40.sp,
        letterSpacing = (-0.01).sp,
    ),
    headlineMedium = TextStyle(
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 31.2.sp,
    ),
    headlineSmall = TextStyle(
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 26.sp,
    ),
    titleLarge = TextStyle(
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 24.sp,
    ),
    bodyLarge = TextStyle(
        fontSize = 18.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 28.8.sp,
    ),
    bodyMedium = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 24.sp,
    ),
    bodySmall = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 20.sp,
    ),
    labelLarge = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 22.4.sp,
    ),
    labelMedium = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 19.6.sp,
        letterSpacing = 0.01.sp,
    ),
    labelSmall = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 16.sp,
    ),
)
