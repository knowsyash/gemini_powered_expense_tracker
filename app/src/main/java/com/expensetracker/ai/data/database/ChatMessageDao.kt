package com.expensetracker.ai.data.database

import androidx.room.*
import com.expensetracker.ai.data.model.ChatMessage
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {

    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllChatMessages(): Flow<List<ChatMessage>>

    @Query("SELECT * FROM chat_messages ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentChatMessages(limit: Int = 50): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(chatMessage: ChatMessage): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessages(chatMessages: List<ChatMessage>)

    @Delete suspend fun deleteChatMessage(chatMessage: ChatMessage)

    @Query("DELETE FROM chat_messages") suspend fun deleteAllChatMessages()

    @Query("DELETE FROM chat_messages WHERE timestamp < :cutoffTime")
    suspend fun deleteOldChatMessages(cutoffTime: Long)

    @Query("SELECT COUNT(*) FROM chat_messages") suspend fun getChatMessageCount(): Int
}
