package com.hajun.kaloriku.data.remote

import retrofit2.http.Body
import retrofit2.http.POST

/** Endpoint server AI yang berformat OpenAI-compatible. */
interface AiApiService {

    @POST("chat/completions")
    suspend fun chat(@Body request: ChatRequest): ChatResponse
}
