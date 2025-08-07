package com.expensetracker.ai.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.expensetracker.ai.ExpenseTrackerApplication
import com.expensetracker.ai.R
import com.expensetracker.ai.data.repository.ExpenseRepository
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MonthlyAnalyticsFragment : Fragment() {

    data class MonthData(val month: String, val expense: Double, val income: Double)

    inner class MonthlyChartAdapter : RecyclerView.Adapter<MonthlyChartAdapter.MonthViewHolder>() {

        private var monthDataList = listOf<MonthData>()

        fun updateData(newData: List<MonthData>) {
            monthDataList = newData
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MonthViewHolder {
            val view =
                    LayoutInflater.from(parent.context)
                            .inflate(R.layout.item_daily_chart, parent, false)
            return MonthViewHolder(view)
        }

        override fun onBindViewHolder(holder: MonthViewHolder, position: Int) {
            holder.bind(monthDataList[position])
        }

        override fun getItemCount(): Int = monthDataList.size

        inner class MonthViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
            private val tvDay: TextView = itemView.findViewById(R.id.tvDay)
            private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
            private val viewBar: View = itemView.findViewById(R.id.viewBar)

            fun bind(monthData: MonthData) {
                val netAmount = monthData.income - monthData.expense
                tvAmount.text = "₹${String.format("%.0f", Math.abs(netAmount))}"
                tvDay.text = monthData.month
                tvDate.text = ""

                // Calculate bar height based on amount (max height 60dp, min 10dp)
                val maxAmount = 5000.0 // Adjust based on your data range
                val normalizedHeight =
                        ((Math.abs(netAmount) / maxAmount) * 50 + 10).coerceAtMost(60.0)

                val layoutParams = viewBar.layoutParams
                layoutParams.height =
                        (normalizedHeight * itemView.context.resources.displayMetrics.density)
                                .toInt()
                viewBar.layoutParams = layoutParams

                // Color the bar based on income vs expense
                if (netAmount >= 0) {
                    viewBar.setBackgroundColor(
                            itemView.context.getColor(android.R.color.holo_green_light)
                    )
                } else {
                    viewBar.setBackgroundColor(
                            itemView.context.getColor(android.R.color.holo_red_light)
                    )
                }
            }
        }
    }

    private lateinit var repository: ExpenseRepository
    private lateinit var tvTotalExpenses: TextView
    private lateinit var tvTotalIncome: TextView
    private lateinit var tvSelectedMonth: TextView
    private lateinit var tvNetBalance: TextView
    private lateinit var recyclerViewMonthlyChart: RecyclerView
    private lateinit var monthlyChartAdapter: MonthlyChartAdapter

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_monthly_analytics, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        try {
            repository =
                    ExpenseRepository(
                            (requireActivity().application as ExpenseTrackerApplication).database
                                    .expenseDao()
                    )

            initViews(view)
            loadMonthlyData()
        } catch (e: Exception) {
            println("Error in MonthlyAnalyticsFragment onViewCreated: ${e.message}")
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when fragment becomes visible again (e.g., after adding expense)
        if (::repository.isInitialized) {
            loadMonthlyData()
        }
    }

    fun refreshData() {
        if (::repository.isInitialized) {
            loadMonthlyData()
        }
    }

    private fun initViews(view: View) {
        try {
            tvTotalExpenses = view.findViewById(R.id.tvTotalExpenses)
            tvTotalIncome = view.findViewById(R.id.tvTotalIncome)
            tvSelectedMonth = view.findViewById(R.id.tvSelectedMonth)
            tvNetBalance = view.findViewById(R.id.tvNetBalance)
            recyclerViewMonthlyChart = view.findViewById(R.id.recyclerViewMonthlyChart)

            // Setup RecyclerView
            monthlyChartAdapter = MonthlyChartAdapter()
            recyclerViewMonthlyChart.apply {
                layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                adapter = monthlyChartAdapter
            }

            // Set default values to test if views are working
            tvTotalExpenses.text = "₹0.00"
            tvTotalIncome.text = "₹0.00"
            tvSelectedMonth.text = "Loading..."
            tvNetBalance.text = "₹0.00"
        } catch (e: Exception) {
            println("Error initializing views in MonthlyAnalyticsFragment: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun loadMonthlyData() {
        lifecycleScope.launch {
            try {
                val allExpenses = repository.getAllExpenses().first()
                val today = Date()
                val calendar = Calendar.getInstance()
                calendar.time = today

                // Debug logging
                println("Monthly Analytics - Total expenses in DB: ${allExpenses.size}")

                // Get last 6 months data
                val last6MonthsData = mutableListOf<MonthData>()
                for (i in 5 downTo 0) {
                    calendar.time = today
                    calendar.add(Calendar.MONTH, -i)

                    // Get first day of the month
                    calendar.set(Calendar.DAY_OF_MONTH, 1)
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    val monthStart = calendar.time

                    // Get last day of the month
                    calendar.set(
                            Calendar.DAY_OF_MONTH,
                            calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                    )
                    calendar.set(Calendar.HOUR_OF_DAY, 23)
                    calendar.set(Calendar.MINUTE, 59)
                    calendar.set(Calendar.SECOND, 59)
                    calendar.set(Calendar.MILLISECOND, 999)
                    val monthEnd = calendar.time

                    val monthExpenses =
                            allExpenses.filter { expense ->
                                expense.date.time >= monthStart.time &&
                                        expense.date.time <= monthEnd.time
                            }

                    val totalExpense = monthExpenses.filter { !it.isIncome }.sumOf { it.amount }
                    val totalIncome = monthExpenses.filter { it.isIncome }.sumOf { it.amount }

                    val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())

                    last6MonthsData.add(
                            MonthData(
                                    month = monthFormat.format(monthStart),
                                    expense = totalExpense,
                                    income = totalIncome
                            )
                    )
                }

                // Update RecyclerView
                monthlyChartAdapter.updateData(last6MonthsData)

                // Calculate current month totals
                calendar.time = today
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val monthStart = calendar.time

                calendar.set(
                        Calendar.DAY_OF_MONTH,
                        calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                )
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                val monthEnd = calendar.time

                println("Month range: ${monthStart} to ${monthEnd}")

                val monthlyExpenses =
                        allExpenses.filter { expense ->
                            expense.date.time >= monthStart.time &&
                                    expense.date.time <= monthEnd.time
                        }

                println("Monthly expenses count: ${monthlyExpenses.size}")

                val totalExpense = monthlyExpenses.filter { !it.isIncome }.sumOf { it.amount }
                val totalIncome = monthlyExpenses.filter { it.isIncome }.sumOf { it.amount }
                val netBalance = totalIncome - totalExpense

                val displayFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

                // Show current month data or last 6 months total if no current month data
                if (monthlyExpenses.isEmpty()) {
                    val last6MonthsExpense = last6MonthsData.sumOf { it.expense }
                    val last6MonthsIncome = last6MonthsData.sumOf { it.income }
                    val last6MonthsNet = last6MonthsIncome - last6MonthsExpense

                    tvTotalExpenses.text = "₹${String.format("%.2f", last6MonthsExpense)}"
                    tvTotalIncome.text = "₹${String.format("%.2f", last6MonthsIncome)}"
                    tvNetBalance.text = "₹${String.format("%.2f", last6MonthsNet)}"
                    tvSelectedMonth.text = "Last 6 Months Total"
                } else {
                    tvTotalExpenses.text = "₹${String.format("%.2f", totalExpense)}"
                    tvTotalIncome.text = "₹${String.format("%.2f", totalIncome)}"
                    tvNetBalance.text = "₹${String.format("%.2f", netBalance)}"
                    tvSelectedMonth.text = "This Month: ${displayFormat.format(today)}"
                }
            } catch (e: Exception) {
                println("Error loading monthly data: ${e.message}")
                e.printStackTrace()
                tvTotalExpenses.text = "Error loading data"
                tvTotalIncome.text = "Error loading data"
                tvNetBalance.text = "Error"
                tvSelectedMonth.text = "Error: ${e.message}"
            }
        }
    }
}
