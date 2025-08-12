package com.expensetracker.ai.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "budgets")
data class Budget(
        @PrimaryKey(autoGenerate = true) val id: Long = 0,
        val amount: Double,
        val month: Int, // 1-12
        val year: Int,
        val createdDate: Date = Date(),
        val description: String? = null
)
