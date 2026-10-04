package com.hajun.kaloriku.ui.components

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.hajun.kaloriku.util.formatWhole
import kotlin.math.roundToInt

/**
 * Angka besar yang beranimasi singkat dan memakai tabular numbers ("tnum") supaya
 * lebarnya tidak berubah-ubah saat nilainya diganti.
 */
@Composable
fun AnimatedWholeNumber(
    value: Double,
    style: TextStyle,
    color: Color = Color.Unspecified,
    modifier: Modifier = Modifier,
    durationMillis: Int = 220
) {
    val animated by animateIntAsState(
        targetValue = value.roundToInt(),
        animationSpec = tween(durationMillis = durationMillis),
        label = "angka"
    )
    Text(
        text = animated.formatWhole(),
        style = style.copy(fontFeatureSettings = "tnum"),
        color = color,
        modifier = modifier
    )
}
