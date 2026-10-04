package com.hajun.kaloriku.ui.charts

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hajun.kaloriku.data.DailyGoals
import com.hajun.kaloriku.data.FoodItem
import com.hajun.kaloriku.ui.theme.CarbsColor
import com.hajun.kaloriku.ui.theme.FatColor
import com.hajun.kaloriku.ui.theme.ProteinColor
import com.hajun.kaloriku.util.formatDecimal
import com.hajun.kaloriku.util.formatWhole

/**
 * Cincin kemajuan berbentuk busur 300 derajat, dipakai sebagai angka utama di Beranda
 * dan layar hasil analisis.
 */
@Composable
fun CalorieRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 176.dp,
    strokeWidth: Dp = 15.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val animated = remember { Animatable(0f) }
    LaunchedEffect(progress) {
        animated.animateTo(
            targetValue = progress.coerceIn(0f, 1f),
            animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)
        )
    }

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size)) {
            val stroke = strokeWidth.toPx()
            val inset = stroke / 2f
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            drawArc(
                color = trackColor,
                startAngle = GAUGE_START,
                sweepAngle = GAUGE_SWEEP,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            if (animated.value > 0f) {
                drawArc(
                    color = color,
                    startAngle = GAUGE_START,
                    sweepAngle = GAUGE_SWEEP * animated.value,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
        }
        content()
    }
}

/** Cincin kecil tanpa angka di tengah, untuk capaian protein, karbohidrat, dan lemak. */
@Composable
fun MacroRing(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 46.dp,
    strokeWidth: Dp = 5.dp,
    contentColor: Color = MaterialTheme.colorScheme.primary,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val animated = remember { Animatable(0f) }
    LaunchedEffect(progress) {
        animated.animateTo(progress.coerceIn(0f, 1f), tween(700, easing = FastOutSlowInEasing))
    }
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size)) {
            val stroke = strokeWidth.toPx()
            val inset = stroke / 2f
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            drawArc(
                color = contentColor.copy(alpha = 0.16f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke)
            )
            if (animated.value > 0f) {
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = 360f * animated.value,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
        }
        content()
    }
}

/** Batang kemajuan satu gizi dengan label dan angka. */
@Composable
fun MacroBar(
    label: String,
    value: Double,
    target: Double,
    color: Color,
    modifier: Modifier = Modifier
) {
    val over = target > 0 && value > target
    val progress = if (target <= 0) 0f else (value / target).toFloat().coerceIn(0f, 1f)
    val animated = remember { Animatable(0f) }
    LaunchedEffect(progress) {
        animated.animateTo(progress, tween(700, easing = FastOutSlowInEasing))
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(RoundedCornerShape(50))
                    .background(color)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(
                text = "${value.formatDecimal()} / ${target.formatWhole()} g",
                style = MaterialTheme.typography.labelLarge,
                color = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(9.dp)
                .clip(RoundedCornerShape(50))
                .background(color.copy(alpha = 0.16f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animated.value)
                    .height(9.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (over) MaterialTheme.colorScheme.error else color)
            )
        }
    }
}

/** Susunan tiga ring makro untuk ringkasan harian. */
@Composable
fun MacroRingsRow(totals: FoodItem, goals: DailyGoals, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        MacroRingItem(
            label = "Protein",
            value = totals.proteinG,
            target = goals.proteinG.toDouble(),
            color = ProteinColor
        )
        MacroRingItem(
            label = "Karbo",
            value = totals.carbsG,
            target = goals.carbsG.toDouble(),
            color = CarbsColor
        )
        MacroRingItem(
            label = "Lemak",
            value = totals.fatG,
            target = goals.fatG.toDouble(),
            color = FatColor
        )
    }
}

@Composable
private fun MacroRingItem(label: String, value: Double, target: Double, color: Color) {
    val progress = if (target <= 0) 0f else (value / target).toFloat()
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        MacroRing(progress = progress, color = color) {
            Text(
                text = "${(progress * 100).toInt().coerceAtMost(999)}%",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${value.formatDecimal()} g",
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "$label · ${target.formatWhole()} g",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Satu batang bertumpuk yang menunjukkan porsi kalori dari tiap makro. */
@Composable
fun StackedMacroBar(item: FoodItem, modifier: Modifier = Modifier) {
    val proteinKcal = (item.proteinG * 4).coerceAtLeast(0.0)
    val carbsKcal = (item.carbsG * 4).coerceAtLeast(0.0)
    val fatKcal = (item.fatG * 9).coerceAtLeast(0.0)
    val total = proteinKcal + carbsKcal + fatKcal

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(50))
    ) {
        if (total <= 0.0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            )
            return@Row
        }
        Box(
            modifier = Modifier
                .weight((proteinKcal / total).toFloat().coerceAtLeast(0.001f))
                .height(10.dp)
                .background(ProteinColor)
        )
        Box(
            modifier = Modifier
                .weight((carbsKcal / total).toFloat().coerceAtLeast(0.001f))
                .height(10.dp)
                .background(CarbsColor)
        )
        Box(
            modifier = Modifier
                .weight((fatKcal / total).toFloat().coerceAtLeast(0.001f))
                .height(10.dp)
                .background(FatColor)
        )
    }
}

/** Grafik batang tujuh hari dengan garis target putus-putus di latar belakang. */
@Composable
fun WeeklyBarChart(
    labels: List<String>,
    values: List<Double>,
    target: Double,
    modifier: Modifier = Modifier,
    highlighted: Int = -1,
    height: Dp = 150.dp,
    barColor: Color = MaterialTheme.colorScheme.primary,
    overColor: Color = MaterialTheme.colorScheme.error,
    emptyColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    targetColor: Color = MaterialTheme.colorScheme.outline
) {
    val maxValue = maxOf(target, values.maxOrNull() ?: 0.0).coerceAtLeast(1.0)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val slot = size.width / values.size.coerceAtLeast(1)
        val barWidth = (slot * 0.52f).coerceAtMost(30.dp.toPx())
        val radius = barWidth / 2f

        if (target > 0.0) {
            val y = size.height - (target / maxValue * size.height).toFloat()
            drawLine(
                color = targetColor.copy(alpha = 0.7f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
            )
        }

        values.forEachIndexed { index, value ->
            val centerX = slot * index + slot / 2f
            val left = centerX - barWidth / 2f
            val ratio = (value / maxValue).toFloat().coerceIn(0f, 1f)
            val barHeight = (size.height * ratio).coerceAtLeast(if (value > 0) radius * 2 else 3.dp.toPx())
            val top = size.height - barHeight
            val color = when {
                value <= 0.0 -> emptyColor
                target > 0.0 && value > target -> overColor
                index == highlighted -> barColor
                else -> barColor.copy(alpha = 0.35f)
            }
            drawRoundRect(
                color = color,
                topLeft = Offset(left, top),
                size = Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius)
            )
        }
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        labels.forEachIndexed { index, label ->
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (index == highlighted) FontWeight.Bold else FontWeight.Medium,
                    color = if (index == highlighted) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1
                )
            }
        }
    }
}

private const val GAUGE_START = 150f
private const val GAUGE_SWEEP = 300f