package com.hajun.kaloriku

import com.hajun.kaloriku.data.BarcodeClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Menguji parser barcode memakai respons asli dari Open Food Facts
 * (produk Teh Pucuk Harum, diambil langsung dari layanan).
 */
class BarcodeRealResponseTest {

    private fun fixture(): String =
        javaClass.classLoader!!.getResourceAsStream("off_teh_pucuk.json")
            ?.bufferedReader()?.use { it.readText() }
            ?: error("fixture tidak ditemukan")

    @Test
    fun parseRealResponse_readsProductAndServing() {
        val item = BarcodeClient.parseProduct("8996001600146", fixture())

        assertEquals("Teh Pucuk Harum Jasmine 350 ml (Mayora)", item.name)
        // Sajian 240 g, nilai per 100 g = 29,17 kkal -> 70 kkal per sajian
        assertEquals(240.0, item.grams, 0.01)
        assertEquals(70.0, item.calories, 0.5)
        assertEquals(0.0, item.proteinG, 0.01)
        assertEquals(18.0, item.carbsG, 0.5)
        assertEquals(0.0, item.fatG, 0.01)
        assertTrue(item.name.contains("Teh Pucuk"))
    }
}
