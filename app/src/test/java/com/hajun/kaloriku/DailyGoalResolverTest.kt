package com.hajun.kaloriku

import com.hajun.kaloriku.data.ActivityLevel
import com.hajun.kaloriku.data.CalorieCalculator
import com.hajun.kaloriku.data.DailyGoals
import com.hajun.kaloriku.data.Gender
import com.hajun.kaloriku.data.Goal
import com.hajun.kaloriku.data.Profile
import com.hajun.kaloriku.data.dailyGoalsFlow
import com.hajun.kaloriku.data.resolveDailyGoals
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Test

class DailyGoalResolverTest {
    private val profile = Profile(Gender.PRIA, 21, 63.5, 170.0, ActivityLevel.SEDANG, Goal.JAGA)

    @Test
    fun defaultsWithoutProfile() {
        assertEquals(DailyGoals.fromCalories(2000), resolveDailyGoals(null, null))
    }

    @Test
    fun profileSetsCaloriesAndDefaultMacros() {
        val target = CalorieCalculator.dailyTarget(profile)
        assertEquals(DailyGoals.fromCalories(target), resolveDailyGoals(profile, null))
    }

    @Test
    fun customMacrosUseCurrentProfileCaloriesNotStaleSavedCalories() {
        val saved = DailyGoals(2000, 110, 280, 70)
        assertEquals(saved.copy(calories = CalorieCalculator.dailyTarget(profile)), resolveDailyGoals(profile, saved))
    }

    @Test
    fun preservesExplicitZeroMacroTargets() {
        val saved = DailyGoals(2500, 0, 0, 0)
        assertEquals(DailyGoals(2000, 0, 0, 0), resolveDailyGoals(null, saved))
    }

    @Test
    fun changingSavedGoalsEmitsWithoutChangingProfile() = runBlocking {
        val profiles = MutableStateFlow<Profile?>(profile)
        val saved = MutableStateFlow<DailyGoals?>(null)
        val values = mutableListOf<DailyGoals>()
        val collector = launch(start = CoroutineStart.UNDISPATCHED) {
            dailyGoalsFlow(profiles, saved).take(2).toList(values)
        }
        while (values.isEmpty()) yield()
        saved.value = DailyGoals(1200, 111, 222, 55)
        collector.join()
        assertEquals(111, values.last().proteinG)
        assertEquals(222, values.last().carbsG)
        assertEquals(CalorieCalculator.dailyTarget(profile), values.last().calories)
    }

    @Test
    fun profileChangesCaloriesButKeepsCustomMacros() = runBlocking {
        val profiles = MutableStateFlow<Profile?>(profile)
        val saved = MutableStateFlow<DailyGoals?>(DailyGoals(2000, 100, 250, 65))
        val values = mutableListOf<DailyGoals>()
        val collector = launch(start = CoroutineStart.UNDISPATCHED) {
            dailyGoalsFlow(profiles, saved).take(2).toList(values)
        }
        while (values.isEmpty()) yield()
        profiles.value = profile.copy(goal = Goal.TURUN)
        collector.join()
        assertEquals(100, values.last().proteinG)
        assertEquals(CalorieCalculator.dailyTarget(profiles.value!!), values.last().calories)
    }
}
