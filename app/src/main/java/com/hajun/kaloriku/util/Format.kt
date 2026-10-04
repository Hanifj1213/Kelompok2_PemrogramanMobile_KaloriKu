package com.hajun.kaloriku.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

val IndonesianLocale: Locale = Locale.forLanguageTag("id-ID")

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", IndonesianLocale)
private val dayTitleFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", IndonesianLocale)
private val dayShortFormatter = DateTimeFormatter.ofPattern("EEE", IndonesianLocale)

/** Contoh: 1234.6 -> "1.235" */
fun Double.formatWhole(): String = roundToInt().formatWhole()

fun Int.formatWhole(): String = String.format(IndonesianLocale, "%,d", this)

/** Contoh: 12.34 -> "12,3" */
fun Double.formatDecimal(): String = String.format(IndonesianLocale, "%.1f", this)

fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()

fun Long.formatTime(): String =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).format(timeFormatter)

/** Contoh: "Rabu, 1 Oktober 2026" */
fun LocalDate.formatDayTitle(): String = format(dayTitleFormatter)

/** Contoh: "Rab" */
fun LocalDate.formatDayShort(): String = format(dayShortFormatter)

private val dayMonthFormatter = DateTimeFormatter.ofPattern("d MMM", IndonesianLocale)

/** Contoh: "1 Okt" */
fun LocalDate.formatDayMonth(): String = format(dayMonthFormatter)

/** Judul hari yang enak dibaca: "Hari ini", "Kemarin", lalu tanggal lengkap. */
fun LocalDate.formatRelativeDay(today: LocalDate = LocalDate.now()): String = when (this) {
    today -> "Hari ini"
    today.minusDays(1) -> "Kemarin"
    else -> formatDayTitle()
}

/** Sapaan sesuai jam perangkat. */
fun greetingFor(hour: Int): String = when (hour) {
    in 4..10 -> "Selamat pagi"
    in 11..14 -> "Selamat siang"
    in 15..17 -> "Selamat sore"
    else -> "Selamat malam"
}

/** Contoh: 500 -> "500 ml", 1500 -> "1,5 L" */
fun formatVolume(milliliters: Int): String =
    if (milliliters >= 1000) "${(milliliters / 1000.0).formatDecimal()} L" else "$milliliters ml"
