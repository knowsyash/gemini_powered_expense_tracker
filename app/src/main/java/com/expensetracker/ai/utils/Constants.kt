package com.expensetracker.ai.utils

object Constants {
    // Gemini API key
    const val GEMINI_API_KEY = "AIzaSyBSkWGm4z-WFLLVPMyrRRfkHf8UC3oj4cc"
    
    // Expense categories
    val EXPENSE_CATEGORIES = listOf(
        "Food & Dining",
        "Transportation",
        "Shopping",
        "Entertainment",
        "Bills & Utilities",
        "Healthcare",
        "Education",
        "Travel",
        "Groceries",
        "Other"
    )
    
    // Date formats
    const val DATE_FORMAT = "MMM dd, yyyy"
    const val DATE_FORMAT_FULL = "EEEE, MMMM dd, yyyy"
}
