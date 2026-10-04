package com.hajun.kaloriku.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    // Teks hijau dan tombol berteks putih memakai Green700 (kontras 4,97:1 di atas putih).
    // Green600 tetap warna merek untuk cincin, ikon aktif, dan aksen non-teks.
    primary = Green700,
    onPrimary = Color.White,
    primaryContainer = Green100,
    onPrimaryContainer = Green900,
    inversePrimary = Green500,
    secondary = Orange500,
    onSecondary = Color.White,
    secondaryContainer = Orange100,
    onSecondaryContainer = Orange900,
    tertiary = Green700,
    tertiaryContainer = Green100,
    onTertiaryContainer = Green900,
    background = Canvas,
    onBackground = Ink900,
    surface = Color.White,
    onSurface = Ink900,
    surfaceVariant = Color(0xFFEEF3EF),
    onSurfaceVariant = Ink500,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF9FBFA),
    surfaceContainer = Color(0xFFF1F5F2),
    surfaceContainerHigh = Color(0xFFEAF0EC),
    surfaceContainerHighest = Color(0xFFE3EAE5),
    outline = Ink300,
    outlineVariant = Line,
    error = Red500,
    onError = Color.White,
    errorContainer = Red100,
    onErrorContainer = Color(0xFF5C1013)
)

private val DarkColorScheme = darkColorScheme(
    primary = Green500,
    onPrimary = Green900,
    primaryContainer = Color(0xFF14432C),
    onPrimaryContainer = Green100,
    secondary = Color(0xFFFFA25C),
    onSecondary = Orange900,
    secondaryContainer = Color(0xFF4A2508),
    onSecondaryContainer = Orange100,
    tertiary = Green500,
    tertiaryContainer = Color(0xFF14432C),
    onTertiaryContainer = Green100,
    background = DarkCanvas,
    onBackground = Color(0xFFE6EEE9),
    surface = DarkSurface,
    onSurface = Color(0xFFE6EEE9),
    surfaceVariant = DarkSurfaceHigh,
    onSurfaceVariant = Color(0xFF9AA9A0),
    surfaceContainerLowest = DarkCanvas,
    surfaceContainerLow = DarkSurface,
    surfaceContainer = DarkSurfaceHigh,
    surfaceContainerHigh = Color(0xFF23332A),
    surfaceContainerHighest = Color(0xFF2B3C32),
    outline = Color(0xFF55665C),
    outlineVariant = DarkLine,
    error = Color(0xFFFF6B6F),
    errorContainer = Color(0xFF4A1416),
    onErrorContainer = Red100
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun KaloriKuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
