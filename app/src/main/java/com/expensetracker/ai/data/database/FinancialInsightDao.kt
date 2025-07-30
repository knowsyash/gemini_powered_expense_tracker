package com.expensetracker.ai.data.database

import androidx.room.*
import com.expensetracker.ai.data.model.FinancialInsight
import com.expensetracker.ai.data.model.InsightPriority
import com.expensetracker.ai.data.model.InsightType
import java.util.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FinancialInsightDao {

    @Query("SELECT * FROM financial_insights ORDER BY generatedDate DESC")
    fun getAllInsights(): Flow<List<FinancialInsight>>

    @Query(
            "SELECT * FROM financial_insights WHERE isRead = 0 ORDER BY priority DESC, generatedDate DESC"
    )
    fun getUnreadInsights(): Flow<List<FinancialInsight>>

    @Query("SELECT * FROM financial_insights WHERE insightType = :type ORDER BY generatedDate DESC")
    fun getInsightsByType(type: InsightType): Flow<List<FinancialInsight>>

    @Query(
            "SELECT * FROM financial_insights WHERE priority = :priority ORDER BY generatedDate DESC"
    )
    fun getInsightsByPriority(priority: InsightPriority): Flow<List<FinancialInsight>>

    @Query("SELECT * FROM financial_insights WHERE id = :id")
    suspend fun getInsightById(id: Long): FinancialInsight?

    @Query("SELECT COUNT(*) FROM financial_insights WHERE isRead = 0")
    suspend fun getUnreadInsightCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInsight(insight: FinancialInsight): Long

    @Update suspend fun updateInsight(insight: FinancialInsight)

    @Delete suspend fun deleteInsight(insight: FinancialInsight)

    @Query("UPDATE financial_insights SET isRead = 1 WHERE id = :id")
    suspend fun markInsightAsRead(id: Long)

    @Query("UPDATE financial_insights SET actionTaken = 1 WHERE id = :id")
    suspend fun markActionTaken(id: Long)

    @Query("DELETE FROM financial_insights WHERE generatedDate < :cutoffDate")
    suspend fun deleteOldInsights(cutoffDate: Date)
}
