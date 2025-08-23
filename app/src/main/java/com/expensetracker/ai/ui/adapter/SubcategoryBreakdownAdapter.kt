package com.expensetracker.ai.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.expensetracker.ai.R

data class SubcategoryItem(
        val name: String,
        val amount: Double,
        val transactionCount: Int,
        val percentage: Float,
        val icon: String
)

class SubcategoryBreakdownAdapter :
        RecyclerView.Adapter<SubcategoryBreakdownAdapter.SubcategoryViewHolder>() {

    private var subcategories: List<SubcategoryItem> = emptyList()

    fun updateData(data: List<SubcategoryItem>) {
        this.subcategories = data
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubcategoryViewHolder {
        val view =
                LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_subcategory_breakdown, parent, false)
        return SubcategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: SubcategoryViewHolder, position: Int) {
        holder.bind(subcategories[position])
    }

    override fun getItemCount(): Int = subcategories.size

    class SubcategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvSubcategoryIcon: TextView = itemView.findViewById(R.id.tvSubcategoryIcon)
        private val tvSubcategoryName: TextView = itemView.findViewById(R.id.tvSubcategoryName)
        private val tvSubcategoryTransactions: TextView =
                itemView.findViewById(R.id.tvSubcategoryTransactions)
        private val tvSubcategoryAmount: TextView = itemView.findViewById(R.id.tvSubcategoryAmount)
        private val tvSubcategoryPercentage: TextView =
                itemView.findViewById(R.id.tvSubcategoryPercentage)
        private val progressSubcategory: ProgressBar =
                itemView.findViewById(R.id.progressSubcategory)

        fun bind(item: SubcategoryItem) {
            tvSubcategoryIcon.text = item.icon
            tvSubcategoryName.text = item.name
            tvSubcategoryTransactions.text =
                    "${item.transactionCount} transaction${if(item.transactionCount != 1) "s" else ""}"
            tvSubcategoryAmount.text = "₹${String.format("%.2f", item.amount)}"
            tvSubcategoryPercentage.text = "${String.format("%.1f", item.percentage)}%"

            // Set progress bar
            progressSubcategory.progress = item.percentage.toInt()
        }
    }
}
