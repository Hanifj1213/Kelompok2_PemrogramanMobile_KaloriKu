package com.hajun.kaloriku

import android.app.Application
import com.hajun.kaloriku.data.AiClient
import com.hajun.kaloriku.data.BarcodeClient
import com.hajun.kaloriku.data.FoodDatabase
import com.hajun.kaloriku.data.MealLogRepository

/**
 * Menyimpan objek yang dipakai bersama seluruh aplikasi supaya semua layar
 * membaca data yang sama.
 */
class AppContainer(application: Application) {
    val repository = MealLogRepository(application)
    val ai = AiClient(BuildConfig.AI_BASE_URL, BuildConfig.AI_API_KEY, BuildConfig.AI_MODEL)
    val barcode = BarcodeClient()

    init {
        FoodDatabase.ensureLoaded(application)
    }
}

class KaloriKuApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
