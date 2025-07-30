package com.expensetracker.ai.services

import android.content.Context
import com.expensetracker.ai.data.repository.ExpenseRepository
import com.expensetracker.ai.network.CurrencyApiService
import com.expensetracker.ai.network.RetrofitClient
import com.expensetracker.ai.utils.CurrencyConstants
import kotlinx.coroutines.flow.first

class CurrencyService(
        private val context: Context,
        private val expenseRepository: ExpenseRepository
) {
    private val currencyApiService: CurrencyApiService by lazy { RetrofitClient.currencyApiService }

    /** Extract currency and amount from text message */
    @OptIn(ExperimentalStdlibApi::class)
    fun extractCurrencyAndAmount(message: String): CurrencyAmount? {
        val patterns =
                listOf(
                        // "10 USD", "100 EUR", etc.
                        "([0-9]+(?:\\.[0-9]+)?)\\s*([A-Z]{3})".toRegex(RegexOption.IGNORE_CASE),
                        // "$10", "€100", "£50", etc.
                        "([\\$€£¥])([0-9]+(?:\\.[0-9]+)?)".toRegex(),
                        // "10 dollars", "100 euros", etc.
                        "([0-9]+(?:\\.[0-9]+)?)\\s*(dollars?|euros?|pounds?|yen|yuan)".toRegex(
                                RegexOption.IGNORE_CASE
                        )
                )

        for (pattern in patterns) {
            val match = pattern.find(message)
            if (match != null) {
                return when (pattern) {
                    patterns[0] -> {
                        val amount = match.groupValues[1].toDoubleOrNull()
                        val currency = match.groupValues[2].uppercase()
                        if (amount != null &&
                                        CurrencyConstants.SUPPORTED_CURRENCIES.containsKey(currency)
                        ) {
                            CurrencyAmount(amount, currency)
                        } else null
                    }
                    patterns[1] -> {
                        val symbol = match.groupValues[1]
                        val amount = match.groupValues[2].toDoubleOrNull()
                        val currency =
                                when (symbol) {
                                    "$" -> "USD"
                                    "€" -> "EUR"
                                    "£" -> "GBP"
                                    "¥" -> "JPY"
                                    else -> null
                                }
                        if (amount != null && currency != null) {
                            CurrencyAmount(amount, currency)
                        } else null
                    }
                    patterns[2] -> {
                        val amount = match.groupValues[1].toDoubleOrNull()
                        val currencyWord = match.groupValues[2].lowercase()
                        val currency =
                                when {
                                    currencyWord.startsWith("dollar") -> "USD"
                                    currencyWord.startsWith("euro") -> "EUR"
                                    currencyWord.startsWith("pound") -> "GBP"
                                    currencyWord == "yen" -> "JPY"
                                    currencyWord == "yuan" -> "CNY"
                                    else -> null
                                }
                        if (amount != null && currency != null) {
                            CurrencyAmount(amount, currency)
                        } else null
                    }
                    else -> null
                }
            }
        }

        return null
    }

    /** Convert foreign currency to INR */
    suspend fun convertToINR(amount: Double, fromCurrency: String): ConversionResult? {
        if (fromCurrency == "INR") {
            return ConversionResult(amount, 1.0, fromCurrency, "INR")
        }

        return try {
            // For now, use offline rates since API key might not be configured
            val conversionResult = getOfflineConversionRate(amount, fromCurrency)
            if (conversionResult != null) {
                return conversionResult
            }

            // Try API if offline fails
            val response =
                    currencyApiService.convertCurrency(
                            apiKey = CurrencyConstants.EXCHANGE_RATE_API_KEY,
                            fromCurrency = fromCurrency,
                            toCurrency = "INR",
                            amount = amount
                    )

            if (response.isSuccessful && response.body()?.success == true) {
                val conversionResponse = response.body()!!
                ConversionResult(
                        convertedAmount = conversionResponse.result,
                        exchangeRate = conversionResponse.info.rate,
                        fromCurrency = fromCurrency,
                        toCurrency = "INR"
                )
            } else {
                // Fallback to stored rates or default rates
                getOfflineConversionRate(amount, fromCurrency)
            }
        } catch (e: Exception) {
            // Always fallback to stored rates
            getOfflineConversionRate(amount, fromCurrency)
        }
    }

    /** Get current exchange rate for a currency pair */
    suspend fun getExchangeRate(fromCurrency: String, toCurrency: String = "INR"): Double? {
        if (fromCurrency == toCurrency) return 1.0

        return try {
            val response =
                    currencyApiService.getExchangeRates(
                            apiKey = CurrencyConstants.EXCHANGE_RATE_API_KEY,
                            baseCurrency = fromCurrency,
                            targetCurrency = toCurrency
                    )

            if (response.isSuccessful && response.body()?.success == true) {
                response.body()?.rates?.get(toCurrency)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /** Update all transactions with latest exchange rates */
    suspend fun updateAllTransactionRates(): Int {
        var updatedCount = 0

        try {
            val allExpenses = expenseRepository.getAllExpenses().first()
            val foreignCurrencyExpenses =
                    allExpenses.filter {
                        it.originalCurrency != null && it.originalCurrency != "INR"
                    }

            for (expense in foreignCurrencyExpenses) {
                expense.originalCurrency?.let { currency ->
                    expense.originalAmount?.let { originalAmount ->
                        val conversionResult = convertToINR(originalAmount, currency)
                        if (conversionResult != null) {
                            val updatedExpense =
                                    expense.copy(
                                            amount = conversionResult.convertedAmount,
                                            exchangeRate = conversionResult.exchangeRate,
                                            lastRateUpdate = System.currentTimeMillis()
                                    )
                            expenseRepository.updateExpense(updatedExpense)
                            updatedCount++
                        }
                    }
                }
            }

            // Refresh data after updates
            if (updatedCount > 0) {
                expenseRepository.refreshData()
            }
        } catch (e: Exception) {
            // Log error but don't throw
        }

        return updatedCount
    }

    /** Fallback conversion using approximate rates (updated July 2025) */
    private fun getOfflineConversionRate(amount: Double, fromCurrency: String): ConversionResult? {
        // Approximate exchange rates as of July 2025
        val approximateRates =
                mapOf(
                        "USD" to 83.25,
                        "EUR" to 90.15,
                        "GBP" to 105.50,
                        "JPY" to 0.56,
                        "CAD" to 61.20,
                        "AUD" to 55.30,
                        "CHF" to 92.80,
                        "CNY" to 11.60,
                        "SGD" to 62.40,
                        "HKD" to 10.65
                )

        val rate = approximateRates[fromCurrency]
        return if (rate != null) {
            ConversionResult(
                    convertedAmount = amount * rate,
                    exchangeRate = rate,
                    fromCurrency = fromCurrency,
                    toCurrency = "INR"
            )
        } else null
    }
}

data class CurrencyAmount(val amount: Double, val currency: String)

data class ConversionResult(
        val convertedAmount: Double,
        val exchangeRate: Double,
        val fromCurrency: String,
        val toCurrency: String
)
