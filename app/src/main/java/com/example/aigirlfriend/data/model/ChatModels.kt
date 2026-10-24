package com.example.aigirlfriend.data.model

import com.google.gson.annotations.SerializedName

data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double = 0.8,
    val max_tokens: Int = 500
)

data class ChatMessage(
    val role: String,
    val content: Any = ""
)

data class ChatResponse(
    val choices: List<Choice>,
    val usage: Usage? = null
)

data class Choice(
    val message: Message,
    val index: Int = 0,
    val finish_reason: String? = null
)

data class Message(
    val role: String,
    val content: Any = ""
)

data class Usage(
    val prompt_tokens: Int = 0,
    val completion_tokens: Int = 0,
    val total_tokens: Int = 0
)
