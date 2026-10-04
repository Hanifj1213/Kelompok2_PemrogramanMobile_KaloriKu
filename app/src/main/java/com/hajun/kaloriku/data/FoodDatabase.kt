package com.hajun.kaloriku.data

import android.content.Context
import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * Satu bahan makanan dari basis data gizi (lihat assets/foods.csv).
 * Nilai gizi disimpan per 100 gram bahan.
 */
data class Food(
    val id: Int,
    val name: String,
    val category: String,
    val caloriesPer100g: Double,
    val proteinPer100g: Double,
    val carbsPer100g: Double,
    val fatPer100g: Double,
    val portions: List<Portion>
) {
    /** Mengubah nilai per 100 gram menjadi [FoodItem] sesuai berat porsi. */
    fun toFoodItem(grams: Double): FoodItem {
        val ratio = grams / 100.0
        return FoodItem(
            name = name,
            grams = grams,
            calories = caloriesPer100g * ratio,
            proteinG = proteinPer100g * ratio,
            carbsG = carbsPer100g * ratio,
            fatG = fatPer100g * ratio
        )
    }

    /** Kata kunci untuk pencarian: nama plus kategorinya. */
    val normalizedName: String = name.lowercase(java.util.Locale.ROOT)
    val searchText: String = "$name $category".lowercase(java.util.Locale.ROOT)
}

/** Takaran rumah tangga, misalnya "1 piring" = 150 gram. */
data class Portion(val label: String, val grams: Double)

/**
 * Basis data makanan lokal yang dibaca dari assets/foods.csv
 * (Tabel Komposisi Pangan Indonesia, Kemenkes RI).
 */
object FoodDatabase {

    private var loaded: List<Food> = emptyList()

    val foods: List<Food> get() = loaded

    val categories: List<String>
        get() = loaded.map { it.category }.distinct().sorted()

    fun ensureLoaded(context: Context) {
        if (loaded.isNotEmpty()) return
        loaded = context.assets.open("foods.csv")
            .bufferedReader()
            .useLines { lines ->
                lines.mapNotNull(::parseLine)
                    .toList()
            }
    }

    private fun parseLine(line: String): Food? {
        val trimmed = line.trim()
        if (trimmed.isEmpty() || trimmed.startsWith("#")) return null
        val parts = trimmed.split(',')
        if (parts.size < 8) return null
        return try {
            Food(
                id = parts[0].trim().toInt(),
                name = parts[1].trim(),
                category = parts[2].trim(),
                caloriesPer100g = parts[3].trim().toDouble(),
                proteinPer100g = parts[4].trim().toDouble(),
                carbsPer100g = parts[5].trim().toDouble(),
                fatPer100g = parts[6].trim().toDouble(),
                portions = parsePortions(parts[7].trim())
            )
        } catch (_: NumberFormatException) {
            null
        }
    }

    /** Format kolom takaran: "1 piring=150 g;1 centong=100 g" */
    private fun parsePortions(raw: String): List<Portion> =
        raw.split(';').mapNotNull { chunk ->
            val pieces = chunk.split('=')
            if (pieces.size != 2) return@mapNotNull null
            val label = pieces[0].trim()
            val grams = pieces[1].removeSuffix(" g").trim().toDoubleOrNull() ?: return@mapNotNull null
            Portion(label, grams)
        }

    /**
     * Pencarian sederhana: cocokkan setiap kata kunci, urutkan berdasarkan
     * seberapa awal kata itu muncul di nama makanan.
     */
    fun search(query: String, limit: Int = 40): List<Food> {
        val keywords = query.lowercase(java.util.Locale.ROOT).trim().split(' ').filter { it.isNotEmpty() }
        if (keywords.isEmpty()) return loaded.take(limit)

        return loaded.mapNotNull { food ->
            val name = food.normalizedName
            val searchText = food.searchText
            var score = 0
            for (keyword in keywords) {
                val indexInName = name.indexOf(keyword)
                when {
                    indexInName == 0 -> score += 100
                    indexInName > 0 -> score += 50 - indexInName.coerceAtMost(40)
                    searchText.contains(keyword) -> score += 10
                    else -> return@mapNotNull null
                }
            }
            score to food
        }
            .sortedByDescending { it.first }
            .take(limit)
            .map { it.second }
    }

    /** Mencari satu makanan dengan nama paling mirip, untuk hasil input suara. */
    fun findBestMatch(name: String): Food? {
        val query = name.lowercase(java.util.Locale.ROOT).trim()
        if (query.isEmpty()) return null
        loaded.firstOrNull { it.normalizedName == query }?.let { return it }
        return search(query, limit = 1).firstOrNull()
    }
}

/** Target gizi harian yang bisa disesuaikan pengguna. */
data class DailyGoals(
    val calories: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int
) {
    companion object {
        /** Target gizi bawaan, dihitung dari target kalori (15% protein, 55% karbo, 30% lemak). */
        fun fromCalories(calories: Int) = DailyGoals(
            calories = calories,
            proteinG = (calories * 0.15 / 4).roundToInt(),
            carbsG = (calories * 0.55 / 4).roundToInt(),
            fatG = (calories * 0.30 / 9).roundToInt()
        )
    }
}

/** Catatan minum air. Satu entri = satu gelas. */
data class WaterEntry(val id: Long, val timestamp: Long, val glasses: Int)

/** Catatan berat badan untuk melihat progres. */
data class WeightEntry(val date: LocalDate, val weightKg: Double)
