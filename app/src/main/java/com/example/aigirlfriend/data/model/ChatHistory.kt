package com.example.aigirlfriend.data.model

data class ChatHistory(
    val role: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isUser: Boolean = false,
    val attachmentUri: String? = null
)
