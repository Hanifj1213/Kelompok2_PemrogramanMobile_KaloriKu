package com.hajun.kaloriku.data

import com.hajun.kaloriku.data.remote.AiApiService
import com.hajun.kaloriku.data.remote.ChatMessage
import com.hajun.kaloriku.data.remote.ChatRequest
import com.hajun.kaloriku.data.remote.ContentPart
import com.hajun.kaloriku.data.remote.ImageUrl
import java.util.Base64

/**
 * Mengirim foto makanan ke server AI dan mengembalikan hasil analisis.
 * Semua kegagalan jaringan/parsing dipetakan ke [NetworkResult.Error] dengan pesan Indonesia.
 */
class FoodAnalysisRepository(
    private val api: AiApiService,
    private val baseUrl: String,
    private val model: String
) {
    private val messages = ErrorMessages.ai(baseUrl, model)

    suspend fun analyzeFood(jpeg: ByteArray): NetworkResult<AnalysisResult> {
        if (baseUrl.isBlank()) {
            return NetworkResult.Error(messages.blankBaseUrl)
        }
        return safeApiCall(messages) {
            val response = api.chat(buildRequest(jpeg))
            val content = response.choices.firstOrNull()?.message?.content
            // AiParser.parseAnalysis menangani code fence markdown dari Gemini.
            AiParser.parseAnalysis(AiContent.contentToText(content))
        }
    }

    private fun buildRequest(jpeg: ByteArray): ChatRequest {
        val dataUrl = "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(jpeg)
        val content = listOf(
            ContentPart(type = "text", text = PROMPT),
            ContentPart(type = "image_url", imageUrl = ImageUrl(dataUrl))
        )
        return ChatRequest(
            model = model,
            messages = listOf(ChatMessage(role = "user", content = content))
        )
    }

    companion object {
        /** Prompt tetap sama seperti yang dipakai klien HTTP lama. */
        val PROMPT = """
            Kamu adalah ahli gizi. Identifikasi setiap makanan dan minuman yang terlihat di foto ini.
            Untuk setiap item, perkirakan berat porsi yang terlihat dalam gram, lalu hitung kalori,
            protein, karbohidrat, dan lemaknya berdasarkan data gizi standar. Untuk makanan Indonesia,
            utamakan Tabel Komposisi Pangan Indonesia (TKPI). Pisahkan komponen yang berbeda
            (misalnya nasi putih, ayam goreng, sambal) menjadi item tersendiri.
            Gunakan nama makanan dalam Bahasa Indonesia.
            Jika foto tidak berisi makanan atau minuman, isi "is_food" dengan false dan "items" dengan array kosong.
            Balas HANYA dengan JSON dengan format berikut:
            {
              "is_food": true,
              "items": [
                {"name": "Nasi putih", "grams": 150, "calories": 195, "protein_g": 4.1, "carbs_g": 42.3, "fat_g": 0.3}
              ],
              "note": "Catatan singkat dalam Bahasa Indonesia, misalnya asumsi porsi atau saran gizi"
            }
        """.trimIndent()
    }
}
