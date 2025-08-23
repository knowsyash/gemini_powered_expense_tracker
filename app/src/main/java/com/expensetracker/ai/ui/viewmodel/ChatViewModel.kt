package com.expensetracker.ai.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.expensetracker.ai.data.model.*
import com.expensetracker.ai.data.repository.BudgetRepository
import com.expensetracker.ai.data.repository.ChatMessageRepository
import com.expensetracker.ai.data.repository.ExpenseRepository
import com.expensetracker.ai.network.RetrofitClient
import com.expensetracker.ai.utils.Constants
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(kotlin.ExperimentalStdlibApi::class)
class ChatViewModel(
        private val chatMessageRepository: ChatMessageRepository,
        private val expenseRepository: ExpenseRepository,
        private val budgetRepository: BudgetRepository
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
                if (userMessage.lowercase().contains("clear all chat") ||
                                userMessage.lowercase().contains("clear chat") ||
                                userMessage.lowercase().contains("clear previous chat") ||
                                userMessage.lowercase().contains("delete all chat") ||
                                userMessage.lowercase().contains("delete all chats") ||
                                userMessage.lowercase().contains("delete chat") ||
                                userMessage.lowercase().contains("delete chats") ||
                                (userMessage.lowercase().contains("clear all") &&
                                        !userMessage.lowercase().contains("transaction"))
                ) {
                        clearAllMessages()
                        return
                }

                // Check if user wants to delete all transactions
                if (userMessage.lowercase().contains("delete all transaction") ||
                                userMessage.lowercase().contains("delete all transactions") ||
                                userMessage.lowercase().contains("clear all transaction") ||
                                userMessage.lowercase().contains("clear all transactions") ||
                                userMessage.lowercase().contains("remove all transaction") ||
                                userMessage.lowercase().contains("remove all transactions") ||
                                userMessage.lowercase().contains("clear transaction") ||
                                userMessage.lowercase().contains("clear transactions") ||
                                userMessage.lowercase().contains("delete transaction") ||
                                userMessage.lowercase().contains("delete transactions")
                ) {
                        deleteAllTransactions(userMessage)
                        return
                }

                // Check if user wants to delete recent transactions
                if (userMessage.lowercase().contains("clear recent") ||
                                userMessage.lowercase().contains("delete recent") ||
                                userMessage.lowercase().contains("remove recent") ||
                                userMessage.lowercase().contains("clear last") ||
                                userMessage.lowercase().contains("delete last") ||
                                userMessage.lowercase().contains("remove last")
                ) {
                        deleteRecentTransactions(userMessage)
                        return
                }

                // Check if user asks for categories
                if (userMessage.lowercase().contains("categories") ||
                                userMessage.lowercase().contains("what are the categories")
                ) {
                        showCategories(userMessage)
                        return
                }

                // Check if user asks for specific date transactions
                if (userMessage.lowercase().contains("transaction") &&
                                (userMessage.lowercase().contains("on") ||
                                        userMessage.lowercase().contains("from") ||
                                        userMessage.lowercase().contains("sunday") ||
                                        userMessage.lowercase().contains("monday") ||
                                        userMessage.lowercase().contains("tuesday") ||
                                        userMessage.lowercase().contains("wednesday") ||
                                        userMessage.lowercase().contains("thursday") ||
                                        userMessage.lowercase().contains("friday") ||
                                        userMessage.lowercase().contains("saturday") ||
                                        userMessage.lowercase().contains("january") ||
                                        userMessage.lowercase().contains("february") ||
                                        userMessage.lowercase().contains("march") ||
                                        userMessage.lowercase().contains("april") ||
                                        userMessage.lowercase().contains("may") ||
                                        userMessage.lowercase().contains("june") ||
                                        userMessage.lowercase().contains("july") ||
                                        userMessage.lowercase().contains("august") ||
                                        userMessage.lowercase().contains("september") ||
                                        userMessage.lowercase().contains("october") ||
                                        userMessage.lowercase().contains("november") ||
                                        userMessage.lowercase().contains("december") ||
                                        userMessage.lowercase().contains("today") ||
                                        userMessage.lowercase().contains("yesterday") ||
                                        userMessage.lowercase().contains("last week") ||
                                        userMessage.lowercase().contains("this week"))
                ) {
                        searchTransactionsByDate(userMessage)
                        return
                }

                // Check if user wants to set budget - more specific detection to avoid conflicts
                val budgetKeywords =
                        listOf(
                                "budget",
                                "limit",
                                "allocate",
                                "plan"
                        ) // Removed "spend" to avoid conflicts
                val setBudgetKeywords =
                        listOf(
                                "set budget",
                                "my budget",
                                "budget is",
                                "budget of",
                                "monthly budget"
                        )
                val monthKeywords = listOf("month", "monthly")
                val numberPattern = ".*\\d+.*".toRegex()
                val currencySymbols = listOf("₹", "rs", "rupees", "inr")

                val containsBudgetKeyword =
                        budgetKeywords.any { userMessage.lowercase().contains(it) }
                val containsSetBudgetKeyword =
                        setBudgetKeywords.any { userMessage.lowercase().contains(it) }
                val containsMonthKeyword =
                        monthKeywords.any { userMessage.lowercase().contains(it) }
                val containsNumber = userMessage.matches(numberPattern)
                val containsCurrency = currencySymbols.any { userMessage.lowercase().contains(it) }

                android.util.Log.d("ChatViewModel", "Budget detection - Message: '$userMessage'")
                android.util.Log.d(
                        "ChatViewModel",
                        "Budget keyword: $containsBudgetKeyword, Set budget: $containsSetBudgetKeyword, Month: $containsMonthKeyword, Number: $containsNumber, Currency: $containsCurrency"
                )

                // More specific budget detection to avoid capturing regular transactions
                if (containsSetBudgetKeyword ||
                                (containsBudgetKeyword && containsMonthKeyword && containsNumber) ||
                                (userMessage.lowercase().contains("budget") &&
                                        containsNumber &&
                                        containsCurrency)
                ) {
                        android.util.Log.d("ChatViewModel", "🎯 Budget command detected!")
                        handleBudgetCommand(userMessage)
                        return
                }

                addMessage(
                        ChatMessage.create(
                                userMessage,
                                true,
                                messageType = MessageType.USER_MESSAGE
                        )
                )

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
                                        android.util.Log.d(
                                                "ChatViewModel",
                                                "✅ Successfully processed as transaction"
                                        )
                                        return transactionResult
                                }
                        }

                        // SECOND PRIORITY: Check for balance/summary requests
                        val balanceResult = tryHandleBalanceQuery(message)
                        if (balanceResult != null) {
                                android.util.Log.d(
                                        "ChatViewModel",
                                        "✅ Successfully processed as balance query"
                                )
                                return balanceResult
                        }

                        // THIRD PRIORITY: Use Gemini AI for general financial advice
                        android.util.Log.d(
                                "ChatViewModel",
                                "🔄 Processing as general query with Gemini"
                        )
                        getGeminiResponse(message)
                } catch (e: Exception) {
                        android.util.Log.e(
                                "ChatViewModel",
                                "❌ Error processing message: ${e.message}"
                        )
                        "I encountered an error processing your request. Please try again or rephrase your message.\n\nFor transactions, try: 'I earned 200rs' or 'I spent 100 on food'"
                } finally {
                        _isLoading.value = false
                }
        }

        private suspend fun tryParseTransaction(message: String): String? {
                android.util.Log.d(
                        "ChatViewModel",
                        "🔍 Attempting to parse transaction: '$message'"
                )

                // Look for amount patterns (improved to catch more formats)
                val amountRegex =
                        Regex(
                                """(\d+(?:\.\d{1,2})?)(?:\s*(?:rs|rupees|₹|dollars?|usd))?""",
                                RegexOption.IGNORE_CASE
                        )
                val amountMatch = amountRegex.find(message)

                if (amountMatch == null) {
                        android.util.Log.d(
                                "ChatViewModel",
                                "❌ No amount found in message: '$message'"
                        )
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
                val category = extractCategory(message.lowercase())

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
                android.util.Log.d("ChatViewModel", "🤖 Analyzing transaction: '$message'")

                // First, try the improved local classifier (faster and more reliable)
                val localResult = improvedLocalClassifier(message)
                if (localResult != null) {
                        android.util.Log.d(
                                "ChatViewModel",
                                "✅ Local classifier result: ${if (localResult) "INCOME" else "EXPENSE"}"
                        )
                        return localResult
                } else {
                        // If local classifier is unsure, try Gemini API
                        return try {
                                val prompt =
                                        """
Classify this financial transaction as INCOME or EXPENSE:

"$message"

INCOME (money received): earned, got, received, salary, bonus, refund, cashback, gave me, paid me
EXPENSE (money spent): spent, paid, bought, cost, lost, gave to, paid for

Reply with only: INCOME or EXPENSE
"""

                                val request =
                                        GeminiRequest(
                                                contents =
                                                        listOf(
                                                                Content(
                                                                        parts = listOf(Part(prompt))
                                                                )
                                                        )
                                        )
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
                                                        android.util.Log.d(
                                                                "ChatViewModel",
                                                                "✅ AI: INCOME confirmed for '$message'"
                                                        )
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
                                android.util.Log.e(
                                        "ChatViewModel",
                                        "❌ Gemini API exception: ${e.message}"
                                )
                                return fallbackTransactionClassification(message)
                        }
                }
        }

        private fun improvedLocalClassifier(message: String): Boolean? {
                val lowerMessage = message.lowercase()
                android.util.Log.d("ChatViewModel", "🔍 Local classifier analyzing: '$message'")

                // Strong INCOME indicators with high confidence
                val strongIncomePatterns =
                        listOf(
                                "i earned",
                                "i got",
                                "i received",
                                "earned",
                                "got paid",
                                "received",
                                "salary",
                                "bonus",
                                "refund",
                                "cashback",
                                "gave me",
                                "paid me",
                                "friend gave",
                                "got money",
                                "received money",
                                "income",
                                "profit"
                        )

                // Strong EXPENSE indicators with high confidence
                val strongExpensePatterns =
                        listOf(
                                "i spent",
                                "i paid",
                                "spent",
                                "paid",
                                "bought",
                                "cost",
                                "gave to",
                                "paid for",
                                "lost",
                                "expense",
                                "purchase",
                                "bill",
                                "fee"
                        )

                // Check strong income patterns
                for (pattern in strongIncomePatterns) {
                        if (lowerMessage.contains(pattern)) {
                                android.util.Log.d(
                                        "ChatViewModel",
                                        "✅ Strong INCOME pattern found: '$pattern'"
                                )
                                return true
                        }
                }

                // Check strong expense patterns
                for (pattern in strongExpensePatterns) {
                        if (lowerMessage.contains(pattern)) {
                                android.util.Log.d(
                                        "ChatViewModel",
                                        "✅ Strong EXPENSE pattern found: '$pattern'"
                                )
                                return false
                        }
                }

                // Special case for "spend" variations
                if (lowerMessage.contains("spend") && !lowerMessage.contains("spending money on me")
                ) {
                        android.util.Log.d("ChatViewModel", "✅ 'spend' detected -> EXPENSE")
                        return false
                }

                // If no strong patterns found, return null to indicate uncertainty
                android.util.Log.d(
                        "ChatViewModel",
                        "❓ Local classifier uncertain about: '$message'"
                )
                return null
        }

        private fun fallbackTransactionClassification(message: String): Boolean {
                val lowerMessage = message.lowercase()
                android.util.Log.d(
                        "ChatViewModel",
                        "🔄 Using enhanced fallback classification for: '$message'"
                )

                // Enhanced income indicators
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
                                "earn",
                                "commission",
                                "dividend",
                                "interest",
                                "allowance",
                                "stipend",
                                "reward",
                                "prize"
                        )

                // Enhanced expense indicators
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
                                "lose",
                                "fee",
                                "charge",
                                "tax",
                                "fine",
                                "donation",
                                "tip"
                        )

                // Score-based approach for better accuracy
                var incomeScore = 0
                var expenseScore = 0

                // Check income keywords and patterns
                for (keyword in incomeKeywords) {
                        if (lowerMessage.contains(keyword)) {
                                incomeScore += 2
                                android.util.Log.d(
                                        "ChatViewModel",
                                        "� Income keyword found: '$keyword' (+2)"
                                )
                        }
                }

                // Check expense keywords and patterns
                for (keyword in expenseKeywords) {
                        if (lowerMessage.contains(keyword)) {
                                expenseScore += 2
                                android.util.Log.d(
                                        "ChatViewModel",
                                        "� Expense keyword found: '$keyword' (+2)"
                                )
                        }
                }

                // Additional pattern matching with higher weights
                if (lowerMessage.matches(Regex(".*i\\s+(earned|got|received).*"))) {
                        incomeScore += 3
                        android.util.Log.d(
                                "ChatViewModel",
                                "📈 Strong income pattern: 'I earned/got/received' (+3)"
                        )
                }

                if (lowerMessage.matches(Regex(".*i\\s+(spent|paid|lost).*"))) {
                        expenseScore += 3
                        android.util.Log.d(
                                "ChatViewModel",
                                "� Strong expense pattern: 'I spent/paid/lost' (+3)"
                        )
                }

                // Context-based scoring
                if (lowerMessage.contains("give") && lowerMessage.contains("me")) {
                        incomeScore += 1
                } else if (lowerMessage.contains("give") && !lowerMessage.contains("me")) {
                        expenseScore += 1
                }

                android.util.Log.d(
                        "ChatViewModel",
                        "🎯 Final scores - Income: $incomeScore, Expense: $expenseScore"
                )

                return when {
                        incomeScore > expenseScore -> {
                                android.util.Log.d(
                                        "ChatViewModel",
                                        "✅ Classified as INCOME (score: $incomeScore vs $expenseScore)"
                                )
                                true
                        }
                        expenseScore > incomeScore -> {
                                android.util.Log.d(
                                        "ChatViewModel",
                                        "✅ Classified as EXPENSE (score: $expenseScore vs $incomeScore)"
                                )
                                false
                        }
                        else -> {
                                android.util.Log.d(
                                        "ChatViewModel",
                                        "⚖️ Tie score, defaulting to EXPENSE for safety"
                                )
                                false // Default to expense when uncertain
                        }
                }
        }

        private fun extractCategory(message: String): String {
                val lowerMessage = message.lowercase()

                // First check if this looks like a person name expense
                if (isProbablyPersonName(lowerMessage)) {
                        return "Friend"
                }

                // Enhanced category detection with subcategories using whole word matching
                val categoryKeywords =
                        mapOf(
                                // Travel category
                                "flight" to "Flight",
                                "airplane" to "Flight",
                                "plane" to "Flight",
                                "hotel" to "Hotel",
                                "accommodation" to "Hotel",
                                "stay" to "Hotel",
                                "taxi" to "Taxi/Uber",
                                "uber" to "Taxi/Uber",
                                "ola" to "Taxi/Uber",
                                "cab" to "Taxi/Uber",
                                "bus" to "Bus/Train",
                                "train" to "Bus/Train",
                                "metro" to "Local Transport",
                                "transport" to "Local Transport",
                                "petrol" to "Fuel",
                                "fuel" to "Fuel",
                                "gas" to "Fuel",
                                "diesel" to "Fuel",
                                "travel" to "Other Travel",
                                "trip" to "Other Travel",
                                "vacation" to "Other Travel",

                                // Food category
                                "restaurant" to "Restaurant",
                                "dining" to "Restaurant",
                                "lunch" to "Restaurant",
                                "dinner" to "Restaurant",
                                "breakfast" to "Restaurant",
                                "grocery" to "Groceries",
                                "groceries" to "Groceries",
                                "supermarket" to "Groceries",
                                "market" to "Groceries",
                                "coffee" to "Coffee/Tea",
                                "tea" to "Coffee/Tea",
                                "cafe" to "Coffee/Tea",
                                "snack" to "Snacks",
                                "snacks" to "Snacks",
                                "chips" to "Snacks",
                                "delivery" to "Delivery",
                                "zomato" to "Delivery",
                                "swiggy" to "Delivery",
                                "dominos" to "Fast Food",
                                "mcdonald" to "Fast Food",
                                "kfc" to "Fast Food",
                                "pizza" to "Fast Food",
                                "burger" to "Fast Food",
                                "food" to "Other Food",
                                "eat" to "Other Food",

                                // Stationary category
                                "book" to "Books",
                                "books" to "Books",
                                "novel" to "Books",
                                "pen" to "Pens/Pencils",
                                "pencil" to "Pens/Pencils",
                                "marker" to "Pens/Pencils",
                                "notebook" to "Notebooks",
                                "diary" to "Notebooks",
                                "copy" to "Notebooks",
                                "office" to "Office Supplies",
                                "supplies" to "Office Supplies",
                                "computer" to "Electronics",
                                "laptop" to "Electronics",
                                "phone" to "Electronics",
                                "mobile" to "Electronics",
                                "software" to "Software",
                                "app" to "Software",
                                "subscription" to "Software",
                                "printer" to "Printer Items",
                                "ink" to "Printer Items",
                                "paper" to "Printer Items",
                                "art" to "Art Supplies",
                                "paint" to "Art Supplies",
                                "canvas" to "Art Supplies",
                                "stationary" to "Other Stationary",

                                // Miscellaneous category
                                "friend" to "Friend",
                                "buddy" to "Friend",
                                "pal" to "Friend",
                                "mate" to "Friend",
                                "family" to "Family",
                                "mom" to "Family",
                                "dad" to "Family",
                                "brother" to "Family",
                                "sister" to "Family",
                                "parent" to "Family",
                                "gift" to "Gift",
                                "present" to "Gift",
                                "birthday" to "Gift",
                                "donation" to "Donation",
                                "charity" to "Donation",
                                "movie" to "Entertainment",
                                "cinema" to "Entertainment",
                                "game" to "Entertainment",
                                "entertainment" to "Entertainment",
                                "fun" to "Entertainment",
                                "doctor" to "Health",
                                "medicine" to "Health",
                                "hospital" to "Health",
                                "health" to "Health",
                                "medical" to "Health",
                                "pharmacy" to "Health",
                                "personal" to "Personal Care",
                                "haircut" to "Personal Care",
                                "salon" to "Personal Care",
                                "cosmetics" to "Personal Care",
                                "bill" to "Bills",
                                "electricity" to "Bills",
                                "internet" to "Bills",
                                "rent" to "Bills",
                                "utility" to "Bills",
                                "miscellaneous" to "Other Misc",
                                "misc" to "Other Misc",
                                "other" to "Other Misc"
                        )

                // Find matching subcategory using whole word matching
                for ((keyword, subcategory) in categoryKeywords) {
                        if (containsWholeWord(lowerMessage, keyword)) {
                                return subcategory
                        }
                }

                // Default to "Other Misc" if no category found
                return "Other Misc"
        }

        private fun isProbablyPersonName(message: String): Boolean {
                // Common Indian names and patterns that suggest person names
                val commonNames =
                        listOf(
                                "khandelwal",
                                "sharma",
                                "gupta",
                                "agarwal",
                                "bansal",
                                "jain",
                                "mittal",
                                "goel",
                                "singh",
                                "kumar",
                                "pandey",
                                "verma",
                                "tiwari",
                                "mishra",
                                "yadav",
                                "shah",
                                "patel",
                                "mehta",
                                "thakur",
                                "rajput",
                                "arora",
                                "kapoor",
                                "malhotra",
                                "bhatia",
                                "rahul",
                                "rohit",
                                "amit",
                                "ankit",
                                "vikash",
                                "priya",
                                "pooja",
                                "sneha",
                                "neha",
                                "deepak",
                                "suresh",
                                "rajesh",
                                "mahesh",
                                "ramesh",
                                "dinesh",
                                "mukesh",
                                "prakash",
                                "aman",
                                "raman",
                                "karan",
                                "arjun",
                                "varun",
                                "tarun",
                                "gaurav",
                                "saurav",
                                "aditya",
                                "ankur",
                                "ashish",
                                "manish",
                                "girish",
                                "harish",
                                "nitish",
                                "ravi",
                                "sunita",
                                "anita",
                                "kavita",
                                "namita",
                                "sangita",
                                "geeta",
                                "sita",
                                "rita",
                                "vishal",
                                "shubham",
                                "akshay",
                                "ajay",
                                "vijay",
                                "sanjay",
                                "manoj",
                                "anil"
                        )

                // Check for patterns that suggest person names
                val namePatterns =
                        listOf(
                                "on [a-z]+wal\\b".toRegex(), // ends with 'wal' like Khandelwal
                                "on [a-z]+kar\\b".toRegex(), // ends with 'kar' like Sarkar
                                "to [a-z]+\\s(bhai|ji|sir|madam)\\b".toRegex(), // followed by
                                // titles
                                "for [a-z]+\\s(uncle|aunty|anna|didi)\\b".toRegex() // family titles
                        )

                // Check if any common name is mentioned
                for (name in commonNames) {
                        if (containsWholeWord(message, name)) {
                                return true
                        }
                }

                // Check name patterns
                for (pattern in namePatterns) {
                        if (pattern.find(message) != null) {
                                return true
                        }
                }

                // Check for typical person name contexts
                val personContexts =
                        listOf(
                                "paid to",
                                "gave to",
                                "lent to",
                                "borrowed from",
                                "owed to",
                                "spend on",
                                "spent on",
                                "pay to",
                                "money to",
                                "cash to"
                        )

                for (context in personContexts) {
                        if (message.contains(context)) {
                                // Extract what comes after the context
                                val afterContext = message.substringAfter(context).trim()
                                val firstWord = afterContext.split(" ").firstOrNull()?.lowercase()

                                // If it's a single word that's not a common item, likely a name
                                if (firstWord != null &&
                                                firstWord.length > 2 &&
                                                !isCommonItem(firstWord) &&
                                                firstWord.all { it.isLetter() }
                                ) {
                                        return true
                                }
                        }
                }

                return false
        }

        private fun containsWholeWord(text: String, word: String): Boolean {
                val pattern = "\\b${Regex.escape(word)}\\b".toRegex()
                return pattern.find(text) != null
        }

        private fun isCommonItem(word: String): Boolean {
                val commonItems =
                        setOf(
                                "food",
                                "lunch",
                                "dinner",
                                "breakfast",
                                "coffee",
                                "tea",
                                "snack",
                                "drink",
                                "book",
                                "pen",
                                "pencil",
                                "paper",
                                "notebook",
                                "bag",
                                "clothes",
                                "shirt",
                                "fuel",
                                "petrol",
                                "taxi",
                                "bus",
                                "train",
                                "ticket",
                                "bill",
                                "rent",
                                "medicine",
                                "hospital",
                                "doctor",
                                "movie",
                                "game",
                                "gift",
                                "shopping"
                        )
                return commonItems.contains(word)
        }

        private fun generateUniqueId(): String {
                val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
                return (1..5).map { chars[Random.nextInt(chars.length)] }.joinToString("")
        }

        private suspend fun tryHandleBalanceQuery(message: String): String? {
                val lowerMessage = message.lowercase()

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
                                        GeminiRequest(
                                                contents =
                                                        listOf(
                                                                Content(
                                                                        parts = listOf(Part(prompt))
                                                                )
                                                        )
                                        )
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
                                        GeminiRequest(
                                                contents =
                                                        listOf(
                                                                Content(
                                                                        parts = listOf(Part(prompt))
                                                                )
                                                        )
                                        )
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
                                                        "✅ Gemini general response received"
                                                )
                                                return aiResponse.trim()
                                        }
                                }
                        }

                        android.util.Log.w("ChatViewModel", "⚠️ Gemini response was empty")
                        "I'm here to help with your finances! 💰\n\nFor transactions, try:\n• 'I earned 200rs'\n• 'I spent 100 on food'\n• 'I got 500 from friend'\n\nYou can also ask for your balance or financial advice!"
                } catch (e: Exception) {
                        android.util.Log.e(
                                "ChatViewModel",
                                "❌ Error getting Gemini response: ${e.message}"
                        )
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
                                        message =
                                                "✅ All previous messages have been cleared successfully!",
                                        isUser = false,
                                        messageType = MessageType.AI_RESPONSE
                                )
                        chatMessageRepository.insertChatMessage(confirmMessage)
                }
        }

        private fun showCategories(userMessage: String) {
                addMessage(
                        ChatMessage.create(
                                userMessage,
                                true,
                                messageType = MessageType.USER_MESSAGE
                        )
                )

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
                addMessage(
                        ChatMessage.create(
                                userMessage,
                                true,
                                messageType = MessageType.USER_MESSAGE
                        )
                )

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
                addMessage(
                        ChatMessage.create(
                                userMessage,
                                true,
                                messageType = MessageType.USER_MESSAGE
                        )
                )

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
                                val recentTransactions =
                                        expenses.sortedByDescending { it.date }.take(5)
                                val recentCount = recentTransactions.size

                                // Calculate totals of recent transactions
                                val recentIncome =
                                        recentTransactions.filter { it.isIncome }.sumOf {
                                                it.amount
                                        }
                                val recentExpenses =
                                        recentTransactions.filter { !it.isIncome }.sumOf {
                                                it.amount
                                        }

                                // Delete recent transactions
                                recentTransactions.forEach { expense ->
                                        expenseRepository.deleteExpense(expense)
                                }

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
                addMessage(
                        ChatMessage.create(
                                userMessage,
                                true,
                                messageType = MessageType.USER_MESSAGE
                        )
                )

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
                                android.util.Log.d(
                                        "ChatViewModel",
                                        "Found ${transactions.size} transactions"
                                )

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
                                        // If no transactions found with date filter, let's also try
                                        // a manual filter as
                                        // backup
                                        val manuallyFiltered =
                                                allTransactions.filter { transaction ->
                                                        transaction.date.time >=
                                                                parsedDateRange.first &&
                                                                transaction.date.time <=
                                                                        parsedDateRange.second
                                                }
                                        android.util.Log.d(
                                                "ChatViewModel",
                                                "Manual filter found: ${manuallyFiltered.size} transactions"
                                        )

                                        if (manuallyFiltered.isNotEmpty()) {
                                                // Use manually filtered results
                                                val transactionsList =
                                                        formatTransactionsList(
                                                                manuallyFiltered,
                                                                parsedDateRange
                                                        )
                                                addMessage(
                                                        ChatMessage.create(
                                                                message = transactionsList,
                                                                isUser = false,
                                                                messageType =
                                                                        MessageType.AI_RESPONSE
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
                                        val transactionsList =
                                                formatTransactionsList(
                                                        transactions,
                                                        parsedDateRange
                                                )

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
                        val currentDate =
                                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                        // Check for simple patterns first before using AI
                        val today = Calendar.getInstance()

                        // Handle "today" or "29 july" or "july 29" patterns
                        if (userQuery.lowercase().contains("today") ||
                                        userQuery.lowercase().contains("29 july") ||
                                        userQuery.lowercase().contains("july 29") ||
                                        userQuery.lowercase().contains("29th july") ||
                                        userQuery.lowercase().contains("july 29th")
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

                        val request =
                                GeminiRequest(
                                        contents = listOf(Content(parts = listOf(Part(prompt))))
                                )
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
                                                        startTimestamp =
                                                                line.substringAfter(
                                                                                "START_TIMESTAMP:"
                                                                        )
                                                                        .toLongOrNull()
                                                } else if (line.startsWith("END_TIMESTAMP:")) {
                                                        endTimestamp =
                                                                line.substringAfter(
                                                                                "END_TIMESTAMP:"
                                                                        )
                                                                        .toLongOrNull()
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
                        android.util.Log.e(
                                "ChatViewModel",
                                "Error parsing date with Gemini: ${e.message}"
                        )
                        null
                }
        }

        private fun formatTransactionsList(
                transactions: List<Expense>,
                dateRange: Pair<Long, Long>
        ): String {
                val startDate =
                        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                                .format(Date(dateRange.first))
                val endDate =
                        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                                .format(Date(dateRange.second))

                val totalIncome = transactions.filter { it.isIncome }.sumOf { it.amount }
                val totalExpenses = transactions.filter { !it.isIncome }.sumOf { it.amount }
                val netAmount = totalIncome - totalExpenses

                val dateRangeText =
                        if (startDate == endDate) startDate else "$startDate to $endDate"

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

        private fun handleBudgetCommand(userMessage: String) {
                addMessage(
                        ChatMessage.create(
                                userMessage,
                                true,
                                messageType = MessageType.USER_MESSAGE
                        )
                )

                viewModelScope.launch {
                        val response = processBudgetMessage(userMessage)
                        addMessage(
                                ChatMessage.create(
                                        message = response,
                                        isUser = false,
                                        messageType = MessageType.AI_RESPONSE
                                )
                        )
                }
        }

        private suspend fun processBudgetMessage(message: String): String {
                _isLoading.value = true
                return try {
                        android.util.Log.d(
                                "ChatViewModel",
                                "🔄 Processing budget message: '$message'"
                        )

                        // First try direct pattern matching for common formats
                        val directResult = tryDirectPatternMatching(message)
                        if (directResult != null) {
                                android.util.Log.d(
                                        "ChatViewModel",
                                        "✅ Direct pattern match successful!"
                                )
                                setBudgetAndReturnMessage(directResult)
                        } else {
                                // Use Gemini API to parse budget information
                                val budgetInfo = parseBudgetWithGemini(message)
                                if (budgetInfo != null) {
                                        setBudgetAndReturnMessage(budgetInfo)
                                } else {
                                        getFailureMessage()
                                }
                        }
                } catch (e: Exception) {
                        android.util.Log.e(
                                "ChatViewModel",
                                "Error processing budget: ${e.message}",
                                e
                        )
                        "❌ Sorry, I encountered an error while setting your budget. Please try again."
                } finally {
                        _isLoading.value = false
                }
        }

        private fun tryDirectPatternMatching(message: String): BudgetInfo? {
                return try {
                        val lowerMessage = message.lowercase()

                        // Pattern 1: "set monthly budget of 1000rs" or similar
                        val pattern1 =
                                "(set|my)\\s+(?:monthly\\s+)?budget\\s+(?:of\\s+|is\\s+)?([0-9]+)(?:rs|₹|rupees)?".toRegex()
                        val match1 = pattern1.find(lowerMessage)
                        if (match1 != null) {
                                val amount = match1.groupValues[2].toDoubleOrNull()
                                if (amount != null && amount > 0) {
                                        val calendar = java.util.Calendar.getInstance()
                                        return BudgetInfo(
                                                amount,
                                                calendar.get(java.util.Calendar.MONTH) + 1,
                                                calendar.get(java.util.Calendar.YEAR),
                                                "Monthly budget"
                                        )
                                }
                        }

                        // Pattern 2: "1000rs monthly budget" or "monthly budget 1000"
                        val pattern2 =
                                "(?:([0-9]+)(?:rs|₹|rupees)?\\s+)?(?:monthly\\s+)?budget(?:\\s+([0-9]+)(?:rs|₹|rupees)?)?".toRegex()
                        val match2 = pattern2.find(lowerMessage)
                        if (match2 != null) {
                                val amount =
                                        (match2.groupValues[1].toDoubleOrNull()
                                                ?: match2.groupValues[2].toDoubleOrNull())
                                if (amount != null && amount > 0) {
                                        val calendar = java.util.Calendar.getInstance()
                                        return BudgetInfo(
                                                amount,
                                                calendar.get(java.util.Calendar.MONTH) + 1,
                                                calendar.get(java.util.Calendar.YEAR),
                                                "Monthly budget"
                                        )
                                }
                        }

                        null
                } catch (e: Exception) {
                        android.util.Log.e(
                                "ChatViewModel",
                                "Direct pattern matching failed: ${e.message}"
                        )
                        null
                }
        }

        private suspend fun setBudgetAndReturnMessage(budgetInfo: BudgetInfo): String {
                // Set the budget
                budgetRepository.setBudgetForMonth(
                        budgetInfo.amount,
                        budgetInfo.month,
                        budgetInfo.year,
                        budgetInfo.description
                )

                val monthName =
                        java.text.SimpleDateFormat("MMMM", java.util.Locale.getDefault())
                                .format(
                                        java.util.Calendar.getInstance()
                                                .apply {
                                                        set(
                                                                java.util.Calendar.MONTH,
                                                                budgetInfo.month - 1
                                                        )
                                                }
                                                .time
                                )

                return "✅ **Budget Set Successfully!**\n\n" +
                        "💰 **Amount:** ₹${String.format("%.2f", budgetInfo.amount)}\n" +
                        "📅 **Month:** $monthName ${budgetInfo.year}\n" +
                        "${if (budgetInfo.description != null) "📝 **Note:** ${budgetInfo.description}\n" else ""}" +
                        "\n🎯 **Your budget is now active!** Check Monthly/Daily Analytics to track your progress.\n\n" +
                        "💡 **Tip:** I can understand budget commands in many ways:\n" +
                        "• \"My budget is ₹3000\"\n" +
                        "• \"Set this month budget of 1000\"\n" +
                        "• \"I plan to spend 5000 this month\"\n" +
                        "• \"Monthly limit is 2500\""
        }

        private fun getFailureMessage(): String {
                return "❌ I couldn't understand your budget request. I can help you set budgets in many natural ways!\n\n" +
                        "✨ **Try saying it naturally:**\n" +
                        "• \"Set this month budget of 1000\"\n" +
                        "• \"My budget is ₹5000\"\n" +
                        "• \"I want to spend 3000 this month\"\n" +
                        "• \"Monthly limit is 2500\"\n" +
                        "• \"Plan to spend 1500\"\n" +
                        "• \"Allocate 4000 for this month\"\n\n" +
                        "💬 Just mention a number and words like 'budget', 'spend', 'month', or 'limit' - I'll understand!"
        }

        private suspend fun parseBudgetWithGemini(message: String): BudgetInfo? {
                return try {
                        val currentDate = java.util.Date()
                        val currentMonth =
                                java.text.SimpleDateFormat("MMMM", java.util.Locale.getDefault())
                                        .format(currentDate)
                        val currentYear =
                                java.text.SimpleDateFormat("yyyy", java.util.Locale.getDefault())
                                        .format(currentDate)
                        val currentMonthNum =
                                java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1

                        val prompt =
                                """
                        I need to extract budget information from user messages. The user might express budget setting in many different ways.
                        
                        User message: "$message"
                        Current date: $currentMonth $currentYear (Month number: $currentMonthNum)
                        
                        Please analyze if this message contains budget-setting intent and extract the information.
                        
                        Examples of what users might say:
                        - "set this month budget of 1000"
                        - "Set monthly budget of 1000rs"
                        - "my budget is 5000"
                        - "I want to spend 3000 this month"
                        - "monthly limit is 2500"
                        - "allocate 4000 for this month"
                        - "plan to spend 1500"
                        - "budget 2000 for January"
                        - "set 800 as my monthly budget"
                        - "I can spend 6000 this month"
                        - "monthly allocation is 3500"
                        - "expense limit 4500"
                        - "this month I want to spend maximum 2000"
                        - "1000rs monthly budget"
                        - "budget this month 5000"
                        
                        Extract budget information and return ONLY a JSON object:
                        {
                            "amount": [numeric value without currency symbols like ₹, rs, rupees, INR],
                            "month": [1-12 where 1=January, 12=December],
                            "year": [4-digit year],
                            "description": [brief description like "Monthly budget" or null]
                        }
                        
                        Rules:
                        1. Extract ANY numeric value that could be a budget amount (remove ₹, Rs, INR, commas, etc.)
                        2. For month: 
                           - If "this month" or no month specified → use current month ($currentMonthNum)
                           - If specific month name → convert to number (January=1, February=2, etc.)
                           - If "next month" → current month + 1
                           - If "last month" → current month - 1
                        3. For year:
                           - If not specified → use current year ($currentYear)
                           - If specified → use that year
                        4. Look for budget-related keywords: budget, spend, limit, allocate, plan, monthly, month
                        5. If no clear budget intent OR no amount found → return: null
                        
                        Return only the JSON object or null, nothing else.
                        """.trimIndent()

                        val request =
                                GeminiRequest(
                                        contents = listOf(Content(parts = listOf(Part(prompt))))
                                )
                        val response =
                                RetrofitClient.geminiApiService.generateContent(
                                        apiKey = Constants.GEMINI_API_KEY,
                                        request = request
                                )

                        val responseText =
                                if (response.isSuccessful && response.body() != null) {
                                        response.body()!!
                                                .candidates
                                                .firstOrNull()
                                                ?.content
                                                ?.parts
                                                ?.firstOrNull()
                                                ?.text
                                } else {
                                        null
                                }
                        android.util.Log.d("ChatViewModel", "Gemini budget response: $responseText")

                        if (responseText?.trim() == "null") {
                                return null
                        }

                        responseText?.let { jsonStr ->
                                try {
                                        // Clean the JSON response - handle various formats
                                        var cleanJson =
                                                jsonStr.trim()
                                                        .removePrefix("```json")
                                                        .removePrefix("```")
                                                        .removeSuffix("```")
                                                        .trim()

                                        // Handle case where response might have extra text
                                        val jsonStart = cleanJson.indexOf("{")
                                        val jsonEnd = cleanJson.lastIndexOf("}") + 1
                                        if (jsonStart >= 0 && jsonEnd > jsonStart) {
                                                cleanJson = cleanJson.substring(jsonStart, jsonEnd)
                                        }

                                        android.util.Log.d(
                                                "ChatViewModel",
                                                "Cleaned JSON: $cleanJson"
                                        )

                                        // Enhanced regex patterns to handle more variations
                                        val amountRegex =
                                                "\"amount\"\\s*:\\s*([0-9]+\\.?[0-9]*)".toRegex()
                                        val monthRegex = "\"month\"\\s*:\\s*([0-9]+)".toRegex()
                                        val yearRegex = "\"year\"\\s*:\\s*([0-9]+)".toRegex()
                                        val descRegex =
                                                "\"description\"\\s*:\\s*(?:\"([^\"]*)\"|null)".toRegex()

                                        val amount =
                                                amountRegex
                                                        .find(cleanJson)
                                                        ?.groupValues
                                                        ?.get(1)
                                                        ?.toDoubleOrNull()
                                        val month =
                                                monthRegex
                                                        .find(cleanJson)
                                                        ?.groupValues
                                                        ?.get(1)
                                                        ?.toIntOrNull()
                                        val year =
                                                yearRegex
                                                        .find(cleanJson)
                                                        ?.groupValues
                                                        ?.get(1)
                                                        ?.toIntOrNull()
                                        val description =
                                                descRegex.find(cleanJson)?.groupValues?.get(1)

                                        android.util.Log.d(
                                                "ChatViewModel",
                                                "Parsed - Amount: $amount, Month: $month, Year: $year, Desc: $description"
                                        )

                                        if (amount != null &&
                                                        month != null &&
                                                        year != null &&
                                                        month in 1..12 &&
                                                        amount > 0
                                        ) {
                                                BudgetInfo(amount, month, year, description)
                                        } else {
                                                // Try fallback parsing if structured parsing fails
                                                tryFallbackParsing(jsonStr)
                                        }
                                } catch (e: Exception) {
                                        android.util.Log.e(
                                                "ChatViewModel",
                                                "Error parsing budget JSON: ${e.message}"
                                        )
                                        // Try fallback parsing
                                        tryFallbackParsing(jsonStr)
                                }
                        }
                } catch (e: Exception) {
                        android.util.Log.e(
                                "ChatViewModel",
                                "Error calling Gemini for budget parsing: ${e.message}",
                                e
                        )
                        null
                }
        }

        private fun tryFallbackParsing(text: String): BudgetInfo? {
                return try {
                        android.util.Log.d(
                                "ChatViewModel",
                                "Attempting fallback parsing for: $text"
                        )

                        // Enhanced number extraction that handles currency formats
                        val numberRegex =
                                "([0-9]+(?:\\.[0-9]+)?)(?:\\s*(?:rs|₹|rupees|inr)?)?".toRegex(
                                        RegexOption.IGNORE_CASE
                                )
                        val numbers =
                                numberRegex
                                        .findAll(text)
                                        .map { it.groupValues[1].toDoubleOrNull() }
                                        .filterNotNull()

                        // Use the largest reasonable number as budget amount (more flexible range)
                        val amount = numbers.filter { it >= 50 && it <= 10000000 }.maxOrNull()

                        android.util.Log.d(
                                "ChatViewModel",
                                "Fallback found numbers: $numbers, selected amount: $amount"
                        )

                        if (amount != null) {
                                // Use current month and year as defaults
                                val calendar = java.util.Calendar.getInstance()
                                val currentMonth = calendar.get(java.util.Calendar.MONTH) + 1
                                val currentYear = calendar.get(java.util.Calendar.YEAR)

                                android.util.Log.d(
                                        "ChatViewModel",
                                        "Fallback parsed - Amount: $amount, Month: $currentMonth, Year: $currentYear"
                                )
                                BudgetInfo(amount, currentMonth, currentYear, "Monthly budget")
                        } else {
                                android.util.Log.d(
                                        "ChatViewModel",
                                        "Fallback parsing failed - no valid amount found"
                                )
                                null
                        }
                } catch (e: Exception) {
                        android.util.Log.e("ChatViewModel", "Fallback parsing failed: ${e.message}")
                        null
                }
        }

        private data class BudgetInfo(
                val amount: Double,
                val month: Int,
                val year: Int,
                val description: String?
        )

        class Factory(
                private val chatMessageRepository: ChatMessageRepository,
                private val expenseRepository: ExpenseRepository,
                private val budgetRepository: BudgetRepository
        ) : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
                                @Suppress("UNCHECKED_CAST")
                                return ChatViewModel(
                                        chatMessageRepository,
                                        expenseRepository,
                                        budgetRepository
                                ) as
                                        T
                        }
                        throw IllegalArgumentException("Unknown ViewModel class")
                }
        }
}
