package com.expensetracker.ai

import android.app.Application
import androidx.work.*
import com.expensetracker.ai.data.database.ExpenseDatabase
import com.expensetracker.ai.data.repository.BudgetRepository
import com.expensetracker.ai.data.repository.ChatMessageRepository
import com.expensetracker.ai.data.repository.ExpenseRepository
import com.expensetracker.ai.data.repository.FinancialInsightRepository
import com.expensetracker.ai.data.repository.SavingsGoalRepository
import com.expensetracker.ai.workers.ExchangeRateUpdateWorker
import java.util.concurrent.TimeUnit

class ExpenseTrackerApplication : Application() {

    val database by lazy { ExpenseDatabase.getDatabase(this) }

    val expenseRepository by lazy { ExpenseRepository(database.expenseDao()) }

    val savingsGoalRepository by lazy { SavingsGoalRepository(database.savingsGoalDao()) }

    val financialInsightRepository by lazy {
        FinancialInsightRepository(database.financialInsightDao())
    }

    val chatMessageRepository by lazy { ChatMessageRepository(database.chatMessageDao()) }

    val budgetRepository by lazy { BudgetRepository(database.budgetDao()) }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Schedule daily exchange rate updates
        scheduleExchangeRateUpdates()
    }

    private fun scheduleExchangeRateUpdates() {
        val constraints =
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        val exchangeRateUpdateRequest =
                PeriodicWorkRequestBuilder<ExchangeRateUpdateWorker>(
                                24,
                                TimeUnit.HOURS // Run every 24 hours
                        )
                        .setConstraints(constraints)
                        .setInitialDelay(1, TimeUnit.HOURS) // Start after 1 hour
                        .build()

        WorkManager.getInstance(this)
                .enqueueUniquePeriodicWork(
                        "exchange_rate_update",
                        ExistingPeriodicWorkPolicy.KEEP,
                        exchangeRateUpdateRequest
                )
    }

    companion object {
        lateinit var instance: ExpenseTrackerApplication
            private set
    }
}
