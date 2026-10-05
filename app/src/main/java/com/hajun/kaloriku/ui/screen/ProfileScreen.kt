package com.hajun.kaloriku.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hajun.kaloriku.R
import com.hajun.kaloriku.data.CalorieCalculator
import com.hajun.kaloriku.data.Profile
import com.hajun.kaloriku.ui.MainViewModel
import com.hajun.kaloriku.ui.components.AppCard
import com.hajun.kaloriku.ui.components.BmiCard
import com.hajun.kaloriku.ui.components.IconBadge
import com.hajun.kaloriku.ui.theme.Spacing
import com.hajun.kaloriku.util.bmiCategory
import com.hajun.kaloriku.util.calculateBmi
import com.hajun.kaloriku.util.formatDecimal
import com.hajun.kaloriku.util.formatWhole

/** Profil pengguna: ringkasan target, status gizi IMT, data tubuh, dan pengingat makan. */
@Composable
fun ProfileScreen(
    viewModel: MainViewModel,
    onOpenGoals: () -> Unit,
    onOpenEditProfile: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val existing by viewModel.profile.collectAsStateWithLifecycle()
    val goals by viewModel.dailyGoals.collectAsStateWithLifecycle()

    val remindersOn by viewModel.remindersEnabled.collectAsStateWithLifecycle()
    val reminderTimes by viewModel.reminderTimes.collectAsStateWithLifecycle()
    var notificationDenied by rememberSaveable { mutableStateOf(false) }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notificationDenied = !granted
    }

    val currentBmi = calculateBmi(weightKg = existing?.weightKg, heightCm = existing?.heightCm)

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

        item { TargetSummaryCard(profile = existing, bmi = currentBmi, goalsCalories = goals.calories, onOpenGoals = onOpenGoals) }

        item { BmiCard(bmi = currentBmi) }

        item {
            AppCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = onOpenEditProfile,
                contentPadding = PaddingValues(Spacing.lg)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(
                        icon = R.drawable.ic_person,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        container = MaterialTheme.colorScheme.primaryContainer
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = Spacing.md)
                    ) {
                        Text("Data tubuh & aktivitas", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = if (existing != null) {
                                "${existing?.gender?.label} · ${existing?.ageYears} tahun · ${existing?.weightKg?.formatDecimal()} kg · ${existing?.heightCm?.formatDecimal()} cm"
                            } else {
                                "Belum diisi · Ketuk untuk menghitung target tubuhmu"
                            },
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
            AppCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = onOpenGoals,
                contentPadding = PaddingValues(Spacing.lg)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(
                        icon = R.drawable.ic_flag_fill,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        container = MaterialTheme.colorScheme.primaryContainer
                    )
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
                            text = "Notifikasi sarapan (${reminderTimes.formatBreakfast()}), siang (${reminderTimes.formatLunch()}), dan malam (${reminderTimes.formatDinner()}).",
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
                if (remindersOn) {
                    Text(
                        text = "Ketuk jadwal untuk mengubah jam makan:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = Spacing.xs)
                    )
                    ReminderTimeRow(
                        icon = R.drawable.ic_wb_sunny,
                        label = "Sarapan",
                        time = reminderTimes.formatBreakfast(),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                            android.app.TimePickerDialog(
                                context,
                                { _, h, m ->
                                    viewModel.setReminderTimes(reminderTimes.copy(breakfastHour = h, breakfastMinute = m))
                                },
                                reminderTimes.breakfastHour,
                                reminderTimes.breakfastMinute,
                                true
                            ).show()
                        }
                    )
                    ReminderTimeRow(
                        icon = R.drawable.ic_lunch_dining,
                        label = "Makan siang",
                        time = reminderTimes.formatLunch(),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                            android.app.TimePickerDialog(
                                context,
                                { _, h, m ->
                                    viewModel.setReminderTimes(reminderTimes.copy(lunchHour = h, lunchMinute = m))
                                },
                                reminderTimes.lunchHour,
                                reminderTimes.lunchMinute,
                                true
                            ).show()
                        }
                    )
                    ReminderTimeRow(
                        icon = R.drawable.ic_nightlight,
                        label = "Makan malam",
                        time = reminderTimes.formatDinner(),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                            android.app.TimePickerDialog(
                                context,
                                { _, h, m ->
                                    viewModel.setReminderTimes(reminderTimes.copy(dinnerHour = h, dinnerMinute = m))
                                },
                                reminderTimes.dinnerHour,
                                reminderTimes.dinnerMinute,
                                true
                            ).show()
                        }
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
private fun TargetSummaryCard(profile: Profile?, bmi: Double?, goalsCalories: Int, onOpenGoals: () -> Unit) {
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
        if (bmi != null) {
            val category = bmiCategory(bmi)
            val label = category?.label ?: "Normal"
            Text(
                text = "IMT ${bmi.formatDecimal()} · $label",
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

@Composable
private fun ReminderTimeRow(
    @DrawableRes icon: Int,
    label: String,
    time: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .weight(1f)
                .padding(start = Spacing.md)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            Text(
                text = time,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
