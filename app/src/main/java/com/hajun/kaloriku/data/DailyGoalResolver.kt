package com.hajun.kaloriku.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

fun resolveDailyGoals(profile: Profile?, savedGoals: DailyGoals?): DailyGoals {
    val calories = profile?.let(CalorieCalculator::dailyTarget) ?: CalorieCalculator.DEFAULT_TARGET
    return savedGoals?.copy(calories = calories) ?: DailyGoals.fromCalories(calories)
}

fun dailyGoalsFlow(profiles: Flow<Profile?>, savedGoals: Flow<DailyGoals?>): Flow<DailyGoals> =
    combine(profiles, savedGoals, ::resolveDailyGoals).distinctUntilChanged()
