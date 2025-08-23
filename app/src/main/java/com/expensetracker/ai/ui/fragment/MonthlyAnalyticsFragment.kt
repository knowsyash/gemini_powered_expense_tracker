package com.expensetracker.ai.ui.fragment

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.expensetracker.ai.ExpenseTrackerApplication
import com.expensetracker.ai.R
import com.expensetracker.ai.data.model.CategoryStructure
import com.expensetracker.ai.data.model.Expense
import com.expensetracker.ai.data.repository.BudgetRepository
import com.expensetracker.ai.data.repository.ExpenseRepository
import com.expensetracker.ai.ui.adapter.CategoryExpenseAdapter
import com.expensetracker.ai.ui.adapter.CategorySummaryItem
import com.expensetracker.ai.ui.adapter.LegendAdapter
import com.expensetracker.ai.ui.adapter.SubcategoryBreakdownAdapter
import com.expensetracker.ai.ui.adapter.SubcategoryItem
import com.expensetracker.ai.ui.view.ChartData
import com.expensetracker.ai.ui.view.PieChartView
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
    private lateinit var budgetRepository: BudgetRepository
    private lateinit var tvTotalBudget: TextView
    private lateinit var tvTotalIncome: TextView
    private lateinit var tvTotalExpenses: TextView
    private lateinit var tvSelectedMonth: TextView
    private lateinit var tvNetBalance: TextView
    private lateinit var recyclerViewMonthlyChart: RecyclerView
    private lateinit var monthlyChartAdapter: MonthlyChartAdapter
    private lateinit var recyclerViewCategoryBreakdown: RecyclerView
    private lateinit var categoryExpenseAdapter: CategoryExpenseAdapter
    private lateinit var btnPreviousMonth: ImageButton
    private lateinit var btnNextMonth: ImageButton
    private lateinit var btnViewMoreCategories: com.google.android.material.button.MaterialButton

    private var currentMonthOffset = 0
    private var currentMonthExpenses: List<Expense> = emptyList()

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
            budgetRepository =
                    BudgetRepository(
                            (requireActivity().application as ExpenseTrackerApplication).database
                                    .budgetDao()
                    )

            initViews(view)
            loadMonthlyData()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun initViews(view: View) {
        tvTotalBudget = view.findViewById(R.id.tvTotalBudget)
        tvTotalIncome = view.findViewById(R.id.tvTotalIncome)
        tvTotalExpenses = view.findViewById(R.id.tvTotalExpenses)
        tvSelectedMonth = view.findViewById(R.id.tvSelectedMonth)
        tvNetBalance = view.findViewById(R.id.tvNetBalance)
        recyclerViewMonthlyChart = view.findViewById(R.id.recyclerViewMonthlyChart)
        recyclerViewCategoryBreakdown = view.findViewById(R.id.recyclerViewCategoryBreakdown)
        btnPreviousMonth = view.findViewById(R.id.btnPreviousMonth)
        btnNextMonth = view.findViewById(R.id.btnNextMonth)
        btnViewMoreCategories = view.findViewById(R.id.btnViewMoreCategories)

        // Setup RecyclerView
        monthlyChartAdapter = MonthlyChartAdapter()
        recyclerViewMonthlyChart.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = monthlyChartAdapter
        }

        // Setup Category Breakdown RecyclerView
        categoryExpenseAdapter = CategoryExpenseAdapter()
        recyclerViewCategoryBreakdown.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = categoryExpenseAdapter
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

        btnViewMoreCategories.setOnClickListener { showCategoryGraphDialog() }

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

                // Always show current month details by default - removed 6-month summary view
                // Calculate specific month totals for the target date
                calendar.time = targetDate
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

                val monthlyExpenses =
                        allExpenses.filter { expense ->
                            expense.date.time >= monthStart.time &&
                                    expense.date.time <= monthEnd.time
                        }

                // Store current month expenses for the dialog
                currentMonthExpenses = monthlyExpenses

                println("Target month expenses count: ${monthlyExpenses.size}")

                val totalExpense = monthlyExpenses.filter { !it.isIncome }.sumOf { it.amount }
                val totalIncome = monthlyExpenses.filter { it.isIncome }.sumOf { it.amount }
                val netBalance = totalIncome - totalExpense

                // Process category breakdown for expenses only
                val expenseTransactions = monthlyExpenses.filter { !it.isIncome }
                val categoryBreakdown = processCategoryBreakdown(expenseTransactions)
                categoryExpenseAdapter.updateData(categoryBreakdown)

                // Get budget information for this month
                val budgetCalendar = Calendar.getInstance()
                budgetCalendar.time = targetDate
                val targetMonth = budgetCalendar.get(Calendar.MONTH) + 1 // 1-based
                val targetYear = budgetCalendar.get(Calendar.YEAR)
                val budget = budgetRepository.getBudgetForMonth(targetMonth, targetYear)

                tvTotalExpenses.text = "₹${String.format("%.2f", totalExpense)}"
                tvTotalIncome.text = "₹${String.format("%.2f", totalIncome)}"

                // Display remaining budget (budget - expenses) in the budget field
                if (budget != null) {
                    val remainingBudget = budget.amount - totalExpense
                    tvTotalBudget.text = "₹${String.format("%.2f", remainingBudget)}"

                    // Color code the remaining budget
                    when {
                        remainingBudget > 0 -> {
                            tvTotalBudget.setTextColor(
                                    requireContext().getColor(android.R.color.holo_green_light)
                            )
                        }
                        remainingBudget == 0.0 -> {
                            tvTotalBudget.setTextColor(
                                    requireContext().getColor(android.R.color.holo_orange_light)
                            )
                        }
                        else -> {
                            tvTotalBudget.setTextColor(
                                    requireContext().getColor(android.R.color.holo_red_light)
                            )
                        }
                    }
                } else {
                    tvTotalBudget.text = "₹0.00" // No budget set
                    tvTotalBudget.setTextColor(
                            requireContext().getColor(android.R.color.darker_gray)
                    )
                }

                // Show simple net balance (income - expenses)
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

                val displayFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                val todayCalendar = Calendar.getInstance()
                todayCalendar.time = Date()

                val monthText =
                        when (currentMonthOffset) {
                            0 -> "This Month: ${displayFormat.format(targetDate)}"
                            -1 -> "Last Month: ${displayFormat.format(targetDate)}"
                            else -> displayFormat.format(targetDate)
                        }
                tvSelectedMonth.text = monthText

                println(
                        "Specific month - Income: $totalIncome, Expense: $totalExpense, Net: $netBalance"
                )

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

    private fun processCategoryBreakdown(
            expenses: List<com.expensetracker.ai.data.model.Expense>
    ): List<CategorySummaryItem> {
        // Group expenses by main category
        val mainCategoryGroups =
                expenses.groupBy { expense ->
                    CategoryStructure.getMainCategoryForSubcategory(expense.category)
                }

        val categorySummaries = mutableListOf<CategorySummaryItem>()

        // Process each main category
        for (mainCategory in CategoryStructure.MAIN_CATEGORIES) {
            val categoryExpenses = mainCategoryGroups[mainCategory] ?: emptyList()

            if (categoryExpenses.isNotEmpty()) {
                val totalAmount = categoryExpenses.sumOf { it.amount }
                val transactionCount = categoryExpenses.size

                // Find the subcategory with highest spending
                val subcategoryGroups = categoryExpenses.groupBy { it.category }
                val topSubcategory =
                        subcategoryGroups.maxByOrNull { (_, expenseList) ->
                            expenseList.sumOf { it.amount }
                        }

                val topSubcategoryName = topSubcategory?.key ?: "None"
                val topSubcategoryAmount = topSubcategory?.value?.sumOf { it.amount } ?: 0.0

                categorySummaries.add(
                        CategorySummaryItem(
                                mainCategory = mainCategory,
                                totalAmount = totalAmount,
                                transactionCount = transactionCount,
                                topSubcategory = topSubcategoryName,
                                topSubcategoryAmount = topSubcategoryAmount
                        )
                )
            }
        }

        // Sort by amount descending
        return categorySummaries.sortedByDescending { it.totalAmount }
    }

    private fun showCategoryGraphDialog() {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_category_graph)

        // Set dialog properties
        dialog.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Initialize dialog views
        val tvDialogTitle = dialog.findViewById<TextView>(R.id.tvDialogTitle)
        val btnCloseDialog = dialog.findViewById<ImageButton>(R.id.btnCloseDialog)
        val btnClose =
                dialog.findViewById<com.google.android.material.button.MaterialButton>(
                        R.id.btnClose
                )
        val chartContainer = dialog.findViewById<ViewGroup>(R.id.chartContainer)
        val tvChartCenter = dialog.findViewById<TextView>(R.id.tvChartCenter)
        val recyclerViewLegend = dialog.findViewById<RecyclerView>(R.id.recyclerViewLegend)

        // Set title with current month
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MONTH, currentMonthOffset)
        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        tvDialogTitle.text = "Category Breakdown - ${monthFormat.format(calendar.time)}"

        // Create chart data
        val chartDataList =
                createChartDataFromExpenses(currentMonthExpenses.filter { !it.isIncome })

        if (chartDataList.isNotEmpty()) {
            // Create and add pie chart
            val pieChart = PieChartView(requireContext())
            pieChart.setData(chartDataList)
            chartContainer.removeAllViews()
            chartContainer.addView(
                    pieChart,
                    ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                    )
            )

            // Set total amount in center
            val totalAmount = chartDataList.sumOf { it.value }
            tvChartCenter.text = "Total\n₹${String.format("%.2f", totalAmount)}"

            // Setup legend RecyclerView
            val legendAdapter = LegendAdapter()
            recyclerViewLegend.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = legendAdapter
            }
            legendAdapter.updateData(chartDataList)

            // Set click listener for miscellaneous category
            legendAdapter.setOnCategoryClickListener { categoryName ->
                if (categoryName == "Miscellaneous") {
                    dialog.dismiss()
                    showSubcategoryBreakdownDialog()
                }
            }
        } else {
            tvChartCenter.text = "No Data\n₹0.00"
        }

        // Set close button listeners
        btnCloseDialog.setOnClickListener { dialog.dismiss() }
        btnClose.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    private fun createChartDataFromExpenses(expenses: List<Expense>): List<ChartData> {
        if (expenses.isEmpty()) return emptyList()

        val categoryTotals =
                expenses
                        .groupBy { expense ->
                            CategoryStructure.getMainCategoryForSubcategory(expense.category)
                        }
                        .mapValues { (_, expenseList) -> expenseList.sumOf { it.amount } }

        val totalAmount = categoryTotals.values.sum()
        if (totalAmount == 0.0) return emptyList()

        return categoryTotals
                .map { (category, amount) ->
                    val percentage = (amount / totalAmount * 100).toFloat()
                    val color =
                            when (category) {
                                "Travel" ->
                                        ContextCompat.getColor(
                                                requireContext(),
                                                R.color.travel_color
                                        )
                                "Food" ->
                                        ContextCompat.getColor(requireContext(), R.color.food_color)
                                "Stationary" ->
                                        ContextCompat.getColor(
                                                requireContext(),
                                                R.color.stationary_color
                                        )
                                "Miscellaneous" ->
                                        ContextCompat.getColor(requireContext(), R.color.misc_color)
                                else -> ContextCompat.getColor(requireContext(), R.color.misc_color)
                            }

                    ChartData(
                            label = category,
                            value = amount,
                            color = color,
                            percentage = percentage
                    )
                }
                .sortedByDescending { it.value }
    }

    private fun showSubcategoryBreakdownDialog() {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_subcategory_breakdown)

        // Set dialog properties
        dialog.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Initialize dialog views
        val tvSubcategoryDialogTitle = dialog.findViewById<TextView>(R.id.tvSubcategoryDialogTitle)
        val btnCloseSubcategoryDialog =
                dialog.findViewById<ImageButton>(R.id.btnCloseSubcategoryDialog)
        val btnCloseSubcategory =
                dialog.findViewById<com.google.android.material.button.MaterialButton>(
                        R.id.btnCloseSubcategory
                )
        val tvTotalMiscAmount = dialog.findViewById<TextView>(R.id.tvTotalMiscAmount)
        val recyclerViewSubcategories =
                dialog.findViewById<RecyclerView>(R.id.recyclerViewSubcategories)
        val layoutSubcategoryEmptyState =
                dialog.findViewById<View>(R.id.layoutSubcategoryEmptyState)

        // Set title with current month
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MONTH, currentMonthOffset)
        val monthFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
        tvSubcategoryDialogTitle.text = "💼 Miscellaneous - ${monthFormat.format(calendar.time)}"

        // Get miscellaneous expenses
        val miscExpenses =
                currentMonthExpenses.filter {
                    !it.isIncome &&
                            CategoryStructure.getMainCategoryForSubcategory(it.category) ==
                                    "Miscellaneous"
                }

        if (miscExpenses.isNotEmpty()) {
            val totalMiscAmount = miscExpenses.sumOf { it.amount }
            tvTotalMiscAmount.text = "₹${String.format("%.2f", totalMiscAmount)}"

            // Create subcategory breakdown
            val subcategoryData = createSubcategoryBreakdown(miscExpenses, totalMiscAmount)

            // Setup RecyclerView
            val subcategoryAdapter = SubcategoryBreakdownAdapter()
            recyclerViewSubcategories.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = subcategoryAdapter
            }
            subcategoryAdapter.updateData(subcategoryData)

            recyclerViewSubcategories.visibility = View.VISIBLE
            layoutSubcategoryEmptyState.visibility = View.GONE
        } else {
            tvTotalMiscAmount.text = "₹0.00"
            recyclerViewSubcategories.visibility = View.GONE
            layoutSubcategoryEmptyState.visibility = View.VISIBLE
        }

        // Set close button listeners
        btnCloseSubcategoryDialog.setOnClickListener { dialog.dismiss() }
        btnCloseSubcategory.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    private fun createSubcategoryBreakdown(
            miscExpenses: List<Expense>,
            totalAmount: Double
    ): List<SubcategoryItem> {
        val subcategoryTotals =
                miscExpenses.groupBy { it.category }.mapValues { (_, expenseList) ->
                    Pair(expenseList.sumOf { it.amount }, expenseList.size)
                }

        return subcategoryTotals
                .map { (subcategory, amountAndCount) ->
                    val amount = amountAndCount.first
                    val count = amountAndCount.second
                    val percentage =
                            if (totalAmount > 0) (amount / totalAmount * 100).toFloat() else 0f

                    val icon = getSubcategoryIcon(subcategory)

                    SubcategoryItem(
                            name = subcategory,
                            amount = amount,
                            transactionCount = count,
                            percentage = percentage,
                            icon = icon
                    )
                }
                .sortedByDescending { it.amount }
    }

    private fun getSubcategoryIcon(subcategory: String): String {
        return when (subcategory.lowercase()) {
            "friend", "friends" -> "👥"
            "family" -> "👨‍👩‍👧‍👦"
            "gift", "gifts" -> "🎁"
            "donation", "charity" -> "💝"
            "entertainment" -> "🎬"
            "health", "medical" -> "🏥"
            "personal care" -> "💅"
            "bills", "utilities" -> "📄"
            "other misc", "miscellaneous", "random" -> "💼"
            else -> "💰"
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when fragment becomes visible to show updated budget info
        loadMonthlyData()
    }

    fun refreshData() {
        loadMonthlyData()
    }
}
