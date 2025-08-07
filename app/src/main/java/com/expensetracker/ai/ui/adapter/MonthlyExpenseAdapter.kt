package com.expensetracker.ai.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.expensetracker.ai.R
import com.expensetracker.ai.ui.viewmodel.MonthlyExpenseData

class MonthlyExpenseAdapter : ListAdapter<MonthlyExpenseData, MonthlyExpenseAdapter.MonthlyExpenseViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MonthlyExpenseViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_expense, parent, false)
        return MonthlyExpenseViewHolder(view)
    }

    override fun onBindViewHolder(holder: MonthlyExpenseViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class MonthlyExpenseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
        private val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)

        fun bind(data: MonthlyExpenseData) {
            tvAmount.text = "₹${String.format("%.0f", data.totalExpense)}"
            tvDescription.text = "${data.transactionCount} transactions"
            tvDate.text = data.monthString
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<MonthlyExpenseData>() {
        override fun areItemsTheSame(oldItem: MonthlyExpenseData, newItem: MonthlyExpenseData): Boolean {
            return oldItem.month == newItem.month
        }

        override fun areContentsTheSame(oldItem: MonthlyExpenseData, newItem: MonthlyExpenseData): Boolean {
            return oldItem == newItem
        }
    }
}
