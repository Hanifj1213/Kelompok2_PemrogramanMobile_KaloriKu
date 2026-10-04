package com.hajun.kaloriku

import com.hajun.kaloriku.data.ActivityLevel
import com.hajun.kaloriku.data.FoodItem
import com.hajun.kaloriku.data.Gender
import com.hajun.kaloriku.data.Goal
import com.hajun.kaloriku.data.MealEntry
import com.hajun.kaloriku.data.MealJson
import com.hajun.kaloriku.data.MealType
import com.hajun.kaloriku.data.Profile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MealJsonTest {

    @Test
    fun entries_roundTrip() {
        val entries = listOf(
            MealEntry(
                id = 1L,
                timestamp = 1_790_000_000_000L,
                mealType = MealType.MAKAN_SIANG,
                items = listOf(FoodItem("Rendang", 100.0, 193.0, 22.6, 7.8, 7.9).scaled(1.5))
            )
        )
        assertEquals(entries, MealJson.entriesFromJson(MealJson.entriesToJson(entries)))
    }

    @Test
    fun entries_invalidJson_returnsEmpty() {
        assertTrue(MealJson.entriesFromJson("bukan json").isEmpty())
        assertTrue(MealJson.entriesFromJson(null).isEmpty())
    }

    @Test
    fun profile_roundTrip() {
        val profile = Profile(Gender.WANITA, 21, 55.5, 160.0, ActivityLevel.RENDAH, Goal.TURUN)
        assertEquals(profile, MealJson.profileFromJson(MealJson.profileToJson(profile)))
        assertNull(MealJson.profileFromJson("{}"))
    }
}
