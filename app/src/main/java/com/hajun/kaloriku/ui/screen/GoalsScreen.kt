package com.hajun.kaloriku.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import com.hajun.kaloriku.util.parseIntegerInput
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hajun.kaloriku.R
import com.hajun.kaloriku.data.DailyGoals
import com.hajun.kaloriku.data.FoodItem
import com.hajun.kaloriku.ui.MainViewModel
import com.hajun.kaloriku.ui.charts.StackedMacroBar
import com.hajun.kaloriku.ui.components.AppCard
import com.hajun.kaloriku.ui.components.IconBadge
import com.hajun.kaloriku.ui.components.KaloriTopBar
import com.hajun.kaloriku.ui.components.PrimaryButton
import com.hajun.kaloriku.ui.components.SecondaryButton
import com.hajun.kaloriku.ui.theme.CarbsColor
import com.hajun.kaloriku.ui.theme.FatColor
import com.hajun.kaloriku.ui.theme.ProteinColor
import com.hajun.kaloriku.ui.theme.Spacing
import com.hajun.kaloriku.util.formatDecimal
import com.hajun.kaloriku.util.formatWhole

/** Mengatur target gizi harian, dengan saran otomatis dari target kalori. */
@Composable
fun GoalsScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val goals by viewModel.dailyGoals.collectAsStateWithLifecycle()

    val profile by viewModel.profile.collectAsStateWithLifecycle()
    var protein by rememberSaveable { mutableStateOf(goals.proteinG.toString()) }
    var carbs by rememberSaveable { mutableStateOf(goals.carbsG.toString()) }
    var fat by rememberSaveable { mutableStateOf(goals.fatG.toString()) }
    val proteinValue = parseIntegerInput(protein, 0..1000)
    val carbsValue = parseIntegerInput(carbs, 0..2000)
    val fatValue = parseIntegerInput(fat, 0..1000)
    val valid = proteinValue != null && carbsValue != null && fatValue != null

    val suggestion = remember(goals.calories) { DailyGoals.fromCalories(goals.calories) }
    val proteinKcal = (proteinValue ?: 0) * 4
    val carbsKcal = (carbsValue ?: 0) * 4
    val fatKcal = (fatValue ?: 0) * 9
    val totalKcal = proteinKcal + carbsKcal + fatKcal
    val preview = FoodItem(
        name = "Target",
        grams = ((proteinValue ?: 0) + (carbsValue ?: 0) + (fatValue ?: 0)).toDouble(),
        calories = totalKcal.toDouble(),
        proteinG = (proteinValue ?: 0).toDouble(),
        carbsG = (carbsValue ?: 0).toDouble(),
        fatG = (fatValue ?: 0).toDouble()
    )

    Column(modifier = Modifier.fillMaxSize()) {
        KaloriTopBar(
            title = "Target gizi",
            subtitle = "Kalori ${goals.calories.formatWhole()} kkal",
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .imePadding(),
            contentPadding = PaddingValues(start = Spacing.screen, end = Spacing.screen, bottom = Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            item {
                AppCard(modifier = Modifier.fillMaxWidth(), spacing = Spacing.md) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(icon = R.drawable.ic_flag_fill, tint = MaterialTheme.colorScheme.primary)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = Spacing.md)
                        ) {
                            Text("Target kalori aktif", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = if (profile == null) {
                                    "${goals.calories.formatWhole()} kkal · target umum, isi profil untuk menyesuaikan"
                                } else {
                                    "${goals.calories.formatWhole()} kkal dari profil (rumus Mifflin-St Jeor)"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(
                        text = "Atur pecahan gizinya di bawah. Saran otomatis memakai 15% protein, " +
                            "55% karbohidrat, dan 30% lemak dari target kalori.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                AppCard(modifier = Modifier.fillMaxWidth(), spacing = Spacing.md) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = totalKcal.formatWhole(),
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = " kkal dari makro",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = Spacing.xs, bottom = Spacing.md)
                        )
                    }
                    StackedMacroBar(item = preview)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        MacroShare("Protein", proteinKcal, totalKcal, ProteinColor)
                        MacroShare("Karbohidrat", carbsKcal, totalKcal, CarbsColor)
                        MacroShare("Lemak", fatKcal, totalKcal, FatColor)
                    }
                    Text(
                        text = if (!valid) {
                            "Lengkapi angka yang valid untuk melihat ringkasan target."
                        } else if (kotlin.math.abs(totalKcal - goals.calories) <= goals.calories * 0.1) {
                            "Sudah mendekati target kalori harianmu."
                        } else {
                            "Selisih ${kotlin.math.abs(totalKcal - goals.calories).formatWhole()} kkal dari target kalori."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                GoalSlider(
                    label = "Protein",
                    value = protein,
                    range = 0..1000,
                    hint = "Saran ${suggestion.proteinG} g",
                    color = ProteinColor,
                    icon = R.drawable.ic_egg_alt,
                    onValueChange = { protein = it }
                )
            }
            item {
                GoalSlider(
                    label = "Karbohidrat",
                    value = carbs,
                    range = 0..2000,
                    hint = "Saran ${suggestion.carbsG} g",
                    color = CarbsColor,
                    icon = R.drawable.ic_bakery_dining,
                    onValueChange = { carbs = it }
                )
            }
            item {
                GoalSlider(
                    label = "Lemak",
                    value = fat,
                    range = 0..1000,
                    hint = "Saran ${suggestion.fatG} g",
                    color = FatColor,
                    icon = R.drawable.ic_restaurant,
                    onValueChange = { fat = it }
                )
            }

            item {
                SecondaryButton(
                    text = "Pakai saran otomatis",
                    onClick = {
                        protein = suggestion.proteinG.toString()
                        carbs = suggestion.carbsG.toString()
                        fat = suggestion.fatG.toString()
                    },
                    icon = R.drawable.ic_auto_awesome,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                PrimaryButton(
                    text = "Simpan target",
                    onClick = {
                        if (proteinValue != null && carbsValue != null && fatValue != null) {
                            viewModel.saveGoals(DailyGoals(goals.calories, proteinValue, carbsValue, fatValue))
                            onBack()
                        }
                    },
                    enabled = valid,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun MacroShare(label: String, kcal: Int, total: Int, color: Color) {
    val percent = if (total <= 0) 0 else (kcal * 100 / total)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "$percent%", style = MaterialTheme.typography.titleMedium, color = color)
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun GoalSlider(
    label: String,
    value: String,
    range: IntRange,
    hint: String,
    color: Color,
    icon: Int,
    onValueChange: (String) -> Unit
) {
    val parsed = parseIntegerInput(value, range)
    AppCard(modifier = Modifier.fillMaxWidth(), spacing = Spacing.sm) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon = icon, tint = color, size = 40.dp, iconSize = 20.dp)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = Spacing.md)
            ) {
                Text(label, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                isError = parsed == null,
                modifier = Modifier.width(104.dp).semantics { contentDescription = "Target $label (gram)" },
                suffix = { Text("g") },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                )
            )
        }
        if (parsed == null) {
            Text("Isi angka ${range.first}–${range.last} gram.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        Slider(
            value = (parsed ?: range.first).toFloat(),
            onValueChange = { onValueChange(it.toInt().toString()) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            colors = androidx.compose.material3.SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color,
                inactiveTrackColor = color.copy(alpha = 0.18f)
            )
        )
    }
}