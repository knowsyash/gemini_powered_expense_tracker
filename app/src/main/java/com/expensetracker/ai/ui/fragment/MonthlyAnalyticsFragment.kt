package com.expensetracker.ai.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
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
            val view = LayoutInflater.from(parent.context)
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
                val normalizedHeight = ((Math.abs(netAmount) / maxAmount) * 50 + 10).coerceAtMost(60.0)

                val layoutParams = viewBar.layoutParams
                layoutParams.height = (normalizedHeight * itemView.context.resources.displayMetrics.density).toInt()
                viewBar.layoutParams = layoutParams

                // Color the bar based on income vs expense
                if (netAmount >= 0) {
                    viewBar.setBackgroundColor(itemView.context.getColor(android.R.color.holo_green_light))
                } else {
                    viewBar.setBackgroundColor(itemView.context.getColor(android.R.color.holo_red_light))
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
    private lateinit var btnPreviousMonth: ImageButton
    private lateinit var btnNextMonth: ImageButton

    private var currentMonthOffset = 0

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
            repository = ExpenseRepository(
                (requireActivity().application as ExpenseTrackerApplication).database.expenseDao()
            )

            initViews(view)
            loadMonthlyData()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun initViews(view: View) {
        tvTotalExpenses = view.findViewById(R.id.tvTotalExpenses)
        tvTotalIncome = view.findViewById(R.id.tvTotalIncome)
        tvSelectedMonth = view.findViewById(R.id.tvSelectedMonth)
        tvNetBalance = view.findViewById(R.id.tvNetBalance)
        recyclerViewMonthlyChart = view.findViewById(R.id.recyclerViewMonthlyChart)
        btnPreviousMonth = view.findViewById(R.id.btnPreviousMonth)
        btnNextMonth = view.findViewById(R.id.btnNextMonth)

        // Setup RecyclerView
        monthlyChartAdapter = MonthlyChartAdapter()
        recyclerViewMonthlyChart.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = monthlyChartAdapter
        }

        // Setup navigation buttons
        btnPreviousMonth.setOnClickListener {
            currentMonthOffset--
            loadMonthlyData()
            updateNavigationButtons()
            Toast.makeText(context, "Showing previous month", Toast.LENGTH_SHORT).show()
        }

        btnNextMonth.setOnClickListener {
            // Don't allow going beyond current month
            if (currentMonthOffset < 0) {
                currentMonthOffset++
                loadMonthlyData()
                updateNavigationButtons()
                Toast.makeText(context, "Showing next month", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Cannot view future months", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Initial button state update
        updateNavigationButtons()
    }

    private fun updateNavigationButtons() {
        // Enable/disable navigation buttons based on current position
        btnPreviousMonth.isEnabled = true // Always allow going to previous months
        btnNextMonth.isEnabled = currentMonthOffset < 0 // Only allow next if we're in past months
        
        // Update button appearance
        btnNextMonth.alpha = if (btnNextMonth.isEnabled) 1.0f else 0.5f
    }

    private fun loadMonthlyData() {
        lifecycleScope.launch {
            try {
                val allExpenses = repository.getAllExpenses().first()
                val today = Date()
                val calendar = Calendar.getInstance()

                // Get the target month based on currentMonthOffset
                calendar.time = today
                calendar.add(Calendar.MONTH, currentMonthOffset)
                val targetDate = calendar.time

                // Debug logging
                println("Monthly Analytics - Total expenses in DB: ${allExpenses.size}")
                println("Current month offset: $currentMonthOffset")

                // Get last 6 months data (for the chart)
                val last6MonthsData = mutableListOf<MonthData>()
                for (i in 5 downTo 0) {
                    calendar.time = targetDate
                    calendar.add(Calendar.MONTH, -i)

                    // Get first day of the month
                    calendar.set(Calendar.DAY_OF_MONTH, 1)
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    val monthStart = calendar.time

                    // Get last day of the month
                    calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                    calendar.set(Calendar.HOUR_OF_DAY, 23)
                    calendar.set(Calendar.MINUTE, 59)
                    calendar.set(Calendar.SECOND, 59)
                    calendar.set(Calendar.MILLISECOND, 999)
                    val monthEnd = calendar.time

                    val monthExpenses = allExpenses.filter { expense ->
                        expense.date.time >= monthStart.time && expense.date.time <= monthEnd.time
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

                // Show Last 6 Months Total by default (currentMonthOffset == 0)
                if (currentMonthOffset == 0) {
                    // Calculate last 6 months totals
                    val last6MonthsExpense = last6MonthsData.sumOf { it.expense }
                    val last6MonthsIncome = last6MonthsData.sumOf { it.income }
                    val last6MonthsNet = last6MonthsIncome - last6MonthsExpense

                    tvTotalExpenses.text = "₹${String.format("%.2f", last6MonthsExpense)}"
                    tvTotalIncome.text = "₹${String.format("%.2f", last6MonthsIncome)}"
                    
                    // Format net balance with proper sign and color indication
                    if (last6MonthsNet >= 0) {
                        tvNetBalance.text = "₹${String.format("%.2f", last6MonthsNet)}"
                        tvNetBalance.setTextColor(requireContext().getColor(android.R.color.holo_green_light))
                    } else {
                        tvNetBalance.text = "-₹${String.format("%.2f", Math.abs(last6MonthsNet))}"
                        tvNetBalance.setTextColor(requireContext().getColor(android.R.color.holo_red_light))
                    }
                    
                    tvSelectedMonth.text = "Last 6 Months Total"

                    println("Last 6 months - Income: $last6MonthsIncome, Expense: $last6MonthsExpense, Net: $last6MonthsNet")
                } else {
                    // Calculate specific month totals
                    calendar.time = targetDate
                    calendar.set(Calendar.DAY_OF_MONTH, 1)
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    val monthStart = calendar.time

                    calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                    calendar.set(Calendar.HOUR_OF_DAY, 23)
                    calendar.set(Calendar.MINUTE, 59)
                    calendar.set(Calendar.SECOND, 59)
                    calendar.set(Calendar.MILLISECOND, 999)
                    val monthEnd = calendar.time

                    val monthlyExpenses = allExpenses.filter { expense ->
                        expense.date.time >= monthStart.time && expense.date.time <= monthEnd.time
                    }

                    println("Target month expenses count: ${monthlyExpenses.size}")

                    val totalExpense = monthlyExpenses.filter { !it.isIncome }.sumOf { it.amount }
                    val totalIncome = monthlyExpenses.filter { it.isIncome }.sumOf { it.amount }
                    val netBalance = totalIncome - totalExpense

                    tvTotalExpenses.text = "₹${String.format("%.2f", totalExpense)}"
                    tvTotalIncome.text = "₹${String.format("%.2f", totalIncome)}"
                    
                    // Format net balance with proper sign and color indication
                    if (netBalance >= 0) {
                        tvNetBalance.text = "₹${String.format("%.2f", netBalance)}"
                        tvNetBalance.setTextColor(requireContext().getColor(android.R.color.holo_green_light))
                    } else {
                        tvNetBalance.text = "-₹${String.format("%.2f", Math.abs(netBalance))}"
                        tvNetBalance.setTextColor(requireContext().getColor(android.R.color.holo_red_light))
                    }

                    val displayFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                    val todayCalendar = Calendar.getInstance()
                    todayCalendar.time = Date()
                    
                    val monthText = when (currentMonthOffset) {
                        0 -> "This Month: ${displayFormat.format(targetDate)}"
                        -1 -> "Last Month: ${displayFormat.format(targetDate)}"
                        else -> displayFormat.format(targetDate)
                    }
                    tvSelectedMonth.text = monthText

                    println("Specific month - Income: $totalIncome, Expense: $totalExpense, Net: $netBalance")
                }
                
                // Update navigation buttons after loading data
                updateNavigationButtons()
                
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

    fun refreshData() {
        loadMonthlyData()
    }
}
