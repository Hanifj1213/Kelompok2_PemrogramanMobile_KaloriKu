package com.hajun.kaloriku.util

import java.math.BigDecimal

private val decimalInputPattern = Regex("(?:[0-9]+(?:[.,][0-9]*)?|[.,][0-9]+)")
private val integerInputPattern = Regex("[0-9]+")

fun parseDecimalInput(text: String, range: ClosedFloatingPointRange<Double>): Double? {
    val trimmed = text.trim()
    if (!decimalInputPattern.matches(trimmed)) return null
    return trimmed.replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() && it in range }
}

fun parseIntegerInput(text: String, range: IntRange): Int? {
    val trimmed = text.trim()
    if (!integerInputPattern.matches(trimmed)) return null
    return trimmed.toIntOrNull()?.takeIf { it in range }
}

fun Double.toPlainInput(): String =
    if (isFinite()) BigDecimal.valueOf(this).stripTrailingZeros().toPlainString() else ""
