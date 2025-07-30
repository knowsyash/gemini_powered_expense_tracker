package com.expensetracker.ai.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.expensetracker.ai.data.model.*
import com.expensetracker.ai.data.repository.ChatMessageRepository
import com.expensetracker.ai.data.repository.ExpenseRepository
import com.expensetracker.ai.network.RetrofitClient
import com.expensetracker.ai.utils.Constants
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ChatViewModel(
        private val chatMessageRepository: ChatMessageRepository,
        private val expenseRepository: ExpenseRepository
) : ViewModel() {

    private val _chatMessages = MutableLiveData<List<ChatMessage>>()
    val chatMessages: LiveData<List<ChatMessage>> = _chatMessages

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        loadChatMessages()
        setupWelcomeMessage()
    }

    private fun loadChatMessages() {
        viewModelScope.launch {
            chatMessageRepository.getAllChatMessages().collect { messages ->
                // Only show the last 20 messages to avoid overwhelming the user
                _chatMessages.value = messages.takeLast(20)
            }
        }
    }

    private fun setupWelcomeMessage() {
        viewModelScope.launch {
            val existingMessageCount = chatMessageRepository.getChatMessageCount()
            if (existingMessageCount == 0) {
                val welcomeMessage =
                        """Welcome to your Financial Assistant!

I can help you track expenses, manage income, and provide financial insights.

Try saying:
• "I spent 500 on food" (expense)
• "My friend gave me 2000rs" (income)
• "Add 200 rs" (income)
• "I earned 5000 salary" (income)
• "Paid 50 for uber" (expense)
• "What's my balance?"
• "Show me spending analysis"
• "Show transactions from July 5th" (date search)
• "Transactions on Sunday" (day search)
• "Yesterday's transactions" (recent search)
• "Clear all transactions" (delete all data)
• "Clear recent transactions" (delete last 5)
• "Clear chat" (clear chat history)

How can I help you today?"""

                addMessage(
                        ChatMessage.create(
                                message = welcomeMessage,
                                isUser = false,
                                messageType = MessageType.AI_RESPONSE
                        )
                )
            }
        }
    }

    fun sendMessage(userMessage: String) {
        // Check if user wants to clear chat FIRST (before transaction deletion checks)
        if (userMessage.toLowerCase().contains("clear all chat") ||
                        userMessage.toLowerCase().contains("clear chat") ||
                        userMessage.toLowerCase().contains("clear previous chat") ||
                        userMessage.toLowerCase().contains("delete all chat") ||
                        userMessage.toLowerCase().contains("delete all chats") ||
                        userMessage.toLowerCase().contains("delete chat") ||
                        userMessage.toLowerCase().contains("delete chats") ||
                        (userMessage.toLowerCase().contains("clear all") &&
                                !userMessage.toLowerCase().contains("transaction"))
        ) {
            clearAllMessages()
            return
        }

        // Check if user wants to delete all transactions
        if (userMessage.toLowerCase().contains("delete all transaction") ||
                        userMessage.toLowerCase().contains("delete all transactions") ||
                        userMessage.toLowerCase().contains("clear all transaction") ||
                        userMessage.toLowerCase().contains("clear all transactions") ||
                        userMessage.toLowerCase().contains("remove all transaction") ||
                        userMessage.toLowerCase().contains("remove all transactions") ||
                        userMessage.toLowerCase().contains("clear transaction") ||
                        userMessage.toLowerCase().contains("clear transactions") ||
                        userMessage.toLowerCase().contains("delete transaction") ||
                        userMessage.toLowerCase().contains("delete transactions")
        ) {
            deleteAllTransactions(userMessage)
            return
        }

        // Check if user wants to delete recent transactions
        if (userMessage.toLowerCase().contains("clear recent") ||
                        userMessage.toLowerCase().contains("delete recent") ||
                        userMessage.toLowerCase().contains("remove recent") ||
                        userMessage.toLowerCase().contains("clear last") ||
                        userMessage.toLowerCase().contains("delete last") ||
                        userMessage.toLowerCase().contains("remove last")
        ) {
            deleteRecentTransactions(userMessage)
            return
        }

        // Check if user asks for categories
        if (userMessage.toLowerCase().contains("categories") ||
                        userMessage.toLowerCase().contains("what are the categories")
        ) {
            showCategories(userMessage)
            return
        }

        // Check if user asks for specific date transactions
        if (userMessage.toLowerCase().contains("transaction") &&
                        (userMessage.toLowerCase().contains("on") ||
                                userMessage.toLowerCase().contains("from") ||
                                userMessage.toLowerCase().contains("sunday") ||
                                userMessage.toLowerCase().contains("monday") ||
                                userMessage.toLowerCase().contains("tuesday") ||
                                userMessage.toLowerCase().contains("wednesday") ||
                                userMessage.toLowerCase().contains("thursday") ||
                                userMessage.toLowerCase().contains("friday") ||
                                userMessage.toLowerCase().contains("saturday") ||
                                userMessage.toLowerCase().contains("january") ||
                                userMessage.toLowerCase().contains("february") ||
                                userMessage.toLowerCase().contains("march") ||
                                userMessage.toLowerCase().contains("april") ||
                                userMessage.toLowerCase().contains("may") ||
                                userMessage.toLowerCase().contains("june") ||
                                userMessage.toLowerCase().contains("july") ||
                                userMessage.toLowerCase().contains("august") ||
                                userMessage.toLowerCase().contains("september") ||
                                userMessage.toLowerCase().contains("october") ||
                                userMessage.toLowerCase().contains("november") ||
                                userMessage.toLowerCase().contains("december") ||
                                userMessage.toLowerCase().contains("today") ||
                                userMessage.toLowerCase().contains("yesterday") ||
                                userMessage.toLowerCase().contains("last week") ||
                                userMessage.toLowerCase().contains("this week"))
        ) {
            searchTransactionsByDate(userMessage)
            return
        }

        addMessage(ChatMessage.create(userMessage, true, messageType = MessageType.USER_MESSAGE))

        viewModelScope.launch {
            val response = processMessage(userMessage)
            addMessage(
                    ChatMessage.create(
                            message = response,
                            isUser = false,
                            messageType = MessageType.AI_RESPONSE
                    )
            )
        }
    }

    private suspend fun processMessage(message: String): String {
        _isLoading.value = true
        return try {
            android.util.Log.d("ChatViewModel", "🔄 Processing message: '$message'")

            // FIRST PRIORITY: Try to parse as a transaction (this is most important)
            // Check if message contains any number - if yes, try transaction parsing
            if (message.matches(Regex(".*\\d+.*"))) {
                android.util.Log.d(
                        "ChatViewModel",
                        "🔍 Message contains numbers, attempting transaction parsing"
                )
                val transactionResult = tryParseTransaction(message)
                if (transactionResult != null) {
                    android.util.Log.d("ChatViewModel", "✅ Successfully processed as transaction")
                    return transactionResult
                }
            }

            // SECOND PRIORITY: Check for balance/summary requests
            val balanceResult = tryHandleBalanceQuery(message)
            if (balanceResult != null) {
                android.util.Log.d("ChatViewModel", "✅ Successfully processed as balance query")
                return balanceResult
            }

            // THIRD PRIORITY: Use Gemini AI for general financial advice
            android.util.Log.d("ChatViewModel", "🔄 Processing as general query with Gemini")
            getGeminiResponse(message)
        } catch (e: Exception) {
            android.util.Log.e("ChatViewModel", "❌ Error processing message: ${e.message}")
            "I encountered an error processing your request. Please try again or rephrase your message.\n\nFor transactions, try: 'I earned 200rs' or 'I spent 100 on food'"
        } finally {
            _isLoading.value = false
        }
    }

    private suspend fun tryParseTransaction(message: String): String? {
        android.util.Log.d("ChatViewModel", "🔍 Attempting to parse transaction: '$message'")

        // Look for amount patterns (improved to catch more formats)
        val amountRegex =
                Regex(
                        """(\d+(?:\.\d{1,2})?)(?:\s*(?:rs|rupees|₹|dollars?|usd))?""",
                        RegexOption.IGNORE_CASE
                )
        val amountMatch = amountRegex.find(message)

        if (amountMatch == null) {
            android.util.Log.d("ChatViewModel", "❌ No amount found in message: '$message'")
            return null
        }

        val amount = amountMatch.groupValues[1].toDoubleOrNull()
        if (amount == null || amount <= 0) {
            android.util.Log.d(
                    "ChatViewModel",
                    "❌ Invalid amount found: '${amountMatch.groupValues[1]}'"
            )
            return null
        }

        android.util.Log.d("ChatViewModel", "✅ Amount detected: ₹$amount")

        // ALWAYS use Gemini AI to determine transaction type - THIS IS CRITICAL
        val isIncome = determineTransactionTypeWithGemini(message)
        android.util.Log.d(
                "ChatViewModel",
                "🤖 Gemini AI classified as: ${if (isIncome) "INCOME" else "EXPENSE"}"
        )

        // Extract category
        val category = extractCategory(message.toLowerCase())

        // Create description
        val description = if (message.length > 50) message.take(47) + "..." else message

        // Generate unique ID
        val uniqueId = generateUniqueId()

        // Create expense
        val expense =
                Expense(
                        uniqueId = uniqueId,
                        amount = amount,
                        category = category,
                        description = description,
                        date = Date(),
                        isIncome = isIncome
                )

        // Save to database
        expenseRepository.insertExpense(expense)
        android.util.Log.d(
                "ChatViewModel",
                "💾 Transaction saved: ${if (isIncome) "+" else "-"}₹$amount"
        )

        // Return confirmation message
        val type = if (isIncome) "Income" else "Expense"
        val symbol = if (isIncome) "+" else "-"

        return """
✅ **Transaction Added Successfully!**

💰 **Amount:** ${symbol}₹${String.format("%.2f", amount)}
📁 **Category:** $category
📝 **Description:** $description
🆔 **ID:** $uniqueId
⏰ **Date:** ${SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date())}
🤖 **Type:** $type (AI classified)

Your $type has been recorded in the system.
        """.trimIndent()
    }

    private suspend fun determineTransactionTypeWithGemini(message: String): Boolean {
        android.util.Log.d("ChatViewModel", "🤖 Calling Gemini AI to classify: '$message'")

        return try {
            val prompt =
                    """
You are an expert financial transaction classifier. Analyze this message and determine if it represents INCOME or EXPENSE.

Message: "$message"

INCOME Examples (money COMING TO user):
- "I earned 2000rs" → INCOME
- "I get 200rs" → INCOME  
- "I got 200rs" → INCOME
- "received 500rs" → INCOME
- "salary 3000" → INCOME
- "bonus 1000" → INCOME
- "friend gave me 500" → INCOME
- "cashback 100" → INCOME
- "refund 200" → INCOME

EXPENSE Examples (money GOING FROM user):
- "I spent 500 on food" → EXPENSE
- "I lost 200rs" → EXPENSE
- "paid 100 for coffee" → EXPENSE
- "bought something for 300" → EXPENSE
- "gave 200 to friend" → EXPENSE

Key Rules:
- "get/got/received/earned/salary/bonus/refund/cashback" = INCOME
- "spent/lost/paid/bought/gave" = EXPENSE
- If unclear, default to EXPENSE

Respond with EXACTLY ONE WORD: "INCOME" or "EXPENSE"
"""

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
                                ?.trim()
                                ?.uppercase()

                android.util.Log.d(
                        "ChatViewModel",
                        "🤖 Gemini AI Response: '$aiResponse' for message: '$message'"
                )

                return when (aiResponse) {
                    "INCOME" -> {
                        android.util.Log.d("ChatViewModel", "✅ AI: INCOME confirmed for '$message'")
                        true
                    }
                    "EXPENSE" -> {
                        android.util.Log.d(
                                "ChatViewModel",
                                "✅ AI: EXPENSE confirmed for '$message'"
                        )
                        false
                    }
                    else -> {
                        android.util.Log.w(
                                "ChatViewModel",
                                "⚠️ AI unclear response: '$aiResponse'. Using fallback for '$message'"
                        )
                        fallbackTransactionClassification(message)
                    }
                }
            } else {
                android.util.Log.e(
                        "ChatViewModel",
                        "❌ Gemini API request failed. Response code: ${response.code()}"
                )
                return fallbackTransactionClassification(message)
            }
        } catch (e: Exception) {
            android.util.Log.e("ChatViewModel", "❌ Gemini API exception: ${e.message}")
            return fallbackTransactionClassification(message)
        }
    }

    private fun fallbackTransactionClassification(message: String): Boolean {
        val lowerMessage = message.toLowerCase()
        android.util.Log.d("ChatViewModel", "🔄 Using fallback classification for: '$message'")

        // Strong income indicators (updated with more patterns)
        val incomeKeywords =
                listOf(
                        "earned",
                        "income",
                        "salary",
                        "bonus",
                        "received",
                        "got paid",
                        "refund",
                        "cashback",
                        "revenue",
                        "profit",
                        "gave me",
                        "paid me",
                        "get",
                        "got",
                        "receive",
                        "earn"
                )

        // Strong expense indicators (updated with more patterns)
        val expenseKeywords =
                listOf(
                        "spent",
                        "paid",
                        "bought",
                        "purchased",
                        "cost",
                        "expense",
                        "shopping",
                        "bill",
                        "gave to",
                        "paid for",
                        "lost",
                        "lose"
                )

        // Check income keywords first
        for (keyword in incomeKeywords) {
            if (lowerMessage.contains(keyword)) {
                android.util.Log.d(
                        "ChatViewModel",
                        "🔄 Fallback: Found INCOME keyword '$keyword' in '$message'"
                )
                return true
            }
        }

        // Check expense keywords
        for (keyword in expenseKeywords) {
            if (lowerMessage.contains(keyword)) {
                android.util.Log.d(
                        "ChatViewModel",
                        "🔄 Fallback: Found EXPENSE keyword '$keyword' in '$message'"
                )
                return false
            }
        }

        // Special pattern checks
        if (lowerMessage.contains("i get") ||
                        lowerMessage.contains("i got") ||
                        lowerMessage.contains("i receive") ||
                        lowerMessage.contains("i earn")
        ) {
            android.util.Log.d(
                    "ChatViewModel",
                    "🔄 Fallback: Income pattern detected in '$message'"
            )
            return true
        }

        if (lowerMessage.contains("i lost") ||
                        lowerMessage.contains("i spend") ||
                        lowerMessage.contains("i pay") ||
                        lowerMessage.contains("i give")
        ) {
            android.util.Log.d(
                    "ChatViewModel",
                    "🔄 Fallback: Expense pattern detected in '$message'"
            )
            return false
        }

        // Default to expense if completely unclear
        android.util.Log.d(
                "ChatViewModel",
                "🔄 Fallback: No clear indicators, defaulting to EXPENSE for '$message'"
        )
        return false
    }

    private fun extractCategory(message: String): String {
        val categoryKeywords =
                mapOf(
                        "food" to "Food & Dining",
                        "restaurant" to "Food & Dining",
                        "lunch" to "Food & Dining",
                        "dinner" to "Food & Dining",
                        "snack" to "Food & Dining",
                        "coffee" to "Food & Dining",
                        "transport" to "Transportation",
                        "uber" to "Transportation",
                        "taxi" to "Transportation",
                        "bus" to "Transportation",
                        "metro" to "Transportation",
                        "petrol" to "Transportation",
                        "fuel" to "Transportation",
                        "shopping" to "Shopping",
                        "clothes" to "Shopping",
                        "shoes" to "Shopping",
                        "entertainment" to "Entertainment",
                        "movie" to "Entertainment",
                        "game" to "Entertainment",
                        "bill" to "Bills & Utilities",
                        "electricity" to "Bills & Utilities",
                        "internet" to "Bills & Utilities",
                        "phone" to "Bills & Utilities",
                        "rent" to "Bills & Utilities",
                        "doctor" to "Healthcare",
                        "medicine" to "Healthcare",
                        "hospital" to "Healthcare",
                        "health" to "Healthcare",
                        "education" to "Education",
                        "course" to "Education",
                        "book" to "Education",
                        "travel" to "Travel",
                        "hotel" to "Travel",
                        "flight" to "Travel",
                        "grocery" to "Food & Dining",
                        "groceries" to "Food & Dining"
                )

        for ((keyword, category) in categoryKeywords) {
            if (message.contains(keyword)) {
                return category
            }
        }

        return "Other"
    }

    private fun generateUniqueId(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..5).map { chars[Random.nextInt(chars.length)] }.joinToString("")
    }

    private suspend fun tryHandleBalanceQuery(message: String): String? {
        val lowerMessage = message.toLowerCase()

        if (lowerMessage.contains("balance") ||
                        lowerMessage.contains("total") ||
                        lowerMessage.contains("summary") ||
                        lowerMessage.contains("how much")
        ) {

            val totalIncome = expenseRepository.getTotalIncome()
            val totalExpenses = expenseRepository.getTotalExpenses()
            val balance = totalIncome - totalExpenses

            return """
📊 **Financial Summary**

💰 **Total Income:** +₹${String.format("%.2f", totalIncome)}
💸 **Total Expenses:** -₹${String.format("%.2f", totalExpenses)}
📈 **Current Balance:** ₹${String.format("%.2f", balance)}

${if (balance >= 0) "✅ You're in good financial shape!" else "⚠️ Consider reviewing your expenses."}
            """.trimIndent()
        }

        return null
    }

    private suspend fun getGeminiResponse(message: String): String {
        return try {
            // Check if this might be a transaction that wasn't caught
            if (message.matches(Regex(".*\\d+.*"))) {
                android.util.Log.d(
                        "ChatViewModel",
                        "🤔 Message has numbers but wasn't parsed as transaction, asking Gemini"
                )
                val prompt =
                        """
The user said: "$message"

This message contains numbers. Is this a financial transaction? If so, help me understand:
1. Is this income (money coming to user) or expense (money going from user)?  
2. What's the amount?

If it's NOT a transaction, just provide a helpful financial assistant response.

Examples of transactions:
- "today I get 200rs" = Income of 200rs
- "I lost 200rs today" = Expense of 200rs  
- "got 500 from friend" = Income of 500rs

Provide a helpful response under 150 words.
"""
                val request =
                        GeminiRequest(contents = listOf(Content(parts = listOf(Part(prompt)))))
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
                        android.util.Log.d(
                                "ChatViewModel",
                                "✅ Gemini transaction analysis response received"
                        )
                        return aiResponse.trim() +
                                "\n\n💡 *Tip: For clearer transactions, try 'I earned 200rs' or 'I spent 100 on food'*"
                    }
                }
            } else {
                // Regular financial advice
                val prompt =
                        """
You are a helpful financial assistant. The user said: "$message"

Provide helpful, practical financial advice or answer their question. Keep it under 150 words and be friendly.
"""
                val request =
                        GeminiRequest(contents = listOf(Content(parts = listOf(Part(prompt)))))
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
                        android.util.Log.d("ChatViewModel", "✅ Gemini general response received")
                        return aiResponse.trim()
                    }
                }
            }

            android.util.Log.w("ChatViewModel", "⚠️ Gemini response was empty")
            "I'm here to help with your finances! 💰\n\nFor transactions, try:\n• 'I earned 200rs'\n• 'I spent 100 on food'\n• 'I got 500 from friend'\n\nYou can also ask for your balance or financial advice!"
        } catch (e: Exception) {
            android.util.Log.e("ChatViewModel", "❌ Error getting Gemini response: ${e.message}")
            "I'm having trouble with my AI connection right now. 🤖\n\nYou can still add transactions like:\n• 'I earned 200rs'\n• 'I spent 100 on food'\n\nOr ask for your balance!"
        }
    }

    private fun addMessage(message: ChatMessage) {
        viewModelScope.launch {
            chatMessageRepository.insertChatMessage(message)
            chatMessageRepository.cleanupOldMessages()
        }
    }

    fun clearAllMessages() {
        viewModelScope.launch {
            chatMessageRepository.clearAllMessages()

            // Add confirmation message
            val confirmMessage =
                    ChatMessage.create(
                            message = "✅ All previous messages have been cleared successfully!",
                            isUser = false,
                            messageType = MessageType.AI_RESPONSE
                    )
            chatMessageRepository.insertChatMessage(confirmMessage)
        }
    }

    private fun showCategories(userMessage: String) {
        addMessage(ChatMessage.create(userMessage, true, messageType = MessageType.USER_MESSAGE))

        viewModelScope.launch {
            val categoriesMessage =
                    """
📋 **Available Expense Categories:**

🍕 **Food & Dining**
🚗 **Transportation** 
🛍️ **Shopping**
🎬 **Entertainment**
💡 **Bills & Utilities**
🏥 **Healthcare**
📚 **Education**
✈️ **Travel**
🏠 **Housing**
👕 **Clothing**
💼 **Business**
🎁 **Gifts & Donations**
📱 **Technology**
💰 **Other**

Simply mention any category when adding expenses!
Example: "I spent ₹500 on food"
""".trimIndent()

            addMessage(
                    ChatMessage.create(
                            message = categoriesMessage,
                            isUser = false,
                            messageType = MessageType.AI_RESPONSE
                    )
            )
        }
    }

    private fun deleteAllTransactions(userMessage: String) {
        addMessage(ChatMessage.create(userMessage, true, messageType = MessageType.USER_MESSAGE))

        viewModelScope.launch {
            try {
                // Get current totals before deletion for confirmation
                val totalExpenses = expenseRepository.getTotalExpenses()
                val totalIncome = expenseRepository.getTotalIncome()
                val balance = totalIncome - totalExpenses
                val allExpenses = expenseRepository.getAllExpenses()
                val expenses = allExpenses.first()
                val transactionCount = expenses.size

                // Delete all transactions
                expenseRepository.deleteAllExpenses()

                val confirmationMessage =
                        """
🗑️ **All Transactions Deleted Successfully!**

📊 **Previous Summary:**
💰 **Total Income:** +₹${String.format("%.2f", totalIncome)}
💸 **Total Expenses:** -₹${String.format("%.2f", totalExpenses)}
📈 **Previous Balance:** ₹${String.format("%.2f", balance)}
🔢 **Transactions Deleted:** $transactionCount

📈 **Current Status:**
💰 **Total Income:** ₹0.00
💸 **Total Expenses:** ₹0.00
📈 **Current Balance:** ₹0.00

✅ Your account has been reset to zero. All transaction history has been cleared.
""".trimIndent()

                addMessage(
                        ChatMessage.create(
                                message = confirmationMessage,
                                isUser = false,
                                messageType = MessageType.AI_RESPONSE
                        )
                )
            } catch (e: Exception) {
                val errorMessage =
                        """
❌ **Error Deleting Transactions**

Sorry, there was an error while trying to delete all transactions: ${e.message}

Please try again later or check your app settings.
""".trimIndent()

                addMessage(
                        ChatMessage.create(
                                message = errorMessage,
                                isUser = false,
                                messageType = MessageType.AI_RESPONSE
                        )
                )
            }
        }
    }

    private fun deleteRecentTransactions(userMessage: String) {
        addMessage(ChatMessage.create(userMessage, true, messageType = MessageType.USER_MESSAGE))

        viewModelScope.launch {
            try {
                val allExpenses = expenseRepository.getAllExpenses()
                val expenses = allExpenses.first()

                if (expenses.isEmpty()) {
                    val noTransactionsMessage =
                            """
❌ **No Transactions to Delete**

There are no transactions to clear. Your account is already empty.
""".trimIndent()

                    addMessage(
                            ChatMessage.create(
                                    message = noTransactionsMessage,
                                    isUser = false,
                                    messageType = MessageType.AI_RESPONSE
                            )
                    )
                    return@launch
                }

                // Get the last 5 transactions (most recent)
                val recentTransactions = expenses.sortedByDescending { it.date }.take(5)
                val recentCount = recentTransactions.size

                // Calculate totals of recent transactions
                val recentIncome = recentTransactions.filter { it.isIncome }.sumOf { it.amount }
                val recentExpenses = recentTransactions.filter { !it.isIncome }.sumOf { it.amount }

                // Delete recent transactions
                recentTransactions.forEach { expense -> expenseRepository.deleteExpense(expense) }

                // Get updated totals
                val updatedAllExpenses = expenseRepository.getAllExpenses()
                val updatedExpenses = updatedAllExpenses.first()
                val totalIncome = expenseRepository.getTotalIncome()
                val totalExpensesAmount = expenseRepository.getTotalExpenses()
                val balance = totalIncome - totalExpensesAmount

                val confirmationMessage =
                        """
🗑️ **Recent Transactions Deleted Successfully!**

📊 **Deleted Transactions:**
💰 **Recent Income Deleted:** -₹${String.format("%.2f", recentIncome)}
💸 **Recent Expenses Deleted:** -₹${String.format("%.2f", recentExpenses)}
🔢 **Transactions Deleted:** $recentCount (most recent)

📈 **Current Status:**
💰 **Total Income:** +₹${String.format("%.2f", totalIncome)}
💸 **Total Expenses:** -₹${String.format("%.2f", totalExpensesAmount)}
📈 **Current Balance:** ₹${String.format("%.2f", balance)}
📝 **Remaining Transactions:** ${updatedExpenses.size}

✅ Your recent transactions have been cleared successfully.
""".trimIndent()

                addMessage(
                        ChatMessage.create(
                                message = confirmationMessage,
                                isUser = false,
                                messageType = MessageType.AI_RESPONSE
                        )
                )
            } catch (e: Exception) {
                val errorMessage =
                        """
❌ **Error Deleting Recent Transactions**

Sorry, there was an error while trying to delete recent transactions: ${e.message}

Please try again later or check your app settings.
""".trimIndent()

                addMessage(
                        ChatMessage.create(
                                message = errorMessage,
                                isUser = false,
                                messageType = MessageType.AI_RESPONSE
                        )
                )
            }
        }
    }

    private fun searchTransactionsByDate(userMessage: String) {
        addMessage(ChatMessage.create(userMessage, true, messageType = MessageType.USER_MESSAGE))

        viewModelScope.launch {
            try {
                // Use Gemini AI to parse the date from the user query
                val parsedDateRange = parseDateWithGemini(userMessage)

                if (parsedDateRange == null) {
                    val errorMessage =
                            """
❌ **Could not understand the date**

I couldn't parse the date from your request. Please try again with formats like:
• "Show transactions from July 5th"
• "Transactions on Sunday"
• "Show me yesterday's transactions"
• "Transactions from last week"
""".trimIndent()

                    addMessage(
                            ChatMessage.create(
                                    message = errorMessage,
                                    isUser = false,
                                    messageType = MessageType.AI_RESPONSE
                            )
                    )
                    return@launch
                }

                // Get transactions from database for the parsed date range
                val transactions =
                        expenseRepository.getExpensesByDateRangeSync(
                                parsedDateRange.first,
                                parsedDateRange.second
                        )

                // Debug logging
                android.util.Log.d(
                        "ChatViewModel",
                        "Searching transactions for range: ${parsedDateRange.first} to ${parsedDateRange.second}"
                )
                android.util.Log.d(
                        "ChatViewModel",
                        "Date range: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(parsedDateRange.first))} to ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(parsedDateRange.second))}"
                )
                android.util.Log.d("ChatViewModel", "Found ${transactions.size} transactions")

                // Also get ALL transactions to see what we have
                val allTransactions = expenseRepository.getAllExpenses().first()
                android.util.Log.d(
                        "ChatViewModel",
                        "Total transactions in database: ${allTransactions.size}"
                )
                allTransactions.forEach { transaction ->
                    android.util.Log.d(
                            "ChatViewModel",
                            "Transaction: ${transaction.date.time} (${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(transaction.date)}) - ${transaction.description} - ₹${transaction.amount}"
                    )
                }

                if (transactions.isEmpty()) {
                    // If no transactions found with date filter, let's also try a manual filter as
                    // backup
                    val manuallyFiltered =
                            allTransactions.filter { transaction ->
                                transaction.date.time >= parsedDateRange.first &&
                                        transaction.date.time <= parsedDateRange.second
                            }
                    android.util.Log.d(
                            "ChatViewModel",
                            "Manual filter found: ${manuallyFiltered.size} transactions"
                    )

                    if (manuallyFiltered.isNotEmpty()) {
                        // Use manually filtered results
                        val transactionsList =
                                formatTransactionsList(manuallyFiltered, parsedDateRange)
                        addMessage(
                                ChatMessage.create(
                                        message = transactionsList,
                                        isUser = false,
                                        messageType = MessageType.AI_RESPONSE
                                )
                        )
                        return@launch
                    }

                    val noTransactionsMessage =
                            """
📅 **No Transactions Found**

No transactions found for the specified date/period.

🔍 **Debug Info:**
• Date range searched: ${SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(parsedDateRange.first))} to ${SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(parsedDateRange.second))}
• Total transactions in database: ${allTransactions.size}

📋 **All your transactions:**
${allTransactions.take(5).joinToString("\n") { "• ${SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(it.date)} - ${it.description} - ₹${it.amount}" }}

Would you like to:
• Check a different date
• View all transactions with "what's my balance?"
• Add a new transaction
""".trimIndent()

                    addMessage(
                            ChatMessage.create(
                                    message = noTransactionsMessage,
                                    isUser = false,
                                    messageType = MessageType.AI_RESPONSE
                            )
                    )
                } else {
                    // Format and display the found transactions
                    val transactionsList = formatTransactionsList(transactions, parsedDateRange)

                    addMessage(
                            ChatMessage.create(
                                    message = transactionsList,
                                    isUser = false,
                                    messageType = MessageType.AI_RESPONSE
                            )
                    )
                }
            } catch (e: Exception) {
                val errorMessage =
                        """
❌ **Error Searching Transactions**

Sorry, there was an error while searching for transactions: ${e.message}

Please try again later.
""".trimIndent()

                addMessage(
                        ChatMessage.create(
                                message = errorMessage,
                                isUser = false,
                                messageType = MessageType.AI_RESPONSE
                        )
                )
            }
        }
    }

    private suspend fun parseDateWithGemini(userQuery: String): Pair<Long, Long>? {
        return try {
            val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            // Check for simple patterns first before using AI
            val today = Calendar.getInstance()

            // Handle "today" or "29 july" or "july 29" patterns
            if (userQuery.toLowerCase().contains("today") ||
                            userQuery.toLowerCase().contains("29 july") ||
                            userQuery.toLowerCase().contains("july 29") ||
                            userQuery.toLowerCase().contains("29th july") ||
                            userQuery.toLowerCase().contains("july 29th")
            ) {

                // Set to today (July 29, 2025)
                today.set(Calendar.HOUR_OF_DAY, 0)
                today.set(Calendar.MINUTE, 0)
                today.set(Calendar.SECOND, 0)
                today.set(Calendar.MILLISECOND, 0)
                val startTime = today.timeInMillis

                today.set(Calendar.HOUR_OF_DAY, 23)
                today.set(Calendar.MINUTE, 59)
                today.set(Calendar.SECOND, 59)
                today.set(Calendar.MILLISECOND, 999)
                val endTime = today.timeInMillis

                android.util.Log.d(
                        "ChatViewModel",
                        "Date parsing for '$userQuery': Start=$startTime, End=$endTime"
                )
                return Pair(startTime, endTime)
            }

            val prompt =
                    """
You are a date parser. Parse the following user query and return the date range in milliseconds.

Current date: $currentDate (July 29, 2025)

User query: "$userQuery"

Instructions:
- If user mentions "29 July" or "July 29" or "29th July", use July 29, 2025
- If user mentions specific date like "July 5th" or "5th July", calculate that date for current year 2025
- If user mentions day names like "Sunday", "Monday", find the most recent occurrence
- If user mentions "today", use current date (July 29, 2025)
- If user mentions "yesterday", use July 28, 2025
- If user mentions "last week" or "this week", provide the week range
- If user mentions month name, provide the full month range for current year

Respond in this EXACT format:
START_TIMESTAMP:XXXXXXXXXX
END_TIMESTAMP:XXXXXXXXXX

Where XXXXXXXXXX is the timestamp in milliseconds.
For a single day, make start and end timestamps cover the full day (00:00:00 to 23:59:59).

Example for July 29, 2025:
START_TIMESTAMP:1753862400000
END_TIMESTAMP:1753948799999
"""

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
                                ?.trim()

                android.util.Log.d(
                        "ChatViewModel",
                        "Gemini AI response for '$userQuery': $aiResponse"
                )

                if (aiResponse != null) {
                    val lines = aiResponse.split("\n")
                    var startTimestamp: Long? = null
                    var endTimestamp: Long? = null

                    for (line in lines) {
                        if (line.startsWith("START_TIMESTAMP:")) {
                            startTimestamp = line.substringAfter("START_TIMESTAMP:").toLongOrNull()
                        } else if (line.startsWith("END_TIMESTAMP:")) {
                            endTimestamp = line.substringAfter("END_TIMESTAMP:").toLongOrNull()
                        }
                    }

                    if (startTimestamp != null && endTimestamp != null) {
                        android.util.Log.d(
                                "ChatViewModel",
                                "Parsed timestamps: Start=$startTimestamp, End=$endTimestamp"
                        )
                        return Pair(startTimestamp, endTimestamp)
                    }
                }
            }

            android.util.Log.e(
                    "ChatViewModel",
                    "Failed to parse date with Gemini for: '$userQuery'"
            )
            null
        } catch (e: Exception) {
            android.util.Log.e("ChatViewModel", "Error parsing date with Gemini: ${e.message}")
            null
        }
    }

    private fun formatTransactionsList(
            transactions: List<Expense>,
            dateRange: Pair<Long, Long>
    ): String {
        val startDate =
                SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(dateRange.first))
        val endDate =
                SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(dateRange.second))

        val totalIncome = transactions.filter { it.isIncome }.sumOf { it.amount }
        val totalExpenses = transactions.filter { !it.isIncome }.sumOf { it.amount }
        val netAmount = totalIncome - totalExpenses

        val dateRangeText = if (startDate == endDate) startDate else "$startDate to $endDate"

        val transactionItems =
                transactions.sortedByDescending { it.date }.take(10).joinToString("\n\n") {
                        transaction ->
                    val symbol = if (transaction.isIncome) "+" else "-"
                    val type = if (transaction.isIncome) "Income" else "Expense"
                    val date =
                            SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                                    .format(transaction.date)

                    """💰 ${symbol}₹${String.format("%.2f", transaction.amount)} | $type
📁 Category: ${transaction.category}
📝 ${transaction.description}
🆔 ID: ${transaction.uniqueId}
⏰ $date"""
                }

        val limitText =
                if (transactions.size > 10)
                        "\n\n📋 Showing latest 10 of ${transactions.size} transactions"
                else ""

        return """
📅 **Transactions for $dateRangeText**

📊 **Summary:**
💰 **Total Income:** +₹${String.format("%.2f", totalIncome)}
💸 **Total Expenses:** -₹${String.format("%.2f", totalExpenses)}
📈 **Net Amount:** ₹${String.format("%.2f", netAmount)}
🔢 **Transaction Count:** ${transactions.size}

📋 **Transaction Details:**

$transactionItems$limitText
        """.trimIndent()
    }

    class Factory(
            private val chatMessageRepository: ChatMessageRepository,
            private val expenseRepository: ExpenseRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ChatViewModel(chatMessageRepository, expenseRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
