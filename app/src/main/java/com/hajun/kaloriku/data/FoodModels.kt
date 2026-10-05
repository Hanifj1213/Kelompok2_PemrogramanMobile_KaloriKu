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

// R8: Navigation 2.10 mencari kelas enum argumen lewat Class.forName di Android,
// jadi nama kelas ini tidak boleh diubah. Konstanta juga tidak boleh diganti nama
// karena disimpan lewat .name di MealJson.
@androidx.annotation.Keep
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

/** Jadwal jam pengingat untuk sarapan, makan siang, dan makan malam. */
data class ReminderTimes(
    val breakfastHour: Int = 8,
    val breakfastMinute: Int = 0,
    val lunchHour: Int = 13,
    val lunchMinute: Int = 0,
    val dinnerHour: Int = 19,
    val dinnerMinute: Int = 0
) {
    fun formatBreakfast(): String = String.format(java.util.Locale.US, "%02d.%02d", breakfastHour, breakfastMinute)
    fun formatLunch(): String = String.format(java.util.Locale.US, "%02d.%02d", lunchHour, lunchMinute)
    fun formatDinner(): String = String.format(java.util.Locale.US, "%02d.%02d", dinnerHour, dinnerMinute)
}
