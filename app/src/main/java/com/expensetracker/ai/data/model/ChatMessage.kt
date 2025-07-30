package com.expensetracker.ai.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MessageType {
    USER_MESSAGE,
    AI_RESPONSE,
    SYSTEM_NOTIFICATION,
    FINANCIAL_INSIGHT,
    SAVINGS_GOAL_UPDATE,
    EARNING_ANALYSIS
}

@Entity(tableName = "chat_messages")
data class ChatMessage(
        @PrimaryKey(autoGenerate = true) val id: Long = 0,
        val message: String,
        val isUser: Boolean,
        val timestamp: Long = System.currentTimeMillis(),
        val messageType: String =
                if (isUser) MessageType.USER_MESSAGE.name else MessageType.AI_RESPONSE.name,
        val metadata: String? = null // JSON string for storing additional context
) {
    // Helper functions to work with MessageType enum
    fun getMessageTypeEnum(): MessageType {
        return try {
            MessageType.valueOf(messageType)
        } catch (e: IllegalArgumentException) {
            if (isUser) MessageType.USER_MESSAGE else MessageType.AI_RESPONSE
        }
    }

    companion object {
        fun create(
                message: String,
                isUser: Boolean,
                messageType: MessageType =
                        if (isUser) MessageType.USER_MESSAGE else MessageType.AI_RESPONSE,
                metadata: String? = null
        ): ChatMessage {
            return ChatMessage(
                    message = message,
                    isUser = isUser,
                    messageType = messageType.name,
                    metadata = metadata
            )
        }
    }
}
