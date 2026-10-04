package com.hajun.kaloriku.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.key
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.text.input.ImeAction
import com.hajun.kaloriku.util.parseDecimalInput
import com.hajun.kaloriku.util.toPlainInput
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hajun.kaloriku.R
import com.hajun.kaloriku.data.AnalysisResult
import com.hajun.kaloriku.data.MealType
import com.hajun.kaloriku.data.sumNutrition
import com.hajun.kaloriku.ui.AnalysisState
import com.hajun.kaloriku.ui.EditableItem
import com.hajun.kaloriku.ui.MainViewModel
import com.hajun.kaloriku.ui.charts.CalorieRing
import com.hajun.kaloriku.ui.charts.MacroRingsRow
import com.hajun.kaloriku.ui.charts.StackedMacroBar
import com.hajun.kaloriku.ui.components.AppCard
import com.hajun.kaloriku.ui.components.EmptyState
import com.hajun.kaloriku.ui.components.IconBadge
import com.hajun.kaloriku.ui.components.KaloriTopBar
import com.hajun.kaloriku.ui.components.MacroPills
import com.hajun.kaloriku.ui.components.PrimaryButton
import com.hajun.kaloriku.ui.components.SecondaryButton
import com.hajun.kaloriku.ui.components.accent
import com.hajun.kaloriku.ui.components.iconRes
import com.hajun.kaloriku.ui.theme.Green600
import com.hajun.kaloriku.ui.theme.Green700
import com.hajun.kaloriku.ui.theme.Orange500
import com.hajun.kaloriku.util.formatDecimal
import com.hajun.kaloriku.util.formatWhole
import com.hajun.kaloriku.ui.components.tapNoRipple

private val portionOptions = listOf(0.5 to "½", 1.0 to "1", 1.5 to "1½", 2.0 to "2")

/**
 * Hasil analisis foto, barcode, atau suara. Semua angka bisa disunting
 * sebelum disimpan ke catatan harian.
 */
@Composable
fun ResultScreen(
    viewModel: MainViewModel,
    onAddAnother: () -> Unit,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val state = viewModel.analysisState
    val photo = viewModel.photo
    val items = viewModel.editableItems
    val totals = items.map { it.current }.sumNutrition()
    var invalidGrams by rememberSaveable { mutableStateOf(emptyList<Long>()) }
    val canSave = items.isNotEmpty() && items.none { it.id in invalidGrams }

    Column(modifier = Modifier.fillMaxSize().imePadding()) {
        KaloriTopBar(
            title = "Hasil analisis",
            subtitle = if (state is AnalysisState.Success) {
                "${items.size} makanan · ${totals.calories.formatWhole()} kkal"
            } else {
                null
            },
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (photo != null) {
                Image(
                    bitmap = photo.asImageBitmap(),
                    contentDescription = "Foto makanan yang dianalisis",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(MaterialTheme.shapes.large)
                )
            }

            when (state) {
                AnalysisState.Idle -> EmptyState(
                    icon = R.drawable.ic_no_food,
                    title = "Belum ada bahan",
                    message = "Pilih foto, cari makanan, scan barcode, atau catat lewat suara."
                )

                AnalysisState.Loading -> LoadingContent()

                is AnalysisState.Error -> ErrorContent(
                    message = state.message,
                    onRetry = if (viewModel.canRetry) viewModel::retry else null,
                    onBack = onBack
                )

                is AnalysisState.Success -> SuccessContent(
                    result = state.result,
                    items = items,
                    mealType = viewModel.mealType,
                    onMealTypeChange = { viewModel.mealType = it },
                    onPortionChange = viewModel::setPortion,
                    onGramsChange = viewModel::setGrams,
                    onRemove = viewModel::removeItem,
                    onAddAnother = onAddAnother,
                    onBack = onBack,
                    onValidityChange = { id, valid ->
                        invalidGrams = if (valid) invalidGrams - id else (invalidGrams + id).distinct()
                    }
                )
            }
        }

        if (state is AnalysisState.Success && items.isNotEmpty()) {
            Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 12.dp) {
                Column(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Total",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${totals.calories.formatWhole()} kkal",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                    PrimaryButton(
                        text = "Simpan ke catatan",
                        onClick = { if (canSave && viewModel.saveMeal()) onSaved() },
                        enabled = canSave,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    AppCard(modifier = Modifier.fillMaxWidth(), spacing = 16.dp) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(
                    Brush.linearGradient(listOf(Green600.copy(alpha = 0.18f), Green700.copy(alpha = 0.08f)))
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularProgressIndicator(color = Green600)
                Text("AI sedang membaca fotomu…", style = MaterialTheme.typography.titleSmall)
            }
        }
        Text(
            text = "Biasanya perlu beberapa detik. Pastikan foto terlihat jelas dan tidak gelap.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: (() -> Unit)?, onBack: () -> Unit) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer,
        border = false,
        spacing = 14.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(
                icon = R.drawable.ic_error,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                container = Color.White.copy(alpha = 0.45f)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text("Analisis gagal", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
        if (onRetry != null) {
            PrimaryButton(text = "Coba lagi", onClick = onRetry, modifier = Modifier.fillMaxWidth())
        }
        SecondaryButton(text = "Pilih foto lain", onClick = onBack, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun SuccessContent(
    result: AnalysisResult,
    items: List<EditableItem>,
    mealType: MealType,
    onMealTypeChange: (MealType) -> Unit,
    onPortionChange: (Int, Double) -> Unit,
    onGramsChange: (Int, Double) -> Unit,
    onRemove: (Int) -> Unit,
    onAddAnother: () -> Unit,
    onBack: () -> Unit,
    onValidityChange: (Long, Boolean) -> Unit
) {
    if (!result.isFood) {
        AppCard(modifier = Modifier.fillMaxWidth(), spacing = 12.dp) {
            EmptyState(
                icon = R.drawable.ic_no_food,
                title = "Makanan tidak terdeteksi",
                message = result.note.ifBlank { "Pastikan foto berisi makanan dan terlihat jelas." }
            )
        }
        SecondaryButton(text = "Pilih foto lain", onClick = onBack, modifier = Modifier.fillMaxWidth())
        return
    }

    val totals = items.map { it.current }.sumNutrition()
    ResultSummaryCard(totals = totals)

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "Rincian makanan",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onAddAnother) {
            Icon(painterResource(R.drawable.ic_add), contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Tambah")
        }
    }

    if (items.isEmpty()) {
        AppCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                "Semua item sudah dihapus. Tambahkan makanan lain atau pilih foto baru.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }

    items.forEachIndexed { index, item ->
        key(item.id) {
            FoodItemCard(
                item = item,
                onPortionChange = { onPortionChange(index, it) },
                onGramsChange = { onGramsChange(index, it) },
                onRemove = { onRemove(index) },
                onValidityChange = { onValidityChange(item.id, it) }
            )
        }
    }

    if (result.note.isNotBlank()) {
        AppCard(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            spacing = 8.dp
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.ic_lightbulb),
                    contentDescription = null,
                    tint = Orange500,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sumber dan catatan", style = MaterialTheme.typography.titleSmall)
            }
            Text(text = result.note, style = MaterialTheme.typography.bodyMedium)
        }
    }

    Text("Waktu makan", style = MaterialTheme.typography.titleMedium)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MealType.entries.forEach { type ->
            FilterChip(
                selected = type == mealType,
                onClick = { onMealTypeChange(type) },
                shape = RoundedCornerShape(50),
                label = { Text(type.label) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(type.iconRes),
                        contentDescription = null,
                        tint = if (type == mealType) MaterialTheme.colorScheme.primary else type.accent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
        }
    }

    Text(
        text = if (result.note.contains("TKPI") || result.note.contains("Open Food Facts")) {
            "Data gizi mengikuti sumber yang tercantum. Sesuaikan berat dengan porsi yang benar-benar dimakan."
        } else {
            "Nilai gizi dari AI adalah perkiraan. Sesuaikan berat atau porsi bila perlu."
        },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/** Kartu ringkasan: cincin kalori besar dan cincin capaian gizi. */
@Composable
private fun ResultSummaryCard(totals: com.hajun.kaloriku.data.FoodItem) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        border = false,
        color = MaterialTheme.colorScheme.surface,
        contentPadding = PaddingValues(18.dp),
        spacing = 16.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            CalorieRing(
                progress = 1f,
                size = 150.dp,
                strokeWidth = 13.dp,
                color = Green600,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = totals.calories.formatWhole(),
                        style = MaterialTheme.typography.displaySmall
                    )
                    Text(
                        text = "kkal",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = "${totals.grams.formatWhole()} g total berat sajian",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        StackedMacroBar(item = totals)
        MacrosFromItem(totals)
    }
}

@Composable
private fun MacrosFromItem(item: com.hajun.kaloriku.data.FoodItem) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        MacroColumn("Protein", item.proteinG, Color(0xFF3B82F6))
        MacroColumn("Karbohidrat", item.carbsG, Color(0xFFF59E0B))
        MacroColumn("Lemak", item.fatG, Color(0xFFA855F7))
    }
}

@Composable
private fun MacroColumn(label: String, grams: Double, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text = "${grams.formatDecimal()} g", style = MaterialTheme.typography.titleMedium, color = color)
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Kartu satu makanan yang beratnya bisa disunting langsung. */
@Composable
private fun FoodItemCard(
    item: EditableItem,
    onPortionChange: (Double) -> Unit,
    onGramsChange: (Double) -> Unit,
    onRemove: () -> Unit,
    onValidityChange: (Boolean) -> Unit
) {
    val food = item.current
    var gramsText by rememberSaveable(item.id) { mutableStateOf(food.grams.toPlainInput()) }
    var syncedGrams by rememberSaveable(item.id) { mutableStateOf(food.grams) }
    val grams = parseDecimalInput(gramsText, 1.0..3000.0)
    LaunchedEffect(food.grams) {
        if (food.grams != syncedGrams) {
            gramsText = food.grams.toPlainInput()
            syncedGrams = food.grams
        }
    }
    LaunchedEffect(gramsText) { onValidityChange(grams != null) }

    AppCard(modifier = Modifier.fillMaxWidth(), spacing = 12.dp) {
        Row(verticalAlignment = Alignment.Top) {
            IconBadge(
                icon = R.drawable.ic_restaurant,
                tint = MaterialTheme.colorScheme.primary,
                size = 40.dp,
                iconSize = 20.dp
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp, end = 8.dp)
            ) {
                Text(text = food.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${food.grams.formatWhole()} g · ${food.calories.formatWhole()} kkal",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = food.calories.formatWhole(),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "kkal",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        StackedMacroBar(item = food)
        MacroPills(item = food)

        OutlinedTextField(
            value = gramsText,
            onValueChange = { input ->
                gramsText = input
                val parsed = parseDecimalInput(input, 1.0..3000.0)
                onValidityChange(parsed != null)
                if (parsed != null) {
                    syncedGrams = parsed
                    onGramsChange(parsed)
                }
            },
            label = { Text("Berat (g)") },
            isError = grams == null,
            supportingText = if (grams == null) ({ Text("Isi berat 1–3000 g sebelum menyimpan.") }) else null,
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth()
        )
        Text("Porsi cepat", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            portionOptions.forEach { (factor, label) ->
                val portionGrams = item.base.grams * factor
                FilterChip(
                    selected = grams != null && item.portion == factor,
                    onClick = {
                        gramsText = portionGrams.toPlainInput()
                        syncedGrams = portionGrams
                        onValidityChange(true)
                        onPortionChange(factor)
                    },
                    enabled = portionGrams.isFinite() && portionGrams in 1.0..3000.0,
                    label = { Text("$label porsi") },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.heightIn(min = 48.dp)
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(
                onClick = onRemove,
                colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(painterResource(R.drawable.ic_delete), contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Hapus item")
            }
        }
    }
}

