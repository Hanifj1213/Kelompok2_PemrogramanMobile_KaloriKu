package com.hajun.kaloriku.data

import org.json.JSONObject

/**
 * Membaca produk dari respons JSON Open Food Facts.
 * Dipindah dari BarcodeClient agar bisa diuji tanpa jaringan.
 */
object ProductParser {

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
