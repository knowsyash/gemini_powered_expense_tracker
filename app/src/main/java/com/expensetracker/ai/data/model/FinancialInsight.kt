package com.expensetracker.ai.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "financial_insights")
data class FinancialInsight(
        @PrimaryKey(autoGenerate = true) val id: Long = 0,
        val title: String,
        val description: String,
        val insightType: InsightType,
        val relevantData: String, // JSON string of relevant financial data
        val generatedDate: Date = Date(),
        val isRead: Boolean = false,
        val actionTaken: Boolean = false,
        val priority: InsightPriority = InsightPriority.MEDIUM,
        val aiConfidence: Double = 0.0 // 0.0 to 1.0
)

enum class InsightType {
    SPENDING_PATTERN,
    SAVING_OPPORTUNITY,
    BUDGET_WARNING,
    INCOME_ANALYSIS,
    GOAL_PROGRESS,
    CATEGORY_OPTIMIZATION,
    MONTHLY_SUMMARY
}

enum class InsightPriority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}
