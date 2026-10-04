package com.hajun.kaloriku.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import com.hajun.kaloriku.util.toLocalDate
import java.time.LocalDate

/** Menyimpan seluruh data pengguna secara lokal di HP. */
class MealLogRepository(context: Context, preferencesName: String = PREFS_NAME) {

    private val prefs = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)

    private val _entries = MutableStateFlow(MealJson.entriesFromJson(prefs.getString(KEY_ENTRIES, null)))
    val entries: StateFlow<List<MealEntry>> = _entries.asStateFlow()

    private val _profile = MutableStateFlow(MealJson.profileFromJson(prefs.getString(KEY_PROFILE, null)))
    val profile: StateFlow<Profile?> = _profile.asStateFlow()

    private val _waterEntries = MutableStateFlow(ExtraJson.waterFromJson(prefs.getString(KEY_WATER, null)))
    val waterEntries: StateFlow<List<WaterEntry>> = _waterEntries.asStateFlow()

    private val _weightEntries = MutableStateFlow(ExtraJson.weightsFromJson(prefs.getString(KEY_WEIGHTS, null)))
    val weightEntries: StateFlow<List<WeightEntry>> = _weightEntries.asStateFlow()

    private val _goals = MutableStateFlow(ExtraJson.goalsFromJson(prefs.getString(KEY_GOALS, null)))
    val goals: StateFlow<DailyGoals?> = _goals.asStateFlow()

    private val _remindersEnabled = MutableStateFlow(prefs.getBoolean(KEY_REMINDERS_ENABLED, true))
    val remindersEnabled: StateFlow<Boolean> = _remindersEnabled.asStateFlow()

    fun setRemindersEnabled(enabled: Boolean) {
        _remindersEnabled.value = enabled
        prefs.edit().putBoolean(KEY_REMINDERS_ENABLED, enabled).apply()
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

    fun addWater(timestamp: Long = System.currentTimeMillis(), glasses: Int = 1) {
        _waterEntries.update { it + WaterEntry(id = timestamp, timestamp = timestamp, glasses = glasses) }
        prefs.edit().putString(KEY_WATER, ExtraJson.waterToJson(_waterEntries.value)).apply()
    }

    /** Mengurangi satu gelas terakhir pada hari tertentu. */
    fun removeLastWater(date: LocalDate) {
        val lastToday = _waterEntries.value
            .filter { it.timestamp.toLocalDate() == date }
            .maxByOrNull { it.timestamp } ?: return
        _waterEntries.update { list -> list.filterNot { it.id == lastToday.id } }
        prefs.edit().putString(KEY_WATER, ExtraJson.waterToJson(_waterEntries.value)).apply()
    }

    /** Menyimpan berat badan. Satu tanggal hanya punya satu catatan (yang terbaru menimpa). */
    fun saveWeight(date: LocalDate, weightKg: Double) {
        _weightEntries.update { list ->
            (list.filterNot { it.date == date } + WeightEntry(date, weightKg)).sortedBy { it.date }
        }
        prefs.edit().putString(KEY_WEIGHTS, ExtraJson.weightsToJson(_weightEntries.value)).apply()
    }

    fun deleteWeight(date: LocalDate) {
        _weightEntries.update { list -> list.filterNot { it.date == date } }
        prefs.edit().putString(KEY_WEIGHTS, ExtraJson.weightsToJson(_weightEntries.value)).apply()
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
        const val KEY_WATER = "water"
        const val KEY_WEIGHTS = "weights"
        const val KEY_GOALS = "goals"
        const val KEY_REMINDERS_ENABLED = "reminders_enabled"
    }
}
