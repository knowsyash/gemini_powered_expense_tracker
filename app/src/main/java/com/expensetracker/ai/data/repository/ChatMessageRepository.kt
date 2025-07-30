package com.expensetracker.ai.data.repository

import com.expensetracker.ai.data.database.ChatMessageDao
import com.expensetracker.ai.data.model.ChatMessage
import kotlinx.coroutines.flow.Flow

class ChatMessageRepository(private val chatMessageDao: ChatMessageDao) {

        fun getAllChatMessages(): Flow<List<ChatMessage>> = chatMessageDao.getAllChatMessages()

        fun getRecentChatMessages(limit: Int = 50): Flow<List<ChatMessage>> =
                chatMessageDao.getRecentChatMessages(limit)

        suspend fun insertChatMessage(chatMessage: ChatMessage): Long =
                chatMessageDao.insertChatMessage(chatMessage)

        suspend fun insertChatMessages(chatMessages: List<ChatMessage>) =
                chatMessageDao.insertChatMessages(chatMessages)

        suspend fun deleteChatMessage(chatMessage: ChatMessage) =
                chatMessageDao.deleteChatMessage(chatMessage)

        suspend fun deleteAllChatMessages() = chatMessageDao.deleteAllChatMessages()

        suspend fun clearAllMessages() = chatMessageDao.deleteAllChatMessages()

        suspend fun deleteOldChatMessages(cutoffTime: Long) =
                chatMessageDao.deleteOldChatMessages(cutoffTime)

        suspend fun getChatMessageCount(): Int = chatMessageDao.getChatMessageCount()

        // Helper method to clean up old messages (keep only last 100 messages)
        suspend fun cleanupOldMessages() {
                val count = getChatMessageCount()
                if (count > 100) {
                        // Keep only the latest 100 messages
                        val cutoffTime =
                                System.currentTimeMillis() -
                                        (30 * 24 * 60 * 60 * 1000L) // 30 days ago
                        deleteOldChatMessages(cutoffTime)
                }
        }
}
