package com.hajun.kaloriku

import com.hajun.kaloriku.data.executeCancellable
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CancellableHttpTest {
    private class FakeConnection : HttpURLConnection(URL("http://127.0.0.1")) {
        val disconnected = AtomicBoolean(false)
        val closed = CountDownLatch(1)
        override fun connect() {}
        override fun usingProxy() = false
        override fun disconnect() {
            disconnected.set(true)
            closed.countDown()
        }
    }

    @Test fun successAlwaysDisconnects() = runBlocking {
        val connection = FakeConnection()
        assertEquals("ok", connection.executeCancellable { "ok" })
        assertTrue(connection.disconnected.get())
    }

    @Test fun failureAlwaysDisconnectsAndPropagates() = runBlocking {
        val connection = FakeConnection()
        var failed = false
        try {
            connection.executeCancellable { throw IllegalStateException("test failure") }
        } catch (_: IllegalStateException) {
            failed = true
        }
        assertTrue(failed)
        assertTrue(connection.disconnected.get())
    }

    @Test fun cancellingDisconnectsInFlightWorkAndSkipsLateResult() = runBlocking {
        val connection = FakeConnection()
        val started = CountDownLatch(1)
        val delivered = AtomicBoolean(false)
        val job = launch(kotlinx.coroutines.Dispatchers.Default) {
            connection.executeCancellable {
                started.countDown()
                check(connection.closed.await(5, TimeUnit.SECONDS))
                "late result"
            }
            delivered.set(true)
        }
        assertTrue(started.await(5, TimeUnit.SECONDS))
        job.cancel()
        job.join()
        assertTrue(connection.disconnected.get())
        assertFalse(delivered.get())
    }
}
