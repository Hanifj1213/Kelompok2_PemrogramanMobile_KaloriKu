package com.hajun.kaloriku

import com.hajun.kaloriku.data.FoodItem
import com.hajun.kaloriku.data.MealEntry
import com.hajun.kaloriku.data.MealType
import com.hajun.kaloriku.data.findEntry
import com.hajun.kaloriku.ui.navigation.SearchRoute
import com.hajun.kaloriku.ui.navigation.resolveManualMealSelection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Menguji pencarian catatan dan pemilihan waktu makan dari [SearchRoute]. */
class MealDetailRouteTest {

    private fun entry(id: Long, type: MealType = MealType.MAKAN_SIANG): MealEntry =
        MealEntry(
            id = id,
            timestamp = 1_700_000_000_000,
            mealType = type,
            items = listOf(FoodItem("Uji", 100.0, 200.0, 5.0, 30.0, 8.0))
        )

    @Test
    fun findEntry_returnsMatchingEntry() {
        val entries = listOf(entry(1L), entry(2L), entry(3L))
        assertEquals(2L, entries.findEntry(2L)?.id)
    }

    @Test
    fun findEntry_returnsNullWhenMissing() {
        val entries = listOf(entry(1L), entry(2L))
        assertNull(entries.findEntry(99L))
    }

    @Test
    fun findEntry_onEmptyListReturnsNull() {
        assertNull(emptyList<MealEntry>().findEntry(1L))
    }

    @Test
    fun selection_startFreshResetsMealTypeToFreshValue() {
        val selection = resolveManualMealSelection(
            route = SearchRoute(),
            currentMealType = MealType.MAKAN_MALAM,
            freshMealType = MealType.SARAPAN
        )
        assertTrue(selection.startFresh)
        assertEquals(MealType.SARAPAN, selection.mealType)
    }

    @Test
    fun selection_appendToDraftKeepsCurrentMealType() {
        val selection = resolveManualMealSelection(
            route = SearchRoute(appendToDraft = true),
            currentMealType = MealType.MAKAN_MALAM,
            freshMealType = MealType.SARAPAN
        )
        assertEquals(false, selection.startFresh)
        assertEquals(MealType.MAKAN_MALAM, selection.mealType)
    }

    @Test
    fun selection_explicitMealTypeOverridesBase() {
        val explicit = resolveManualMealSelection(
            route = SearchRoute(mealType = MealType.CAMILAN),
            currentMealType = MealType.MAKAN_MALAM,
            freshMealType = MealType.SARAPAN
        )
        assertTrue(explicit.startFresh)
        assertEquals(MealType.CAMILAN, explicit.mealType)
    }

    @Test
    fun selection_explicitMealTypeWorksWithAppendToDraft() {
        val explicit = resolveManualMealSelection(
            route = SearchRoute(mealType = MealType.SARAPAN, appendToDraft = true),
            currentMealType = MealType.MAKAN_MALAM,
            freshMealType = MealType.CAMILAN
        )
        assertEquals(false, explicit.startFresh)
        assertEquals(MealType.SARAPAN, explicit.mealType)
    }
}
