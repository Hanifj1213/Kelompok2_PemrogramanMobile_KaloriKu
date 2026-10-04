package com.hajun.kaloriku.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hajun.kaloriku.R
import com.hajun.kaloriku.data.MealEntry
import com.hajun.kaloriku.data.Stats
import com.hajun.kaloriku.ui.MainViewModel
import com.hajun.kaloriku.ui.charts.WeeklyBarChart
import com.hajun.kaloriku.ui.components.EmptyState
import com.hajun.kaloriku.ui.components.MealEntryCard
import com.hajun.kaloriku.ui.components.PrimaryButton
import com.hajun.kaloriku.ui.components.StatTile
import com.hajun.kaloriku.ui.theme.Spacing
import com.hajun.kaloriku.util.formatDayMonth
import com.hajun.kaloriku.util.formatRelativeDay
import com.hajun.kaloriku.util.formatWhole
import com.hajun.kaloriku.util.toLocalDate
import java.time.LocalDate

/** Riwayat makan lengkap dengan ringkasan tujuh hari terakhir di bagian atas. */
@Composable
fun HistoryScreen(
    viewModel: MainViewModel,
    onAddFood: () -> Unit,
    onOpenEntry: (Long) -> Unit
) {
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val target by viewModel.dailyTarget.collectAsStateWithLifecycle()
    val goals by viewModel.dailyGoals.collectAsStateWithLifecycle()

    val byDate = remember(entries) {
        entries.groupBy { it.timestamp.toLocalDate() }
            .mapValues { (_, list) -> list.sortedBy { it.timestamp } }
    }
    val today = LocalDate.now()
    val lastSevenDays = remember(today) { (6 downTo 0).map { today.minusDays(it.toLong()) } }
    val weekly = remember(entries, goals, lastSevenDays) {
        Stats.weeklySummaries(entries, lastSevenDays, goals.calories, goals.proteinG)
    }
    val days = remember(byDate) { byDate.keys.sortedDescending() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(
            start = Spacing.screen,
            end = Spacing.screen,
            top = Spacing.sm,
            bottom = Spacing.xxl
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        item {
            Column {
                Text("Riwayat", style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = "${entries.size} catatan tersimpan",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            WeeklySummaryCard(
                averageCalories = weekly.averageCalories,
                daysOverTarget = weekly.daysOverTarget,
                recordedDays = weekly.recordedDays,
                days = lastSevenDays,
                totals = weekly.days.map { it.calories },
                target = target
            )
        }

        if (entries.isNotEmpty()) {
            item { SummaryTiles(entries = entries) }
        }

        if (days.isEmpty()) {
            item {
                EmptyState(
                    icon = R.drawable.ic_calendar_month,
                    message = "Riwayat masih kosong. Catat makanan pertamamu.",
                    action = { PrimaryButton(text = "Catat", onClick = onAddFood) }
                )
            }
        }

        days.forEach { date ->
            val dayEntries = byDate.getValue(date)
            item(key = "header-$date") {
                DayHeader(date = date, entries = dayEntries, target = target)
            }
            items(dayEntries, key = { it.id }) { entry ->
                MealEntryCard(entry = entry, onClick = { onOpenEntry(entry.id) }, modifier = Modifier.animateItem())
            }
        }
    }
}

@Composable
private fun WeeklySummaryCard(
    averageCalories: Double,
    daysOverTarget: Int,
    recordedDays: Int,
    days: List<LocalDate>,
    totals: List<Double>,
    target: Int
) {
    androidx.compose.material3.Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier.padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Text("7 hari terakhir", style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(text = averageCalories.formatWhole(), style = MaterialTheme.typography.headlineMedium)
                Text(
                    text = " kkal rata-rata/hari",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = Spacing.xs, bottom = Spacing.xs)
                )
            }
            Text(
                text = "$daysOverTarget dari $recordedDays hari melebihi target",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            WeeklyBarChart(
                labels = days.map { it.formatDayMonth() },
                values = totals,
                target = target.toDouble(),
                highlighted = days.lastIndex
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                LegendDot(color = MaterialTheme.colorScheme.primary)
                Text("Hari ini", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(Spacing.md))
                LegendDot(color = MaterialTheme.colorScheme.error)
                Text("Melebihi", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(Spacing.md))
                Text(
                    text = "Target ${target.formatWhole()} kkal",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun LegendDot(color: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier
            .size(8.dp)
            .background(color, CircleShape)
    )
    Spacer(modifier = Modifier.width(Spacing.xs))
}

@Composable
private fun SummaryTiles(entries: List<MealEntry>) {
    val totalCalories = entries.sumOf { it.totalCalories }
    val totalItems = entries.sumOf { it.items.size }
    val recordedDays = entries.map { it.timestamp.toLocalDate() }.distinct().size
    val perDay = if (recordedDays == 0) 0.0 else totalCalories / recordedDays

    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        StatTile(
            label = "Rata-rata",
            value = perDay.formatWhole(),
            unit = "kkal",
            modifier = Modifier.weight(1f)
        )
        StatTile(
            label = "Hari",
            value = recordedDays.toString(),
            unit = "hari",
            modifier = Modifier.weight(1f)
        )
        StatTile(
            label = "Makanan",
            value = totalItems.toString(),
            unit = "item",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun DayHeader(date: LocalDate, entries: List<MealEntry>, target: Int) {
    val calories = entries.sumOf { it.totalCalories }
    val over = calories > target
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = date.formatRelativeDay(), style = MaterialTheme.typography.titleSmall)
            Text(
                text = if (over) "Melebihi target ${(calories - target).formatWhole()} kkal" else {
                    "Sisa ${(target - calories).formatWhole()} kkal dari target"
                },
                style = MaterialTheme.typography.labelSmall,
                color = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = "${calories.formatWhole()} kkal",
            style = MaterialTheme.typography.titleMedium,
            color = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
    }
}
