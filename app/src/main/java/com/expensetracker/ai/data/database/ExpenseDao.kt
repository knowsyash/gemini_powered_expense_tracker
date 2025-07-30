package com.expensetracker.ai.data.database

import androidx.room.*
import com.expensetracker.ai.data.model.Expense
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    @Query("SELECT * FROM expenses ORDER BY date DESC") fun getAllExpenses(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE id = :id") suspend fun getExpenseById(id: Long): Expense?

    @Query("SELECT * FROM expenses WHERE uniqueId = :uniqueId")
    suspend fun getExpenseByUniqueId(uniqueId: String): Expense?

    @Query(
            "SELECT * FROM expenses WHERE uniqueId LIKE :partialId || '%' ORDER BY date DESC LIMIT 10"
    )
    suspend fun searchExpensesByUniqueId(partialId: String): List<Expense>

    @Query("SELECT * FROM expenses WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getExpensesByDateRange(startDate: Long, endDate: Long): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE category = :category ORDER BY date DESC")
    fun getExpensesByCategory(category: String): Flow<List<Expense>>

    @Query("SELECT SUM(amount) FROM expenses WHERE isIncome = 0")
    suspend fun getTotalExpenses(): Double?

    @Query("SELECT SUM(amount) FROM expenses WHERE isIncome = 1")
    suspend fun getTotalIncome(): Double?

    @Query("SELECT SUM(amount) FROM expenses WHERE category = :category AND isIncome = 0")
    suspend fun getTotalExpensesByCategory(category: String): Double?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense): Long

    @Update suspend fun updateExpense(expense: Expense)

    @Delete suspend fun deleteExpense(expense: Expense)

    @Query("DELETE FROM expenses") suspend fun deleteAllExpenses()
}
