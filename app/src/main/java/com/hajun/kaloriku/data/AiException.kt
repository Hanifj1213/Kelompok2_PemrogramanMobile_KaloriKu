package com.hajun.kaloriku.data

/**
 * Kesalahan tingkat domain saat memanggil layanan jaringan (server AI atau Open Food Facts).
 * Pesan di dalamnya sudah siap tampil ke pengguna (Bahasa Indonesia).
 */
class AiException(message: String, cause: Throwable? = null) : Exception(message, cause)
