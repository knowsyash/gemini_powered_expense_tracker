package com.expensetracker.ai.data.repository

import com.expensetracker.ai.data.database.FinancialInsightDao
import com.expensetracker.ai.data.model.FinancialInsight
import com.expensetracker.ai.data.model.InsightPriority
import com.expensetracker.ai.data.model.InsightType
import java.util.*
import kotlinx.coroutines.flow.Flow

class FinancialInsightRepository(private val financialInsightDao: FinancialInsightDao) {

    fun getAllInsights(): Flow<List<FinancialInsight>> = financialInsightDao.getAllInsights()

    fun getUnreadInsights(): Flow<List<FinancialInsight>> = financialInsightDao.getUnreadInsights()

    fun getInsightsByType(type: InsightType): Flow<List<FinancialInsight>> =
            financialInsightDao.getInsightsByType(type)

    fun getInsightsByPriority(priority: InsightPriority): Flow<List<FinancialInsight>> =
            financialInsightDao.getInsightsByPriority(priority)

    suspend fun getInsightById(id: Long): FinancialInsight? = financialInsightDao.getInsightById(id)

    suspend fun getUnreadInsightCount(): Int = financialInsightDao.getUnreadInsightCount()

    suspend fun insertInsight(insight: FinancialInsight): Long =
            financialInsightDao.insertInsight(insight)

    suspend fun updateInsight(insight: FinancialInsight) =
            financialInsightDao.updateInsight(insight)

    suspend fun deleteInsight(insight: FinancialInsight) =
            financialInsightDao.deleteInsight(insight)

    suspend fun markInsightAsRead(id: Long) = financialInsightDao.markInsightAsRead(id)

    suspend fun markActionTaken(id: Long) = financialInsightDao.markActionTaken(id)

    suspend fun cleanupOldInsights(daysToKeep: Int = 90) {
        val cutoffDate =
                Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -daysToKeep) }.time
        financialInsightDao.deleteOldInsights(cutoffDate)
    }
}
