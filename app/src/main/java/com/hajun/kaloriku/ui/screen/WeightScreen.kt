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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hajun.kaloriku.R
import com.hajun.kaloriku.data.Stats
import com.hajun.kaloriku.data.WeightEntry
import com.hajun.kaloriku.ui.MainViewModel
import com.hajun.kaloriku.ui.charts.WeightLineChart
import com.hajun.kaloriku.ui.components.AppCard
import com.hajun.kaloriku.ui.components.ConfirmDeleteDialog
import com.hajun.kaloriku.ui.components.EmptyState
import com.hajun.kaloriku.ui.components.IconBadge
import com.hajun.kaloriku.ui.components.KaloriTopBar
import com.hajun.kaloriku.ui.components.PrimaryButton
import com.hajun.kaloriku.ui.components.StatTile
import com.hajun.kaloriku.ui.theme.Green600
import com.hajun.kaloriku.ui.theme.Orange500
import com.hajun.kaloriku.util.formatDayTitle
import com.hajun.kaloriku.util.formatDecimal
import java.time.LocalDate

/** Catatan berat badan beserta grafik progres dan perubahan dari waktu ke waktu. */
@Composable
fun WeightScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val weights by viewModel.weightEntries.collectAsStateWithLifecycle()
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    var showDialog by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<WeightEntry?>(null) }

    val sorted = remember(weights) { weights.sortedBy { it.date } }
    val latest = sorted.lastOrNull()
    val change = remember(weights) { Stats.weightChange(weights) }

    Column(modifier = Modifier.fillMaxSize()) {
        KaloriTopBar(title = "Berat badan", subtitle = "Pantau progresmu", onBack = onBack)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                AppCard(modifier = Modifier.fillMaxWidth(), spacing = 14.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(
                            icon = R.drawable.ic_monitor_weight_fill,
                            tint = WeightAccent,
                            container = WeightAccent.copy(alpha = 0.14f),
                            size = 48.dp,
                            iconSize = 24.dp
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 14.dp)
                        ) {
                            Text(
                                text = "Berat saat ini",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = latest?.weightKg?.formatDecimal() ?: "—",
                                    style = MaterialTheme.typography.displaySmall
                                )
                                Text(
                                    text = " kg",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                            }
                        }
                    }

                    change?.let {
                        val sign = if (it > 0) "+" else ""
                        val color = if (it > 0) Orange500 else Green600
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(
                                    if (it > 0) R.drawable.ic_trending_up else R.drawable.ic_trending_down
                                ),
                                contentDescription = null,
                                tint = color,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = " $sign${it.formatDecimal()} kg sejak catatan pertama",
                                style = MaterialTheme.typography.bodyMedium,
                                color = color
                            )
                        }
                    }

                    if (profile != null) {
                        val heightM = profile!!.heightCm / 100.0
                        if (heightM > 0 && latest != null) {
                            val bmi = latest!!.weightKg / (heightM * heightM)
                            Text(
                                text = "IMT ${bmi.formatDecimal()} berdasarkan tinggi ${profile!!.heightCm.formatDecimal()} cm",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                PrimaryButton(
                    text = if (viewModel.hasWeightToday()) "Perbarui berat hari ini" else "Catat berat hari ini",
                    onClick = { showDialog = true },
                    icon = R.drawable.ic_add,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (sorted.size >= 2) {
                item {
                    AppCard(modifier = Modifier.fillMaxWidth(), spacing = 10.dp) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Grafik progres",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${sorted.size} catatan",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        WeightLineChart(entries = sorted, lineColor = WeightAccent)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            StatTile(
                                label = "Terendah",
                                value = sorted.minOf { it.weightKg }.formatDecimal(),
                                unit = "kg",
                                modifier = Modifier.weight(1f)
                            )
                            StatTile(
                                label = "Tertinggi",
                                value = sorted.maxOf { it.weightKg }.formatDecimal(),
                                unit = "kg",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Riwayat penimbangan",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            if (sorted.isEmpty()) {
                item {
                    EmptyState(
                        icon = R.drawable.ic_monitor_weight,
                        title = "Belum ada catatan",
                        message = "Timbang berat badan secara berkala, misalnya sekali seminggu."
                    )
                }
            } else {
                items(sorted.reversed(), key = { it.date.toString() }) { entry ->
                    WeightRow(
                        entry = entry,
                        previous = sorted.getOrNull(sorted.indexOf(entry) - 1),
                        onDelete = { pendingDelete = entry }
                    )
                }
            }

            item {
                Text(
                    text = "Catatan di tanggal yang sama akan digantikan oleh angka terbaru.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showDialog) {
        WeightDialog(
            initial = latest?.weightKg,
            onDismiss = { showDialog = false },
            onConfirm = { weight ->
                viewModel.saveWeight(LocalDate.now(), weight)
                showDialog = false
            }
        )
    }

    val toDelete = pendingDelete
    if (toDelete != null) {
        ConfirmDeleteDialog(
            title = "Hapus catatan?",
            message = "Catatan ${toDelete.date.formatDayTitle()} akan dihapus.",
            onConfirm = {
                viewModel.deleteWeight(toDelete.date)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null }
        )
    }
}

@Composable
private fun WeightRow(entry: WeightEntry, previous: WeightEntry?, onDelete: () -> Unit) {
    val diff = previous?.let { entry.weightKg - it.weightKg }
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 6.dp, bottom = 12.dp)
    ) {
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
                Text(text = entry.date.formatDayTitle(), style = MaterialTheme.typography.titleSmall)
                Text(
                    text = "${entry.weightKg.formatDecimal()} kg" +
                        if (diff != null) {
                            " · ${if (diff > 0) "+" else ""}${diff.formatDecimal()} kg dari sebelumnya"
                        } else {
                            ""
                        },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(
                onClick = onDelete,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(painterResource(R.drawable.ic_delete), contentDescription = "Hapus", modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun WeightDialog(initial: Double?, onDismiss: () -> Unit, onConfirm: (Double) -> Unit) {
    var text by remember { mutableStateOf(initial?.formatPlain().orEmpty()) }
    val value = text.replace(',', '.').toDoubleOrNull()
    val valid = value != null && value in 20.0..300.0

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                painter = painterResource(R.drawable.ic_monitor_weight),
                contentDescription = null,
                tint = WeightAccent
            )
        },
        title = { Text("Catat berat badan") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Berat badan (kg)") },
                    isError = text.isNotBlank() && !valid,
                    supportingText = if (text.isNotBlank() && !valid) {
                        { Text("Isi antara 20–300 kg") }
                    } else null,
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Timbang di waktu yang sama setiap kali, misalnya pagi sebelum makan.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = { if (valid) onConfirm(value) }, enabled = valid) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
        shape = RoundedCornerShape(28.dp)
    )
}

private val WeightAccent = androidx.compose.ui.graphics.Color(0xFF6366F1)

private fun Double.formatPlain(): String =
    if (this % 1.0 == 0.0) toLong().toString() else toString()