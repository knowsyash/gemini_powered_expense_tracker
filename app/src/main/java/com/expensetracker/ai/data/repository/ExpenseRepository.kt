package com.expensetracker.ai.data.repository

import com.expensetracker.ai.data.database.ExpenseDao
import com.expensetracker.ai.data.model.Expense
import java.util.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class ExpenseRepository(private val expenseDao: ExpenseDao) {

    // Cache for frequently accessed data
    private var cachedTotalExpenses: Double? = null
    private var cachedTotalIncome: Double? = null
    private var lastCacheUpdate: Long = 0
    private val cacheValidityDuration = 5 * 60 * 1000L // 5 minutes

    fun getAllExpenses(): Flow<List<Expense>> = expenseDao.getAllExpenses()

    suspend fun getExpenseById(id: Long): Expense? = expenseDao.getExpenseById(id)

    suspend fun getExpenseByUniqueId(uniqueId: String): Expense? =
            expenseDao.getExpenseByUniqueId(uniqueId)

    suspend fun searchExpensesByUniqueId(partialId: String): List<Expense> =
            expenseDao.searchExpensesByUniqueId(partialId)

    fun getExpensesByDateRange(startDate: Long, endDate: Long): Flow<List<Expense>> =
            expenseDao.getExpensesByDateRange(startDate, endDate)

    fun getExpensesByCategory(category: String): Flow<List<Expense>> =
            expenseDao.getExpensesByCategory(category)

    suspend fun getTotalExpenses(): Double {
        // Always get fresh data for UI updates - cache for shorter periods
        val currentTime = System.currentTimeMillis()
        if (cachedTotalExpenses == null || (currentTime - lastCacheUpdate) > 10000L
        ) { // 10 seconds cache
            cachedTotalExpenses = expenseDao.getTotalExpenses() ?: 0.0
            lastCacheUpdate = currentTime
        }
        return cachedTotalExpenses ?: 0.0
    }

    suspend fun getTotalIncome(): Double {
        // Always get fresh data for UI updates - cache for shorter periods
        val currentTime = System.currentTimeMillis()
        if (cachedTotalIncome == null || (currentTime - lastCacheUpdate) > 10000L
        ) { // 10 seconds cache
            cachedTotalIncome = expenseDao.getTotalIncome() ?: 0.0
            lastCacheUpdate = currentTime
        }
        return cachedTotalIncome ?: 0.0
    }

    suspend fun getTotalExpensesByCategory(category: String): Double =
            expenseDao.getTotalExpensesByCategory(category) ?: 0.0

    suspend fun insertExpense(expense: Expense): Long {
        invalidateCache()
        return expenseDao.insertExpense(expense)
    }

    suspend fun updateExpense(expense: Expense) {
        invalidateCache()
        expenseDao.updateExpense(expense)
    }

    suspend fun deleteExpense(expense: Expense) {
        invalidateCache()
        expenseDao.deleteExpense(expense)
    }

    suspend fun deleteAllExpenses() {
        invalidateCache()
        expenseDao.deleteAllExpenses()
    }

    // Additional utility methods for AI analysis
    suspend fun getExpensesSummary(): Map<String, Any> {
        // Always get fresh data for summary to ensure accuracy
        refreshData()
        val expenses = getAllExpenses().first()
        val totalExpenses = getTotalExpenses()
        val totalIncome = getTotalIncome()

        val categoryBreakdown = mutableMapOf<String, Any>()
        expenses.filter { !it.isIncome }.groupBy { it.category }.forEach { (category, expenseList)
            ->
            categoryBreakdown[category] = expenseList.sumOf { it.amount }
        }

        return mapOf(
                "totalExpenses" to totalExpenses,
                "totalIncome" to totalIncome,
                "balance" to (totalIncome - totalExpenses),
                "categoryBreakdown" to categoryBreakdown,
                "transactionCount" to expenses.size,
                "lastTransaction" to (expenses.firstOrNull()?.date?.toString() ?: "No transactions")
        )
    }

    suspend fun getSpendingTrends(): Map<String, Double> {
        val expenses = getAllExpenses().first()
        val thirtyDaysAgo =
                Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -30) }.timeInMillis

        val recentExpenses = expenses.filter { !it.isIncome && it.date.time >= thirtyDaysAgo }

        return recentExpenses
                .groupBy { it.category }
                .mapValues { (_, expenseList) -> expenseList.sumOf { it.amount } }
                .toSortedMap(compareByDescending { it })
    }

    fun invalidateCache() {
        cachedTotalExpenses = null
        cachedTotalIncome = null
        lastCacheUpdate = 0
    }

    // Public method to refresh all cached data
    fun refreshData() {
        invalidateCache()
    }

    // Date-based query methods for chatbot
    suspend fun getExpensesByDateRange(startDate: Date, endDate: Date): List<Expense> {
        val expenses = getAllExpenses().first()
        return expenses.filter { expense ->
            expense.date.time >= startDate.time && expense.date.time <= endDate.time
        }
    }

    suspend fun getSpendingByDateRange(startDate: Date, endDate: Date): Map<String, Any> {
        val expenses = getExpensesByDateRange(startDate, endDate)
        val totalSpent = expenses.filter { !it.isIncome }.sumOf { it.amount }
        val totalEarned = expenses.filter { it.isIncome }.sumOf { it.amount }

        val categoryBreakdown =
                expenses
                        .filter { !it.isIncome }
                        .groupBy { it.category }
                        .mapValues { (_, expenseList) -> expenseList.sumOf { it.amount } }
                        .toSortedMap(compareByDescending { it })

        return mapOf(
                "totalSpent" to totalSpent,
                "totalEarned" to totalEarned,
                "transactionCount" to expenses.size,
                "categoryBreakdown" to categoryBreakdown,
                "transactions" to expenses
        )
    }

    suspend fun getExpensesListByDateRange(startDate: Long, endDate: Long): List<Expense> {
        return expenseDao.getExpensesByDateRange(startDate, endDate).first()
    }

    suspend fun getExpensesByDateRangeSync(startDate: Long, endDate: Long): List<Expense> {
        return expenseDao.getExpensesByDateRange(startDate, endDate).first()
    }
}
