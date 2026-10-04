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
}
