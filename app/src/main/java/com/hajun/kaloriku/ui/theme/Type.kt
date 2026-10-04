package com.hajun.kaloriku.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.hajun.kaloriku.R

// Plus Jakarta Sans (SIL Open Font License), satu berkas variable font untuk semua ketebalan.
@OptIn(ExperimentalTextApi::class)
private fun jakarta(weight: FontWeight) = Font(
    resId = R.font.plus_jakarta_sans,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight))
)

val JakartaSans = FontFamily(
    jakarta(FontWeight.Normal),
    jakarta(FontWeight.Medium),
    jakarta(FontWeight.SemiBold),
    jakarta(FontWeight.Bold),
    jakarta(FontWeight.ExtraBold)
)

private fun style(size: Int, line: Int, weight: FontWeight, spacing: Double = 0.0) = TextStyle(
    fontFamily = JakartaSans,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = spacing.sp
)

val Typography = Typography(
    displayLarge = style(52, 60, FontWeight.ExtraBold, -1.5),
    displayMedium = style(44, 52, FontWeight.ExtraBold, -1.0),
    displaySmall = style(36, 44, FontWeight.ExtraBold, -0.8),
    headlineLarge = style(30, 38, FontWeight.Bold, -0.5),
    headlineMedium = style(26, 34, FontWeight.Bold, -0.4),
    headlineSmall = style(22, 30, FontWeight.Bold, -0.2),
    titleLarge = style(20, 28, FontWeight.Bold, -0.2),
    titleMedium = style(16, 24, FontWeight.Bold),
    titleSmall = style(14, 20, FontWeight.SemiBold),
    bodyLarge = style(16, 24, FontWeight.Normal),
    bodyMedium = style(14, 21, FontWeight.Normal),
    bodySmall = style(12, 18, FontWeight.Normal),
    labelLarge = style(14, 20, FontWeight.SemiBold),
    labelMedium = style(12, 16, FontWeight.SemiBold),
    labelSmall = style(11, 16, FontWeight.Medium, 0.2)
)
