package com.hajun.kaloriku.data

import java.io.IOException
import java.net.SocketTimeoutException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import retrofit2.HttpException

/** Hasil panggilan jaringan: sukses berisi data, gagal berisi pesan siap tampil. */
sealed interface NetworkResult<out T> {
    data class Success<T>(val data: T) : NetworkResult<T>
    data class Error(val message: String, val code: Int? = null) : NetworkResult<Nothing>
}

internal fun parseErrorDetails(body: String?): String? =
    body?.takeIf { it.isNotBlank() }?.let { AiParser.extractErrorMessage(it) }

/**
 * Pembungkus panggilan jaringan yang memetakan setiap kegagalan ke pesan Indonesia
 * yang sama dengan yang dulu dipakai klien HTTP lama.
 *
 * @param messages pemetaan pesan per-endpoint (kode HTTP, pesan detail opsional).
 * @param block blok yang menjalankan panggilan Retrofit.
 */
suspend fun <T> safeApiCall(
    messages: ErrorMessages,
    block: suspend () -> T
): NetworkResult<T> = try {
    NetworkResult.Success(block())
} catch (e: CancellationException) {
    // Pembatalan coroutine harus dilempar ulang, bukan diubah jadi Error,
    // supaya "keluar dari layar = permintaan dibatalkan" tetap berlaku.
    throw e
} catch (e: SocketTimeoutException) {
    NetworkResult.Error(messages.timeout, null)
} catch (e: HttpException) {
    val code = e.code()
    val body = try {
        e.response()?.errorBody()?.string()
    } catch (_: IOException) {
        null
    }
    NetworkResult.Error(messages.http(code, parseErrorDetails(body)), code)
} catch (e: SerializationException) {
    NetworkResult.Error(messages.unreadable, null)
} catch (e: IllegalArgumentException) {
    NetworkResult.Error(messages.unreadable)
} catch (e: IOException) {
    NetworkResult.Error(messages.io, null)
} catch (e: AiException) {
    NetworkResult.Error(e.message ?: messages.unreadable, null)
}

/** Pesan error per layanan, supaya tidak ada teks Indonesia yang terduplikasi. */
data class ErrorMessages(
    val blankBaseUrl: String,
    val timeout: String,
    val io: String,
    val unreadable: String,
    val http: (code: Int, detail: String?) -> String
) {
    companion object {
        /** Pesan untuk server AI (OpenAI-compatible). */
        fun ai(baseUrl: String, model: String): ErrorMessages = ErrorMessages(
            blankBaseUrl = "Base URL server AI belum diisi. Periksa AI_BASE_URL di local.properties.",
            timeout = "Server AI terlalu lama merespons. Pastikan server lokal (misalnya 9router) " +
                "menyala, lalu coba lagi.",
            io = "Gagal terhubung ke server AI di $baseUrl. Periksa koneksi dan pastikan server menyala.",
            unreadable = "Respons AI tidak bisa dibaca. Coba lagi.",
            http = { code, detail ->
                when (code) {
                    400 -> "Permintaan ditolak server AI" + (detail?.let { ": $it" } ?: ".")
                    401, 403 -> "API key tidak valid atau tidak punya akses. Periksa AI_API_KEY di local.properties."
                    404 -> "Endpoint atau model \"$model\" tidak ditemukan. Periksa AI_BASE_URL dan AI_MODEL."
                    429 -> "Terlalu banyak permintaan atau kuota habis. Tunggu sebentar lalu coba lagi."
                    in 500..599 -> "Server AI sedang bermasalah ($code). Coba lagi nanti."
                    else -> "Terjadi kesalahan ($code)" + (detail?.let { ": $it" } ?: ".")
                }
            }
        )

        /** Pesan untuk Open Food Facts. */
        fun openFoodFacts(): ErrorMessages = ErrorMessages(
            blankBaseUrl = "Alamat Open Food Facts belum diisi.",
            timeout = "Open Food Facts terlalu lama merespons. Coba lagi.",
            io = "Gagal terhubung ke Open Food Facts. Periksa koneksi internet.",
            unreadable = "Respons Open Food Facts tidak bisa dibaca.",
            http = { code, _ -> "Gagal menghubungi Open Food Facts ($code). Coba lagi nanti." }
        )
    }
}
