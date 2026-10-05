package com.hajun.kaloriku.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.hajun.kaloriku.R
import com.hajun.kaloriku.ui.navigation.HistoryRoute
import com.hajun.kaloriku.ui.navigation.HomeRoute
import com.hajun.kaloriku.ui.navigation.ProfileRoute
import com.hajun.kaloriku.ui.theme.Spacing

/** Satu tab di bilah bawah. Rutenya berupa objek bertipe, bukan string. */
data class BottomTab(
    val route: Any,
    val label: String,
    @param:DrawableRes val icon: Int,
    @param:DrawableRes val selectedIcon: Int
)

val bottomTabs = listOf(
    BottomTab(HomeRoute, "Beranda", R.drawable.ic_home, R.drawable.ic_home_fill),
    BottomTab(HistoryRoute, "Riwayat", R.drawable.ic_calendar_month, R.drawable.ic_calendar_month_fill),
    BottomTab(ProfileRoute, "Profil", R.drawable.ic_person, R.drawable.ic_person_fill)
)

/** Bilah navigasi bawah Material 3. Tab terpilih dihitung pemanggil lewat hierarki rute. */
@Composable
fun KaloriBottomBar(
    selectedRoute: Any?,
    onSelect: (Any) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    NavigationBar(
        modifier = Modifier.navigationBarsPadding(),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        bottomTabs.forEach { tab ->
            val selected = selectedRoute == tab.route
            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (!selected) {
                        haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                        onSelect(tab.route)
                    }
                },
                icon = {
                    Icon(
                        painter = painterResource(if (selected) tab.selectedIcon else tab.icon),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.testTag("tab_${tab.label}")
            )
        }
    }
}

/** Pilihan cara mencatat makanan, muncul dari tombol Catat. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMealSheet(
    onDismiss: () -> Unit,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onSearch: () -> Unit
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
                .padding(start = Spacing.xl, end = Spacing.xl, top = Spacing.xs, bottom = Spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Text("Catat makanan", style = MaterialTheme.typography.headlineSmall)
            Text(
                text = "Pilih cara mencatat yang paling cepat buatmu.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            AddMealRow(
                icon = R.drawable.ic_photo_camera,
                title = "Foto makanan",
                subtitle = "AI memperkirakan kalori dan gizinya",
                highlighted = true,
                onClick = onCamera
            )
            AddMealRow(
                icon = R.drawable.ic_image,
                title = "Pilih dari galeri",
                subtitle = "Analisis foto yang sudah ada",
                onClick = onGallery
            )
            AddMealRow(
                icon = R.drawable.ic_search,
                title = "Cari di daftar makanan",
                subtitle = "180 makanan Indonesia dari TKPI Kemenkes",
                onClick = onSearch
            )
        }
    }
}

@Composable
private fun AddMealRow(
    @DrawableRes icon: Int,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    highlighted: Boolean = false
) {
    val colors = if (highlighted) {
        ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            headlineColor = MaterialTheme.colorScheme.onPrimaryContainer,
            leadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
            supportingColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    } else {
        ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface)
    }
    ListItem(
        headlineContent = { Text(title, style = MaterialTheme.typography.titleSmall) },
        supportingContent = { Text(subtitle, style = MaterialTheme.typography.bodySmall) },
        leadingContent = {
            IconBadge(
                icon = icon,
                tint = if (highlighted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
                container = if (highlighted) {
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.14f)
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                }
            )
        },
        colors = colors,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
    )
}
