package com.hajun.kaloriku.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hajun.kaloriku.R
import com.hajun.kaloriku.data.Stats
import com.hajun.kaloriku.ui.MainViewModel
import com.hajun.kaloriku.ui.charts.MacroBar
import com.hajun.kaloriku.ui.charts.WeeklyBarChart
import com.hajun.kaloriku.ui.components.AppCard
import com.hajun.kaloriku.ui.components.EmptyState
import com.hajun.kaloriku.ui.components.IconBadge
import com.hajun.kaloriku.ui.components.StatTile
import com.hajun.kaloriku.ui.theme.CarbsColor
import com.hajun.kaloriku.ui.theme.FatColor
import com.hajun.kaloriku.ui.theme.Green600
import com.hajun.kaloriku.ui.theme.Orange500
import com.hajun.kaloriku.ui.theme.ProteinColor
import com.hajun.kaloriku.util.formatDayMonth
import com.hajun.kaloriku.util.formatDecimal
import com.hajun.kaloriku.util.formatRelativeDay
import com.hajun.kaloriku.util.formatWhole
import java.time.LocalDate
import com.hajun.kaloriku.ui.components.clipPill
import com.hajun.kaloriku.ui.components.tapNoRipple
import androidx.compose.foundation.layout.statusBarsPadding

/** Ringkasan seminggu terakhir: rata-rata gizi, jumlah hari tercatat, dan catatan otomatis. */
@Composable
fun WeeklyScreen(viewModel: MainViewModel, onOpenWeight: () -> Unit) {
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val goals by viewModel.dailyGoals.collectAsStateWithLifecycle()
    val weights by viewModel.weightEntries.collectAsStateWithLifecycle()

    val today = LocalDate.now()
    val days = remember(today) { (6 downTo 0).map { today.minusDays(it.toLong()) } }
    val summary = remember(entries, goals) {
        Stats.weeklySummaries(entries, days, goals.calories, goals.proteinG)
    }
    val weightChange = remember(weights) { Stats.weightChange(weights) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Laporan mingguan", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        text = "7 hari terakhir · ${summary.recordedDays} hari tercatat",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconBadge(
                    icon = R.drawable.ic_bar_chart_fill,
                    tint = MaterialTheme.colorScheme.primary,
                    container = MaterialTheme.colorScheme.primaryContainer,
                    size = 42.dp,
                    iconSize = 20.dp
                )
            }
        }

        item {
            AverageCard(
                averageCalories = summary.averageCalories,
                targetCalories = goals.calories,
                daysOverTarget = summary.daysOverTarget,
                recordedDays = summary.recordedDays
            )
        }

        item {
            AppCard(modifier = Modifier.fillMaxWidth(), spacing = 14.dp) {
                Text("Kalori harian", style = MaterialTheme.typography.titleMedium)
                WeeklyBarChart(
                    labels = summary.days.map { it.date.formatDayMonth() },
                    values = summary.days.map { it.calories },
                    target = goals.calories.toDouble(),
                    highlighted = summary.days.lastIndex
                )
            }
        }

        item {
            AppCard(modifier = Modifier.fillMaxWidth(), spacing = 14.dp) {
                Text("Rata-rata gizi per hari", style = MaterialTheme.typography.titleMedium)
                MacroBar(
                    label = "Protein",
                    value = summary.averageProteinG,
                    target = goals.proteinG.toDouble(),
                    color = ProteinColor
                )
                MacroBar(
                    label = "Karbohidrat",
                    value = summary.averageCarbsG,
                    target = goals.carbsG.toDouble(),
                    color = CarbsColor
                )
                MacroBar(
                    label = "Lemak",
                    value = summary.averageFatG,
                    target = goals.fatG.toDouble(),
                    color = FatColor
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text(
                    text = "Dihitung dari ${summary.recordedDays} hari yang ada catatannya.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(
                    label = "Hari melebihi target",
                    value = summary.daysOverTarget.toString(),
                    unit = "hari",
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    label = "Perubahan berat",
                    value = weightChange?.let { (if (it > 0) "+" else "") + it.formatDecimal() } ?: "—",
                    unit = if (weightChange != null) "kg" else null,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Catatan untukmu",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                IconBadge(
                    icon = R.drawable.ic_lightbulb_fill,
                    tint = Orange500,
                    size = 34.dp,
                    iconSize = 18.dp,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                )
            }
        }

        if (summary.insights.isEmpty()) {
            item {
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Belum ada catatan khusus. Terus catat makananmu supaya laporannya makin berguna.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            summary.insights.forEach { insight ->
                item {
                    AppCard(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        spacing = 8.dp
                    ) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                painter = painterResource(R.drawable.ic_lightbulb),
                                contentDescription = null,
                                tint = Orange500,
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .size(18.dp)
                            )
                            Text(
                                text = insight,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 10.dp)
                            )
                        }
                    }
                }
            }
        }

        item {
            AppCard(modifier = Modifier.fillMaxWidth(), spacing = 10.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(
                        icon = R.drawable.ic_monitor_weight,
                        tint = WeightAccent,
                        size = 40.dp,
                        iconSize = 20.dp
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp)
                    ) {
                        Text("Pantau berat badan", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = if (weights.isEmpty()) {
                                "Belum ada catatan berat badan."
                            } else {
                                "${weights.size} catatan · terakhir ${weights.maxByOrNull { it.date }?.weightKg?.formatDecimal()} kg"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        painter = painterResource(R.drawable.ic_chevron_right),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier
                            .size(20.dp)
                            .tapNoRipple(onClick = onOpenWeight)
                    )
                }
            }
        }

        item {
            Text(
                text = "Laporan dihitung dari catatanmu sendiri. Angka gizi bersifat perkiraan.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AverageCard(
    averageCalories: Double,
    targetCalories: Int,
    daysOverTarget: Int,
    recordedDays: Int
) {
    val ratio = if (targetCalories <= 0) 0f else (averageCalories / targetCalories).toFloat()
    val over = averageCalories > targetCalories

    AppCard(
        modifier = Modifier.fillMaxWidth(),
        border = false,
        color = MaterialTheme.colorScheme.surface,
        contentPadding = PaddingValues(18.dp),
        spacing = 10.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(
                icon = R.drawable.ic_local_fire_department_fill,
                tint = if (over) Orange500 else Green600,
                size = 44.dp,
                iconSize = 22.dp
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(
                    text = "Rata-rata harian",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(text = averageCalories.formatWhole(), style = MaterialTheme.typography.headlineMedium)
                    Text(
                        text = " kkal",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }
        }
        LinearProgressIndicator(
            progress = { ratio.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clipPill(),
            color = if (over) Orange500 else Green600,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
        Text(
            text = "Target ${targetCalories.formatWhole()} kkal · " +
                "$daysOverTarget dari $recordedDays hari melebihi target",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private val WeightAccent: Color = Color(0xFF6366F1)
