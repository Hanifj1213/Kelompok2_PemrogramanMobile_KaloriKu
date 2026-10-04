package com.hajun.kaloriku

import com.hajun.kaloriku.data.FoodItem
import com.hajun.kaloriku.data.NetworkResult
import com.hajun.kaloriku.data.ProductRepository
import com.hajun.kaloriku.data.remote.NetworkModule
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
 * Menguji ProductRepository (Open Food Facts) lewat MockWebServer.
 */
class ProductRepositoryTest {

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

    private fun repository(readTimeoutSeconds: Long = 5): ProductRepository {
        val client = NetworkModule.createOpenFoodFactsClient(
            connectTimeoutSeconds = 5,
            readTimeoutSeconds = readTimeoutSeconds
        )
        val service = NetworkModule.createOpenFoodFactsService(server.url("/").toString(), client)
        return ProductRepository(service)
    }

    private fun fixture(): String =
        javaClass.classLoader!!.getResourceAsStream("off_teh_pucuk.json")
            ?.bufferedReader()?.use { it.readText() }
            ?: error("fixture tidak ditemukan")

    @Test
    fun productFound_returnsSuccessAndSendsQueryFields() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(fixture()))
        val result = repository().lookup("8996001600146")
        assertTrue(result is NetworkResult.Success)
        val item = (result as NetworkResult.Success).data
        assertEquals("Teh Pucuk Harum Jasmine 350 ml (Mayora)", item.name)
        assertEquals(240.0, item.grams, 0.01)

        val request = server.takeRequest()
        assertTrue(request.path!!.contains("/api/v2/product/8996001600146.json"))
        assertTrue(request.path!!.contains("fields="))
        assertEquals("KaloriKu/1.0 (Android)", request.getHeader("User-Agent"))
    }

    @Test
    fun statusZeroWithHttp200_returnsNotFoundMessage() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody("{\"status\":0,\"status_verbose\":\"product not found\"}")
        )
        val result = repository().lookup("0000") as NetworkResult.Error
        assertEquals("Produk dengan barcode 0000 belum ada di Open Food Facts.", result.message)
    }

    @Test
    fun serverError_mapsToOffMessage() = runTest {
        server.enqueue(MockResponse().setResponseCode(500).setBody("oops"))
        val result = repository().lookup("123") as NetworkResult.Error
        assertEquals(500, result.code)
        assertEquals("Gagal menghubungi Open Food Facts (500). Coba lagi nanti.", result.message)
    }

    @Test
    fun timeout_mapsToTimeoutMessage() = runTest {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        val result = repository(readTimeoutSeconds = 1).lookup("123") as NetworkResult.Error
        assertEquals("Open Food Facts terlalu lama merespons. Coba lagi.", result.message)
    }

    @Test
    fun ioError_mapsToConnectionMessage() = runTest {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))
        val result = repository().lookup("123") as NetworkResult.Error
        assertEquals("Gagal terhubung ke Open Food Facts. Periksa koneksi internet.", result.message)
    }

    @Test
    fun unreadableResponse_returnsReadableError() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("<html>bukan json</html>"))
        val result = repository().lookup("123") as NetworkResult.Error
        assertEquals("Respons Open Food Facts tidak bisa dibaca.", result.message)
    }

    @Test
    fun cancellation_propagatesAndDoesNotBecomeError() = runTest {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        var result: NetworkResult<FoodItem>? = null
        val job = launch(Dispatchers.Default) {
            result = repository().lookup("123")
        }
        server.takeRequest(3, java.util.concurrent.TimeUnit.SECONDS) ?: error("permintaan tidak sampai")
        job.cancel()
        job.join()
        assertTrue(job.isCancelled)
        assertNull(result)
    }
}
