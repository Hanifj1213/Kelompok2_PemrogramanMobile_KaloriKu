package com.hajun.kaloriku.data

import java.time.LocalDate
import com.hajun.kaloriku.util.toLocalDate

/**
 * Hitungan statistik yang dipakai layar Beranda dan Riwayat.
 * Semua fungsi di sini murni (tanpa akses Android) supaya mudah diuji.
 */
object Stats {

    /** Ringkasan satu hari. */
    data class DaySummary(
        val date: LocalDate,
        val calories: Double,
        val proteinG: Double,
        val carbsG: Double,
        val fatG: Double,
        val entries: Int
    )

    fun summarize(date: LocalDate, entries: List<MealEntry>): DaySummary {
        val dayItems = entries.filter { it.timestamp.toLocalDate() == date }.flatMap { it.items }
        return DaySummary(
            date = date,
            calories = dayItems.sumOf { it.calories },
            proteinG = dayItems.sumOf { it.proteinG },
            carbsG = dayItems.sumOf { it.carbsG },
            fatG = dayItems.sumOf { it.fatG },
            entries = entries.count { it.timestamp.toLocalDate() == date }
        )
    }

    /** Rangkuman mingguan berisi rata-rata gizi dan catatan yang perlu diperhatikan. */
    data class WeeklySummary(
        val days: List<DaySummary>,
        val averageCalories: Double,
        val averageProteinG: Double,
        val averageCarbsG: Double,
        val averageFatG: Double,
        val recordedDays: Int,
        val daysOverTarget: Int,
        val insights: List<String>
    )

    fun weeklySummaries(
        entries: List<MealEntry>,
        days: List<LocalDate>,
        targetCalories: Int,
        targetProteinG: Int
    ): WeeklySummary {
        val summaries = days.map { summarize(it, entries) }
        val recorded = summaries.filter { it.entries > 0 }
        val count = recorded.size.coerceAtLeast(1)

        val avgCalories = recorded.sumOf { it.calories } / count
        val avgProtein = recorded.sumOf { it.proteinG } / count
        val avgCarbs = recorded.sumOf { it.carbsG } / count
        val avgFat = recorded.sumOf { it.fatG } / count
        val overTarget = recorded.count { it.calories > targetCalories }

        val insights = buildList {
            if (recorded.isEmpty()) {
                add("Belum ada catatan pada periode ini. Mulai catat makananmu hari ini.")
                return@buildList
            }
            val lowDays = recorded.count { it.proteinG < targetProteinG * 0.8 }
            if (lowDays >= 3) {
                add("Protein kurang dari 80% target pada $lowDays hari. Tambahkan telur, tempe, tahu, atau ikan.")
            }
            if (overTarget >= 3) {
                add("Kalori melebihi target pada $overTarget hari. Perhatikan porsi dan camilan.")
            }
            if (recorded.size < days.size) {
                val missed = days.size - recorded.size
                add("Ada $missed hari tanpa catatan, jadi rata-rata bisa kurang tepat.")
            }
            if (kotlin.math.abs(avgCalories - targetCalories) <= 200.0) {
                add("Rata-rata kalorimu sudah mendekati target $targetCalories kkal. Pertahankan.")
            }
        }
        return WeeklySummary(
            days = summaries,
            averageCalories = avgCalories,
            averageProteinG = avgProtein,
            averageCarbsG = avgCarbs,
            averageFatG = avgFat,
            recordedDays = recorded.size,
            daysOverTarget = overTarget,
            insights = insights
        )
    }
}
