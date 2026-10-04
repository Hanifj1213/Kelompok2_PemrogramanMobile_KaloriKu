package com.hajun.kaloriku

import com.hajun.kaloriku.data.AiParser
import com.hajun.kaloriku.data.remote.AiApiService
import com.hajun.kaloriku.data.remote.ChatMessage
import com.hajun.kaloriku.data.remote.ChatRequest
import com.hajun.kaloriku.data.remote.ContentPart
import com.hajun.kaloriku.data.remote.ImageUrl
import com.hajun.kaloriku.data.remote.NetworkModule
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Menguji normalisasi base URL, konfigurasi serialisasi, dan bahwa base URL
 * tanpa '/' tetap menghasilkan permintaan yang berhasil.
 */
class NetworkModuleTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun normalizeBaseUrl_appendsSlashWhenMissing() {
        assertEquals("https://host/v1/", NetworkModule.normalizeBaseUrl("https://host/v1"))
    }

    @Test
    fun normalizeBaseUrl_keepsTrailingSlashAndTrims() {
        assertEquals("https://host/v1/", NetworkModule.normalizeBaseUrl("  https://host/v1/  "))
    }

    private fun emptyRequest() = ChatRequest(model = "m", messages = emptyList())

    @Test
    fun encodeDefaults_keepsStreamFalse_andDropsNullFields() {
        val request = ChatRequest(
            model = "m",
            messages = listOf(
                ChatMessage(
                    role = "user",
                    content = listOf(ContentPart(type = "image_url", imageUrl = ImageUrl("data:x")))
                )
            )
        )
        val json = NetworkModule.json.encodeToString(ChatRequest.serializer(), request)
        assertTrue("stream=false wajib terkirim", json.contains("\"stream\":false"))
        // explicitNulls = false: medan text null tidak ikut terkirim.
        assertFalse(json.contains("\"text\":null"))
    }

    @Test
    fun baseUrlWithoutTrailingSlash_stillSendsRequest() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody("{\"choices\":[{\"message\":{\"content\":\"halo\"}}]}")
        )
        val client = NetworkModule.createAiClient()
        // Sengaja tanpa '/' di akhir untuk menguji normalisasi.
        val baseUrl = server.url("/v1").toString()
        val service: AiApiService = NetworkModule.createAiService(baseUrl, client)
        service.chat(emptyRequest())
        assertTrue(server.takeRequest().path!!.endsWith("/v1/chat/completions"))
    }

    @Test
    fun createAiService_parsesChatResponse() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody("{\"choices\":[{\"message\":{\"content\":\"{\\\"is_food\\\":false,\\\"items\\\":[]}\"}}]}")
        )
        val service = NetworkModule.createAiService(
            server.url("/v1/").toString(),
            NetworkModule.createAiClient()
        )
        val response = service.chat(emptyRequest())
        assertEquals(1, response.choices.size)
    }

    @Test
    fun errorMessageBody_stringFormIsSupported() {
        assertEquals("API key required", AiParser.extractErrorMessage("{\"error\":\"API key required\"}"))
    }

    @Test
    fun errorMessageBody_objectFormIsSupported() {
        assertEquals("boom", AiParser.extractErrorMessage("{\"error\":{\"message\":\"boom\"}}"))
    }
}
