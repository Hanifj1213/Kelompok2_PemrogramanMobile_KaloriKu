package com.hajun.kaloriku.data

data class FoodItem(
    val name: String,
    val grams: Double,
    val calories: Double,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double
) {
    fun scaled(factor: Double) = copy(
        grams = grams * factor,
        calories = calories * factor,
        proteinG = proteinG * factor,
        carbsG = carbsG * factor,
        fatG = fatG * factor
    )
}

fun List<FoodItem>.sumNutrition() = FoodItem(
    name = "Total",
    grams = sumOf { it.grams },
    calories = sumOf { it.calories },
    proteinG = sumOf { it.proteinG },
    carbsG = sumOf { it.carbsG },
    fatG = sumOf { it.fatG }
)

data class AnalysisResult(
    val isFood: Boolean,
    val items: List<FoodItem>,
    val note: String
)

enum class MealType(val label: String) {
    SARAPAN("Sarapan"),
    MAKAN_SIANG("Makan Siang"),
    MAKAN_MALAM("Makan Malam"),
    CAMILAN("Camilan");

    companion object {
        fun fromHour(hour: Int): MealType = when (hour) {
            in 4..10 -> SARAPAN
            in 11..15 -> MAKAN_SIANG
            in 16..21 -> MAKAN_MALAM
            else -> CAMILAN
        }
    }
}

data class MealEntry(
    val id: Long,
    val timestamp: Long,
    val mealType: MealType,
    val items: List<FoodItem>
) {
    val totalCalories: Double get() = items.sumOf { it.calories }
}
