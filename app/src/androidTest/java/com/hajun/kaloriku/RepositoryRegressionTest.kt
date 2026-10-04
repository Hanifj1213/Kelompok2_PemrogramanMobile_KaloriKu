package com.hajun.kaloriku

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.hajun.kaloriku.data.DailyGoals
import com.hajun.kaloriku.data.MealLogRepository
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RepositoryRegressionTest {
    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val name = "repository_regression_${System.nanoTime()}"

    @After fun cleanTestPreferences() {
        context.deleteSharedPreferences(name)
    }

    @Test fun reminderDefaultIsEnabled() {
        assertTrue(MealLogRepository(context, name).remindersEnabled.value)
    }

    @Test fun reminderOptOutSurvivesRepositoryRecreationWithoutProfile() {
        val first = MealLogRepository(context, name)
        first.setRemindersEnabled(false)
        assertFalse(first.remindersEnabled.value)
        assertFalse(MealLogRepository(context, name).remindersEnabled.value)
        assertEquals(null, first.profile.value)
    }

    @Test fun reminderCanBeReenabledAndPersisted() {
        val first = MealLogRepository(context, name)
        first.setRemindersEnabled(false)
        first.setRemindersEnabled(true)
        assertTrue(MealLogRepository(context, name).remindersEnabled.value)
    }

    @Test fun customGoalsUpdateAndPersistImmediately() {
        val goals = DailyGoals(2000, 123, 280, 65)
        val first = MealLogRepository(context, name)
        first.saveGoals(goals)
        assertEquals(goals, first.goals.value)
        assertEquals(goals, MealLogRepository(context, name).goals.value)
    }
}
