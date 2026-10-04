package com.hajun.kaloriku.data

import java.net.HttpURLConnection
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

internal suspend fun <T> HttpURLConnection.executeCancellable(block: () -> T): T =
    withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { disconnect() }
            try {
                val result = block()
                if (continuation.isActive) continuation.resume(result)
            } catch (error: Exception) {
                if (continuation.isActive) continuation.resumeWithException(error)
            } finally {
                disconnect()
            }
        }
    }
