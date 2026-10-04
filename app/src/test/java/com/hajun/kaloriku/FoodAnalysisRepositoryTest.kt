package com.hajun.kaloriku

import com.hajun.kaloriku.data.AnalysisResult
import com.hajun.kaloriku.data.FoodAnalysisRepository
import com.hajun.kaloriku.data.NetworkResult
import com.hajun.kaloriku.data.remote.NetworkModule
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Menguji FoodAnalysisRepository lewat MockWebServer (tanpa jaringan sungguhan).
 */
class FoodAnalysisRepositoryTest {

    private lateinit var server: MockWebServer
    private val model = "test/model-x"

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun repository(
        apiKey: String = "",
        baseUrl: String = server.url("/v1/").toString(),
        readTimeoutSeconds: Long = 5
    ): FoodAnalysisRepository {
        val client = NetworkModule.createAiClient(
            apiKey = apiKey,
            connectTimeoutSeconds = 5,
            readTimeoutSeconds = readTimeoutSeconds
        )
        val service = NetworkModule.createAiService(baseUrl, client)
        return FoodAnalysisRepository(service, baseUrl, model)
    }

    private fun analysisJson(): String =
        "{\"is_food\":true,\"items\":[{\"name\":\"Nasi putih\",\"grams\":150," +
            "\"calories\":195,\"protein_g\":4.1,\"carbs_g\":42.3,\"fat_g\":0.3}]}"

    private fun stringContentBody(): String =
        "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"${analysisJson().replace("\"", "\\\"")}\"}}]}"

    @Test
    fun success_stringContent_returnsParsedItems() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(stringContentBody()))
        val result = repository().analyzeFood(byteArrayOf(1, 2, 3))
        assertTrue(result is NetworkResult.Success)
        val analysis = (result as NetworkResult.Success).data
        assertEquals(1, analysis.items.size)
        assertEquals("Nasi putih", analysis.items[0].name)
        assertEquals(195.0, analysis.items[0].calories, 0.001)
    }

    @Test
    fun success_arrayContentWithReasoning_skipsReasoningPart() = runTest {
        val body = "{\"choices\":[{\"message\":{\"content\":[" +
            "{\"type\":\"reasoning\",\"text\":\"berpikir...\"}," +
            "{\"type\":\"text\",\"text\":\"${analysisJson().replace("\"", "\\\"")}\"}]}}]}"
        server.enqueue(MockResponse().setResponseCode(200).setBody(body))
        val result = repository().analyzeFood(byteArrayOf(1))
        assertTrue(result is NetworkResult.Success)
        assertEquals(1, (result as NetworkResult.Success).data.items.size)
    }

    @Test
    fun requestBody_containsStreamFalseAndConfiguredModel() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(stringContentBody()))
        repository().analyzeFood(byteArrayOf(1))
        val request = server.takeRequest()
        val body = request.body.readUtf8()
        assertTrue(body.contains("\"stream\":false"))
        assertTrue(body.contains("\"model\":\"$model\""))
        assertTrue(body.contains("\"temperature\":0.2"))
        assertTrue(body.contains("data:image/jpeg;base64,"))
    }

    @Test
    fun requestPath_endsWithChatCompletions() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(stringContentBody()))
        repository().analyzeFood(byteArrayOf(1))
        val path = server.takeRequest().path!!
        assertTrue(path.endsWith("/chat/completions"))
    }

    @Test
    fun authorizationHeader_presentWhenKeyIsSet() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(stringContentBody()))
        repository(apiKey = "rahasia").analyzeFood(byteArrayOf(1))
        assertEquals("Bearer rahasia", server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun authorizationHeader_absentWhenKeyIsBlank() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(stringContentBody()))
        repository(apiKey = "   ").analyzeFood(byteArrayOf(1))
        assertNull(server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun unauthorized_mapsToApiKeyMessage() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(401)
                .setBody("{\"error\":\"API key required for remote API access\"}")
        )
        val result = repository().analyzeFood(byteArrayOf(1))
        assertTrue(result is NetworkResult.Error)
        result as NetworkResult.Error
        assertEquals(401, result.code)
        assertEquals(
            "API key tidak valid atau tidak punya akses. Periksa AI_API_KEY di local.properties.",
            result.message
        )
    }

    @Test
    fun notFound_mapsToModelMessage() = runTest {
        server.enqueue(MockResponse().setResponseCode(404).setBody("{\"error\":{\"message\":\"no model\"}}"))
        val result = repository().analyzeFood(byteArrayOf(1)) as NetworkResult.Error
        assertEquals(404, result.code)
        assertEquals("Endpoint atau model \"$model\" tidak ditemukan. Periksa AI_BASE_URL dan AI_MODEL.", result.message)
    }

    @Test
    fun tooManyRequests_mapsToQuotaMessage() = runTest {
        server.enqueue(MockResponse().setResponseCode(429).setBody("{}"))
        val result = repository().analyzeFood(byteArrayOf(1)) as NetworkResult.Error
        assertEquals(429, result.code)
        assertEquals("Terlalu banyak permintaan atau kuota habis. Tunggu sebentar lalu coba lagi.", result.message)
    }

    @Test
    fun badRequest_mapsToBadRequestMessageWithDetail() = runTest {
        server.enqueue(MockResponse().setResponseCode(400).setBody("{\"error\":\"bad payload\"}"))
        val result = repository().analyzeFood(byteArrayOf(1)) as NetworkResult.Error
        assertEquals(400, result.code)
        assertEquals("Permintaan ditolak server AI: bad payload", result.message)
    }

    @Test
    fun serverError_mapsToServerMessage() = runTest {
        server.enqueue(MockResponse().setResponseCode(500).setBody("boom"))
        val result = repository().analyzeFood(byteArrayOf(1)) as NetworkResult.Error
        assertEquals(500, result.code)
        assertEquals("Server AI sedang bermasalah (500). Coba lagi nanti.", result.message)
    }

    @Test
    fun timeout_mapsToTimeoutMessage() = runTest {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        val result = repository(readTimeoutSeconds = 1).analyzeFood(byteArrayOf(1)) as NetworkResult.Error
        assertEquals(
            "Server AI terlalu lama merespons. Pastikan server lokal (misalnya 9router) menyala, lalu coba lagi.",
            result.message
        )
    }

    @Test
    fun ioError_mapsToConnectionMessage() = runTest {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))
        val baseUrl = server.url("/v1/").toString()
        val result = repository(baseUrl = baseUrl).analyzeFood(byteArrayOf(1)) as NetworkResult.Error
        assertEquals(
            "Gagal terhubung ke server AI di $baseUrl. Periksa koneksi dan pastikan server menyala.",
            result.message
        )
    }

    @Test
    fun malformedJson_returnsErrorInsteadOfCrashing() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("{ini bukan json"))
        val result = repository().analyzeFood(byteArrayOf(1))
        assertTrue(result is NetworkResult.Error)
        assertEquals("Respons AI tidak bisa dibaca. Coba lagi.", (result as NetworkResult.Error).message)
    }

    @Test
    fun blankBaseUrl_returnsErrorWithoutSendingRequest() = runTest {
        // Service dibangun dari URL server yang valid, tetapi baseUrl repository kosong.
        val repo = FoodAnalysisRepository(
            NetworkModule.createAiService(server.url("/v1/").toString(), NetworkModule.createAiClient()),
            "  ",
            model
        )
        val result = repo.analyzeFood(byteArrayOf(1))
        assertTrue(result is NetworkResult.Error)
        assertEquals(
            "Base URL server AI belum diisi. Periksa AI_BASE_URL di local.properties.",
            (result as NetworkResult.Error).message
        )
        assertEquals(0, server.requestCount)
    }

    @Test
    fun cancellation_propagatesAndDoesNotBecomeError() = runTest {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        var result: NetworkResult<AnalysisResult>? = null
        val job = launch(Dispatchers.Default) {
            result = repository().analyzeFood(byteArrayOf(1))
        }
        // Tunggu permintaan benar-benar sampai ke server sebelum dibatalkan.
        server.takeRequest(3, TimeUnit.SECONDS) ?: error("permintaan tidak sampai ke MockWebServer")
        job.cancel()
        job.join()
        assertTrue(job.isCancelled)
        assertNull("pembatalan tidak boleh menjadi Error", result)
    }
}
