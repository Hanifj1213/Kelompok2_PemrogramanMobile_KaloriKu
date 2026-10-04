package com.hajun.kaloriku

import com.hajun.kaloriku.data.FoodDatabase
import com.hajun.kaloriku.data.FoodItem
import com.hajun.kaloriku.data.MealEntry
import com.hajun.kaloriku.data.MealType
import com.hajun.kaloriku.util.CsvExport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class CsvExportTest {

    @Test
    fun build_producesHeaderAndRows() {
        val timestamp = LocalDate.parse("2026-10-02").atTime(12, 30)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val entries = listOf(
            MealEntry(
                id = 1L,
                timestamp = timestamp,
                mealType = MealType.MAKAN_SIANG,
                items = listOf(
                    FoodItem("Nasi putih", 150.0, 270.0, 4.5, 59.7, 0.45),
                    FoodItem("Ayam goreng", 80.0, 208.0, 20.0, 4.0, 12.8)
                )
            )
        )
        val csv = CsvExport.build(entries)
        val lines = csv.trim().lines()

        assertEquals(3, lines.size) // header + 2 baris makanan
        assertTrue(lines[0].startsWith("tanggal;waktu;waktu_makan;nama_makanan"))
        assertTrue(lines[1].contains("2026-10-02;12:30;Makan Siang;Nasi putih;150;270"))
        assertTrue(lines[2].contains("Ayam goreng"))
    }

    @Test
    fun build_sanitizesSemicolonInName() {
        val entries = listOf(
            MealEntry(
                id = 1L,
                timestamp = 0L,
                mealType = MealType.CAMILAN,
                items = listOf(FoodItem("Kue; lapis", 50.0, 100.0, 1.0, 20.0, 2.0))
            )
        )
        val csv = CsvExport.build(entries)
        // Titik koma di nama diganti koma supaya kolom CSV tidak rusak
        assertTrue(csv.contains("Kue, lapis"))
        assertEquals(2, csv.trim().lines().size)
    }

    @Test
    fun build_emptyEntries_returnsHeaderOnly() {
        val csv = CsvExport.build(emptyList())
        assertEquals(1, csv.trim().lines().size)
    }

    @Test
    fun fileName_containsDate() {
        assertTrue(CsvExport.fileName(LocalDate.parse("2026-10-02")).contains("2026-10-02"))
    }
}
