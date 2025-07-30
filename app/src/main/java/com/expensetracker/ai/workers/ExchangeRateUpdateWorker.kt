package com.expensetracker.ai.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.expensetracker.ai.ExpenseTrackerApplication
import com.expensetracker.ai.services.CurrencyService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ExchangeRateUpdateWorker(appContext: Context, workerParams: WorkerParameters) :
        CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result =
            withContext(Dispatchers.IO) {
                try {
                    val application = applicationContext as ExpenseTrackerApplication
                    val repository = application.expenseRepository
                    val currencyService = CurrencyService(applicationContext, repository)

                    // Update all foreign currency transactions
                    val updatedCount = currencyService.updateAllTransactionRates()

                    // Log the update result
                    if (updatedCount > 0) {
                        println("Exchange rate update: Updated $updatedCount transactions")
                    }

                    Result.success()
                } catch (exception: Exception) {
                    // Log the error and retry
                    println("Exchange rate update failed: ${exception.message}")
                    Result.retry()
                }
            }
}
