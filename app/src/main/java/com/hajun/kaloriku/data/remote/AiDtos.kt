package com.hajun.kaloriku.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * DTO untuk endpoint OpenAI-compatible `POST /chat/completions`.
 * Nama medan di JSON memakai snake_case, jadi dipetakan lewat [SerialName].
 */
@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    // encodeDefaults = true wajib: kalau `stream` tidak ikut terkirim, 9router
    // membalas text/event-stream, bukan JSON.
    val stream: Boolean = false,
    val temperature: Double = 0.2
)

@Serializable
data class ChatMessage(
    val role: String,
    val content: List<ContentPart>
)

/**
 * Satu bagian content dalam pesan. `type` bernilai `"text"` atau `"image_url"`.
 * Untuk bagian gambar dipakai medan [imageUrl]; untuk teks dipakai [text].
 */
@Serializable
data class ContentPart(
    val type: String,
    val text: String? = null,
    @SerialName("image_url") val imageUrl: ImageUrl? = null
)

@Serializable
data class ImageUrl(
    val url: String
)

@Serializable
data class ChatResponse(
    val choices: List<ChatChoice> = emptyList()
)

@Serializable
data class ChatChoice(
    val message: ChatResponseMessage? = null
)

@Serializable
data class ChatResponseMessage(
    val role: String? = null,
    // Bisa berupa string biasa atau array bagian (mis. reasoning + text), jadi mentah dulu.
    val content: JsonElement? = null
)
