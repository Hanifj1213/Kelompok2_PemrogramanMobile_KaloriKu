package com.hajun.kaloriku.data

import com.hajun.kaloriku.data.remote.OpenFoodFactsApiService

/**
 * Mencari data gizi makanan kemasan lewat barcode di Open Food Facts.
 * Produk yang tidak ada dibalas HTTP 200 dengan "status": 0, bukan 404.
 */
class ProductRepository(
    private val api: OpenFoodFactsApiService
) {
    private val messages = ErrorMessages.openFoodFacts()

    suspend fun lookup(barcode: String): NetworkResult<FoodItem> =
        safeApiCall(messages) {
            val body = api.product(barcode).string()
            ProductParser.parseProduct(barcode, body)
        }
}
