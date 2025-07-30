package com.expensetracker.ai.data.database

import androidx.room.*
import com.expensetracker.ai.data.model.SavingsGoal
import java.util.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SavingsGoalDao {

    @Query("SELECT * FROM savings_goals ORDER BY priority ASC, targetDate ASC")
    fun getAllSavingsGoals(): Flow<List<SavingsGoal>>

    @Query(
            "SELECT * FROM savings_goals WHERE isCompleted = 0 ORDER BY priority ASC, targetDate ASC"
    )
    fun getActiveSavingsGoals(): Flow<List<SavingsGoal>>

    @Query("SELECT * FROM savings_goals WHERE isCompleted = 1 ORDER BY createdDate DESC")
    fun getCompletedSavingsGoals(): Flow<List<SavingsGoal>>

    @Query("SELECT * FROM savings_goals WHERE id = :id")
    suspend fun getSavingsGoalById(id: Long): SavingsGoal?

    @Query("SELECT * FROM savings_goals WHERE category = :category")
    fun getSavingsGoalsByCategory(category: String): Flow<List<SavingsGoal>>

    @Query("SELECT SUM(targetAmount) FROM savings_goals WHERE isCompleted = 0")
    suspend fun getTotalSavingsTarget(): Double?

    @Query("SELECT SUM(currentAmount) FROM savings_goals WHERE isCompleted = 0")
    suspend fun getTotalCurrentSavings(): Double?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavingsGoal(savingsGoal: SavingsGoal): Long

    @Update suspend fun updateSavingsGoal(savingsGoal: SavingsGoal)

    @Delete suspend fun deleteSavingsGoal(savingsGoal: SavingsGoal)

    @Query("UPDATE savings_goals SET currentAmount = currentAmount + :amount WHERE id = :goalId")
    suspend fun addToSavingsGoal(goalId: Long, amount: Double)

    @Query("UPDATE savings_goals SET isCompleted = 1 WHERE currentAmount >= targetAmount")
    suspend fun markCompletedGoals()
}
