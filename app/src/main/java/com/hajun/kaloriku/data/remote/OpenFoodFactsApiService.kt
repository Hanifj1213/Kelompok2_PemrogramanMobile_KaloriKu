package com.hajun.kaloriku.data.remote

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Endpoint Open Food Facts. Mengembalikan [ResponseBody] mentah supaya
 * `ProductParser.parseProduct()` bisa memakai org.json seperti sebelumnya.
 */
interface OpenFoodFactsApiService {

    @GET("api/v2/product/{barcode}.json")
    suspend fun product(
        @Path("barcode") barcode: String,
        @Query("fields") fields: String = DEFAULT_FIELDS
    ): ResponseBody

    companion object {
        const val DEFAULT_FIELDS = "product_name,brands,nutriments,serving_quantity,quantity"
    }
}
