package com.hajun.kaloriku.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
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
import com.hajun.kaloriku.ui.theme.Green600
import com.hajun.kaloriku.ui.theme.Orange500
import com.hajun.kaloriku.util.formatDecimal
import com.hajun.kaloriku.util.formatWhole
import androidx.compose.foundation.layout.statusBarsPadding

private val AGE_RANGE = 10..100
private val WEIGHT_RANGE = 20.0..300.0
private val HEIGHT_RANGE = 100.0..250.0

/** Profil pengguna, status gizi (IMT), dan pengaturan pengingat. */
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
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Profil", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        text = "Target kalori dan pengaturanmu",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconBadge(
                    icon = R.drawable.ic_tune,
                    tint = MaterialTheme.colorScheme.primary,
                    container = MaterialTheme.colorScheme.primaryContainer,
                    size = 42.dp,
                    iconSize = 20.dp
                )
            }
        }

        item { ProfileHeader(profile = profile) }

        if (profile != null) {
            item { TargetCard(profile = profile) }
        }

        item {
            AppCard(modifier = Modifier.fillMaxWidth(), spacing = 14.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(icon = R.drawable.ic_person, tint = Green600)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 12.dp)
                    ) {
                        Text("Data tubuh", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Dipakai untuk menghitung target kalorimu",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Gender.entries.forEach { option ->
                        FilterChip(
                            selected = option == gender,
                            onClick = { gender = option },
                            shape = RoundedCornerShape(50),
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
            AppCard(modifier = Modifier.fillMaxWidth(), spacing = 10.dp) {
                Text("Tingkat aktivitas", style = MaterialTheme.typography.titleMedium)
                Column(modifier = Modifier.selectableGroup()) {
                    ActivityLevel.entries.forEach { option ->
                        ActivityRow(
                            option = option,
                            selected = option == activity,
                            onSelect = { activity = option }
                        )
                    }
                }
            }
        }

        item {
            AppCard(modifier = Modifier.fillMaxWidth(), spacing = 10.dp) {
                Text("Tujuan", style = MaterialTheme.typography.titleMedium)
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(Goal.entries) { option ->
                        FilterChip(
                            selected = option == goal,
                            onClick = { goal = option },
                            shape = RoundedCornerShape(50),
                            label = { Text(option.label) }
                        )
                    }
                }
            }
        }

        if (profile != null) {
            item { BmiCard(profile = profile) }
        }

        item {
            AppCard(modifier = Modifier.fillMaxWidth(), spacing = 12.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(icon = R.drawable.ic_notifications, tint = Orange500)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp)
                    ) {
                        Text("Pengingat makan", style = MaterialTheme.typography.titleMedium)
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
                Text(
                    "Pengaturan disimpan langsung. Waktu notifikasi bisa bergeser mengikuti pengaturan baterai Android.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            ShortcutTile(
                icon = R.drawable.ic_flag_fill,
                label = "Target gizi",
                detail = "${goals.calories.formatWhole()} kkal",
                tint = Green600,
                onClick = onOpenGoals,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            PrimaryButton(
                text = "Simpan profil",
                onClick = {
                    if (profile != null) {
                        viewModel.saveProfile(profile)
                        Toast.makeText(context, "Profil berhasil disimpan. Target kalori diperbarui.", Toast.LENGTH_SHORT).show()
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
private fun ProfileHeader(profile: Profile?) {
    AppCard(modifier = Modifier.fillMaxWidth(), spacing = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_person_fill),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(30.dp)
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp)
            ) {
                Text(
                    text = if (profile == null) "Profil belum lengkap" else "Profil kamu",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = if (profile == null) {
                        "Isi data tubuh untuk menghitung target kalori."
                    } else {
                        "${profile.gender.label} · ${profile.ageYears} tahun · ${profile.activity.label.lowercase()}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (profile != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(
                    label = "Berat",
                    value = profile.weightKg.formatDecimal(),
                    unit = "kg",
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    label = "Tinggi",
                    value = profile.heightCm.formatDecimal(),
                    unit = "cm",
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    label = "Tujuan",
                    value = profile.goal.label.split(" ").first(),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun TargetCard(profile: Profile) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        border = false,
        color = MaterialTheme.colorScheme.primaryContainer,
        spacing = 10.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(
                icon = R.drawable.ic_flag_fill,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                container = Color.White.copy(alpha = 0.5f)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(
                    text = "Target harian",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = CalorieCalculator.dailyTarget(profile).formatWhole(),
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = " kkal",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(bottom = 5.dp)
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(
                label = "BMR (istirahat)",
                value = CalorieCalculator.bmr(profile).formatWhole(),
                unit = "kkal",
                color = Color.White.copy(alpha = 0.55f),
                modifier = Modifier.weight(1f)
            )
            StatTile(
                label = "TDEE (aktivitas)",
                value = CalorieCalculator.tdee(profile).formatWhole(),
                unit = "kkal",
                color = Color.White.copy(alpha = 0.55f),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/** Kartu status gizi berdasarkan IMT (indeks massa tubuh). */
@Composable
private fun BmiCard(profile: Profile) {
    val weight = profile.weightKg
    val heightM = profile.heightCm / 100.0
    if (heightM <= 0) return
    val bmi = weight / (heightM * heightM)

    val (label, advice, color) = when {
        bmi < 18.5 -> Triple(
            "Kurang berat",
            "Tambahkan asupan bergizi dan konsultasikan dengan ahli gizi.",
            Orange500
        )
        bmi < 23.0 -> Triple(
            "Normal",
            "Berat badanmu dalam rentang sehat. Pertahankan pola makan.",
            Green600
        )
        bmi < 25.0 -> Triple("Sedikit berlebih", "Jaga porsi dan tingkatkan aktivitas fisik.", Orange500)
        bmi < 30.0 -> Triple(
            "Berlebih",
            "Kurangi camilan manis dan perbanyak sayur serta jalan kaki.",
            Orange500
        )
        else -> Triple("Obesitas", "Sebaiknya konsultasikan dengan dokter atau ahli gizi.", Color(0xFFE5484D))
    }

    AppCard(
        modifier = Modifier.fillMaxWidth(),
        color = color.copy(alpha = 0.10f),
        border = false,
        spacing = 8.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(
                icon = R.drawable.ic_health_and_safety,
                tint = color,
                container = Color.White.copy(alpha = 0.6f),
                size = 44.dp,
                iconSize = 22.dp
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(
                    text = "Status gizi (IMT)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(text = "${bmi.formatDecimal()} · $label", style = MaterialTheme.typography.titleMedium)
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

@Composable
private fun ActivityRow(option: ActivityLevel, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .selectable(selected = selected, onClick = onSelect, role = Role.RadioButton)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(
                text = option.label,
                style = MaterialTheme.typography.bodyLarge,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
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
private fun ShortcutTile(
    icon: Int,
    label: String,
    detail: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppCard(
        modifier = modifier,
        onClick = onClick,
        contentPadding = PaddingValues(14.dp),
        spacing = 8.dp
    ) {
        IconBadge(icon = icon, tint = tint, size = 40.dp, iconSize = 20.dp)
        Column {
            Text(label, style = MaterialTheme.typography.titleSmall)
            Text(
                text = detail,
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
        shape = RoundedCornerShape(14.dp),
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