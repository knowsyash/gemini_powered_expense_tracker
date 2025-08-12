package com.expensetracker.ai.utils

object Constants {
    // Gemini API key - Replace with your actual API key
    const val GEMINI_API_KEY = "YOUR_GEMINI_API_KEY_HERE"

    // Expense categories
    val EXPENSE_CATEGORIES =
            listOf(
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
