package com.hajun.kaloriku.ui.screen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hajun.kaloriku.data.ActivityLevel
import com.hajun.kaloriku.data.Gender
import com.hajun.kaloriku.data.Goal
import com.hajun.kaloriku.data.Profile
import com.hajun.kaloriku.ui.MainViewModel
import com.hajun.kaloriku.ui.components.AppCard
import com.hajun.kaloriku.ui.components.KaloriTopBar
import com.hajun.kaloriku.ui.components.PrimaryButton
import com.hajun.kaloriku.ui.theme.Spacing

private val AGE_RANGE = 10..100
private val WEIGHT_RANGE = 20.0..300.0
private val HEIGHT_RANGE = 100.0..250.0

/** Layar formulir khusus untuk mengubah data tubuh, aktivitas, dan tujuan. */
@Composable
fun EditProfileScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val existing by viewModel.profile.collectAsStateWithLifecycle()

    var gender by rememberSaveable { mutableStateOf(existing?.gender ?: Gender.PRIA) }
    var age by rememberSaveable { mutableStateOf(existing?.ageYears?.toString().orEmpty()) }
    var weight by rememberSaveable { mutableStateOf(existing?.weightKg?.toInputText().orEmpty()) }
    var height by rememberSaveable { mutableStateOf(existing?.heightCm?.toInputText().orEmpty()) }
    var activity by rememberSaveable { mutableStateOf(existing?.activity ?: ActivityLevel.SEDANG) }
    var goal by rememberSaveable { mutableStateOf(existing?.goal ?: Goal.JAGA) }

    val ageValue = age.trim().toIntOrNull()?.takeIf { it in AGE_RANGE }
    val weightValue = weight.toDecimalOrNull()?.takeIf { it in WEIGHT_RANGE }
    val heightValue = height.toDecimalOrNull()?.takeIf { it in HEIGHT_RANGE }
    val profile = if (ageValue != null && weightValue != null && heightValue != null) {
        Profile(gender, ageValue, weightValue, heightValue, activity, goal)
    } else {
        null
    }

    Column(modifier = Modifier.fillMaxSize()) {
        KaloriTopBar(
            title = "Data tubuh & aktivitas",
            subtitle = "Menyesuaikan target kalori dengan tubuhmu",
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
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
                AppCard(modifier = Modifier.fillMaxWidth(), spacing = Spacing.md) {
                    Text("Data tubuh", style = MaterialTheme.typography.titleMedium)
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
                AppCard(modifier = Modifier.fillMaxWidth(), spacing = Spacing.md) {
                    Text("Aktivitas & tujuan", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Tingkat aktivitas",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                PrimaryButton(
                    text = "Simpan",
                    onClick = {
                        if (profile != null) {
                            haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                            viewModel.saveProfile(profile)
                            Toast.makeText(context, "Profil tersimpan. Target kalori diperbarui.", Toast.LENGTH_SHORT).show()
                            onBack()
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
