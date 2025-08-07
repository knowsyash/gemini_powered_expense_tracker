package com.expensetracker.ai.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.expensetracker.ai.data.model.Expense
import com.expensetracker.ai.data.repository.ExpenseRepository
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AnalyticsViewModel(private val repository: ExpenseRepository) : ViewModel() {

    private val _dailyExpenses = MutableLiveData<List<DailyExpenseData>>()
    val dailyExpenses: LiveData<List<DailyExpenseData>> = _dailyExpenses

    private val _monthlyExpenses = MutableLiveData<List<MonthlyExpenseData>>()
    val monthlyExpenses: LiveData<List<MonthlyExpenseData>> = _monthlyExpenses

    private val _dailyTotalExpense = MutableLiveData<Double>()
    val dailyTotalExpense: LiveData<Double> = _dailyTotalExpense

    private val _dailyTotalIncome = MutableLiveData<Double>()
    val dailyTotalIncome: LiveData<Double> = _dailyTotalIncome

    private val _monthlyTotalExpense = MutableLiveData<Double>()
    val monthlyTotalExpense: LiveData<Double> = _monthlyTotalExpense

    private val _monthlyTotalIncome = MutableLiveData<Double>()
    val monthlyTotalIncome: LiveData<Double> = _monthlyTotalIncome

    private val _categoryBreakdown = MutableLiveData<List<CategoryData>>()
    val categoryBreakdown: LiveData<List<CategoryData>> = _categoryBreakdown

    private val _selectedDate = MutableLiveData<Date>()
    val selectedDate: LiveData<Date> = _selectedDate

    private val _selectedMonth = MutableLiveData<Date>()
    val selectedMonth: LiveData<Date> = _selectedMonth

    init {
        _selectedDate.value = Date()
        _selectedMonth.value = Date()
        // Don't load data automatically to prevent crashes
    }

    fun setSelectedDate(date: Date) {
        _selectedDate.value = date
        loadDailyData()
    }

    fun setSelectedMonth(date: Date) {
        _selectedMonth.value = date
        loadMonthlyData()
    }

    fun loadDailyData() {
        viewModelScope.launch {
            val selectedDate = _selectedDate.value ?: Date()
            val dailyData = getDailyExpenseData(selectedDate)
            _dailyExpenses.value = dailyData

            // Calculate totals for selected day
            val startOfDay = getStartOfDay(selectedDate)
            val endOfDay = getEndOfDay(selectedDate)

            val allExpenses = repository.getAllExpenses().first()
            val dayExpenses =
                    allExpenses.filter { expense ->
                        expense.date.time >= startOfDay.time && expense.date.time <= endOfDay.time
                    }

            val totalExpense = dayExpenses.filter { !it.isIncome }.sumOf { it.amount }
            val totalIncome = dayExpenses.filter { it.isIncome }.sumOf { it.amount }

            _dailyTotalExpense.value = totalExpense
            _dailyTotalIncome.value = totalIncome

            // Load category breakdown for the day
            loadCategoryBreakdown(dayExpenses.filter { !it.isIncome })
        }
    }

    fun loadMonthlyData() {
        viewModelScope.launch {
            val selectedMonth = _selectedMonth.value ?: Date()
            val monthlyData = getMonthlyExpenseData(selectedMonth)
            _monthlyExpenses.value = monthlyData

            // Calculate totals for selected month
            val (startOfMonth, endOfMonth) = getMonthRange(selectedMonth)

            val allExpenses = repository.getAllExpenses().first()
            val monthExpenses =
                    allExpenses.filter { expense ->
                        expense.date.time >= startOfMonth.time &&
                                expense.date.time <= endOfMonth.time
                    }

            val totalExpense = monthExpenses.filter { !it.isIncome }.sumOf { it.amount }
            val totalIncome = monthExpenses.filter { it.isIncome }.sumOf { it.amount }

            _monthlyTotalExpense.value = totalExpense
            _monthlyTotalIncome.value = totalIncome

            // Load category breakdown for the month
            loadCategoryBreakdown(monthExpenses.filter { !it.isIncome })
        }
    }

    private suspend fun getDailyExpenseData(selectedDate: Date): List<DailyExpenseData> {
        val allExpenses = repository.getAllExpenses().first()
        val calendar = Calendar.getInstance()

        // Get last 7 days including selected date
        val dailyDataList = mutableListOf<DailyExpenseData>()

        for (i in 6 downTo 0) {
            calendar.time = selectedDate
            calendar.add(Calendar.DAY_OF_MONTH, -i)

            val dayStart = getStartOfDay(calendar.time)
            val dayEnd = getEndOfDay(calendar.time)

            val dayExpenses =
                    allExpenses.filter { expense ->
                        expense.date.time >= dayStart.time && expense.date.time <= dayEnd.time
                    }

            val totalExpense = dayExpenses.filter { !it.isIncome }.sumOf { it.amount }
            val totalIncome = dayExpenses.filter { it.isIncome }.sumOf { it.amount }

            dailyDataList.add(
                    DailyExpenseData(
                            date = calendar.time,
                            totalExpense = totalExpense,
                            totalIncome = totalIncome,
                            transactionCount = dayExpenses.size,
                            netAmount = totalIncome - totalExpense
                    )
            )
        }

        return dailyDataList
    }

    private suspend fun getMonthlyExpenseData(selectedMonth: Date): List<MonthlyExpenseData> {
        val allExpenses = repository.getAllExpenses().first()
        val calendar = Calendar.getInstance()

        // Get last 6 months including selected month
        val monthlyDataList = mutableListOf<MonthlyExpenseData>()

        for (i in 5 downTo 0) {
            calendar.time = selectedMonth
            calendar.add(Calendar.MONTH, -i)

            val (monthStart, monthEnd) = getMonthRange(calendar.time)

            val monthExpenses =
                    allExpenses.filter { expense ->
                        expense.date.time >= monthStart.time && expense.date.time <= monthEnd.time
                    }

            val totalExpense = monthExpenses.filter { !it.isIncome }.sumOf { it.amount }
            val totalIncome = monthExpenses.filter { it.isIncome }.sumOf { it.amount }

            monthlyDataList.add(
                    MonthlyExpenseData(
                            month = calendar.time,
                            totalExpense = totalExpense,
                            totalIncome = totalIncome,
                            transactionCount = monthExpenses.size,
                            netAmount = totalIncome - totalExpense
                    )
            )
        }

        return monthlyDataList
    }

    private fun loadCategoryBreakdown(expenses: List<Expense>) {
        val categoryTotals =
                expenses
                        .groupBy { it.category }
                        .mapValues { (_, expenseList) -> expenseList.sumOf { it.amount } }
                        .toList()
                        .sortedByDescending { it.second }

        val totalAmount = categoryTotals.sumOf { it.second }

        val categoryDataList =
                categoryTotals.map { (category, amount) ->
                    CategoryData(
                            name = category,
                            amount = amount,
                            percentage = if (totalAmount > 0) (amount / totalAmount * 100) else 0.0,
                            icon = getCategoryIcon(category)
                    )
                }

        _categoryBreakdown.value = categoryDataList
    }

    private fun getStartOfDay(date: Date): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.time
    }

    private fun getEndOfDay(date: Date): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.time
    }

    private fun getMonthRange(date: Date): Pair<Date, Date> {
        val calendar = Calendar.getInstance()
        calendar.time = date

        // Start of month
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfMonth = calendar.time

        // End of month
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endOfMonth = calendar.time

        return Pair(startOfMonth, endOfMonth)
    }

    private fun getCategoryIcon(category: String): String {
        // Manual case-insensitive comparison without using toLowerCase
        return when {
            // Food categories
            category.equals("food", true) ||
                    category.equals("food & dining", true) ||
                    category.equals("Food", true) ||
                    category.equals("FOOD", true) ||
                    category.equals("Food & Dining", true) -> "�"

            // Transportation categories
            category.equals("transportation", true) ||
                    category.equals("transport", true) ||
                    category.equals("Transportation", true) ||
                    category.equals("Transport", true) -> "🚗"

            // Shopping categories
            category.equals("shopping", true) || category.equals("Shopping", true) -> "🛍️"

            // Entertainment categories
            category.equals("entertainment", true) || category.equals("Entertainment", true) -> "🎬"

            // Bills categories
            category.equals("bills", true) ||
                    category.equals("bills & utilities", true) ||
                    category.equals("Bills", true) ||
                    category.equals("Bills & Utilities", true) -> "📋"

            // Healthcare categories
            category.equals("healthcare", true) ||
                    category.equals("health", true) ||
                    category.equals("Healthcare", true) ||
                    category.equals("Health", true) -> "🏥"

            // Education categories
            category.equals("education", true) || category.equals("Education", true) -> "📚"

            // Travel categories
            category.equals("travel", true) || category.equals("Travel", true) -> "✈️"

            // Groceries categories
            category.equals("groceries", true) || category.equals("Groceries", true) -> "�"
            else -> "💳"
        }
    }

    // Navigate to previous/next day
    fun navigateToPreviousDay() {
        val currentDate = _selectedDate.value ?: Date()
        val calendar = Calendar.getInstance()
        calendar.time = currentDate
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        setSelectedDate(calendar.time)
    }

    fun navigateToNextDay() {
        val currentDate = _selectedDate.value ?: Date()
        val calendar = Calendar.getInstance()
        calendar.time = currentDate
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        setSelectedDate(calendar.time)
    }

    // Navigate to previous/next month
    fun navigateToPreviousMonth() {
        val currentMonth = _selectedMonth.value ?: Date()
        val calendar = Calendar.getInstance()
        calendar.time = currentMonth
        calendar.add(Calendar.MONTH, -1)
        setSelectedMonth(calendar.time)
    }

    fun navigateToNextMonth() {
        val currentMonth = _selectedMonth.value ?: Date()
        val calendar = Calendar.getInstance()
        calendar.time = currentMonth
        calendar.add(Calendar.MONTH, 1)
        setSelectedMonth(calendar.time)
    }
}

// Data classes for analytics
data class DailyExpenseData(
        val date: Date,
        val totalExpense: Double,
        val totalIncome: Double,
        val transactionCount: Int,
        val netAmount: Double
) {
    val dateString: String
        get() = SimpleDateFormat("MMM dd", Locale.getDefault()).format(date)

    val dayOfWeek: String
        get() = SimpleDateFormat("EEE", Locale.getDefault()).format(date)
}

data class MonthlyExpenseData(
        val month: Date,
        val totalExpense: Double,
        val totalIncome: Double,
        val transactionCount: Int,
        val netAmount: Double
) {
    val monthString: String
        get() = SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(month)

    val shortMonthString: String
        get() = SimpleDateFormat("MMM", Locale.getDefault()).format(month)
}

data class CategoryData(
        val name: String,
        val amount: Double,
        val percentage: Double,
        val icon: String
)

class AnalyticsViewModelFactory(private val repository: ExpenseRepository) :
        ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AnalyticsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST") return AnalyticsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
