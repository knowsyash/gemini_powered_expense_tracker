package com.expensetracker.ai.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.expensetracker.ai.data.model.Expense
import com.expensetracker.ai.data.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class ExpenseViewModel(private val repository: ExpenseRepository) : ViewModel() {

    val allExpenses: Flow<List<Expense>> = repository.getAllExpenses()

    private val _totalExpenses = MutableLiveData<Double>()
    val totalExpenses: LiveData<Double> = _totalExpenses

    private val _totalIncome = MutableLiveData<Double>()
    val totalIncome: LiveData<Double> = _totalIncome

    private val _balance = MutableLiveData<Double>()
    val balance: LiveData<Double> = _balance

    init {
        // Observe expenses changes and refresh totals automatically
        viewModelScope.launch { allExpenses.collect { refreshTotals() } }
    }

    fun insertExpense(expense: Expense) {
        viewModelScope.launch {
            repository.insertExpense(expense)
            refreshTotals()
        }
    }

    fun updateExpense(expense: Expense) {
        viewModelScope.launch {
            repository.updateExpense(expense)
            refreshTotals()
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
            refreshTotals()
        }
    }

    fun getExpensesByCategory(category: String): Flow<List<Expense>> {
        return repository.getExpensesByCategory(category)
    }

    fun getExpensesByDateRange(startDate: Long, endDate: Long): Flow<List<Expense>> {
        return repository.getExpensesByDateRange(startDate, endDate)
    }

    // Public method to force refresh totals - useful when data is updated from other components
    fun forceRefreshTotals() {
        refreshTotals()
    }

    private fun refreshTotals() {
        viewModelScope.launch {
            // Force cache invalidation to get fresh data
            repository.refreshData()
            
            val expenses = repository.getTotalExpenses()
            val income = repository.getTotalIncome()

            _totalExpenses.value = expenses
            _totalIncome.value = income
            _balance.value = income - expenses
        }
    }
}

class ExpenseViewModelFactory(private val repository: ExpenseRepository) :
        ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExpenseViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST") return ExpenseViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
