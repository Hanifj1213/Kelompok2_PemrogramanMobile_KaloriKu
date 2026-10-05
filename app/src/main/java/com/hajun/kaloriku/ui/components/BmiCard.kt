package com.hajun.kaloriku.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.hajun.kaloriku.R
import com.hajun.kaloriku.ui.theme.BmiNormalColor
import com.hajun.kaloriku.ui.theme.BmiObesityColor
import com.hajun.kaloriku.ui.theme.BmiOverweightColor
import com.hajun.kaloriku.ui.theme.BmiUnderweightColor
import com.hajun.kaloriku.ui.theme.Spacing
import com.hajun.kaloriku.util.BMI_SCALE_MAX
import com.hajun.kaloriku.util.BMI_SCALE_MIN
import com.hajun.kaloriku.util.BmiCategory
import com.hajun.kaloriku.util.bmiCategory
import com.hajun.kaloriku.util.bmiMarkerFraction
import com.hajun.kaloriku.util.formatDecimal
import com.hajun.kaloriku.util.formatWhole

/** Kartu IMT tanpa ViewModel; nilai dan kategori mengikuti berat/tinggi yang sedang diisi. */
@Composable
fun BmiCard(bmi: Double?, modifier: Modifier = Modifier) {
    val category = bmiCategory(bmi)
    val value = bmi?.takeIf { category != null }
    val onSurface = MaterialTheme.colorScheme.onSurface

    AppCard(modifier = modifier.fillMaxWidth(), spacing = Spacing.md) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            IconBadge(icon = R.drawable.ic_health_and_safety, tint = onSurface)
            Text(
                text = "Indeks massa tubuh (IMT)",
                style = MaterialTheme.typography.titleSmall,
                color = onSurface,
                modifier = Modifier.weight(1f)
            )
        }

        // Angka dan kategori ditumpuk agar tetap terbaca saat ukuran font diperbesar.
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            Text(
                text = value?.formatDecimal() ?: "Belum dihitung",
                style = if (value != null) {
                    MaterialTheme.typography.headlineLarge.copy(fontFeatureSettings = "tnum")
                } else {
                    MaterialTheme.typography.titleMedium
                },
                color = onSurface,
                modifier = Modifier.fillMaxWidth().testTag("bmi_value")
            )
            if (category != null) {
                Text(
                    text = category.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = onSurface,
                    modifier = Modifier.fillMaxWidth().testTag("bmi_status")
                )
            }
        }

        BmiVisualScale(bmi = value)

        if (value == null) {
            Text(
                text = "Isi berat dan tinggi badan untuk menghitung IMT secara otomatis.",
                style = MaterialTheme.typography.bodyMedium,
                color = onSurface
            )
        } else if (value < BMI_SCALE_MIN || value > BMI_SCALE_MAX) {
            Text(
                text = "Nilai IMT di luar skala 15–35; penanda ditampilkan di tepi skala.",
                style = MaterialTheme.typography.bodySmall,
                color = onSurface
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            BmiCategory.entries.forEach { item ->
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = Spacing.xs)
                            .size(Spacing.md)
                            .clip(MaterialTheme.shapes.extraSmall)
                            .background(item.color())
                    )
                    Text(
                        text = "${item.label} · ${item.rangeLabel()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = onSurface,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        Text(
            text = "IMT adalah panduan umum untuk dewasa, bukan diagnosis.",
            style = MaterialTheme.typography.bodySmall,
            color = onSurface
        )
    }
}

@Composable
internal fun BmiVisualScale(bmi: Double?, modifier: Modifier = Modifier) {
    val fraction = bmiMarkerFraction(bmi)
    val onSurface = MaterialTheme.colorScheme.onSurface
    val scaleState = when {
        fraction == null -> "Belum dihitung; tanpa penanda"
        bmi != null && bmi < BMI_SCALE_MIN -> "IMT ${bmi.formatDecimal()}; penanda di batas bawah skala"
        bmi != null && bmi > BMI_SCALE_MAX -> "IMT ${bmi.formatDecimal()}; penanda di batas atas skala"
        else -> "Penanda IMT ${bmi?.formatDecimal()}"
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(Spacing.xl)
                .testTag("bmi_scale")
                .semantics {
                    contentDescription = "Skala IMT 15 sampai 35"
                    stateDescription = scaleState
                }
        ) {
            val markerHalfWidth = Spacing.sm.toPx().coerceAtMost(size.width / 2f)
            val markerHeight = Spacing.sm.toPx()
            val trackLeft = markerHalfWidth
            val trackWidth = (size.width - trackLeft * 2f).coerceAtLeast(0f)
            val trackTop = markerHeight + Spacing.xs.toPx()
            val trackHeight = Spacing.md.toPx()
            val trackPath = Path().apply {
                addRoundRect(
                    RoundRect(
                        left = trackLeft,
                        top = trackTop,
                        right = trackLeft + trackWidth,
                        bottom = trackTop + trackHeight,
                        cornerRadius = CornerRadius(trackHeight / 2f)
                    )
                )
            }

            // Segmen dan penanda memakai koordinat yang sama tanpa celah antarkategori.
            clipPath(trackPath) {
                BmiCategory.entries.forEach { category ->
                    val start = bmiMarkerFraction(category.lowerInclusive ?: BMI_SCALE_MIN) ?: return@forEach
                    val end = bmiMarkerFraction(category.upperExclusive ?: BMI_SCALE_MAX) ?: return@forEach
                    drawRect(
                        color = category.color(),
                        topLeft = Offset(trackLeft + trackWidth * start, trackTop),
                        size = Size(trackWidth * (end - start), trackHeight)
                    )
                }
            }
            if (fraction != null) {
                val x = trackLeft + trackWidth * fraction
                val markerPath = Path().apply {
                    moveTo(x, markerHeight)
                    lineTo(x - markerHalfWidth, 0f)
                    lineTo(x + markerHalfWidth, 0f)
                    close()
                }
                drawPath(markerPath, color = onSurface)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.sm, vertical = Spacing.xs),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(BMI_SCALE_MIN.formatWhole(), style = MaterialTheme.typography.labelSmall, color = onSurface)
            Text(BMI_SCALE_MAX.formatWhole(), style = MaterialTheme.typography.labelSmall, color = onSurface)
        }
    }
}

private fun BmiCategory.color(): Color = when (this) {
    BmiCategory.KURANG -> BmiUnderweightColor
    BmiCategory.NORMAL -> BmiNormalColor
    BmiCategory.BERLEBIH -> BmiOverweightColor
    BmiCategory.OBESITAS -> BmiObesityColor
}

private fun BmiCategory.rangeLabel(): String {
    val lower = lowerInclusive
    val upper = upperExclusive
    return when {
        lower == null && upper != null -> "IMT < ${upper.formatDecimal()}"
        lower != null && upper == null -> "IMT ≥ ${lower.formatDecimal()}"
        lower != null && upper != null -> "${lower.formatDecimal()} ≤ IMT < ${upper.formatDecimal()}"
        else -> ""
    }
}
