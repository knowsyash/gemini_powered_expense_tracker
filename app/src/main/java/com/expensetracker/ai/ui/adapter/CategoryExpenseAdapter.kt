package com.expensetracker.ai.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.expensetracker.ai.R

data class CategorySummaryItem(
        val mainCategory: String,
        val totalAmount: Double,
        val transactionCount: Int,
        val topSubcategory: String,
        val topSubcategoryAmount: Double
)

class CategoryExpenseAdapter : RecyclerView.Adapter<CategoryExpenseAdapter.CategoryViewHolder>() {

    private var categorySummaries: List<CategorySummaryItem> = emptyList()
    private var onCategoryClickListener: ((CategorySummaryItem) -> Unit)? = null

    fun updateData(summaries: List<CategorySummaryItem>) {
        this.categorySummaries = summaries
        notifyDataSetChanged()
    }

    fun setOnCategoryClickListener(listener: (CategorySummaryItem) -> Unit) {
        this.onCategoryClickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view =
                LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_category_summary, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(categorySummaries[position])
    }

    override fun getItemCount(): Int = categorySummaries.size

    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvCategoryTitle: TextView = itemView.findViewById(R.id.tvCategoryTitle)
        private val tvSummaryText: TextView = itemView.findViewById(R.id.tvSummaryText)
        private val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
        private val tvTopSubcategory: TextView = itemView.findViewById(R.id.tvTopSubcategory)

        fun bind(item: CategorySummaryItem) {
            tvCategoryTitle.text = item.mainCategory
            tvSummaryText.text =
                    "This month you spent in ${item.transactionCount} transaction${if (item.transactionCount > 1) "s" else ""}"
            tvAmount.text = "₹${String.format("%.2f", item.totalAmount)}"
            tvTopSubcategory.text =
                    "Most spent: ${item.topSubcategory} (₹${String.format("%.2f", item.topSubcategoryAmount)})"

            // Set category-specific colors and emojis
            val (colorRes, emoji) =
                    when (item.mainCategory) {
                        "Travel" -> Pair(android.R.color.holo_blue_light, "✈️")
                        "Food" -> Pair(android.R.color.holo_green_light, "🍽️")
                        "Stationary" -> Pair(android.R.color.holo_orange_light, "📚")
                        "Miscellaneous" -> Pair(android.R.color.holo_purple, "🔗")
                        else -> Pair(android.R.color.darker_gray, "💰")
                    }

            tvCategoryTitle.text = "$emoji ${item.mainCategory}"
            tvCategoryTitle.setTextColor(itemView.context.getColor(colorRes))

            itemView.setOnClickListener { onCategoryClickListener?.invoke(item) }
        }
    }
}
