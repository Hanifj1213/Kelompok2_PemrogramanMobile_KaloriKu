package com.hajun.kaloriku.data.remote

import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import okhttp3.MediaType.Companion.toMediaType

/**
 * Membangun Retrofit, OkHttp, dan [Json] yang dipakai aplikasi maupun unit test.
 *
 * Semua parameter (base URL, API key, timeout, logger) bisa disuntik, sehingga test
 * bisa memakai MockWebServer dan tidak bergantung pada [com.hajun.kaloriku.BuildConfig].
 */
object NetworkModule {

    private const val USER_AGENT = "KaloriKu/1.0 (Android)"
    private const val JSON_MEDIA_TYPE = "application/json; charset=utf-8"

    /**
     * Satu instance Json untuk semua DTO.
     * - `ignoreUnknownKeys` supaya medan baru dari server tidak memecah parsing.
     * - `encodeDefaults` wajib: tanpa ini `stream = false` tidak ikut terkirim.
     * - `explicitNulls = false` supaya DTO yang null tidak dikirim sebagai `"x": null`.
     */
    val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    /** Menambahkan '/' di akhir base URL kalau belum ada. Retrofit mensyaratkannya. */
    fun normalizeBaseUrl(url: String): String {
        val trimmed = url.trim()
        return if (trimmed.endsWith("/")) trimmed else "$trimmed/"
    }

    /**
     * Klien HTTP untuk server AI: connect 15 dtk, read 120 dtk (analisis foto bisa lama).
     * Header Authorization hanya ditambahkan kalau API key tidak kosong.
     */
    fun createAiClient(
        apiKey: String = "",
        connectTimeoutSeconds: Long = 15,
        readTimeoutSeconds: Long = 120,
        logging: HttpLoggingInterceptor? = null
    ): OkHttpClient = baseBuilder(connectTimeoutSeconds, readTimeoutSeconds, logging)
        .addInterceptor { chain ->
            val builder = chain.request().newBuilder()
                .header("User-Agent", USER_AGENT)
            if (apiKey.isNotBlank()) builder.header("Authorization", "Bearer $apiKey")
            chain.proceed(builder.build())
        }
        .build()

    /** Interceptor logging hanya untuk build debug, level BASIC (jangan BODY karena foto base64 besar). */
    fun debugLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
            redactHeader("Authorization")
        }

    fun createAiService(baseUrl: String, client: OkHttpClient): AiApiService =
        retrofit(baseUrl, client).create(AiApiService::class.java)

    private fun baseBuilder(
        connectTimeoutSeconds: Long,
        readTimeoutSeconds: Long,
        logging: HttpLoggingInterceptor?
    ): OkHttpClient.Builder {
        val builder = OkHttpClient.Builder()
            .connectTimeout(connectTimeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(readTimeoutSeconds, TimeUnit.SECONDS)
            .writeTimeout(readTimeoutSeconds, TimeUnit.SECONDS)
        if (logging != null) builder.addInterceptor(logging)
        return builder
    }

    private fun retrofit(baseUrl: String, client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(normalizeBaseUrl(baseUrl))
            .client(client)
            .addConverterFactory(json.asConverterFactory(JSON_MEDIA_TYPE.toMediaType()))
            .build()
}
