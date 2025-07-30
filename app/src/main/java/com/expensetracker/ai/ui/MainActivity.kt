package com.expensetracker.ai.ui

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.expensetracker.ai.ExpenseTrackerApplication
import com.expensetracker.ai.data.repository.ExpenseRepository
import com.expensetracker.ai.ui.adapter.ExpenseAdapter
import com.expensetracker.ai.ui.viewmodel.ExpenseViewModel
import com.expensetracker.ai.ui.viewmodel.ExpenseViewModelFactory
import com.google.android.material.button.MaterialButton
import java.util.*
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var expenseAdapter: ExpenseAdapter

    // UI Views
    private lateinit var tvBalance: TextView
    private lateinit var tvIncome: TextView
    private lateinit var tvExpenses: TextView
    private lateinit var recyclerViewExpenses: RecyclerView
    private lateinit var tvNoExpenses: View
    private lateinit var fabAddExpense: MaterialButton
    private lateinit var fabChat: MaterialButton

    private val expenseViewModel: ExpenseViewModel by viewModels {
        ExpenseViewModelFactory(
                ExpenseRepository((application as ExpenseTrackerApplication).database.expenseDao())
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(com.expensetracker.ai.R.layout.activity_main)

        initViews()
        setupRecyclerView()
        setupObservers()
        setupClickListeners()
    }

    private fun initViews() {
        tvBalance = findViewById(com.expensetracker.ai.R.id.tvBalance)
        tvIncome = findViewById(com.expensetracker.ai.R.id.tvIncome)
        tvExpenses = findViewById(com.expensetracker.ai.R.id.tvExpenses)
        recyclerViewExpenses = findViewById(com.expensetracker.ai.R.id.recyclerViewExpenses)
        tvNoExpenses = findViewById(com.expensetracker.ai.R.id.tvNoExpenses)
        fabAddExpense = findViewById(com.expensetracker.ai.R.id.fabAddExpense)
        fabChat = findViewById(com.expensetracker.ai.R.id.fabChat)
    }

    private fun setupRecyclerView() {
        expenseAdapter =
                ExpenseAdapter(
                        onItemClick = { _ ->
                            // Handle expense click - can open edit dialog
                        },
                        onItemLongClick = { expense ->
                            // Handle long click - can show delete dialog
                            expenseViewModel.deleteExpense(expense)
                        }
                )

        recyclerViewExpenses.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = expenseAdapter
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            expenseViewModel.allExpenses.collect { expenses ->
                expenseAdapter.submitList(expenses)
                tvNoExpenses.visibility =
                        if (expenses.isEmpty()) {
                            View.VISIBLE
                        } else {
                            View.GONE
                        }
            }
        }

        expenseViewModel.totalExpenses.observe(this) { total ->
            tvExpenses.text = "₹${String.format("%.2f", total)}"
        }

        expenseViewModel.totalIncome.observe(this) {
            tvIncome.text = "₹${String.format("%.2f", it)}"
        }

        expenseViewModel.balance.observe(this) { balance ->
            tvBalance.text = "₹${String.format("%.2f", balance)}"
        }
    }

    private fun setupClickListeners() {
        fabAddExpense.setOnClickListener {
            startActivity(Intent(this, AddExpenseActivity::class.java))
        }

        fabChat.setOnClickListener { startActivity(Intent(this, ChatActivity::class.java)) }
    }

    override fun onResume() {
        super.onResume()
        // Force refresh the financial summary when returning to MainActivity
        expenseViewModel.forceRefreshTotals()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                startActivity(Intent(this, ChatActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
