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
                val maxAmount = 2000.0 // Adjust based on your daily data range
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

    // Navigation variables
    private var currentDateOffset = 0
    private lateinit var selectedDate: Date

    private lateinit var repository: ExpenseRepository
    private lateinit var tvTotalExpenses: TextView
    private lateinit var tvTotalIncome: TextView
    private lateinit var tvSelectedDate: TextView
    private lateinit var tvNetBalance: TextView
    private lateinit var recyclerViewDailyChart: RecyclerView
    private lateinit var dailyChartAdapter: DailyChartAdapter
    private lateinit var btnPreviousDay: ImageButton
    private lateinit var btnNextDay: ImageButton

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_daily_analytics, container, false)
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
            loadDailyData()
        } catch (e: Exception) {
            println("Error initializing DailyAnalyticsFragment: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun initViews(view: View) {
        try {
            tvTotalExpenses = view.findViewById(R.id.tvTotalExpenses)
            tvTotalIncome = view.findViewById(R.id.tvTotalIncome)
            tvSelectedDate = view.findViewById(R.id.tvSelectedDate)
            tvNetBalance = view.findViewById(R.id.tvNetBalance)
            recyclerViewDailyChart = view.findViewById(R.id.recyclerViewDailyChart)
            btnPreviousDay = view.findViewById(R.id.btnPreviousDay)
            btnNextDay = view.findViewById(R.id.btnNextDay)

            // Setup navigation buttons
            btnPreviousDay.setOnClickListener {
                currentDateOffset--
                val calendar = Calendar.getInstance()
                calendar.time = Date()
                calendar.add(Calendar.DAY_OF_YEAR, currentDateOffset)
                selectedDate = calendar.time
                Toast.makeText(context, "Previous Day", Toast.LENGTH_SHORT).show()
                loadDailyDataForDate(selectedDate)
                updateNavigationButtons()
            }

            btnNextDay.setOnClickListener {
                if (currentDateOffset < 0) {
                    currentDateOffset++
                    val calendar = Calendar.getInstance()
                    calendar.time = Date()
                    calendar.add(Calendar.DAY_OF_YEAR, currentDateOffset)
                    selectedDate = calendar.time
                    Toast.makeText(context, "Next Day", Toast.LENGTH_SHORT).show()
                    loadDailyDataForDate(selectedDate)
                    updateNavigationButtons()
                }
            }

            // Setup RecyclerView
            dailyChartAdapter = DailyChartAdapter()
            recyclerViewDailyChart.apply {
                layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                adapter = dailyChartAdapter
            }
        } catch (e: Exception) {
            println("Error initializing views: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun updateNavigationButtons() {
        // Hide/disable next button if selected date is today or later
        val today = Date()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val selectedDateString = dateFormat.format(selectedDate)
        val todayString = dateFormat.format(today)

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

                tvTotalExpenses.text = "₹${String.format("%.2f", totalExpense)}"
                tvTotalIncome.text = "₹${String.format("%.2f", totalIncome)}"

                // Format net balance with proper sign and color indication
                if (netBalance >= 0) {
                    tvNetBalance.text = "₹${String.format("%.2f", netBalance)}"
                    tvNetBalance.setTextColor(
                            requireContext().getColor(android.R.color.holo_green_light)
                    )
                } else {
                    tvNetBalance.text = "-₹${String.format("%.2f", Math.abs(netBalance))}"
                    tvNetBalance.setTextColor(
                            requireContext().getColor(android.R.color.holo_red_light)
                    )
                }

                val todayCalendar = Calendar.getInstance()
                todayCalendar.time = Date()

                val dateText =
                        when (currentDateOffset) {
                            0 -> "Today: ${displayFormat.format(targetDate)}"
                            -1 -> "Yesterday: ${displayFormat.format(targetDate)}"
                            else -> displayFormat.format(targetDate)
                        }
                tvSelectedDate.text = dateText

                println(
                        "Selected date totals - Income: $totalIncome, Expense: $totalExpense, Net: $netBalance"
                )
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

    fun refreshData() {
        loadDailyData()
    }
}
