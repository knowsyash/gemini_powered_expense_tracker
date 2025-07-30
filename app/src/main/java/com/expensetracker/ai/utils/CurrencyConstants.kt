package com.expensetracker.ai.utils

object CurrencyConstants {
    // Free Exchange Rate API key (exchangerate-api.com)
    const val EXCHANGE_RATE_API_KEY =
            "free_api_key_placeholder" // Users should get their own free API key from
    // exchangerate-api.com

    // Supported currencies
    val SUPPORTED_CURRENCIES =
            mapOf(
                    "USD" to "US Dollar",
                    "EUR" to "Euro",
                    "GBP" to "British Pound",
                    "JPY" to "Japanese Yen",
                    "CAD" to "Canadian Dollar",
                    "AUD" to "Australian Dollar",
                    "CHF" to "Swiss Franc",
                    "CNY" to "Chinese Yuan",
                    "SGD" to "Singapore Dollar",
                    "HKD" to "Hong Kong Dollar",
                    "INR" to "Indian Rupee"
            )

    // Currency symbols
    val CURRENCY_SYMBOLS =
            mapOf(
                    "USD" to "$",
                    "EUR" to "€",
                    "GBP" to "£",
                    "JPY" to "¥",
                    "CAD" to "C$",
                    "AUD" to "A$",
                    "CHF" to "CHF",
                    "CNY" to "¥",
                    "SGD" to "S$",
                    "HKD" to "HK$",
                    "INR" to "₹"
            )

    // Base currency for the app
    const val BASE_CURRENCY = "INR"

    // Exchange rate update interval (24 hours)
    const val EXCHANGE_RATE_UPDATE_INTERVAL_HOURS = 24L
}
