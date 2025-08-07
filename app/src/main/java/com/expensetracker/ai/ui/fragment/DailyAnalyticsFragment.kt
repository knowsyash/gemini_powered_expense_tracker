package com.expensetracker.ai.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
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

class DailyAnalyticsFragment : Fragment() {

    data class DayData(val day: String, val date: String, val expense: Double, val income: Double)

    inner class DailyChartAdapter : RecyclerView.Adapter<DailyChartAdapter.DayViewHolder>() {

        private var dayDataList = listOf<DayData>()

        fun updateData(newData: List<DayData>) {
            dayDataList = newData
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
            val view =
                    LayoutInflater.from(parent.context)
                            .inflate(R.layout.item_daily_chart, parent, false)
            return DayViewHolder(view)
        }

        override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
            holder.bind(dayDataList[position])
        }

        override fun getItemCount(): Int = dayDataList.size

        inner class DayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
            private val tvDay: TextView = itemView.findViewById(R.id.tvDay)
            private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
            private val viewBar: View = itemView.findViewById(R.id.viewBar)

            fun bind(dayData: DayData) {
                val netAmount = dayData.income - dayData.expense
                tvAmount.text = "₹${String.format("%.0f", Math.abs(netAmount))}"
                tvDay.text = dayData.day
                tvDate.text = dayData.date

                // Calculate bar height based on amount (max height 60dp, min 10dp)
                val maxAmount = 1000.0 // You can make this dynamic based on max in dataset
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
    private lateinit var tvSelectedDate: TextView
    private lateinit var tvNetBalance: TextView
    private lateinit var recyclerViewDailyChart: RecyclerView
    private lateinit var btnPreviousDay: ImageButton
    private lateinit var btnNextDay: ImageButton
    private val dailyChartAdapter by lazy { DailyChartAdapter() }
    private var selectedDate = Date() // Track the currently selected date

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_daily_analytics, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        repository =
                ExpenseRepository(
                        (requireActivity().application as ExpenseTrackerApplication).database
                                .expenseDao()
                )

        initViews(view)
        loadDailyData()
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when fragment becomes visible again (e.g., after adding expense)
        if (::repository.isInitialized) {
            loadDailyData()
        }
    }

    fun refreshData() {
        if (::repository.isInitialized) {
            loadDailyDataForDate(selectedDate)
        }
    }

    private fun initViews(view: View) {
        tvTotalExpenses = view.findViewById(R.id.tvTotalExpenses)
        tvTotalIncome = view.findViewById(R.id.tvTotalIncome)
        tvSelectedDate = view.findViewById(R.id.tvSelectedDate)
        tvNetBalance = view.findViewById(R.id.tvNetBalance)
        recyclerViewDailyChart = view.findViewById(R.id.recyclerViewDailyChart)
        btnPreviousDay = view.findViewById(R.id.btnPreviousDay)
        btnNextDay = view.findViewById(R.id.btnNextDay)

        // Setup RecyclerView
        recyclerViewDailyChart.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = dailyChartAdapter
        }

        // Setup navigation buttons
        btnPreviousDay.setOnClickListener {
            navigateToDate(-1)
        }

        btnNextDay.setOnClickListener {
            navigateToDate(1)
        }
    }

    private fun navigateToDate(dayOffset: Int) {
        val calendar = Calendar.getInstance()
        calendar.time = selectedDate
        calendar.add(Calendar.DAY_OF_YEAR, dayOffset)
        selectedDate = calendar.time
        loadDailyDataForDate(selectedDate)
        updateNavigationButtons()
    }

    private fun updateNavigationButtons() {
        val today = Date()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val selectedDateString = dateFormat.format(selectedDate)
        val todayString = dateFormat.format(today)
        
        // Hide/disable next button if selected date is today or later
        if (selectedDateString >= todayString) {
            btnNextDay.visibility = View.INVISIBLE
        } else {
            btnNextDay.visibility = View.VISIBLE
        }
    }

    private fun loadDailyData() {
        selectedDate = Date() // Reset to today when first loading
        loadDailyDataForDate(selectedDate)
        updateNavigationButtons()
    }

    private fun loadDailyDataForDate(targetDate: Date) {
        lifecycleScope.launch {
            try {
                val allExpenses = repository.getAllExpenses().first()
                val calendar = Calendar.getInstance()

                // Debug logging
                println("Daily Analytics - Total expenses in DB: ${allExpenses.size}")

                // Get last 7 days data centered around the selected date
                val last7DaysData = mutableListOf<DayData>()
                for (i in 6 downTo 0) {
                    calendar.time = targetDate
                    calendar.add(Calendar.DAY_OF_YEAR, -i)
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    val dayStart = calendar.time

                    calendar.set(Calendar.HOUR_OF_DAY, 23)
                    calendar.set(Calendar.MINUTE, 59)
                    calendar.set(Calendar.SECOND, 59)
                    calendar.set(Calendar.MILLISECOND, 999)
                    val dayEnd = calendar.time

                    val dayExpenses =
                            allExpenses.filter { expense ->
                                expense.date.time >= dayStart.time &&
                                        expense.date.time <= dayEnd.time
                            }

                    val totalExpense = dayExpenses.filter { !it.isIncome }.sumOf { it.amount }
                    val totalIncome = dayExpenses.filter { it.isIncome }.sumOf { it.amount }

                    val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
                    val dateFormat = SimpleDateFormat("dd", Locale.getDefault())

                    last7DaysData.add(
                            DayData(
                                    day = dayFormat.format(dayStart),
                                    date = dateFormat.format(dayStart),
                                    expense = totalExpense,
                                    income = totalIncome
                            )
                    )
                }

                // Update RecyclerView
                dailyChartAdapter.updateData(last7DaysData)

                // Calculate selected date totals
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val displayFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                val targetDateString = dateFormat.format(targetDate)

                val selectedDateExpenses =
                        allExpenses.filter { expense ->
                            dateFormat.format(expense.date) == targetDateString
                        }

                println("Selected date expenses count: ${selectedDateExpenses.size}")

                val totalExpense = selectedDateExpenses.filter { !it.isIncome }.sumOf { it.amount }
                val totalIncome = selectedDateExpenses.filter { it.isIncome }.sumOf { it.amount }
                val netBalance = totalIncome - totalExpense

                // Update UI with selected date data
                tvTotalExpenses.text = "₹${String.format("%.2f", totalExpense)}"
                tvTotalIncome.text = "₹${String.format("%.2f", totalIncome)}"
                tvNetBalance.text = "₹${String.format("%.2f", netBalance)}"
                
                val today = Date()
                if (dateFormat.format(targetDate) == dateFormat.format(today)) {
                    tvSelectedDate.text = "Today: ${displayFormat.format(targetDate)}"
                } else {
                    tvSelectedDate.text = displayFormat.format(targetDate)
                }

                // Update navigation buttons after loading data
                updateNavigationButtons()

            } catch (e: Exception) {
                println("Error loading daily data: ${e.message}")
                e.printStackTrace()
                tvTotalExpenses.text = "Error loading data"
                tvTotalIncome.text = "Error loading data"
                tvNetBalance.text = "Error"
                tvSelectedDate.text = "Error: ${e.message}"
            }
        }
    }
}
