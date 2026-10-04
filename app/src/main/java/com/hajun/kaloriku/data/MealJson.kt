package com.hajun.kaloriku.data

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** Konversi data aplikasi ke/dari JSON untuk disimpan di SharedPreferences. */
object MealJson {

    fun entriesToJson(entries: List<MealEntry>): String {
        val array = JSONArray()
        entries.forEach { entry ->
            val items = JSONArray()
            entry.items.forEach { item ->
                items.put(
                    JSONObject()
                        .put("name", item.name)
                        .put("grams", item.grams)
                        .put("calories", item.calories)
                        .put("protein_g", item.proteinG)
                        .put("carbs_g", item.carbsG)
                        .put("fat_g", item.fatG)
                )
            }
            array.put(
                JSONObject()
                    .put("id", entry.id)
                    .put("timestamp", entry.timestamp)
                    .put("meal_type", entry.mealType.name)
                    .put("items", items)
            )
        }
        return array.toString()
    }

    fun entriesFromJson(json: String?): List<MealEntry> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(json)
            (0 until array.length()).mapNotNull { i ->
                val obj = array.optJSONObject(i) ?: return@mapNotNull null
                val itemsArray = obj.optJSONArray("items") ?: JSONArray()
                MealEntry(
                    id = obj.getLong("id"),
                    timestamp = obj.getLong("timestamp"),
                    mealType = enumValueOrDefault(obj.optString("meal_type"), MealType.CAMILAN),
                    items = (0 until itemsArray.length()).mapNotNull { j ->
                        itemsArray.optJSONObject(j)?.let { item ->
                            FoodItem(
                                name = item.optString("name"),
                                grams = item.optDouble("grams", 0.0),
                                calories = item.optDouble("calories", 0.0),
                                proteinG = item.optDouble("protein_g", 0.0),
                                carbsG = item.optDouble("carbs_g", 0.0),
                                fatG = item.optDouble("fat_g", 0.0)
                            )
                        }
                    }
                )
            }
        } catch (_: JSONException) {
            emptyList()
        }
    }

    fun profileToJson(profile: Profile): String = JSONObject()
        .put("gender", profile.gender.name)
        .put("age", profile.ageYears)
        .put("weight", profile.weightKg)
        .put("height", profile.heightCm)
        .put("activity", profile.activity.name)
        .put("goal", profile.goal.name)
        .toString()

    fun profileFromJson(json: String?): Profile? {
        if (json.isNullOrBlank()) return null
        return try {
            val obj = JSONObject(json)
            Profile(
                gender = enumValueOrDefault(obj.optString("gender"), Gender.PRIA),
                ageYears = obj.getInt("age"),
                weightKg = obj.getDouble("weight"),
                heightCm = obj.getDouble("height"),
                activity = enumValueOrDefault(obj.optString("activity"), ActivityLevel.SEDANG),
                goal = enumValueOrDefault(obj.optString("goal"), Goal.JAGA)
            )
        } catch (_: JSONException) {
            null
        }
    }

    private inline fun <reified T : Enum<T>> enumValueOrDefault(name: String, default: T): T =
        enumValues<T>().firstOrNull { it.name == name } ?: default
}
