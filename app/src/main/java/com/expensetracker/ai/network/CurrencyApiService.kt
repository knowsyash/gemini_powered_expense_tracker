package com.expensetracker.ai.network

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface CurrencyApiService {
    @GET("v6/latest")
    suspend fun getExchangeRates(
            @Query("access_key") apiKey: String,
            @Query("base") baseCurrency: String = "USD",
            @Query("symbols") targetCurrency: String = "INR"
    ): Response<ExchangeRateResponse>

    @GET("v6/convert")
    suspend fun convertCurrency(
            @Query("access_key") apiKey: String,
            @Query("from") fromCurrency: String,
            @Query("to") toCurrency: String = "INR",
            @Query("amount") amount: Double
    ): Response<ConversionResponse>
}

data class ExchangeRateResponse(
        val success: Boolean,
        val timestamp: Long,
        val base: String,
        val date: String,
        val rates: Map<String, Double>
)

data class ConversionResponse(
        val success: Boolean,
        val query: ConversionQuery,
        val info: ConversionInfo,
        val result: Double
)

data class ConversionQuery(val from: String, val to: String, val amount: Double)

data class ConversionInfo(val timestamp: Long, val rate: Double)
