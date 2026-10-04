package com.hajun.kaloriku.util

import com.hajun.kaloriku.data.MealEntry
import com.hajun.kaloriku.util.formatWhole
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Membuat berkas CSV dari catatan makan, untuk dilampirkan di laporan. */
object CsvExport {

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun build(entries: List<MealEntry>): String = buildString {
        append("tanggal;waktu;waktu_makan;nama_makanan;gram;kalori;protein_g;karbo_g;lemak_g\n")
        entries.sortedBy { it.timestamp }.forEach { entry ->
            val dateTime = Instant.ofEpochMilli(entry.timestamp).atZone(ZoneId.systemDefault())
            val tanggal = dateTime.toLocalDate().toString()
            val waktu = dateTime.format(timeFormatter)
            entry.items.forEach { item ->
                append(tanggal).append(';')
                append(waktu).append(';')
                append(entry.mealType.label).append(';')
                append(item.name.replace(';', ',')).append(';')
                append(item.grams.formatWhole()).append(';')
                append(item.calories.formatWhole()).append(';')
                append(item.proteinG.formatDecimal()).append(';')
                append(item.carbsG.formatDecimal()).append(';')
                append(item.fatG.formatDecimal()).append('\n')
            }
        }
    }

    fun fileName(today: LocalDate): String = "kaloriku-catatan-$today.csv"
}
