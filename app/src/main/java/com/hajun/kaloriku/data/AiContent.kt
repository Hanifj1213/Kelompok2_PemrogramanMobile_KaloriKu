package com.hajun.kaloriku.data

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Logika bersama untuk mengubah bagian `content` dari jawaban AI menjadi teks.
 *
 * Aturannya: string -> string itu sendiri; array -> gabungkan `text` tiap bagian
 * sambil melewati bagian bertipe `"reasoning"`; null/kosong -> [AiException].
 * Hanya ada di satu tempat, dan dipakai oleh [AiParser.extractText] (jalur lama)
 * maupun repository DTO (jalur kotlinx.serialization).
 */
object AiContent {

    /** Pesan standar saat AI tidak mengembalikan jawaban yang bisa dibaca. */
    const val EMPTY_MESSAGE = "AI tidak memberikan jawaban. Coba lagi."

    /** Mengubah elemen content (string atau array bagian) menjadi teks jawaban. */
    fun contentToText(content: JsonElement?): String {
        val text = when (content) {
            null, JsonNull -> ""
            is JsonPrimitive -> if (content.isString) content.content else ""
            is JsonArray -> buildString {
                for (element in content) {
                    val part = element as? JsonObject ?: continue
                    // Lewati bagian "reasoning", ambil teks jawaban final saja.
                    if ((part["type"] as? JsonPrimitive)?.content == "reasoning") continue
                    append((part["text"] as? JsonPrimitive)?.content.orEmpty())
                }
            }
            else -> ""
        }
        if (text.isBlank()) throw AiException(EMPTY_MESSAGE)
        return text
    }
}
