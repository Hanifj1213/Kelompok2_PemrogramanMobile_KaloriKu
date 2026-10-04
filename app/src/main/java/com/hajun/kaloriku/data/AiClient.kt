package com.hajun.kaloriku.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.util.Base64

/**
 * Mengirim foto makanan ke server AI yang menyediakan endpoint OpenAI-compatible
 * (`POST {baseUrl}/chat/completions`), misalnya 9router yang dijalankan di komputer sendiri.
 */
class AiClient(
    private val baseUrl: String,
    private val apiKey: String,
    private val model: String
) {

    suspend fun analyzeFood(jpeg: ByteArray): AnalysisResult = withContext(Dispatchers.IO) {
        if (baseUrl.isBlank()) {
            throw AiException("Base URL server AI belum diisi. Periksa AI_BASE_URL di local.properties.")
        }

        val connection = URL("${baseUrl.trimEnd('/')}/chat/completions").openConnection() as HttpURLConnection
        connection.executeCancellable {
            try {
                connection.requestMethod = "POST"
                connection.connectTimeout = 15_000
                connection.readTimeout = 120_000
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                if (apiKey.isNotBlank()) connection.setRequestProperty("Authorization", "Bearer $apiKey")
                connection.outputStream.use { it.write(buildRequestBody(jpeg).toByteArray(Charsets.UTF_8)) }
    
                val code = connection.responseCode
                val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
    
                if (code !in 200..299) throw AiException(httpErrorMessage(code, body))
                AiParser.parseAnalysis(AiParser.extractText(body))
            } catch (e: SocketTimeoutException) {
                throw AiException(
                    "Server AI terlalu lama merespons. Pastikan server lokal (misalnya 9router) menyala, " +
                        "lalu coba lagi.",
                    e
                )
            } catch (e: IOException) {
                throw AiException(
                    "Gagal terhubung ke server AI di $baseUrl. Periksa koneksi dan pastikan server menyala.",
                    e
                )
            }
        }
    }

    private fun buildRequestBody(jpeg: ByteArray): String {
        val dataUrl = "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(jpeg)
        val content = JSONArray()
            .put(JSONObject().put("type", "text").put("text", PROMPT))
            .put(JSONObject().put("type", "image_url").put("image_url", JSONObject().put("url", dataUrl)))
        val message = JSONObject().put("role", "user").put("content", content)
        return JSONObject()
            .put("model", model)
            .put("stream", false)
            .put("temperature", 0.2)
            .put("messages", JSONArray().put(message))
            .toString()
    }

    private fun httpErrorMessage(code: Int, body: String): String {
        val detail = AiParser.extractErrorMessage(body)
        return when (code) {
            400 -> "Permintaan ditolak server AI" + (detail?.let { ": $it" } ?: ".")
            401, 403 -> "API key tidak valid atau tidak punya akses. Periksa AI_API_KEY di local.properties."
            404 -> "Endpoint atau model \"$model\" tidak ditemukan. Periksa AI_BASE_URL dan AI_MODEL."
            429 -> "Terlalu banyak permintaan atau kuota habis. Tunggu sebentar lalu coba lagi."
            in 500..599 -> "Server AI sedang bermasalah ($code). Coba lagi nanti."
            else -> "Terjadi kesalahan ($code)" + (detail?.let { ": $it" } ?: ".")
        }
    }

    private companion object {
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
