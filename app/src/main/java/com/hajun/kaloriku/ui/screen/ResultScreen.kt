package com.hajun.kaloriku.ui.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hajun.kaloriku.R
import com.hajun.kaloriku.data.AnalysisResult
import com.hajun.kaloriku.data.DailyGoals
import com.hajun.kaloriku.data.FoodItem
import com.hajun.kaloriku.data.MealType
import com.hajun.kaloriku.data.sumNutrition
import com.hajun.kaloriku.ui.AnalysisState
import com.hajun.kaloriku.ui.EditableItem
import com.hajun.kaloriku.ui.MainViewModel
import com.hajun.kaloriku.ui.charts.StackedMacroBar
import com.hajun.kaloriku.ui.components.AnimatedWholeNumber
import com.hajun.kaloriku.ui.components.AppCard
import com.hajun.kaloriku.ui.components.EmptyState
import com.hajun.kaloriku.ui.components.IconBadge
import com.hajun.kaloriku.ui.components.KaloriTopBar
import com.hajun.kaloriku.ui.components.MacroPills
import com.hajun.kaloriku.ui.components.PrimaryButton
import com.hajun.kaloriku.ui.components.SecondaryButton
import com.hajun.kaloriku.ui.components.iconRes
import com.hajun.kaloriku.ui.theme.CarbsColor
import com.hajun.kaloriku.ui.theme.FatColor
import com.hajun.kaloriku.ui.theme.ProteinColor
import com.hajun.kaloriku.ui.theme.Spacing
import com.hajun.kaloriku.util.formatDecimal
import com.hajun.kaloriku.util.formatWhole
import com.hajun.kaloriku.util.parseDecimalInput
import com.hajun.kaloriku.util.toPlainInput

private val portionOptions = listOf(0.5 to "½", 1.0 to "1", 1.5 to "1½", 2.0 to "2")

/**
 * Hasil analisis foto, barcode, atau input manual. Semua angka bisa disunting
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
                .padding(horizontal = Spacing.screen)
                .padding(bottom = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            AnimatedContent(
                targetState = state,
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(140)) },
                label = "hasil"
            ) { current ->
                when (current) {
                    AnalysisState.Idle -> EmptyState(
                        icon = R.drawable.ic_no_food,
                        message = "Belum ada bahan. Pilih foto, cari makanan, atau scan barcode.",
                        action = { PrimaryButton(text = "Kembali", onClick = onBack) }
                    )

                    AnalysisState.Loading -> LoadingContent()

                    is AnalysisState.Error -> ErrorContent(
                        message = current.message,
                        onRetry = if (viewModel.canRetry) viewModel::retry else null,
                        onBack = onBack
                    )

                    is AnalysisState.Success -> SuccessContent(
                        result = current.result,
                        items = items,
                        photo = photo,
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
        }

        if (state is AnalysisState.Success && items.isNotEmpty()) {
            SaveBar(calories = totals.calories, enabled = canSave, onSave = { if (canSave && viewModel.saveMeal()) onSaved() })
        }
    }
}

/** Bar simpan yang menempel di bawah layar. */
@Composable
private fun SaveBar(calories: Double, enabled: Boolean, onSave: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Total",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                AnimatedWholeNumber(
                    value = calories,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            PrimaryButton(
                text = "Simpan",
                onClick = onSave,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun LoadingContent() {
    AppCard(modifier = Modifier.fillMaxWidth(), spacing = Spacing.lg) {
        ShimmerPlaceholder()
        Text("AI sedang membaca fotomu…", style = MaterialTheme.typography.titleSmall)
        Text(
            text = "Biasanya perlu beberapa detik. Pastikan foto terlihat jelas dan tidak gelap.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Kotak shimmer sederhana, tanpa persentase palsu. */
@Composable
private fun ShimmerPlaceholder() {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerAlpha"
    )
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = alpha))
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(14.dp)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = alpha))
        )
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: (() -> Unit)?, onBack: () -> Unit) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer,
        spacing = Spacing.md
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(
                icon = R.drawable.ic_error,
                tint = MaterialTheme.colorScheme.error,
                container = MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = Spacing.md)
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
    photo: android.graphics.Bitmap?,
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
        AppCard(modifier = Modifier.fillMaxWidth(), spacing = Spacing.md) {
            EmptyState(
                icon = R.drawable.ic_no_food,
                message = result.note.ifBlank { "Pastikan foto berisi makanan dan terlihat jelas." }
            )
        }
        SecondaryButton(text = "Pilih foto lain", onClick = onBack, modifier = Modifier.fillMaxWidth())
        return
    }

    val totals = items.map { it.current }.sumNutrition()
    ResultSummaryCard(totals = totals, photo = photo)

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "Rincian makanan",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onAddAnother) {
            Icon(painterResource(R.drawable.ic_add), contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(Spacing.xs))
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
        AppCard(modifier = Modifier.fillMaxWidth(), spacing = Spacing.sm) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.ic_lightbulb),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(Spacing.sm))
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
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        MealType.entries.forEach { type ->
            FilterChip(
                selected = type == mealType,
                onClick = { onMealTypeChange(type) },
                label = { Text(type.label) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(type.iconRes),
                        contentDescription = null,
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

/** Foto beradius 24 dp dengan scrim bawah berisi total kalori. */
@Composable
private fun PhotoWithScrim(photo: android.graphics.Bitmap?, calories: Double) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(MaterialTheme.shapes.large)
    ) {
        if (photo != null) {
            Image(
                bitmap = photo.asImageBitmap(),
                contentDescription = "Foto makanan yang dianalisis",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerHigh))
        }
        // Satu-satunya gradasi yang diizinkan: scrim gelap supaya teks terbaca.
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f))
                    )
                )
                .padding(Spacing.lg),
            contentAlignment = Alignment.BottomStart
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                AnimatedWholeNumber(
                    value = calories,
                    style = MaterialTheme.typography.displaySmall,
                    color = Color.White
                )
                Text(
                    text = " kkal",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    modifier = Modifier.padding(start = Spacing.xs, bottom = Spacing.xs)
                )
            }
        }
    }
}

/** Kartu ringkasan: foto (kalau ada) dengan scrim kalori, lalu bar makro. */
@Composable
private fun ResultSummaryCard(totals: FoodItem, photo: android.graphics.Bitmap?) {
    AppCard(modifier = Modifier.fillMaxWidth(), spacing = Spacing.md) {
        if (photo != null) {
            PhotoWithScrim(photo = photo, calories = totals.calories)
        } else {
            Row(verticalAlignment = Alignment.Bottom) {
                AnimatedWholeNumber(
                    value = totals.calories,
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = " kkal",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = Spacing.xs, bottom = Spacing.xs)
                )
            }
        }
        Text(
            text = "${totals.grams.formatWhole()} g total berat sajian",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        StackedMacroBar(item = totals)
        MacrosFromItem(totals)
    }
}

@Composable
private fun MacrosFromItem(item: FoodItem) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        MacroColumn("Protein", item.proteinG, ProteinColor)
        MacroColumn("Karbohidrat", item.carbsG, CarbsColor)
        MacroColumn("Lemak", item.fatG, FatColor)
    }
}

@Composable
private fun MacroColumn(label: String, grams: Double, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(text = "${grams.formatDecimal()} g", style = MaterialTheme.typography.titleMedium, color = color)
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Kartu satu makanan yang beratnya bisa disunting langsung. */
@OptIn(ExperimentalMaterial3Api::class)
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

    AppCard(modifier = Modifier.fillMaxWidth(), spacing = Spacing.md) {
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
                    .padding(start = Spacing.md, end = Spacing.sm)
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
            shape = MaterialTheme.shapes.medium,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth()
        )

        Text("Porsi cepat", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            portionOptions.forEachIndexed { index, (factor, label) ->
                val portionGrams = item.base.grams * factor
                SegmentedButton(
                    selected = grams != null && item.portion == factor,
                    onClick = {
                        gramsText = portionGrams.toPlainInput()
                        syncedGrams = portionGrams
                        onValidityChange(true)
                        onPortionChange(factor)
                    },
                    enabled = portionGrams.isFinite() && portionGrams in 1.0..3000.0,
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = portionOptions.size),
                    label = { Text(label) }
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(
                onClick = onRemove,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(painterResource(R.drawable.ic_delete), contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(Spacing.xs))
                Text("Hapus")
            }
        }
    }
}
