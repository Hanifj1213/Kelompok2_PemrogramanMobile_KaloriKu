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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.navigationBarsPadding
import com.hajun.kaloriku.util.parseDecimalInput
import com.hajun.kaloriku.util.toPlainInput
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hajun.kaloriku.R
import com.hajun.kaloriku.data.Food
import com.hajun.kaloriku.ui.MainViewModel
import com.hajun.kaloriku.ui.charts.StackedMacroBar
import com.hajun.kaloriku.ui.components.AppCard
import com.hajun.kaloriku.ui.components.EmptyState
import com.hajun.kaloriku.ui.components.IconBadge
import com.hajun.kaloriku.ui.components.KaloriTopBar
import com.hajun.kaloriku.ui.components.MacroPills
import com.hajun.kaloriku.ui.components.PrimaryButton
import com.hajun.kaloriku.ui.components.StepButton
import com.hajun.kaloriku.util.formatDecimal
import com.hajun.kaloriku.util.formatWhole

private const val MAX_RESULTS = 80

/** Pencarian makanan dari basis data TKPI, dipakai kalau AI tidak bisa diandalkan. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodSearchScreen(
    viewModel: MainViewModel,
    onDone: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var query by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingFood by remember { mutableStateOf<Food?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val categories = remember { viewModel.foodCategories }
    val results = remember(query, selectedCategory) {
        val base = if (query.isBlank()) {
            com.hajun.kaloriku.data.FoodDatabase.foods
        } else {
            com.hajun.kaloriku.data.FoodDatabase.search(query, limit = Int.MAX_VALUE)
        }
        val filtered = if (selectedCategory == null) base else base.filter { it.category == selectedCategory }
        filtered.take(MAX_RESULTS)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        KaloriTopBar(title = "Cari makanan", subtitle = "Data TKPI Kemenkes per 100 g", onBack = onBack)

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Nasi, ayam, tempe…") },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_search),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(painterResource(R.drawable.ic_close), contentDescription = "Hapus pencarian")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                        shape = RoundedCornerShape(50),
                        label = { Text("Semua") }
                    )
                }
                items(categories) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = {
                            selectedCategory = if (selectedCategory == category) null else category
                        },
                        shape = RoundedCornerShape(50),
                        label = { Text(category) }
                    )
                }
            }
            Text(
                text = "${results.size} makanan ditemukan",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f).navigationBarsPadding().imePadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (results.isEmpty()) {
                item {
                    EmptyState(
                        icon = R.drawable.ic_no_food,
                        title = "Tidak ditemukan",
                        message = "Coba kata kunci lain, atau pakai foto dan suara untuk mencatat."
                    )
                }
            }
            items(results, key = { it.id }) { food ->
                FoodRow(food = food, onClick = { pendingFood = food })
            }
        }
    }

    val food = pendingFood
    if (food != null) {
        ModalBottomSheet(
            onDismissRequest = { pendingFood = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            PortionSheetContent(
                food = food,
                onConfirm = { grams ->
                    viewModel.addManualFood(food, grams)
                    pendingFood = null
                    Toast.makeText(context, "${food.name} ditambahkan.", Toast.LENGTH_SHORT).show()
                    onDone()
                }
            )
        }
    }
}

@Composable
private fun FoodRow(food: Food, onClick: () -> Unit) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(categoryColor(food.category).copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = food.name.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = categoryColor(food.category)
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    text = food.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    text = "${food.category} · P ${food.proteinPer100g.formatDecimal()} · " +
                        "K ${food.carbsPer100g.formatDecimal()} · L ${food.fatPer100g.formatDecimal()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = food.caloriesPer100g.formatWhole(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "kkal/100g",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Isi lembar pilihan porsi: takaran rumah tangga, berat manual, dan pratinjau gizi. */
@Composable
private fun PortionSheetContent(food: Food, onConfirm: (Double) -> Unit) {
    val defaultGrams = food.portions.firstOrNull()?.grams ?: 100.0
    var manualText by rememberSaveable(food.id) { mutableStateOf(defaultGrams.toPlainInput()) }
    val grams = parseDecimalInput(manualText, 1.0..2000.0)
    val preview = food.toFoodItem(grams ?: 0.0)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column {
            Text(food.name, style = MaterialTheme.typography.headlineSmall)
            Text(
                text = "${food.category} · ${food.caloriesPer100g.formatWhole()} kkal per 100 g",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        AppCard(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            spacing = 10.dp
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = if (grams != null) preview.calories.formatWhole() else "—",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = " kkal",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
            StackedMacroBar(item = preview)
            MacroPills(item = preview)
        }

        if (food.portions.isNotEmpty()) {
            Text("Takaran umum", style = MaterialTheme.typography.titleSmall)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(food.portions) { portion ->
                    val selected = grams == portion.grams
                    FilterChip(
                        selected = selected,
                        onClick = {
                            manualText = portion.grams.toPlainInput()
                        },
                        shape = RoundedCornerShape(50),
                        label = { Text("${portion.label} · ${portion.grams.toInt()} g") }
                    )
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            StepButton(
                icon = R.drawable.ic_remove,
                contentDescription = "Kurangi 10 gram",
                onClick = {
                    val next = ((grams ?: defaultGrams) - 10).coerceAtLeast(1.0)
                    manualText = next.toPlainInput()
                }
            )
            OutlinedTextField(
                value = manualText,
                onValueChange = { manualText = it },
                isError = grams == null,
                supportingText = if (grams == null) ({ Text("Isi berat 1–2000 g") }) else null,
                label = { Text("Berat (g)") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done
                ),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp)
            )
            StepButton(
                icon = R.drawable.ic_add,
                contentDescription = "Tambah 10 gram",
                onClick = {
                    val next = ((grams ?: defaultGrams) + 10).coerceAtMost(2000.0)
                    manualText = next.toPlainInput()
                }
            )
        }

        PrimaryButton(
            text = if (grams != null) "Tambahkan · ${preview.calories.formatWhole()} kkal" else "Isi berat porsi dulu",
            onClick = { grams?.let(onConfirm) },
            enabled = grams != null,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private fun categoryColor(category: String): Color = when (category) {
    "Makanan pokok" -> Color(0xFFF59E0B)
    "Lauk hewani" -> Color(0xFFEF4444)
    "Lauk nabati" -> Color(0xFF16A34A)
    "Sayuran" -> Color(0xFF22C55E)
    "Buah" -> Color(0xFFEC4899)
    "Minuman" -> Color(0xFF0EA5E9)
    "Snack" -> Color(0xFFA855F7)
    "Lemak" -> Color(0xFFF97316)
    "Cereal" -> Color(0xFF8B5CF6)
    else -> Color(0xFF64748B)
}