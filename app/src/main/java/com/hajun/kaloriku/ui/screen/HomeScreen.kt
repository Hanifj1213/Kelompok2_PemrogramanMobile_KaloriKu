package com.hajun.kaloriku.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hajun.kaloriku.R
import com.hajun.kaloriku.data.FoodItem
import com.hajun.kaloriku.data.MealEntry
import com.hajun.kaloriku.data.MealType
import com.hajun.kaloriku.data.Stats
import com.hajun.kaloriku.ui.MainViewModel
import com.hajun.kaloriku.ui.charts.CalorieRing
import com.hajun.kaloriku.ui.charts.MacroBarsColumn
import com.hajun.kaloriku.ui.components.AnimatedWholeNumber
import com.hajun.kaloriku.ui.components.AppCard
import com.hajun.kaloriku.ui.components.EmptyState
import com.hajun.kaloriku.ui.components.IconBadge
import com.hajun.kaloriku.ui.components.MealEntryCard
import com.hajun.kaloriku.ui.components.PrimaryButton
import com.hajun.kaloriku.ui.components.SectionHeader
import com.hajun.kaloriku.ui.components.iconRes
import com.hajun.kaloriku.ui.theme.Spacing
import com.hajun.kaloriku.ui.theme.Green600
import com.hajun.kaloriku.util.formatRelativeDay
import com.hajun.kaloriku.util.formatWhole
import com.hajun.kaloriku.util.greetingFor
import com.hajun.kaloriku.util.toLocalDate
import java.time.LocalDate
import java.time.LocalTime

/**
 * Layar utama: sapaan, cincin kalori sebagai angka besar, capaian gizi,
 * dan daftar makanan hari ini per waktu makan.
 */
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onAddMeal: (MealType) -> Unit,
    onOpenProfile: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenGoals: () -> Unit,
    onOpenEntry: (Long) -> Unit,
    onOpenEditProfile: () -> Unit = onOpenProfile
) {
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val goals by viewModel.dailyGoals.collectAsStateWithLifecycle()
    val profile by viewModel.profile.collectAsStateWithLifecycle()

    val today = LocalDate.now()
    val todayEntries = remember(entries, today) {
        entries.filter { it.timestamp.toLocalDate() == today }.sortedBy { it.timestamp }
    }
    val todayTotals = remember(todayEntries) {
        val items = todayEntries.flatMap { it.items }
        FoodItem(
            name = "Total",
            grams = items.sumOf { it.grams },
            calories = items.sumOf { it.calories },
            proteinG = items.sumOf { it.proteinG },
            carbsG = items.sumOf { it.carbsG },
            fatG = items.sumOf { it.fatG }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding(),
        contentPadding = PaddingValues(
            start = Spacing.screen,
            end = Spacing.screen,
            top = Spacing.sm,
            bottom = Spacing.xxl
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        item { HomeHeader(onOpenProfile = onOpenProfile) }

        item {
            CalorieHeroCard(
                consumed = todayTotals.calories,
                target = goals.calories,
                onOpenHistory = onOpenHistory
            )
        }

        item { MacroCard(totals = todayTotals, goals = goals, onOpenGoals = onOpenGoals) }

        if (profile == null) {
            item { ProfilePromptCard(onOpenProfile = onOpenEditProfile) }
        }

        item {
            SectionHeader(title = "Makanan hari ini", action = "Riwayat", onAction = onOpenHistory)
        }

        if (todayEntries.isEmpty()) {
            item {
                EmptyState(
                    icon = R.drawable.ic_no_food,
                    message = "Belum ada catatan hari ini.",
                    action = { PrimaryButton(text = "Catat", onClick = { onAddMeal(MealType.fromHour(LocalTime.now().hour)) }) }
                )
            }
        } else {
            MealType.entries.forEach { type ->
                val mealEntries = todayEntries.filter { it.mealType == type }
                item(key = "header-${type.name}") {
                    MealGroupHeader(type = type, entries = mealEntries)
                }
                if (mealEntries.isEmpty()) {
                    item(key = "empty-${type.name}") {
                        EmptyMealRow(type = type, onAdd = { onAddMeal(type) })
                    }
                } else {
                    mealEntries.forEach { entry ->
                        item(key = entry.id) {
                            MealEntryCard(entry = entry, onClick = { onOpenEntry(entry.id) })
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Nilai gizi dari foto adalah perkiraan AI. Data pencarian memakai TKPI Kemenkes.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.xs)
            )
        }
    }
}

@Composable
private fun HomeHeader(onOpenProfile: () -> Unit) {
    val hour = remember { LocalTime.now().hour }
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = greetingFor(hour),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(text = LocalDate.now().formatRelativeDay(), style = MaterialTheme.typography.headlineSmall)
        }
        IconButton(onClick = onOpenProfile) {
            IconBadge(
                icon = R.drawable.ic_person,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                container = MaterialTheme.colorScheme.primaryContainer,
                size = 44.dp,
                iconSize = 22.dp,
                shape = CircleShape,
                contentDescription = "Buka profil"
            )
        }
    }
}

/** Kartu hero: cincin kalori dengan sisa kalori sebagai angka besar dan tiga bar makro mini. */
@Composable
private fun CalorieHeroCard(consumed: Double, target: Int, onOpenHistory: () -> Unit) {
    val over = consumed > target
    val remaining = (target - consumed)
    val ratio = if (target <= 0) 0f else (consumed / target).toFloat()
    val ringColor = if (over) MaterialTheme.colorScheme.error else Green600

    AppCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onOpenHistory,
        contentPadding = PaddingValues(Spacing.lg)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CalorieRing(
                progress = ratio,
                size = 132.dp,
                strokeWidth = 13.dp,
                color = ringColor,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (over) "Lebih" else "Sisa",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    AnimatedWholeNumber(
                        value = kotlin.math.abs(remaining).toDouble(),
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "kkal",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                HeroStat(label = "Masuk", value = "${consumed.formatWhole()} kkal")
                HeroStat(label = "Target", value = "${target.formatWhole()} kkal")
                Text(
                    text = if (over) "Sudah melebihi target hari ini"
                    else "${(ratio * 100).toInt()}% dari target harian",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun HeroStat(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(52.dp)
        )
        Text(text = value, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun MacroCard(totals: FoodItem, goals: com.hajun.kaloriku.data.DailyGoals, onOpenGoals: () -> Unit) {
    AppCard(modifier = Modifier.fillMaxWidth(), spacing = Spacing.md) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "Capaian gizi", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            TextButton(onClick = onOpenGoals) {
                Text("Atur target", style = MaterialTheme.typography.labelLarge)
            }
        }
        MacroBarsColumn(totals = totals, goals = goals)
    }
}

@Composable
private fun ProfilePromptCard(onOpenProfile: () -> Unit) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
        spacing = Spacing.md
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(
                icon = R.drawable.ic_person,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                container = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = Spacing.md)
            ) {
                Text("Hitung target pribadimu", style = MaterialTheme.typography.titleSmall)
                Text(
                    text = "Isi usia, berat, dan tinggi badan supaya target kalori sesuai tubuhmu.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        PrimaryButton(
            text = "Isi profil",
            onClick = onOpenProfile,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun MealGroupHeader(type: MealType, entries: List<MealEntry>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(type.iconRes),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(Spacing.sm))
        Text(text = type.label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        if (entries.isNotEmpty()) {
            Text(
                text = "${entries.sumOf { it.totalCalories }.formatWhole()} kkal",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyMealRow(type: MealType, onAdd: () -> Unit) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onAdd,
        contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.md)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Belum ada ${type.label.lowercase()}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Icon(
                painter = painterResource(R.drawable.ic_add),
                contentDescription = "Tambah ${type.label}",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
