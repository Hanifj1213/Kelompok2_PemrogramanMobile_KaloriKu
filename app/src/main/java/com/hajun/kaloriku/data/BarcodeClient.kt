package com.hajun.kaloriku.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL

/**
 * Mencari data gizi makanan kemasan lewat barcode di Open Food Facts.
 * Layanan ini gratis dan tidak memerlukan API key.
 */
class BarcodeClient(private val baseUrl: String = DEFAULT_BASE_URL) {

    suspend fun lookup(barcode: String): FoodItem = withContext(Dispatchers.IO) {
        val url = "$baseUrl/api/v2/product/$barcode.json?fields=product_name,brands,nutriments,serving_quantity,quantity"
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.executeCancellable {
            try {
                connection.requestMethod = "GET"
                connection.connectTimeout = 15_000
                connection.readTimeout = 30_000
                connection.setRequestProperty("User-Agent", "KaloriKu/1.0 (Android; tugas kuliah)")
    
                val code = connection.responseCode
                if (code !in 200..299) {
                    throw AiException("Gagal menghubungi Open Food Facts ($code). Coba lagi nanti.")
                }
                val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                parseProduct(barcode, body)
            } catch (e: SocketTimeoutException) {
                throw AiException("Open Food Facts terlalu lama merespons. Coba lagi.", e)
            } catch (e: IOException) {
                throw AiException("Gagal terhubung ke Open Food Facts. Periksa koneksi internet.", e)
            }
        }
    }

    companion object {
        const val DEFAULT_BASE_URL = "https://world.openfoodfacts.org"

        /**
         * Membaca produk dari respons Open Food Facts. Dipisah dari [lookup]
         * supaya bisa diuji tanpa jaringan.
         */
        fun parseProduct(barcode: String, body: String): FoodItem {
            val root = try {
                JSONObject(body)
            } catch (e: Exception) {
                throw AiException("Respons Open Food Facts tidak bisa dibaca.", e)
            }

            if (root.optInt("status", 1) == 0) {
                throw AiException("Produk dengan barcode $barcode belum ada di Open Food Facts.")
            }
            val product = root.optJSONObject("product")
                ?: throw AiException("Produk dengan barcode $barcode belum ada di Open Food Facts.")

            val name = product.optString("product_name").trim().ifEmpty {
                product.optString("generic_name").trim()
            }.ifEmpty { "Produk $barcode" }
            val brand = product.optString("brands").trim()

            val nutriments = product.optJSONObject("nutriments")
                ?: throw AiException("Produk ini tidak mencantumkan data gizi.")

            // Utamakan nilai per 100 g. Kalau tidak ada, pakai nilai per sajian.
            val per100 = nutriments.optDouble("energy-kcal_100g", Double.NaN)
            val servingKcal = nutriments.optDouble("energy-kcal_serving", Double.NaN)
            val hasPer100 = per100.isFinite()

            val calories = when {
                hasPer100 -> per100
                servingKcal.isFinite() -> servingKcal
                else -> throw AiException("Produk ini tidak mencantumkan angka kalori.")
            }
            val protein = pick(nutriments, "proteins", hasPer100)
            val carbs = pick(nutriments, "carbohydrates", hasPer100)
            val fat = pick(nutriments, "fat", hasPer100)

            val servingGrams = product.optString("serving_quantity").toDoubleOrNull()
            val grams = when {
                servingGrams != null && servingGrams > 0 -> servingGrams
                hasPer100 -> 100.0
                else -> 100.0
            }

            // Kalau sumbernya per sajian, nilai sudah sesuai berat sajian.
            val ratio = if (hasPer100 && servingGrams != null && servingGrams > 0) servingGrams / 100.0 else 1.0

            val displayName = if (brand.isEmpty()) name else "$name ($brand)"
            return FoodItem(
                name = displayName,
                grams = grams,
                calories = calories * ratio,
                proteinG = protein * ratio,
                carbsG = carbs * ratio,
                fatG = fat * ratio
            )
        }

        private fun pick(nutriments: JSONObject, key: String, per100: Boolean): Double {
            val suffix = if (per100) "100g" else "serving"
            return nutriments.optDouble("${key}_$suffix", Double.NaN)
                .takeIf { it.isFinite() }
                ?: nutriments.optDouble("${key}_100g", Double.NaN).takeIf { it.isFinite() }
                ?: 0.0
        }
    }
}
