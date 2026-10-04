package com.hajun.kaloriku

import com.hajun.kaloriku.data.Food
import com.hajun.kaloriku.data.Portion
import org.junit.Assert.assertEquals
import org.junit.Test

class FoodDatabaseTest {

    private val nasi = Food(
        id = 1,
        name = "Nasi putih",
        category = "Makanan pokok",
        caloriesPer100g = 180.0,
        proteinPer100g = 3.0,
        carbsPer100g = 39.8,
        fatPer100g = 0.3,
        portions = listOf(Portion("1 piring", 150.0))
    )

    @Test
    fun toFoodItem_scalesFrom100g() {
        val item = nasi.toFoodItem(150.0)
        assertEquals(270.0, item.calories, 0.01)
        assertEquals(4.5, item.proteinG, 0.01)
        assertEquals(59.7, item.carbsG, 0.01)
        assertEquals(0.45, item.fatG, 0.01)
        assertEquals(150.0, item.grams, 0.01)
    }

    @Test
    fun toFoodItem_halfPortion() {
        val item = nasi.toFoodItem(50.0)
        assertEquals(90.0, item.calories, 0.01)
    }
}
