package com.hajun.kaloriku.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hajun.kaloriku.R
import com.hajun.kaloriku.data.ActivityLevel
import com.hajun.kaloriku.data.CalorieCalculator
import com.hajun.kaloriku.data.Gender
import com.hajun.kaloriku.data.Goal
import com.hajun.kaloriku.data.Profile
import com.hajun.kaloriku.ui.MainViewModel
import com.hajun.kaloriku.ui.components.AppCard
import com.hajun.kaloriku.ui.components.IconBadge
import com.hajun.kaloriku.ui.components.PrimaryButton
import com.hajun.kaloriku.ui.components.StatTile
import com.hajun.kaloriku.ui.theme.Spacing
import com.hajun.kaloriku.util.formatDecimal
import com.hajun.kaloriku.util.formatWhole

private val AGE_RANGE = 10..100
private val WEIGHT_RANGE = 20.0..300.0
private val HEIGHT_RANGE = 100.0..250.0

/** Profil pengguna: kartu ringkas target + IMT di atas, lalu daftar pengaturan berkelompok. */
@Composable
fun ProfileScreen(
    viewModel: MainViewModel,
    onOpenGoals: () -> Unit
) {
    val context = LocalContext.current
    val existing by viewModel.profile.collectAsStateWithLifecycle()
    val goals by viewModel.dailyGoals.collectAsStateWithLifecycle()

    var gender by rememberSaveable { mutableStateOf(existing?.gender ?: Gender.PRIA) }
    var age by rememberSaveable { mutableStateOf(existing?.ageYears?.toString().orEmpty()) }
    var weight by rememberSaveable { mutableStateOf(existing?.weightKg?.toInputText().orEmpty()) }
    var height by rememberSaveable { mutableStateOf(existing?.heightCm?.toInputText().orEmpty()) }
    var activity by rememberSaveable { mutableStateOf(existing?.activity ?: ActivityLevel.SEDANG) }
    var goal by rememberSaveable { mutableStateOf(existing?.goal ?: Goal.JAGA) }

    val remindersOn by viewModel.remindersEnabled.collectAsStateWithLifecycle()
    var notificationDenied by rememberSaveable { mutableStateOf(false) }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notificationDenied = !granted
    }

    val ageValue = age.trim().toIntOrNull()?.takeIf { it in AGE_RANGE }
    val weightValue = weight.toDecimalOrNull()?.takeIf { it in WEIGHT_RANGE }
    val heightValue = height.toDecimalOrNull()?.takeIf { it in HEIGHT_RANGE }
    val profile = if (ageValue != null && weightValue != null && heightValue != null) {
        Profile(gender, ageValue, weightValue, heightValue, activity, goal)
    } else {
        null
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
        item {
            Column {
                Text("Profil", style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = "Target kalori dan pengaturanmu",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item { TargetSummaryCard(profile = profile, goalsCalories = goals.calories, onOpenGoals = onOpenGoals) }

        if (profile != null) {
            item { BmiCard(profile = profile) }
        }

        item {
            SettingsGroup(title = "Data tubuh") {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Gender.entries.forEach { option ->
                        FilterChip(
                            selected = option == gender,
                            onClick = { gender = option },
                            label = { Text(option.label) }
                        )
                    }
                }
                NumberField(
                    label = "Usia (tahun)",
                    value = age,
                    onValueChange = { age = it },
                    isError = age.isNotBlank() && ageValue == null,
                    errorText = "Isi usia antara ${AGE_RANGE.first}–${AGE_RANGE.last} tahun",
                    decimal = false
                )
                NumberField(
                    label = "Berat badan (kg)",
                    value = weight,
                    onValueChange = { weight = it },
                    isError = weight.isNotBlank() && weightValue == null,
                    errorText = "Isi berat antara 20–300 kg"
                )
                NumberField(
                    label = "Tinggi badan (cm)",
                    value = height,
                    onValueChange = { height = it },
                    isError = height.isNotBlank() && heightValue == null,
                    errorText = "Isi tinggi antara 100–250 cm"
                )
            }
        }

        item {
            SettingsGroup(title = "Aktivitas & tujuan") {
                Text("Tingkat aktivitas", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Column(modifier = Modifier.selectableGroup()) {
                    ActivityLevel.entries.forEach { option ->
                        ActivityRow(
                            option = option,
                            selected = option == activity,
                            onSelect = { activity = option }
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Spacing.sm),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        items(Goal.entries) { option ->
                            FilterChip(
                                selected = option == goal,
                                onClick = { goal = option },
                                label = { Text(option.label) }
                            )
                        }
                    }
                }
            }
        }

        item {
            AppCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = onOpenGoals,
                contentPadding = PaddingValues(Spacing.lg)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(icon = R.drawable.ic_flag_fill, tint = MaterialTheme.colorScheme.onPrimaryContainer, container = MaterialTheme.colorScheme.primaryContainer)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = Spacing.md)
                    ) {
                        Text("Target gizi", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "${goals.calories.formatWhole()} kkal · P ${goals.proteinG} K ${goals.carbsG} L ${goals.fatG} g",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        painter = painterResource(R.drawable.ic_chevron_right),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }

        item {
            SettingsGroup(title = "Pengingat makan") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Pengingat makan", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "Notifikasi pagi (08.00), siang (13.00), dan malam (19.00).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = remindersOn,
                        onCheckedChange = { enabled ->
                            viewModel.setRemindersEnabled(enabled)
                            if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                            ) {
                                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        },
                        modifier = Modifier.semantics { contentDescription = "Pengingat makan" }
                    )
                }
                if (notificationDenied && remindersOn) {
                    Text(
                        "Pengingat aktif, tetapi notifikasi belum diizinkan. Aktifkan izin Notifikasi di pengaturan perangkat.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            PrimaryButton(
                text = "Simpan",
                onClick = {
                    if (profile != null) {
                        viewModel.saveProfile(profile)
                        Toast.makeText(context, "Profil tersimpan. Target kalori diperbarui.", Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = profile != null,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Text(
                text = "Target dihitung dengan rumus Mifflin-St Jeor. Ini perkiraan umum, bukan saran medis.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    AppCard(modifier = Modifier.fillMaxWidth(), spacing = Spacing.md) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        content()
    }
}

/** Kartu ringkas: target kalori harian dan IMT. */
@Composable
private fun TargetSummaryCard(profile: Profile?, goalsCalories: Int, onOpenGoals: () -> Unit) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
        spacing = Spacing.sm
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(
                icon = R.drawable.ic_flag_fill,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                container = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = Spacing.md)
            ) {
                Text(
                    text = "Target harian",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = (profile?.let { CalorieCalculator.dailyTarget(it) } ?: goalsCalories).formatWhole(),
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = " kkal",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(start = Spacing.xs, bottom = Spacing.xs)
                    )
                }
            }
        }
        if (profile != null) {
            val bmi = bmiOf(profile)
            Text(
                text = "IMT ${bmi.formatDecimal()} · ${bmiLabel(bmi)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        } else {
            Text(
                text = "Isi data tubuh untuk menghitung targetmu.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        TextButton(onClick = onOpenGoals) {
            Text("Atur target gizi", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

/** Kartu status gizi berdasarkan IMT (indeks massa tubuh). */
@Composable
private fun BmiCard(profile: Profile) {
    val bmi = bmiOf(profile)
    val advice = when {
        bmi < 18.5 -> "Tambahkan asupan bergizi dan konsultasikan dengan ahli gizi."
        bmi < 23.0 -> "Berat badanmu dalam rentang sehat. Pertahankan pola makan."
        bmi < 25.0 -> "Jaga porsi dan tingkatkan aktivitas fisik."
        bmi < 30.0 -> "Kurangi camilan manis dan perbanyak sayur serta jalan kaki."
        else -> "Sebaiknya konsultasikan dengan dokter atau ahli gizi."
    }

    AppCard(modifier = Modifier.fillMaxWidth(), spacing = Spacing.sm) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(
                icon = R.drawable.ic_health_and_safety,
                tint = MaterialTheme.colorScheme.primary,
                size = 44.dp,
                iconSize = 22.dp
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = Spacing.md)
            ) {
                Text(
                    text = "Status gizi (IMT)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(text = "${bmi.formatDecimal()} · ${bmiLabel(bmi)}", style = MaterialTheme.typography.titleMedium)
            }
        }
        Text(text = advice, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = "Ambang batas IMT orang Indonesia: kurang <18,5 · normal 18,5–22,9 · " +
                "berlebih 23–24,9 · obesitas ≥25.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun bmiOf(profile: Profile): Double {
    val heightM = profile.heightCm / 100.0
    return if (heightM <= 0) 0.0 else profile.weightKg / (heightM * heightM)
}

private fun bmiLabel(bmi: Double): String = when {
    bmi < 18.5 -> "Kurang berat"
    bmi < 23.0 -> "Normal"
    bmi < 25.0 -> "Sedikit berlebih"
    bmi < 30.0 -> "Berlebih"
    else -> "Obesitas"
}

@Composable
private fun ActivityRow(option: ActivityLevel, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .selectable(selected = selected, onClick = onSelect, role = Role.RadioButton)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
        Column(modifier = Modifier.padding(start = Spacing.sm)) {
            Text(
                text = option.label,
                style = MaterialTheme.typography.bodyLarge,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = option.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NumberField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean,
    errorText: String,
    decimal: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = isError,
        supportingText = if (isError) ({ Text(errorText) }) else null,
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number,
            imeAction = ImeAction.Next
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

private fun String.toDecimalOrNull(): Double? = trim().replace(',', '.').toDoubleOrNull()

private fun Double.toInputText(): String =
    if (this % 1.0 == 0.0) toLong().toString() else toString()
