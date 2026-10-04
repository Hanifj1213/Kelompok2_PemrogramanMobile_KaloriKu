package com.hajun.kaloriku.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hajun.kaloriku.R
import com.hajun.kaloriku.data.FoodItem
import com.hajun.kaloriku.data.MealEntry
import com.hajun.kaloriku.data.MealType
import com.hajun.kaloriku.ui.theme.CarbsColor
import com.hajun.kaloriku.ui.theme.FatColor
import com.hajun.kaloriku.ui.theme.ProteinColor
import com.hajun.kaloriku.util.formatDecimal
import com.hajun.kaloriku.util.formatTime
import com.hajun.kaloriku.util.formatWhole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KaloriTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        navigationIcon = {
            if (onBack != null) {
                FilledTonalIconButton(
                    onClick = onBack,
                    modifier = Modifier.padding(start = 8.dp, end = 4.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Icon(painter = painterResource(R.drawable.ic_arrow_back), contentDescription = "Kembali")
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.surface
        ),
            )
}

/** Kartu datar dengan garis tipis, dipakai sebagai wadah utama di semua layar. */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    color: Color = MaterialTheme.colorScheme.surface,
    border: Boolean = true,
    shape: Shape = MaterialTheme.shapes.large,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    spacing: Dp = 0.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val stroke = if (border) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null
    val inner: @Composable () -> Unit = {
        Column(
            modifier = Modifier.padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(spacing),
            content = content
        )
    }
    if (onClick != null) {
        Surface(onClick = onClick, modifier = modifier, shape = shape, color = color, border = stroke, content = inner)
    } else {
        Surface(modifier = modifier, shape = shape, color = color, border = stroke, content = inner)
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        if (action != null && onAction != null) {
            TextButton(onClick = onAction, contentPadding = PaddingValues(horizontal = 10.dp)) {
                Text(action, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/** Ikon di dalam kotak berwarna lembut. */
@Composable
fun IconBadge(
    @DrawableRes icon: Int,
    tint: Color,
    modifier: Modifier = Modifier,
    container: Color = tint.copy(alpha = 0.14f),
    size: Dp = 44.dp,
    iconSize: Dp = 22.dp,
    shape: Shape = RoundedCornerShape(14.dp)
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(container),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int? = null,
    enabled: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        enabled = enabled,
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(containerColor = containerColor, contentColor = contentColor),
        contentPadding = PaddingValues(horizontal = 20.dp)
    ) {
        if (icon != null) {
            Icon(painter = painterResource(icon), contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(text = text, style = MaterialTheme.typography.titleSmall, maxLines = 1)
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int? = null,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        enabled = enabled,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        contentPadding = PaddingValues(horizontal = 20.dp)
    ) {
        if (icon != null) {
            Icon(painter = painterResource(icon), contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(text = text, style = MaterialTheme.typography.titleSmall, maxLines = 1)
    }
}

/** Tampilan kosong yang ramah, dengan ikon besar dan penjelasan singkat. */
@Composable
fun EmptyState(
    @DrawableRes icon: Int,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IconBadge(
            icon = icon,
            tint = MaterialTheme.colorScheme.primary,
            container = MaterialTheme.colorScheme.primaryContainer,
            size = 72.dp,
            iconSize = 34.dp,
            shape = CircleShape
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (action != null) {
            Spacer(modifier = Modifier.height(4.dp))
            action()
        }
    }
}

/** Pil kecil berwarna, misalnya "P 12 g". */
@Composable
fun TagPill(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text = text, style = MaterialTheme.typography.labelMedium, color = color)
    }
}

/** Baris tiga pil makro: protein, karbohidrat, lemak. */
@Composable
fun MacroPills(item: FoodItem, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        TagPill("P ${item.proteinG.formatDecimal()} g", ProteinColor)
        TagPill("K ${item.carbsG.formatDecimal()} g", CarbsColor)
        TagPill("L ${item.fatG.formatDecimal()} g", FatColor)
    }
}

/** Kotak kecil berisi satu angka statistik. */
@Composable
fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
    color: Color = MaterialTheme.colorScheme.surfaceContainer
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(color)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Text(text = value, style = MaterialTheme.typography.titleLarge, maxLines = 1)
            if (unit != null) {
                Text(
                    text = " $unit",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }
        }
    }
}

@get:DrawableRes
val MealType.iconRes: Int
    get() = when (this) {
        MealType.SARAPAN -> R.drawable.ic_wb_sunny
        MealType.MAKAN_SIANG -> R.drawable.ic_lunch_dining
        MealType.MAKAN_MALAM -> R.drawable.ic_nightlight
        MealType.CAMILAN -> R.drawable.ic_cookie
    }

val MealType.accent: Color
    get() = when (this) {
        MealType.SARAPAN -> Color(0xFFF59E0B)
        MealType.MAKAN_SIANG -> Color(0xFF16A34A)
        MealType.MAKAN_MALAM -> Color(0xFF6366F1)
        MealType.CAMILAN -> Color(0xFFEC4899)
    }

/** Kartu satu catatan makan. Ketuk untuk melihat rincian per makanan. */
@Composable
fun MealEntryCard(entry: MealEntry, onDelete: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    AppCard(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        onClick = { expanded = !expanded },
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon = entry.mealType.iconRes, tint = entry.mealType.accent)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    text = "${entry.mealType.label} · ${entry.timestamp.formatTime()}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = entry.items.joinToString { it.name },
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = if (expanded) 4 else 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = entry.totalCalories.formatWhole(), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "kkal",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                entry.items.forEach { item ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${item.grams.formatWhole()} g · P ${item.proteinG.formatDecimal()} · " +
                                    "K ${item.carbsG.formatDecimal()} · L ${item.fatG.formatDecimal()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(text = "${item.calories.formatWhole()} kkal", style = MaterialTheme.typography.labelLarge)
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(
                        onClick = { confirmDelete = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_delete),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Hapus catatan")
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        ConfirmDeleteDialog(
            title = "Hapus catatan?",
            message = "Catatan ${entry.mealType.label.lowercase()} ini akan dihapus permanen.",
            onConfirm = {
                confirmDelete = false
                onDelete()
            },
            onDismiss = { confirmDelete = false }
        )
    }
}

@Composable
fun ConfirmDeleteDialog(title: String, message: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(painterResource(R.drawable.ic_delete), contentDescription = null, tint = MaterialTheme.colorScheme.error)
        },
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text("Hapus") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
        shape = RoundedCornerShape(28.dp)
    )
}

/** Tombol bulat kecil untuk menambah atau mengurangi angka. */
@Composable
fun StepButton(@DrawableRes icon: Int, contentDescription: String, onClick: () -> Unit, enabled: Boolean = true) {
    FilledTonalIconButton(
        onClick = onClick,
        enabled = enabled,
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Icon(painterResource(icon), contentDescription = contentDescription, modifier = Modifier.size(20.dp))
    }
}