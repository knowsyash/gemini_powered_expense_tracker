package com.expensetracker.ai.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.expensetracker.ai.R
import com.expensetracker.ai.ui.viewmodel.DailyExpenseData

class DailyExpenseAdapter : ListAdapter<DailyExpenseData, DailyExpenseAdapter.DailyExpenseViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DailyExpenseViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_expense, parent, false)
        return DailyExpenseViewHolder(view)
    }

    override fun onBindViewHolder(holder: DailyExpenseViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DailyExpenseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
        private val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)

        fun bind(data: DailyExpenseData) {
            tvAmount.text = "₹${String.format("%.0f", data.totalExpense)}"
            tvDescription.text = "${data.dayOfWeek} - ${data.transactionCount} transactions"
            tvDate.text = data.dateString
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<DailyExpenseData>() {
        override fun areItemsTheSame(oldItem: DailyExpenseData, newItem: DailyExpenseData): Boolean {
            return oldItem.date == newItem.date
        }

        override fun areContentsTheSame(oldItem: DailyExpenseData, newItem: DailyExpenseData): Boolean {
            return oldItem == newItem
        }
    }
}
