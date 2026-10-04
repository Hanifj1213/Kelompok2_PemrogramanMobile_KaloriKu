package com.hajun.kaloriku.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.toRoute
import com.hajun.kaloriku.R
import com.hajun.kaloriku.data.FoodItem
import com.hajun.kaloriku.data.MealEntry
import com.hajun.kaloriku.data.findEntry
import com.hajun.kaloriku.data.sumNutrition
import com.hajun.kaloriku.ui.MainViewModel
import com.hajun.kaloriku.ui.components.AnimatedWholeNumber
import com.hajun.kaloriku.ui.components.ConfirmDeleteDialog
import com.hajun.kaloriku.ui.components.EmptyState
import com.hajun.kaloriku.ui.components.IconBadge
import com.hajun.kaloriku.ui.components.MacroPills
import com.hajun.kaloriku.ui.components.PrimaryButton
import com.hajun.kaloriku.ui.components.iconRes
import com.hajun.kaloriku.ui.navigation.MealDetailRoute
import com.hajun.kaloriku.ui.theme.Spacing
import com.hajun.kaloriku.util.formatDecimal
import com.hajun.kaloriku.util.formatRelativeDay
import com.hajun.kaloriku.util.formatWhole
import com.hajun.kaloriku.util.toLocalDate

/**
 * Detail satu catatan makan: waktu, total kalori dan makro, daftar item,
 * serta tombol hapus. Dibuka dengan mengetuk kartu catatan di Beranda atau Riwayat.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealDetailScreen(
    viewModel: MainViewModel,
    backStackEntry: NavBackStackEntry,
    onBack: () -> Unit
) {
    val route = backStackEntry.toRoute<MealDetailRoute>()
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val entry = remember(entries, route.entryId) { entries.findEntry(route.entryId) }
    var confirmDelete by remember { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = { Text(if (entry == null) "Catatan" else entry.mealType.label) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = "Kembali"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (entry == null) {
                MissingEntry(onBack = onBack)
            } else {
                EntryContent(entry = entry, onDelete = { confirmDelete = true })
            }
        }
    }

    if (confirmDelete && entry != null) {
        ConfirmDeleteDialog(
            title = "Hapus catatan?",
            message = "Catatan ${entry.mealType.label.lowercase()} ini akan dihapus permanen.",
            onConfirm = {
                confirmDelete = false
                viewModel.deleteEntry(entry.id)
                onBack()
            },
            onDismiss = { confirmDelete = false }
        )
    }
}

@Composable
private fun MissingEntry(onBack: () -> Unit) {
    EmptyState(
        icon = R.drawable.ic_no_food,
        message = "Catatan ini sudah tidak ada.",
        modifier = Modifier.fillMaxSize(),
        action = { PrimaryButton(text = "Kembali", onClick = onBack) }
    )
}

@Composable
private fun EntryContent(entry: MealEntry, onDelete: () -> Unit) {
    val totals = remember(entry) { entry.items.sumNutrition() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Spacing.screen,
            end = Spacing.screen,
            top = Spacing.sm,
            bottom = Spacing.xl
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        item {
            Text(
                text = "${entry.mealType.label} · ${entry.timestamp.toLocalDate().formatRelativeDay()}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item { TotalsCard(totals = totals) }

        item { Text("Rincian makanan", style = MaterialTheme.typography.titleMedium) }

        items(entry.items, key = { it.name + it.grams + it.calories }) { item ->
            ItemRow(item = item, modifier = Modifier.animateItem())
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_delete),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Text("Hapus catatan", modifier = Modifier.padding(start = Spacing.sm))
                }
            }
        }
    }
}

@Composable
private fun TotalsCard(totals: FoodItem) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Text("Total", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        Text(
            text = "${totals.grams.formatWhole()} g total berat sajian",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        MacroPills(item = totals)
    }
}

@Composable
private fun ItemRow(item: FoodItem, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon = R.drawable.ic_restaurant, tint = MaterialTheme.colorScheme.primary)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = Spacing.md)
            ) {
                Text(item.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    text = "${item.grams.formatWhole()} g",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(text = "${item.calories.formatWhole()} kkal", style = MaterialTheme.typography.titleMedium)
        }
        Text(
            text = "P ${item.proteinG.formatDecimal()} g · K ${item.carbsG.formatDecimal()} g · " +
                "L ${item.fatG.formatDecimal()} g",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
