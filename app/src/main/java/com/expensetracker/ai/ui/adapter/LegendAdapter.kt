package com.expensetracker.ai.ui.adapter

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.expensetracker.ai.R
import com.expensetracker.ai.ui.view.ChartData

class LegendAdapter : RecyclerView.Adapter<LegendAdapter.LegendViewHolder>() {

    private var legendData: List<ChartData> = emptyList()
    private var onCategoryClickListener: ((String) -> Unit)? = null

    fun updateData(data: List<ChartData>) {
        this.legendData = data
        notifyDataSetChanged()
    }

    fun setOnCategoryClickListener(listener: (String) -> Unit) {
        this.onCategoryClickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LegendViewHolder {
        val view =
                LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_legend_category, parent, false)
        return LegendViewHolder(view)
    }

    override fun onBindViewHolder(holder: LegendViewHolder, position: Int) {
        holder.bind(legendData[position], onCategoryClickListener)
    }

    override fun getItemCount(): Int = legendData.size

    class LegendViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val colorIndicator: View = itemView.findViewById(R.id.colorIndicator)
        private val tvCategoryName: TextView = itemView.findViewById(R.id.tvCategoryName)
        private val tvCategoryDetails: TextView = itemView.findViewById(R.id.tvCategoryDetails)
        private val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
        private val tvPercentage: TextView = itemView.findViewById(R.id.tvPercentage)

        fun bind(data: ChartData, onCategoryClickListener: ((String) -> Unit)?) {
            // Set color indicator
            val drawable =
                    ContextCompat.getDrawable(itemView.context, R.drawable.circle_background)
                            ?.mutate() as?
                            GradientDrawable
            drawable?.setColor(data.color)
            colorIndicator.background = drawable

            // Set category name with emoji
            val emoji =
                    when (data.label) {
                        "Travel" -> "🚗"
                        "Food" -> "🍽️"
                        "Stationary" -> "📝"
                        "Miscellaneous" -> "💼"
                        else -> ""
                    }
            tvCategoryName.text = "$emoji ${data.label}"

            // Update details text based on category
            if (data.label == "Miscellaneous") {
                tvCategoryDetails.text = "Click to view breakdown"
                itemView.setOnClickListener { onCategoryClickListener?.invoke(data.label) }
                // Add visual feedback for clickable item
                itemView.isClickable = true
                itemView.isFocusable = true
            } else {
                tvCategoryDetails.text = "Click for details"
                itemView.setOnClickListener(null)
                itemView.isClickable = false
                itemView.isFocusable = false
            }

            // Set amount
            tvAmount.text = "₹${String.format("%.2f", data.value)}"

            // Set percentage
            tvPercentage.text = "${String.format("%.1f", data.percentage)}%"
        }
    }
}
