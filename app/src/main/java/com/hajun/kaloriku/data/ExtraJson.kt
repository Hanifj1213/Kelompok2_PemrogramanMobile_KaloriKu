package com.hajun.kaloriku.data

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.time.LocalDate

/** Konversi data tambahan (air, berat, target) ke/dari JSON untuk disimpan lokal. */
object ExtraJson {

    fun waterToJson(entries: List<WaterEntry>): String {
        val array = JSONArray()
        entries.forEach { entry ->
            array.put(
                JSONObject()
                    .put("id", entry.id)
                    .put("timestamp", entry.timestamp)
                    .put("glasses", entry.glasses)
            )
        }
        return array.toString()
    }

    fun waterFromJson(json: String?): List<WaterEntry> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(json)
            (0 until array.length()).mapNotNull { i ->
                array.optJSONObject(i)?.let { obj ->
                    WaterEntry(
                        id = obj.getLong("id"),
                        timestamp = obj.getLong("timestamp"),
                        glasses = obj.optInt("glasses", 1)
                    )
                }
            }
        } catch (_: JSONException) {
            emptyList()
        }
    }

    fun weightsToJson(entries: List<WeightEntry>): String {
        val array = JSONArray()
        entries.forEach { entry ->
            array.put(
                JSONObject()
                    .put("date", entry.date.toString())
                    .put("weight", entry.weightKg)
            )
        }
        return array.toString()
    }

    fun weightsFromJson(json: String?): List<WeightEntry> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(json)
            (0 until array.length()).mapNotNull { i ->
                array.optJSONObject(i)?.let { obj ->
                    val date = runCatching { LocalDate.parse(obj.getString("date")) }.getOrNull()
                        ?: return@mapNotNull null
                    WeightEntry(date, obj.getDouble("weight"))
                }
            }.sortedBy { it.date }
        } catch (_: JSONException) {
            emptyList()
        }
    }

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
