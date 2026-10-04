package com.hajun.kaloriku

import com.hajun.kaloriku.data.ActivityLevel
import com.hajun.kaloriku.data.CalorieCalculator
import com.hajun.kaloriku.data.Gender
import com.hajun.kaloriku.data.Goal
import com.hajun.kaloriku.data.MealType
import com.hajun.kaloriku.data.Profile
import org.junit.Assert.assertEquals
import org.junit.Test

class CalorieCalculatorTest {

    private val man = Profile(Gender.PRIA, 20, 70.0, 175.0, ActivityLevel.SEDANG, Goal.JAGA)

    @Test
    fun bmr_mifflinStJeor() {
        // 10*70 + 6.25*175 - 5*20 + 5 = 1698.75
        assertEquals(1698.75, CalorieCalculator.bmr(man), 0.001)
        // Wanita: -161 menggantikan +5
        assertEquals(1532.75, CalorieCalculator.bmr(man.copy(gender = Gender.WANITA)), 0.001)
    }

    @Test
    fun dailyTarget_appliesActivityAndGoal() {
        // 1698.75 * 1.55 = 2633.06
        assertEquals(2633, CalorieCalculator.dailyTarget(man))
        assertEquals(2133, CalorieCalculator.dailyTarget(man.copy(goal = Goal.TURUN)))
        assertEquals(2933, CalorieCalculator.dailyTarget(man.copy(goal = Goal.NAIK)))
    }

    @Test
    fun dailyTarget_neverBelowMinimum() {
        val small = Profile(Gender.WANITA, 80, 35.0, 140.0, ActivityLevel.SANGAT_RENDAH, Goal.TURUN)
        assertEquals(1200, CalorieCalculator.dailyTarget(small))
    }

    @Test
    fun mealType_fromHour() {
        assertEquals(MealType.SARAPAN, MealType.fromHour(7))
        assertEquals(MealType.MAKAN_SIANG, MealType.fromHour(12))
        assertEquals(MealType.MAKAN_MALAM, MealType.fromHour(19))
        assertEquals(MealType.CAMILAN, MealType.fromHour(23))
    }
}
