package com.hajun.kaloriku.ui.screen

import android.widget.Toast
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hajun.kaloriku.R
import com.hajun.kaloriku.data.FoodItem
import com.hajun.kaloriku.data.MealEntry
import com.hajun.kaloriku.data.MealType
import com.hajun.kaloriku.data.Stats
import com.hajun.kaloriku.ui.MainViewModel
import com.hajun.kaloriku.ui.charts.CalorieRing
import com.hajun.kaloriku.ui.charts.MacroRingsRow
import com.hajun.kaloriku.ui.components.AppCard
import com.hajun.kaloriku.ui.components.EmptyState
import com.hajun.kaloriku.ui.components.IconBadge
import com.hajun.kaloriku.ui.components.MealEntryCard
import com.hajun.kaloriku.ui.components.PrimaryButton
import com.hajun.kaloriku.ui.components.SectionHeader
import com.hajun.kaloriku.ui.components.StepButton
import com.hajun.kaloriku.ui.components.clipPill
import com.hajun.kaloriku.ui.components.tapNoRipple
import com.hajun.kaloriku.ui.components.accent
import com.hajun.kaloriku.ui.components.iconRes
import com.hajun.kaloriku.ui.theme.Green600
import com.hajun.kaloriku.ui.theme.Green700
import com.hajun.kaloriku.ui.theme.Orange500
import com.hajun.kaloriku.ui.theme.WaterColor
import com.hajun.kaloriku.util.formatDecimal
import com.hajun.kaloriku.util.formatRelativeDay
import com.hajun.kaloriku.util.formatVolume
import com.hajun.kaloriku.util.formatWhole
import com.hajun.kaloriku.util.greetingFor
import com.hajun.kaloriku.util.toLocalDate
import java.time.LocalDate
import java.time.LocalTime
import androidx.compose.foundation.layout.statusBarsPadding
import com.hajun.kaloriku.ui.components.SecondaryButton

/**
 * Layar utama: sapaan, cincin kalori, capaian gizi, air minum,
 * pintasan pencatatan, dan daftar makanan hari ini per waktu makan.
 */
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onAddMeal: (MealType) -> Unit,
    onOpenProfile: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenBarcode: () -> Unit,
    onOpenVoice: () -> Unit,
    onOpenGoals: () -> Unit
) {
    val context = LocalContext.current
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val goals by viewModel.dailyGoals.collectAsStateWithLifecycle()
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val waterEntries by viewModel.waterEntries.collectAsStateWithLifecycle()

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
    val streak = remember(entries) { Stats.streak(entries, today) }
    val waterToday = remember(waterEntries, today) { Stats.waterGlasses(today, waterEntries) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { HomeHeader(streak = streak, onOpenProfile = onOpenProfile) }

        item {
            CalorieHeroCard(
                consumed = todayTotals.calories,
                target = goals.calories,
                onOpenHistory = onOpenHistory
            )
        }

        item { MacroCard(totals = todayTotals, goals = goals, onOpenGoals = onOpenGoals) }

        item {
            QuickActionCard(
                onCamera = onCamera,
                onGallery = onGallery,
                onSearch = onOpenSearch,
                onBarcode = onOpenBarcode,
                onVoice = onOpenVoice
            )
        }

        item {
            WaterCard(
                glasses = waterToday,
                onAdd = viewModel::addWater,
                onRemove = viewModel::removeWaterToday
            )
        }

        if (profile == null) {
            item { ProfilePromptCard(onOpenProfile = onOpenProfile) }
        }

        item {
            SectionHeader(title = "Makanan hari ini", action = "Riwayat", onAction = onOpenHistory)
        }

        if (todayEntries.isEmpty()) {
            item {
                EmptyState(
                    icon = R.drawable.ic_no_food,
                    title = "Belum ada catatan",
                    message = "Foto makananmu, atau catat lewat pencarian, barcode, dan suara."
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
                            MealEntryCard(entry = entry, onDelete = { viewModel.deleteEntry(entry.id) })
                        }
                    }
                }
            }
        }

        item {
            ShareRow(
                onShare = {
                    if (!viewModel.shareTodaySummary(context)) {
                        Toast.makeText(context, "Belum ada catatan hari ini.", Toast.LENGTH_SHORT).show()
                    }
                },
                onExport = {
                    if (!viewModel.exportCsv(context)) {
                        Toast.makeText(context, "Belum ada catatan untuk diekspor.", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        item {
            Text(
                text = "Nilai gizi dari foto adalah perkiraan AI. Data pencarian memakai TKPI Kemenkes.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun ShareRow(onShare: () -> Unit, onExport: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        SecondaryButton(
            text = "Bagikan",
            icon = R.drawable.ic_share,
            onClick = onShare,
            modifier = Modifier.weight(1f)
        )
        SecondaryButton(
            text = "Ekspor CSV",
            icon = R.drawable.ic_download,
            onClick = onExport,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun HomeHeader(streak: Int, onOpenProfile: () -> Unit) {
    val hour = remember { LocalTime.now().hour }
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = greetingFor(hour),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = LocalDate.now().formatRelativeDay(),
                style = MaterialTheme.typography.headlineSmall
            )
        }
        if (streak > 0) {
            Row(
                modifier = Modifier
                    .clipPill()
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_local_fire_department_fill),
                    contentDescription = null,
                    tint = Orange500,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "$streak hari",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
        }
        IconButton(onClick = onOpenProfile) {
            IconBadge(
                icon = R.drawable.ic_person,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                container = MaterialTheme.colorScheme.primaryContainer,
                size = 40.dp,
                iconSize = 20.dp,
                shape = CircleShape
            )
        }
    }
}

/** Kartu utama: cincin kalori dengan angka sisa di tengahnya. */
@Composable
private fun CalorieHeroCard(consumed: Double, target: Int, onOpenHistory: () -> Unit) {
    val over = consumed > target
    val remaining = (target - consumed)
    val ratio = if (target <= 0) 0f else (consumed / target).toFloat()
    val ringColor = if (over) Orange500 else Color.White

    AppCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onOpenHistory,
        border = false,
        shape = MaterialTheme.shapes.extraLarge,
        contentPadding = PaddingValues(20.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(
                    Brush.linearGradient(
                        colors = listOf(Green600, Green700),
                        start = androidx.compose.ui.geometry.Offset.Zero,
                        end = androidx.compose.ui.geometry.Offset(900f, 900f)
                    )
                )
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CalorieRing(
                    progress = ratio,
                    size = 188.dp,
                    strokeWidth = 16.dp,
                    color = ringColor,
                    trackColor = Color.White.copy(alpha = 0.22f)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (over) "Lebih" else "Sisa",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        Text(
                            text = kotlin.math.abs(remaining).formatWhole(),
                            style = MaterialTheme.typography.displaySmall,
                            color = Color.White
                        )
                        Text(
                            text = "kkal",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    HeroStat(label = "Masuk", value = "${consumed.formatWhole()} kkal")
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(32.dp)
                            .background(Color.White.copy(alpha = 0.25f))
                    )
                    HeroStat(label = "Target", value = "${target.formatWhole()} kkal")
                }
                Text(
                    text = if (over) {
                        "Sudah melebihi target hari ini"
                    } else {
                        "${(ratio * 100).toInt()}% dari target harian"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }
    }
}

@Composable
private fun HeroStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.8f)
        )
        Text(text = value, style = MaterialTheme.typography.titleMedium, color = Color.White)
    }
}

@Composable
private fun MacroCard(totals: FoodItem, goals: com.hajun.kaloriku.data.DailyGoals, onOpenGoals: () -> Unit) {
    AppCard(modifier = Modifier.fillMaxWidth(), spacing = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "Capaian gizi", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text(
                text = "Atur target",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
                    .tapNoRipple(onClick = onOpenGoals)
            )
        }
        MacroRingsRow(totals = totals, goals = goals)
    }
}

@Composable
private fun QuickActionCard(
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onSearch: () -> Unit,
    onBarcode: () -> Unit,
    onVoice: () -> Unit
) {
    AppCard(modifier = Modifier.fillMaxWidth(), spacing = 14.dp) {
        Text("Catat makanan", style = MaterialTheme.typography.titleMedium)
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { QuickAction(icon = R.drawable.ic_photo_camera, label = "Foto", tint = Green600, onClick = onCamera) }
            item { QuickAction(icon = R.drawable.ic_image, label = "Galeri", tint = Color(0xFF6366F1), onClick = onGallery) }
            item { QuickAction(icon = R.drawable.ic_search, label = "Cari", tint = Color(0xFF0EA5E9), onClick = onSearch) }
            item { QuickAction(icon = R.drawable.ic_barcode_scanner, label = "Barcode", tint = Color(0xFFF59E0B), onClick = onBarcode) }
            item { QuickAction(icon = R.drawable.ic_mic, label = "Suara", tint = Color(0xFFEC4899), onClick = onVoice) }
        }
    }
}

@Composable
private fun QuickAction(
    icon: Int,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .tapNoRipple(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 6.dp)
    ) {
        IconBadge(icon = icon, tint = tint, size = 50.dp, iconSize = 24.dp, shape = RoundedCornerShape(18.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun WaterCard(glasses: Int, onAdd: () -> Unit, onRemove: () -> Unit) {
    val target = 8
    val progress = (glasses.toFloat() / target).coerceIn(0f, 1f)
    AppCard(modifier = Modifier.fillMaxWidth(), spacing = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon = R.drawable.ic_water_drop_fill, tint = WaterColor)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text("Air minum", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "$glasses dari $target gelas · ${formatVolume(glasses * 250)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            StepButton(
                icon = R.drawable.ic_remove,
                contentDescription = "Kurangi satu gelas",
                onClick = onRemove,
                enabled = glasses > 0
            )
            Spacer(modifier = Modifier.width(8.dp))
            StepButton(
                icon = R.drawable.ic_add,
                contentDescription = "Tambah satu gelas",
                onClick = onAdd
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(50))
                .background(WaterColor.copy(alpha = 0.15f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(WaterColor)
            )
        }
    }
}

@Composable
private fun ProfilePromptCard(onOpenProfile: () -> Unit) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.secondaryContainer,
        border = false,
        spacing = 12.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(
                icon = R.drawable.ic_person,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                container = Color.White.copy(alpha = 0.5f)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
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
            .padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(type.iconRes),
            contentDescription = null,
            tint = type.accent,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .tapNoRipple(onClick = onAdd)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
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


