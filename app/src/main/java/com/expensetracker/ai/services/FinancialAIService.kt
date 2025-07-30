package com.expensetracker.ai.services

import com.expensetracker.ai.data.model.*
import com.expensetracker.ai.data.repository.ExpenseRepository
import com.expensetracker.ai.data.repository.FinancialInsightRepository
import com.expensetracker.ai.data.repository.SavingsGoalRepository
import com.expensetracker.ai.network.RetrofitClient
import com.expensetracker.ai.utils.Constants
import java.util.*
import kotlinx.coroutines.flow.first
import org.json.JSONObject

class FinancialAIService(
        private val expenseRepository: ExpenseRepository,
        private val savingsGoalRepository: SavingsGoalRepository,
        private val insightRepository: FinancialInsightRepository
) {

    suspend fun generateFinancialInsight(): FinancialInsight? {
        return try {
            val context = buildComprehensiveFinancialContext()
            val prompt = createInsightGenerationPrompt(context)

            val request = GeminiRequest(contents = listOf(Content(parts = listOf(Part(prompt)))))

            val response =
                    RetrofitClient.geminiApiService.generateContent(
                            apiKey = Constants.GEMINI_API_KEY,
                            request = request
                    )

            if (response.isSuccessful && response.body() != null) {
                val aiResponse =
                        response.body()!!
                                .candidates
                                .firstOrNull()
                                ?.content
                                ?.parts
                                ?.firstOrNull()
                                ?.text
                if (!aiResponse.isNullOrBlank()) {
                    parseInsightFromAIResponse(aiResponse, context)
                } else null
            } else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun analyzeSpendingPatterns(): String {
        val context = buildComprehensiveFinancialContext()
        val prompt = createSpendingAnalysisPrompt(context)

        return executeAIQuery(prompt) ?: "Unable to analyze spending patterns at this time."
    }

    suspend fun generateSavingsAdvice(): String {
        val context = buildComprehensiveFinancialContext()
        val prompt = createSavingsAdvicePrompt(context)

        return executeAIQuery(prompt) ?: "Unable to generate savings advice at this time."
    }

    suspend fun trackEarningsTrends(): String {
        val context = buildComprehensiveFinancialContext()
        val prompt = createEarningsAnalysisPrompt(context)

        return executeAIQuery(prompt) ?: "Unable to analyze earnings trends at this time."
    }

    suspend fun generateMonthlyFinancialSummary(): FinancialInsight? {
        val context = buildComprehensiveFinancialContext()
        val prompt = createMonthlySummaryPrompt(context)

        val aiResponse = executeAIQuery(prompt)
        return if (aiResponse != null) {
            FinancialInsight(
                    title = "Monthly Financial Summary",
                    description = aiResponse,
                    insightType = InsightType.MONTHLY_SUMMARY,
                    relevantData = context,
                    priority = InsightPriority.MEDIUM,
                    aiConfidence = 0.9
            )
        } else null
    }

    private suspend fun buildComprehensiveFinancialContext(): String {
        val expenses = expenseRepository.getAllExpenses().first()
        val savingsGoals = savingsGoalRepository.getAllSavingsGoals().first()
        val totalExpenses = expenseRepository.getTotalExpenses()
        val totalIncome = expenseRepository.getTotalIncome()
        val balance = totalIncome - totalExpenses

        // Recent transactions analysis
        val recentExpenses = expenses.take(20)
        val last30DaysExpenses =
                expenses.filter {
                    it.date.time >=
                            Calendar.getInstance()
                                    .apply { add(Calendar.DAY_OF_MONTH, -30) }
                                    .timeInMillis
                }

        // Category analysis
        val categoryTotals = mutableMapOf<String, Double>()
        Constants.EXPENSE_CATEGORIES.forEach { category ->
            val total = expenseRepository.getTotalExpensesByCategory(category)
            if (total > 0) {
                categoryTotals[category] = total
            }
        }

        // Income analysis
        val incomeTransactions = expenses.filter { it.isIncome }
        val monthlyIncome =
                incomeTransactions
                        .filter {
                            it.date.time >=
                                    Calendar.getInstance()
                                            .apply {
                                                set(Calendar.DAY_OF_MONTH, 1)
                                                set(Calendar.HOUR_OF_DAY, 0)
                                                set(Calendar.MINUTE, 0)
                                                set(Calendar.SECOND, 0)
                                            }
                                            .timeInMillis
                        }
                        .sumOf { it.amount }

        // Savings analysis
        val totalSavingsTarget = savingsGoalRepository.getTotalSavingsTarget()
        val totalCurrentSavings = savingsGoalRepository.getTotalCurrentSavings()
        val activeSavingsGoals = savingsGoals.filter { !it.isCompleted }

        return JSONObject()
                .apply {
                    put("totalExpenses", totalExpenses)
                    put("totalIncome", totalIncome)
                    put("currentBalance", balance)
                    put("monthlyIncome", monthlyIncome)
                    put("last30DaysExpenses", last30DaysExpenses.sumOf { it.amount })
                    put("transactionCount", expenses.size)
                    put("categoryBreakdown", JSONObject(categoryTotals as Map<*, *>))
                    put("savingsTarget", totalSavingsTarget)
                    put("currentSavings", totalCurrentSavings)
                    put("activeSavingsGoals", activeSavingsGoals.size)
                    put("completedGoalsCount", savingsGoals.count { it.isCompleted })
                    put(
                            "averageTransactionAmount",
                            if (expenses.isNotEmpty()) expenses.sumOf { it.amount } / expenses.size
                            else 0.0
                    )
                    put(
                            "highestExpenseCategory",
                            categoryTotals.maxByOrNull { it.value }?.key ?: "None"
                    )
                    put("recentTransactionsCount", recentExpenses.size)
                }
                .toString()
    }

    private fun createInsightGenerationPrompt(context: String): String {
        return """
        As an expert financial advisor AI, analyze the following comprehensive financial data and generate ONE actionable financial insight.

        FINANCIAL DATA:
        $context

        Generate a specific, actionable insight in the following JSON format:
        {
            "title": "Brief insight title (max 50 characters)",
            "description": "Detailed actionable advice (100-200 words)",
            "type": "SPENDING_PATTERN|SAVING_OPPORTUNITY|BUDGET_WARNING|INCOME_ANALYSIS|GOAL_PROGRESS|CATEGORY_OPTIMIZATION",
            "priority": "LOW|MEDIUM|HIGH|CRITICAL",
            "confidence": 0.0-1.0
        }

        Focus on the most important finding that could help improve their financial situation.
        """.trimIndent()
    }

    private fun createSpendingAnalysisPrompt(context: String): String {
        return """
        Analyze the spending patterns from this financial data and provide specific insights:
        
        FINANCIAL DATA:
        $context
        
        Provide a comprehensive spending analysis including:
        1. Top spending categories and trends
        2. Unusual spending patterns or anomalies
        3. Recommendations for optimization
        4. Comparison with typical financial health benchmarks
        
        Keep response concise but informative (150-200 words).
        """.trimIndent()
    }

    private fun createSavingsAdvicePrompt(context: String): String {
        return """
        Based on this financial data, provide personalized savings advice:
        
        FINANCIAL DATA:
        $context
        
        Include:
        1. Assessment of current savings rate
        2. Specific areas where savings can be increased
        3. Goal-oriented savings strategies
        4. Emergency fund recommendations
        
        Provide actionable, specific advice (150-200 words).
        """.trimIndent()
    }

    private fun createEarningsAnalysisPrompt(context: String): String {
        return """
        Analyze the earnings and income trends from this data:
        
        FINANCIAL DATA:
        $context
        
        Provide insights on:
        1. Income stability and trends
        2. Income vs expenses ratio
        3. Opportunities for income improvement
        4. Financial growth recommendations
        
        Keep response focused and actionable (150-200 words).
        """.trimIndent()
    }

    private fun createMonthlySummaryPrompt(context: String): String {
        return """
        Generate a comprehensive monthly financial summary based on this data:
        
        FINANCIAL DATA:
        $context
        
        Include:
        1. Key financial metrics for the month
        2. Progress towards savings goals
        3. Notable spending changes
        4. Recommendations for next month
        
        Provide a well-structured summary (200-250 words).
        """.trimIndent()
    }

    private suspend fun executeAIQuery(prompt: String): String? {
        return try {
            val request = GeminiRequest(contents = listOf(Content(parts = listOf(Part(prompt)))))

            val response =
                    RetrofitClient.geminiApiService.generateContent(
                            apiKey = Constants.GEMINI_API_KEY,
                            request = request
                    )

            if (response.isSuccessful && response.body() != null) {
                response.body()!!.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun parseInsightFromAIResponse(response: String, context: String): FinancialInsight? {
        return try {
            val json = JSONObject(response)

            val typeString = json.getString("type")
            val priorityString = json.getString("priority")

            val insightType = InsightType.valueOf(typeString)
            val priority = InsightPriority.valueOf(priorityString)

            FinancialInsight(
                    title = json.getString("title"),
                    description = json.getString("description"),
                    insightType = insightType,
                    relevantData = context,
                    priority = priority,
                    aiConfidence = json.getDouble("confidence")
            )
        } catch (e: Exception) {
            // Fallback if JSON parsing fails
            FinancialInsight(
                    title = "AI Financial Insight",
                    description = response,
                    insightType = InsightType.SPENDING_PATTERN,
                    relevantData = context,
                    priority = InsightPriority.MEDIUM,
                    aiConfidence = 0.7
            )
        }
    }
}
