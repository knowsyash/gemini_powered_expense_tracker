package com.expensetracker.ai.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.expensetracker.ai.R
import com.expensetracker.ai.data.model.Expense
import com.expensetracker.ai.utils.DateUtils
import java.util.*

class ExpenseAdapter(
        private val onItemClick: (Expense) -> Unit,
        private val onItemLongClick: (Expense) -> Unit
) : ListAdapter<Expense, ExpenseAdapter.ExpenseViewHolder>(ExpenseDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_expense, parent, false)
        return ExpenseViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ExpenseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val categoryText: TextView = itemView.findViewById(R.id.tvCategory)
        private val descriptionText: TextView = itemView.findViewById(R.id.tvDescription)
        private val amountText: TextView = itemView.findViewById(R.id.tvAmount)
        private val dateText: TextView = itemView.findViewById(R.id.tvDate)
        private val typeText: TextView = itemView.findViewById(R.id.tvType)

        fun bind(expense: Expense) {
            // Show category with ID for identification
            categoryText.text = "${expense.category} (ID: ${expense.uniqueId})"
            descriptionText.text = expense.description

            val formattedAmount = "₹${String.format("%.2f", expense.amount)}"
            amountText.text = if (expense.isIncome) "+$formattedAmount" else "-$formattedAmount"
            amountText.setTextColor(
                    if (expense.isIncome) itemView.context.getColor(R.color.income_green)
                    else itemView.context.getColor(R.color.expense_red)
            )

            // Set transaction type with proper color
            typeText.text = if (expense.isIncome) "Income" else "Expense"
            typeText.setTextColor(
                    if (expense.isIncome) itemView.context.getColor(R.color.income_green)
                    else itemView.context.getColor(R.color.expense_red)
            )

            dateText.text = DateUtils.formatDate(expense.date)

            itemView.setOnClickListener { onItemClick(expense) }
            itemView.setOnLongClickListener {
                onItemLongClick(expense)
                true
            }
        }
    }

    class ExpenseDiffCallback : DiffUtil.ItemCallback<Expense>() {
        override fun areItemsTheSame(oldItem: Expense, newItem: Expense): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Expense, newItem: Expense): Boolean {
            return oldItem == newItem
        }
    }
}
