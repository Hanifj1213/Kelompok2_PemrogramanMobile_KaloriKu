package com.hajun.kaloriku.data

import com.hajun.kaloriku.data.remote.NetworkModule
import kotlinx.serialization.json.JsonPrimitive
import org.json.JSONException
import org.json.JSONObject

/** Membaca jawaban server AI yang memakai format OpenAI-compatible. */
object AiParser {

    /** Mengambil teks jawaban dari body respons `chat/completions`. */
    fun extractText(responseBody: String): String {
        val root = parseObject(responseBody)
        val choices = root.optJSONArray("choices")
        if (choices == null || choices.length() == 0) {
            throw AiException(AiContent.EMPTY_MESSAGE)
        }
        val message = choices.optJSONObject(0)?.optJSONObject("message")
            ?: throw AiException(AiContent.EMPTY_MESSAGE)

        // Logika string/array (lewati bagian reasoning) dipakai bersama dengan jalur DTO.
        val content = when (val raw = message.opt("content")) {
            is String -> JsonPrimitive(raw)
            is org.json.JSONArray -> NetworkModule.json.parseToJsonElement(raw.toString())
            else -> null
        }
        return AiContent.contentToText(content)
    }

    /** Mengubah teks JSON dari model menjadi [AnalysisResult]. */
    fun parseAnalysis(text: String): AnalysisResult {
        val json = parseObject(extractJsonObject(text))
        val array = json.optJSONArray("items")
        val items = buildList {
            if (array != null) {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val name = obj.optString("name").trim()
                    if (name.isEmpty()) continue
                    add(
                        FoodItem(
                            name = name,
                            grams = obj.number("grams"),
                            calories = obj.number("calories"),
                            proteinG = obj.number("protein_g"),
                            carbsG = obj.number("carbs_g"),
                            fatG = obj.number("fat_g")
                        )
                    )
                }
            }
        }
        return AnalysisResult(
            isFood = json.optBoolean("is_food", items.isNotEmpty()) && items.isNotEmpty(),
            items = items,
            note = json.optString("note").trim()
        )
    }

    /**
     * Mengambil pesan error dari body respons gagal. Mendukung dua bentuk:
     * `{"error":{"message":"..."}}` dan `{"error":"..."}`.
     */
    fun extractErrorMessage(responseBody: String): String? = try {
        val error = JSONObject(responseBody).opt("error")
        when (error) {
            is JSONObject -> error.optString("message").takeIf { it.isNotBlank() }
            is String -> error.takeIf { it.isNotBlank() }
            else -> null
        }
    } catch (_: JSONException) {
        null
    }

    private fun extractJsonObject(text: String): String {
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        if (start == -1 || end <= start) throw AiException("Respons AI tidak bisa dibaca. Coba lagi.")
        return text.substring(start, end + 1)
    }

    private fun parseObject(text: String): JSONObject = try {
        JSONObject(text)
    } catch (e: JSONException) {
        throw AiException("Respons AI tidak bisa dibaca. Coba lagi.", e)
    }

    private fun JSONObject.number(key: String): Double =
        optDouble(key, 0.0).takeIf { it.isFinite() && it >= 0 } ?: 0.0
}
