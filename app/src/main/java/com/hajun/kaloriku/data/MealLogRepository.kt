package com.hajun.kaloriku.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Menyimpan seluruh data pengguna secara lokal di HP. */
class MealLogRepository(context: Context, preferencesName: String = PREFS_NAME) {

    private val prefs = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)

    private val _entries = MutableStateFlow(MealJson.entriesFromJson(prefs.getString(KEY_ENTRIES, null)))
    val entries: StateFlow<List<MealEntry>> = _entries.asStateFlow()

    private val _profile = MutableStateFlow(MealJson.profileFromJson(prefs.getString(KEY_PROFILE, null)))
    val profile: StateFlow<Profile?> = _profile.asStateFlow()

    private val _goals = MutableStateFlow(ExtraJson.goalsFromJson(prefs.getString(KEY_GOALS, null)))
    val goals: StateFlow<DailyGoals?> = _goals.asStateFlow()

    private val _remindersEnabled = MutableStateFlow(prefs.getBoolean(KEY_REMINDERS_ENABLED, true))
    val remindersEnabled: StateFlow<Boolean> = _remindersEnabled.asStateFlow()

    private val _reminderTimes = MutableStateFlow(ExtraJson.reminderTimesFromJson(prefs.getString(KEY_REMINDER_TIMES, null)))
    val reminderTimes: StateFlow<ReminderTimes> = _reminderTimes.asStateFlow()

    fun setRemindersEnabled(enabled: Boolean) {
        _remindersEnabled.value = enabled
        prefs.edit().putBoolean(KEY_REMINDERS_ENABLED, enabled).apply()
    }

    fun saveReminderTimes(times: ReminderTimes) {
        _reminderTimes.value = times
        prefs.edit().putString(KEY_REMINDER_TIMES, ExtraJson.reminderTimesToJson(times)).apply()
    }

    fun addEntry(entry: MealEntry) {
        _entries.update { it + entry }
        persistEntries()
    }

    fun deleteEntry(id: Long) {
        _entries.update { list -> list.filterNot { it.id == id } }
        persistEntries()
    }

    fun saveProfile(profile: Profile) {
        _profile.value = profile
        prefs.edit().putString(KEY_PROFILE, MealJson.profileToJson(profile)).apply()
    }

    fun saveGoals(goals: DailyGoals) {
        _goals.value = goals
        prefs.edit().putString(KEY_GOALS, ExtraJson.goalsToJson(goals)).apply()
    }

    private fun persistEntries() {
        prefs.edit().putString(KEY_ENTRIES, MealJson.entriesToJson(_entries.value)).apply()
    }

    private companion object {
        const val PREFS_NAME = "kaloriku"
        const val KEY_ENTRIES = "entries"
        const val KEY_PROFILE = "profile"

        /**
         * Kunci lama air minum dan berat badan. Fitur ini sudah dihapus, tetapi
         * kunci tetap disimpan supaya data lama tidak ikut terhapus dari perangkat.
         */
        @Suppress("unused")
        const val KEY_WATER = "water"
        @Suppress("unused")
        const val KEY_WEIGHTS = "weights"

        const val KEY_GOALS = "goals"
        const val KEY_REMINDERS_ENABLED = "reminders_enabled"
        const val KEY_REMINDER_TIMES = "reminder_times"
    }
}
