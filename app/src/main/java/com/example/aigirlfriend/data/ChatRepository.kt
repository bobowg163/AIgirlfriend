package com.example.aigirlfriend.data

import com.example.aigirlfriend.data.model.ChatHistory

class ChatRepository(private val database: ChatDatabase) {

    fun getAllMessages(): List<ChatHistory> = database.getAllMessages()

    fun insertMessage(role: String, content: String, timestamp: Long, isUser: Boolean, attachmentUri: String? = null) {
        database.insertMessage(role, content, timestamp, isUser, attachmentUri)
    }

    fun insertMessages(messages: List<ChatHistory>) {
        messages.forEach { msg ->
            database.insertMessage(msg.role, msg.content, msg.timestamp, msg.isUser, msg.attachmentUri)
        }
    }

    fun clearAllMessages() = database.clearAllMessages()
}
