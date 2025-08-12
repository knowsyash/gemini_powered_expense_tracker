package com.expensetracker.ai.data.dao

import androidx.room.*
import com.expensetracker.ai.data.model.Budget
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets ORDER BY year DESC, month DESC")
    fun getAllBudgets(): Flow<List<Budget>>

    @Query("SELECT * FROM budgets WHERE month = :month AND year = :year LIMIT 1")
    suspend fun getBudgetForMonth(month: Int, year: Int): Budget?

    @Query("SELECT * FROM budgets WHERE month = :month AND year = :year LIMIT 1")
    fun getBudgetForMonthFlow(month: Int, year: Int): Flow<Budget?>

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertBudget(budget: Budget)

    @Update suspend fun updateBudget(budget: Budget)

    @Delete suspend fun deleteBudget(budget: Budget)

    @Query("DELETE FROM budgets WHERE month = :month AND year = :year")
    suspend fun deleteBudgetForMonth(month: Int, year: Int)
}
