package com.expensetracker.ai.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private const val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/"
    private const val CURRENCY_BASE_URL = "https://api.exchangerate-api.com/"

    private val loggingInterceptor =
            HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }

    private val client = OkHttpClient.Builder().addInterceptor(loggingInterceptor).build()

    val geminiApiService: GeminiApiService by lazy {
        Retrofit.Builder()
                .baseUrl(GEMINI_BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(GeminiApiService::class.java)
    }

    val currencyApiService: CurrencyApiService by lazy {
        Retrofit.Builder()
                .baseUrl(CURRENCY_BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(CurrencyApiService::class.java)
    }
}
