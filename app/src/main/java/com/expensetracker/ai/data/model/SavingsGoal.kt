package com.expensetracker.ai.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "savings_goals")
data class SavingsGoal(
        @PrimaryKey(autoGenerate = true) val id: Long = 0,
        val title: String,
        val targetAmount: Double,
        val currentAmount: Double = 0.0,
        val targetDate: Date,
        val createdDate: Date = Date(),
        val category: String = "General", // e.g., "Emergency Fund", "Vacation", "Car", etc.
        val isCompleted: Boolean = false,
        val description: String = "",
        val priority: Int = 1 // 1 = High, 2 = Medium, 3 = Low
) {
    val progressPercentage: Double
        get() = if (targetAmount > 0) (currentAmount / targetAmount) * 100 else 0.0

    val remainingAmount: Double
        get() = maxOf(0.0, targetAmount - currentAmount)
}
