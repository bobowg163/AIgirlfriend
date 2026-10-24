package com.example.aigirlfriend.data.network

import com.example.aigirlfriend.data.api.OpenAIApiService
import com.example.aigirlfriend.data.model.ChatRequest
import com.example.aigirlfriend.data.model.ChatResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkClient {
    const val BASE_URL = "https://api-inference.modelscope.cn/"
    const val DEFAULT_API_KEY = "ms-753bd3be-a78f-4fbf-b007-c4eb7dda9c82"
    const val DEFAULT_MODEL = "Qwen/Qwen3.8-27B"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val apiService: OpenAIApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenAIApiService::class.java)
    }

    suspend fun getChatResponse(
        apiKey: String = DEFAULT_API_KEY,
        messages: List<com.example.aigirlfriend.data.model.ChatMessage>,
        model: String = DEFAULT_MODEL
    ): String {
        val request = ChatRequest(
            model = model,
            messages = messages,
            temperature = 0.8,
            max_tokens = 500
        )
        val response = apiService.getChatCompletion(
            apiKey = "Bearer $apiKey",
            request = request
        )
        return response.choices.firstOrNull()?.message?.content?.toString()
            ?: throw Exception("No response from API")
    }
}