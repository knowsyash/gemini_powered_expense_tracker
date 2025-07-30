package com.expensetracker.ai.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "expenses")
data class Expense(
        @PrimaryKey(autoGenerate = true) val id: Long = 0,
        val uniqueId: String = "", // 5-letter unique identifier
        val amount: Double, // Amount in INR (after conversion)
        val category: String,
        val description: String,
        val date: Date,
        val isIncome: Boolean = false,
        val originalAmount: Double? = null, // Original amount in foreign currency
        val originalCurrency: String? = null, // Original currency code (USD, EUR, etc.)
        val exchangeRate: Double? = null, // Exchange rate used for conversion
        val lastRateUpdate: Long? = null // Timestamp of last rate update
)
