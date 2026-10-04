package com.hajun.kaloriku.data

import kotlin.math.roundToInt

enum class Gender(val label: String) {
    PRIA("Pria"),
    WANITA("Wanita")
}

enum class ActivityLevel(val label: String, val description: String, val factor: Double) {
    SANGAT_RENDAH("Sangat rendah", "Banyak duduk, jarang olahraga", 1.2),
    RENDAH("Ringan", "Olahraga 1–3 kali per minggu", 1.375),
    SEDANG("Sedang", "Olahraga 3–5 kali per minggu", 1.55),
    TINGGI("Tinggi", "Olahraga 6–7 kali per minggu", 1.725),
    SANGAT_TINGGI("Sangat tinggi", "Pekerjaan fisik berat atau latihan 2x sehari", 1.9)
}

enum class Goal(val label: String, val adjustment: Int) {
    TURUN("Turunkan berat", -500),
    JAGA("Jaga berat", 0),
    NAIK("Naikkan berat", 300)
}

data class Profile(
    val gender: Gender,
    val ageYears: Int,
    val weightKg: Double,
    val heightCm: Double,
    val activity: ActivityLevel,
    val goal: Goal
)

object CalorieCalculator {
    const val DEFAULT_TARGET = 2000
    private const val MIN_TARGET = 1200

    /** Basal Metabolic Rate dengan rumus Mifflin-St Jeor (kkal/hari). */
    fun bmr(profile: Profile): Double {
        val base = 10 * profile.weightKg + 6.25 * profile.heightCm - 5 * profile.ageYears
        return when (profile.gender) {
            Gender.PRIA -> base + 5
            Gender.WANITA -> base - 161
        }
    }

    /** Total Daily Energy Expenditure: BMR dikali faktor aktivitas. */
    fun tdee(profile: Profile): Double = bmr(profile) * profile.activity.factor

    fun dailyTarget(profile: Profile): Int =
        (tdee(profile) + profile.goal.adjustment).roundToInt().coerceAtLeast(MIN_TARGET)
}
