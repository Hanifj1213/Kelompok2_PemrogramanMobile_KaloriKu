package com.hajun.kaloriku

import com.hajun.kaloriku.data.FoodItem
import com.hajun.kaloriku.data.MealEntry
import com.hajun.kaloriku.data.MealType
import com.hajun.kaloriku.data.Stats
import com.hajun.kaloriku.data.WaterEntry
import com.hajun.kaloriku.data.WeightEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class StatsTest {

    private val zone: ZoneId = ZoneId.systemDefault()

    private fun millis(date: String, hour: Int = 12): Long =
        LocalDate.parse(date).atTime(hour, 0).atZone(zone).toInstant().toEpochMilli()

    private fun entry(date: String, calories: Double = 500.0, protein: Double = 0.0): MealEntry =
        MealEntry(
            id = millis(date),
            timestamp = millis(date),
            mealType = MealType.MAKAN_SIANG,
            items = listOf(FoodItem("Uji", 100.0, calories, protein, 0.0, 0.0))
        )

    @Test
    fun summarize_countsOnlyThatDay() {
        val entries = listOf(
            entry("2026-10-01", 500.0, 20.0),
            entry("2026-10-01", 300.0, 10.0),
            entry("2026-10-02", 900.0)
        )
        val summary = Stats.summarize(LocalDate.parse("2026-10-01"), entries)
        assertEquals(800.0, summary.calories, 0.01)
        assertEquals(30.0, summary.proteinG, 0.01)
        assertEquals(2, summary.entries)
    }

    @Test
    fun streak_countsConsecutiveDaysIncludingToday() {
        val entries = listOf(
            entry("2026-10-02"),
            entry("2026-10-01"),
            entry("2026-09-30"),
            entry("2026-09-28")
        )
        assertEquals(3, Stats.streak(entries, LocalDate.parse("2026-10-02")))
    }

    @Test
    fun streak_startsFromYesterdayWhenTodayEmpty() {
        val entries = listOf(entry("2026-10-01"), entry("2026-09-30"))
        // Hari ini (2 Okt) belum dicatat, rantai dihitung dari kemarin
        assertEquals(2, Stats.streak(entries, LocalDate.parse("2026-10-02")))
    }

    @Test
    fun streak_zeroWhenNoEntries() {
        assertEquals(0, Stats.streak(emptyList(), LocalDate.parse("2026-10-02")))
    }

    @Test
    fun streak_zeroWhenGapBeforeToday() {
        // Catatan terakhir 3 hari lalu, jadi tidak ada rentetan
        val entries = listOf(entry("2026-09-29"))
        assertEquals(0, Stats.streak(entries, LocalDate.parse("2026-10-02")))
    }

    @Test
    fun weeklySummaries_computesAveragesAndInsights() {
        val entries = listOf(
            entry("2026-09-28", 1000.0, 20.0),
            entry("2026-09-29", 2500.0, 20.0),
            entry("2026-09-30", 2600.0, 20.0),
            entry("2026-10-01", 2700.0, 20.0)
        )
        val days = (6 downTo 0).map { LocalDate.parse("2026-10-04").minusDays(it.toLong()) }
        val summary = Stats.weeklySummaries(entries, days, targetCalories = 2000, targetProteinG = 60)

        assertEquals(4, summary.recordedDays)
        assertEquals((1000.0 + 2500.0 + 2600.0 + 2700.0) / 4, summary.averageCalories, 0.01)
        assertEquals(3, summary.daysOverTarget)
        // Protein 20 g jauh di bawah 80% dari 60 g, muncul saran protein
        assertEquals(true, summary.insights.any { it.contains("Protein") })
        assertEquals(true, summary.insights.any { it.contains("melebihi target") })
    }

    @Test
    fun weeklySummaries_emptyWeek_givesPrompt() {
        val days = (6 downTo 0).map { LocalDate.parse("2026-10-04").minusDays(it.toLong()) }
        val summary = Stats.weeklySummaries(emptyList(), days, 2000, 60)
        assertEquals(0, summary.recordedDays)
        assertEquals(true, summary.insights.first().contains("Belum ada catatan"))
    }

    @Test
    fun waterGlasses_sumsOnlyThatDay() {
        val entries = listOf(
            WaterEntry(1, millis("2026-10-01", 8), 1),
            WaterEntry(2, millis("2026-10-01", 10), 2),
            WaterEntry(3, millis("2026-10-02", 9), 5)
        )
        assertEquals(3, Stats.waterGlasses(LocalDate.parse("2026-10-01"), entries))
        assertEquals(5, Stats.waterGlasses(LocalDate.parse("2026-10-02"), entries))
    }

    @Test
    fun weightChange_needsTwoRecords() {
        assertNull(Stats.weightChange(listOf(WeightEntry(LocalDate.parse("2026-10-01"), 60.0))))
        val change = Stats.weightChange(
            listOf(
                WeightEntry(LocalDate.parse("2026-10-01"), 62.0),
                WeightEntry(LocalDate.parse("2026-09-01"), 65.0)
            )
        )
        assertEquals(-3.0, change!!, 0.01)
    }
}
