package com.hajun.kaloriku

import android.app.Application
import com.hajun.kaloriku.data.FoodAnalysisRepository
import com.hajun.kaloriku.data.FoodDatabase
import com.hajun.kaloriku.data.MealLogRepository
import com.hajun.kaloriku.data.ProductRepository
import com.hajun.kaloriku.data.remote.NetworkModule

/**
 * Menyimpan objek yang dipakai bersama seluruh aplikasi supaya semua layar
 * membaca data yang sama. Retrofit/OkHttp dibangun di sini dari nilai [BuildConfig].
 */
class AppContainer(application: Application) {
    val repository = MealLogRepository(application)

    // Logging HTTP hanya dipasang di build debug, level BASIC.
    private val logging = if (BuildConfig.DEBUG) NetworkModule.debugLoggingInterceptor() else null

    private val aiBaseUrl = BuildConfig.AI_BASE_URL
    private val aiService = NetworkModule.createAiService(
        baseUrl = aiBaseUrl,
        client = NetworkModule.createAiClient(apiKey = BuildConfig.AI_API_KEY, logging = logging)
    )

    private val offService = NetworkModule.createOpenFoodFactsService(
        baseUrl = NetworkModule.OFF_BASE_URL,
        client = NetworkModule.createOpenFoodFactsClient(logging = logging)
    )

    val ai = FoodAnalysisRepository(aiService, aiBaseUrl, BuildConfig.AI_MODEL)
    val barcode = ProductRepository(offService)

    init {
        FoodDatabase.ensureLoaded(application)
    }
}

class KaloriKuApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
