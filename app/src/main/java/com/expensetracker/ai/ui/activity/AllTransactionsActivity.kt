package com.expensetracker.ai.ui.activity

import android.os.Bundle
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.expensetracker.ai.ExpenseTrackerApplication
import com.expensetracker.ai.R
import com.expensetracker.ai.data.repository.ExpenseRepository
import com.expensetracker.ai.ui.adapter.ExpenseAdapter
import com.expensetracker.ai.ui.viewmodel.ExpenseViewModel
import com.expensetracker.ai.ui.viewmodel.ExpenseViewModelFactory
import kotlinx.coroutines.launch

class AllTransactionsActivity : AppCompatActivity() {

    private lateinit var expenseAdapter: ExpenseAdapter
    private lateinit var recyclerViewAllExpenses: RecyclerView

    private val expenseViewModel: ExpenseViewModel by viewModels {
        ExpenseViewModelFactory(
            ExpenseRepository((application as ExpenseTrackerApplication).database.expenseDao())
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_transactions)

        setupToolbar()
        initViews()
        setupRecyclerView()
        setupObservers()
    }

    private fun setupToolbar() {
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "All Transactions"
        }
    }

    private fun initViews() {
        recyclerViewAllExpenses = findViewById(R.id.recyclerViewAllExpenses)
    }

    private fun setupRecyclerView() {
        expenseAdapter = ExpenseAdapter(
            onItemClick = { _ ->
                // Handle expense click - can open edit dialog
            },
            onItemLongClick = { expense ->
                // Handle long click - can show delete dialog
                expenseViewModel.deleteExpense(expense)
            }
        )

        recyclerViewAllExpenses.apply {
            layoutManager = LinearLayoutManager(this@AllTransactionsActivity)
            adapter = expenseAdapter
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            expenseViewModel.allExpenses.collect { expenses ->
                expenseAdapter.submitList(expenses)
            }
        }
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
