package com.hajun.kaloriku

import com.hajun.kaloriku.data.BarcodeClient
import com.hajun.kaloriku.data.AiException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BarcodeClientTest {

    @Test
    fun parseProduct_readsPer100g() {
        val body = """
            {"status":1,"product":{
              "product_name":"Biskuit Gandum",
              "brands":"Contoh",
              "serving_quantity":"30",
              "nutriments":{
                "energy-kcal_100g":450,
                "proteins_100g":8,
                "carbohydrates_100g":70,
                "fat_100g":15
              }}}
        """.trimIndent()
        val item = BarcodeClient.parseProduct("123", body)
        assertEquals("Biskuit Gandum (Contoh)", item.name)
        // 450 kkal/100 g dikonversi ke sajian 30 g
        assertEquals(135.0, item.calories, 0.01)
        assertEquals(2.4, item.proteinG, 0.01)
        assertEquals(21.0, item.carbsG, 0.01)
        assertEquals(4.5, item.fatG, 0.01)
        assertEquals(30.0, item.grams, 0.01)
    }

    @Test
    fun parseProduct_noServing_usesHundredGrams() {
        val body = """
            {"status":1,"product":{
              "product_name":"Susu UHT",
              "nutriments":{"energy-kcal_100g":60,"proteins_100g":3,"carbohydrates_100g":5,"fat_100g":3}}}
        """.trimIndent()
        val item = BarcodeClient.parseProduct("456", body)
        assertEquals(60.0, item.calories, 0.01)
        assertEquals(100.0, item.grams, 0.01)
    }

    @Test
    fun parseProduct_fallsBackToServingValues() {
        val body = """
            {"status":1,"product":{
              "product_name":"Minuman Kotak",
              "nutriments":{"energy-kcal_serving":110,"carbohydrates_serving":25}}}
        """.trimIndent()
        val item = BarcodeClient.parseProduct("789", body)
        assertEquals(110.0, item.calories, 0.01)
        assertEquals(25.0, item.carbsG, 0.01)
    }

    @Test(expected = AiException::class)
    fun parseProduct_notFound_throws() {
        BarcodeClient.parseProduct("000", """{"status":0,"status_verbose":"product not found"}""")
    }

    @Test(expected = AiException::class)
    fun parseProduct_noNutrition_throws() {
        BarcodeClient.parseProduct("111", """{"status":1,"product":{"product_name":"X"}}""")
    }

    @Test
    fun parseProduct_missingName_usesBarcode() {
        val body = """
            {"status":1,"product":{"nutriments":{"energy-kcal_100g":100}}}
        """.trimIndent()
        val item = BarcodeClient.parseProduct("999", body)
        assertTrue(item.name.contains("999"))
    }
}
