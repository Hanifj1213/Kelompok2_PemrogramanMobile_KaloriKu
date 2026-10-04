package com.hajun.kaloriku.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hajun.kaloriku.R
import com.hajun.kaloriku.ui.theme.Green600
import com.hajun.kaloriku.ui.theme.Green700
import com.hajun.kaloriku.ui.theme.Orange500

/** Satu tab di bilah bawah. */
data class BottomTab(
    val route: String,
    val label: String,
    @param:DrawableRes val icon: Int,
    @param:DrawableRes val selectedIcon: Int
)

val bottomTabs = listOf(
    BottomTab("home", "Beranda", R.drawable.ic_home, R.drawable.ic_home_fill),
    BottomTab("history", "Riwayat", R.drawable.ic_calendar_month, R.drawable.ic_calendar_month_fill),
    BottomTab("weekly", "Laporan", R.drawable.ic_bar_chart, R.drawable.ic_bar_chart_fill),
    BottomTab("profile", "Profil", R.drawable.ic_person, R.drawable.ic_person_fill)
)

/**
 * Bilah navigasi bawah dengan tombol bulat "Catat" di tengah.
 * Empat tab utama dibagi dua di kiri dan dua di kanan tombol tersebut.
 */
@Composable
fun KaloriBottomBar(
    currentRoute: String?,
    onSelect: (String) -> Unit,
    onAdd: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 12.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            bottomTabs.take(2).forEach { tab ->
                BottomBarItem(tab = tab, selected = currentRoute == tab.route, onClick = { onSelect(tab.route) })
            }
            CenterAddButton(onClick = onAdd)
            bottomTabs.drop(2).forEach { tab ->
                BottomBarItem(tab = tab, selected = currentRoute == tab.route, onClick = { onSelect(tab.route) })
            }
        }
    }
}

@Composable
private fun RowScope.BottomBarItem(tab: BottomTab, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(16.dp))
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(
                    if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                )
                .padding(horizontal = 14.dp, vertical = 4.dp)
        ) {
            Icon(
                painter = painterResource(if (selected) tab.selectedIcon else tab.icon),
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        }
        Text(
            text = tab.label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun RowScope.CenterAddButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .weight(1f)
            .padding(bottom = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(CircleShape)
                .background(
                    androidx.compose.ui.graphics.Brush.linearGradient(
                        colors = listOf(Green600, Green700)
                    )
                )
                .tapNoRipple(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_add),
                contentDescription = "Catat makanan",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

/** Pilihan cara mencatat makanan, muncul dari tombol tengah. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMealSheet(
    onDismiss: () -> Unit,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onSearch: () -> Unit,
    onBarcode: () -> Unit,
    onVoice: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { androidx.compose.material3.BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Catat makanan", style = MaterialTheme.typography.headlineSmall)
            Text(
                text = "Pilih cara mencatat yang paling cepat buatmu.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))

            HeroOption(
                icon = R.drawable.ic_photo_camera,
                title = "Foto makanan",
                subtitle = "AI memperkirakan kalori dan gizinya",
                onClick = onCamera
            )

            OptionRow(
                icon = R.drawable.ic_image,
                title = "Pilih dari galeri",
                subtitle = "Analisis foto yang sudah ada",
                tint = Color(0xFF6366F1),
                onClick = onGallery
            )
            OptionRow(
                icon = R.drawable.ic_search,
                title = "Cari di daftar makanan",
                subtitle = "180 makanan Indonesia dari TKPI Kemenkes",
                tint = Color(0xFF0EA5E9),
                onClick = onSearch
            )
            OptionRow(
                icon = R.drawable.ic_barcode_scanner,
                title = "Scan barcode kemasan",
                subtitle = "Data gizi dari Open Food Facts",
                tint = Orange500,
                onClick = onBarcode
            )
            OptionRow(
                icon = R.drawable.ic_mic,
                title = "Catat dengan suara",
                subtitle = "Sebutkan makananmu, contoh: nasi goreng dan telur",
                tint = Color(0xFFEC4899),
                onClick = onVoice
            )
        }
    }
}

@Composable
private fun HeroOption(
    @DrawableRes icon: Int,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(
                androidx.compose.ui.graphics.Brush.linearGradient(colors = listOf(Green600, Green700))
            )
            .tapNoRipple(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Color.White)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.88f)
            )
        }
        Icon(
            painter = painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun OptionRow(
    @DrawableRes icon: Int,
    title: String,
    subtitle: String,
    tint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .tapNoRipple(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBadge(icon = icon, tint = tint)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = subtitle,
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

/** Padding standar isi layar. */
val ScreenPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)