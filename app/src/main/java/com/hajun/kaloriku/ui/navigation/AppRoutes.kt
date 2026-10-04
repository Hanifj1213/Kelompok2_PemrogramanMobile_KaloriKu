package com.hajun.kaloriku.ui.navigation

import com.hajun.kaloriku.data.MealType
import kotlinx.serialization.Serializable

/**
 * Rute navigasi bertipe (Navigation Compose 2.10). Menggantikan rute string lama
 * supaya argumen dikirim sebagai properti kelas, bukan tempelan di alamat.
 */

// --- Tab utama ---
@Serializable
data object HomeRoute

@Serializable
data object HistoryRoute

@Serializable
data object ProfileRoute

// --- Layar detail ---
@Serializable
data object ResultRoute

@Serializable
data object BarcodeRoute

@Serializable
data object GoalsRoute

/** Pencarian makanan. `mealType` mengisi waktu makan; `appendToDraft` menambah ke draf yang ada. */
@Serializable
data class SearchRoute(
    val mealType: MealType? = null,
    val appendToDraft: Boolean = false
)

/** Detail satu catatan makan. Dibuka dengan mengetuk kartu catatan. */
@Serializable
data class MealDetailRoute(val entryId: Long)

/** Hasil pemilihan draf dan waktu makan saat membuka [SearchRoute]. */
data class ManualMealSelection(
    val startFresh: Boolean,
    val mealType: MealType
)

/**
 * Menentukan apakah draf dibersihkan dan waktu makan apa yang dipakai saat membuka pencarian.
 *
 * Urutannya penting: saat `startFresh` benar, pemanggil harus menjalankan
 * `resetAnalysis()` lebih dulu, baru menulis `mealType`, karena reset mengembalikan
 * `mealType` ke waktu sekarang ([freshMealType]).
 */
fun resolveManualMealSelection(
    route: SearchRoute,
    currentMealType: MealType,
    freshMealType: MealType
): ManualMealSelection {
    val startFresh = !route.appendToDraft
    val base = if (startFresh) freshMealType else currentMealType
    return ManualMealSelection(startFresh = startFresh, mealType = route.mealType ?: base)
}
