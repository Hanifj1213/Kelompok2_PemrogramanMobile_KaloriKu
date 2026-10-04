package com.hajun.kaloriku.data

import org.json.JSONException
import org.json.JSONObject

class AiException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** Membaca jawaban server AI yang memakai format OpenAI-compatible. */
object AiParser {

    /** Mengambil teks jawaban dari body respons `chat/completions`. */
    fun extractText(responseBody: String): String {
        val root = parseObject(responseBody)
        val choices = root.optJSONArray("choices")
        if (choices == null || choices.length() == 0) {
            throw AiException("AI tidak memberikan jawaban. Coba lagi.")
        }
        val message = choices.optJSONObject(0)?.optJSONObject("message")
            ?: throw AiException("AI tidak memberikan jawaban. Coba lagi.")

        val text = when (val content = message.opt("content")) {
            is String -> content
            is org.json.JSONArray -> buildString {
                for (i in 0 until content.length()) {
                    val part = content.optJSONObject(i) ?: continue
                    // Lewati bagian "reasoning", ambil teks jawaban final saja.
                    if (part.optString("type") == "reasoning") continue
                    append(part.optString("text"))
                }
            }
            else -> ""
        }
        if (text.isBlank()) throw AiException("AI tidak memberikan jawaban. Coba lagi.")
        return text
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

    /** Mengambil pesan error dari body respons gagal, misalnya `{"error":{"message":"..."}}`. */
    fun extractErrorMessage(responseBody: String): String? = try {
        JSONObject(responseBody).optJSONObject("error")?.optString("message")?.takeIf { it.isNotBlank() }
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
