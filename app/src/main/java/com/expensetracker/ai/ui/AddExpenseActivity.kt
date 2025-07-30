package com.expensetracker.ai.ui

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.expensetracker.ai.ExpenseTrackerApplication
import com.expensetracker.ai.R
import com.expensetracker.ai.data.model.Expense
import com.expensetracker.ai.data.repository.ExpenseRepository
import com.expensetracker.ai.databinding.ActivityAddExpenseBinding
import com.expensetracker.ai.ui.viewmodel.ExpenseViewModel
import com.expensetracker.ai.ui.viewmodel.ExpenseViewModelFactory
import com.expensetracker.ai.utils.Constants
import com.expensetracker.ai.utils.DateUtils
import java.util.*

class AddExpenseActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddExpenseBinding
    private var selectedDate = Date()
    private var isIncome = false

    private val expenseViewModel: ExpenseViewModel by viewModels {
        ExpenseViewModelFactory(
                ExpenseRepository((application as ExpenseTrackerApplication).database.expenseDao())
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddExpenseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupSpinner()
        setupClickListeners()
        updateDateDisplay()
    }

    private fun setupToolbar() {
        // Modern layout - no toolbar setup needed
        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun setupSpinner() {
        val adapter =
                ArrayAdapter(
                        this,
                        android.R.layout.simple_dropdown_item_1line,
                        Constants.EXPENSE_CATEGORIES
                )
        binding.spinnerCategory.setAdapter(adapter)
    }

    private fun setupClickListeners() {
        binding.btnSave.setOnClickListener { saveExpense() }
        binding.btnCancel.setOnClickListener { finish() }
        binding.btnBack.setOnClickListener { finish() }
        
        // Transaction type buttons
        binding.btnExpense.setOnClickListener {
            isIncome = false
            updateTransactionTypeUI()
        }
        
        binding.btnIncome.setOnClickListener {
            isIncome = true
            updateTransactionTypeUI()
        }
        
        binding.btnAddExpense.setOnClickListener { saveExpense() }
    }
    
    private fun updateTransactionTypeUI() {
        if (isIncome) {
            // Income selected
            binding.btnIncome.setBackgroundResource(R.drawable.gradient_button)
            binding.btnExpense.setBackgroundResource(R.drawable.modern_card_background)
        } else {
            // Expense selected
            binding.btnExpense.setBackgroundResource(R.drawable.gradient_button)
            binding.btnIncome.setBackgroundResource(R.drawable.modern_card_background)
        }
    }

    private fun updateDateDisplay() {
        // Date display not needed in simplified modern layout
    }

    private fun saveExpense() {
        val amountText = binding.etAmount.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()
        val category = binding.spinnerCategory.text.toString().trim()

        if (amountText.isEmpty()) {
            binding.etAmount.error = "Amount is required"
            return
        }

        if (category.isEmpty()) {
            Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show()
            return
        }

        // Description is now optional - use default if empty
        val finalDescription = if (description.isEmpty()) {
            "${if (isIncome) "Income" else "Expense"} of ₹$amountText"
        } else {
            description
        }

        val amount = try {
            amountText.toDouble()
        } catch (e: NumberFormatException) {
            binding.etAmount.error = "Invalid amount"
            return
        }

        if (amount <= 0) {
            binding.etAmount.error = "Amount must be greater than 0"
            return
        }

        // Generate unique 5-letter ID
        val uniqueId = generateUniqueId()

        val expense = Expense(
            uniqueId = uniqueId,
            amount = amount,
            category = category,
            description = finalDescription,
            date = selectedDate,
            isIncome = isIncome
        )

        expenseViewModel.insertExpense(expense)

        Toast.makeText(
            this,
            if (isIncome) "Income added successfully" else "Expense added successfully",
            Toast.LENGTH_SHORT
        ).show()

        finish()
    }

    private fun generateUniqueId(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..5).map { chars.random() }.joinToString("")
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
