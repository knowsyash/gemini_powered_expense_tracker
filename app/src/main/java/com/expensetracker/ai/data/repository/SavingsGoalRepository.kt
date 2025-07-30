package com.expensetracker.ai.data.repository

import com.expensetracker.ai.data.database.SavingsGoalDao
import com.expensetracker.ai.data.model.SavingsGoal
import kotlinx.coroutines.flow.Flow

class SavingsGoalRepository(private val savingsGoalDao: SavingsGoalDao) {

    fun getAllSavingsGoals(): Flow<List<SavingsGoal>> = savingsGoalDao.getAllSavingsGoals()

    fun getActiveSavingsGoals(): Flow<List<SavingsGoal>> = savingsGoalDao.getActiveSavingsGoals()

    fun getCompletedSavingsGoals(): Flow<List<SavingsGoal>> =
            savingsGoalDao.getCompletedSavingsGoals()

    suspend fun getSavingsGoalById(id: Long): SavingsGoal? = savingsGoalDao.getSavingsGoalById(id)

    fun getSavingsGoalsByCategory(category: String): Flow<List<SavingsGoal>> =
            savingsGoalDao.getSavingsGoalsByCategory(category)

    suspend fun getTotalSavingsTarget(): Double = savingsGoalDao.getTotalSavingsTarget() ?: 0.0

    suspend fun getTotalCurrentSavings(): Double = savingsGoalDao.getTotalCurrentSavings() ?: 0.0

    suspend fun insertSavingsGoal(savingsGoal: SavingsGoal): Long =
            savingsGoalDao.insertSavingsGoal(savingsGoal)

    suspend fun updateSavingsGoal(savingsGoal: SavingsGoal) =
            savingsGoalDao.updateSavingsGoal(savingsGoal)

    suspend fun deleteSavingsGoal(savingsGoal: SavingsGoal) =
            savingsGoalDao.deleteSavingsGoal(savingsGoal)

    suspend fun addToSavingsGoal(goalId: Long, amount: Double) =
            savingsGoalDao.addToSavingsGoal(goalId, amount)

    suspend fun markCompletedGoals() = savingsGoalDao.markCompletedGoals()

    suspend fun getSavingsProgress(): Map<String, Any> {
        val totalTarget = getTotalSavingsTarget()
        val totalCurrent = getTotalCurrentSavings()
        val progressPercentage = if (totalTarget > 0) (totalCurrent / totalTarget) * 100 else 0.0

        return mapOf(
                "totalTarget" to totalTarget,
                "totalCurrent" to totalCurrent,
                "progressPercentage" to progressPercentage,
                "remainingAmount" to maxOf(0.0, totalTarget - totalCurrent)
        )
    }
}
