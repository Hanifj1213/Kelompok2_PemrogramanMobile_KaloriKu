package com.hajun.kaloriku.data

import org.json.JSONException
import org.json.JSONObject

/** Konversi data target gizi ke/dari JSON untuk disimpan lokal. */
object ExtraJson {

    fun goalsToJson(goals: DailyGoals): String = JSONObject()
        .put("calories", goals.calories)
        .put("protein", goals.proteinG)
        .put("carbs", goals.carbsG)
        .put("fat", goals.fatG)
        .toString()

    fun goalsFromJson(json: String?): DailyGoals? {
        if (json.isNullOrBlank()) return null
        return try {
            val obj = JSONObject(json)
            DailyGoals(
                calories = obj.getInt("calories"),
                proteinG = obj.getInt("protein"),
                carbsG = obj.getInt("carbs"),
                fatG = obj.getInt("fat")
            )
        } catch (_: JSONException) {
            null
        }
    }

    fun reminderTimesToJson(times: ReminderTimes): String = JSONObject()
        .put("b_h", times.breakfastHour)
        .put("b_m", times.breakfastMinute)
        .put("l_h", times.lunchHour)
        .put("l_m", times.lunchMinute)
        .put("d_h", times.dinnerHour)
        .put("d_m", times.dinnerMinute)
        .toString()

    fun reminderTimesFromJson(json: String?): ReminderTimes {
        if (json.isNullOrBlank()) return ReminderTimes()
        return try {
            val obj = JSONObject(json)
            ReminderTimes(
                breakfastHour = obj.optInt("b_h", 8),
                breakfastMinute = obj.optInt("b_m", 0),
                lunchHour = obj.optInt("l_h", 13),
                lunchMinute = obj.optInt("l_m", 0),
                dinnerHour = obj.optInt("d_h", 19),
                dinnerMinute = obj.optInt("d_m", 0)
            )
        } catch (_: JSONException) {
            ReminderTimes()
        }
    }
}
