package com.expensetracker.ai.data.repository

import com.expensetracker.ai.data.dao.BudgetDao
import com.expensetracker.ai.data.model.Budget
import java.util.*
import kotlinx.coroutines.flow.Flow

class BudgetRepository(private val budgetDao: BudgetDao) {

    fun getAllBudgets(): Flow<List<Budget>> = budgetDao.getAllBudgets()

    suspend fun getBudgetForMonth(month: Int, year: Int): Budget? {
        return budgetDao.getBudgetForMonth(month, year)
    }

    fun getBudgetForMonthFlow(month: Int, year: Int): Flow<Budget?> {
        return budgetDao.getBudgetForMonthFlow(month, year)
    }

    suspend fun setBudgetForMonth(
            amount: Double,
            month: Int,
            year: Int,
            description: String? = null
    ) {
        val existingBudget = budgetDao.getBudgetForMonth(month, year)
        if (existingBudget != null) {
            val updatedBudget = existingBudget.copy(amount = amount, description = description)
            budgetDao.updateBudget(updatedBudget)
        } else {
            val newBudget =
                    Budget(amount = amount, month = month, year = year, description = description)
            budgetDao.insertBudget(newBudget)
        }
    }

    suspend fun getCurrentMonthBudget(): Budget? {
        val calendar = Calendar.getInstance()
        return getBudgetForMonth(
                calendar.get(Calendar.MONTH) + 1, // Calendar.MONTH is 0-based
                calendar.get(Calendar.YEAR)
        )
    }

    suspend fun deleteBudgetForMonth(month: Int, year: Int) {
        budgetDao.deleteBudgetForMonth(month, year)
    }
}
