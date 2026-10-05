package com.hajun.kaloriku.util

const val BMI_SCALE_MIN = 15.0
const val BMI_SCALE_MAX = 35.0

/** Ambang IMT dewasa untuk panduan umum, bukan diagnosis. */
enum class BmiCategory(
    val label: String,
    val lowerInclusive: Double?,
    val upperExclusive: Double?
) {
    KURANG("Kurang", null, 18.5),
    NORMAL("Normal", 18.5, 23.0),
    BERLEBIH("Berlebih", 23.0, 25.0),
    OBESITAS("Obesitas", 25.0, null)
}

/** IMT hanya membutuhkan berat (kg) dan tinggi (cm), tidak bergantung pada usia. */
fun calculateBmi(weightKg: Double?, heightCm: Double?): Double? {
    if (weightKg == null || heightCm == null ||
        !weightKg.isFinite() || !heightCm.isFinite() || weightKg <= 0.0 || heightCm <= 0.0
    ) {
        return null
    }
    val heightM = heightCm / 100.0
    return (weightKg / (heightM * heightM)).takeIf { it.isFinite() && it > 0.0 }
}

fun bmiCategory(bmi: Double?): BmiCategory? {
    if (bmi == null || !bmi.isFinite() || bmi <= 0.0) return null
    return BmiCategory.entries.first { category ->
        category.upperExclusive?.let { bmi < it } ?: true
    }
}

/** Hanya posisi penanda yang dibatasi ke skala 15–35; nilai IMT asli tidak diubah. */
fun bmiMarkerFraction(bmi: Double?): Float? {
    if (bmi == null || !bmi.isFinite() || bmi <= 0.0) return null
    return ((bmi.coerceIn(BMI_SCALE_MIN, BMI_SCALE_MAX) - BMI_SCALE_MIN) /
        (BMI_SCALE_MAX - BMI_SCALE_MIN)).toFloat()
}
