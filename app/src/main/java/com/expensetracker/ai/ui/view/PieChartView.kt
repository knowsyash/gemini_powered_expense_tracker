package com.expensetracker.ai.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.expensetracker.ai.R

data class ChartData(val label: String, val value: Double, val color: Int, val percentage: Float)

class PieChartView
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
        View(context, attrs, defStyleAttr) {

    private var chartData: List<ChartData> = emptyList()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rectF = RectF()

    // Category colors
    private val categoryColors =
            mapOf(
                    "Travel" to ContextCompat.getColor(context, R.color.travel_color),
                    "Food" to ContextCompat.getColor(context, R.color.food_color),
                    "Stationary" to ContextCompat.getColor(context, R.color.stationary_color),
                    "Miscellaneous" to ContextCompat.getColor(context, R.color.misc_color)
            )

    fun setData(data: List<ChartData>) {
        this.chartData = data
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (chartData.isEmpty()) return

        val centerX = width / 2f
        val centerY = height / 2f
        val radius = minOf(centerX, centerY) * 0.8f

        // Set up the rectangle for the pie chart
        rectF.set(centerX - radius, centerY - radius, centerX + radius, centerY + radius)

        var startAngle = -90f // Start from top

        chartData.forEach { data ->
            val sweepAngle = (data.percentage / 100f) * 360f

            paint.color = data.color
            paint.style = Paint.Style.FILL

            canvas.drawArc(rectF, startAngle, sweepAngle, true, paint)

            // Draw stroke between segments
            paint.color = ContextCompat.getColor(context, R.color.surface_color)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 4f
            canvas.drawArc(rectF, startAngle, sweepAngle, true, paint)

            startAngle += sweepAngle
        }

        // Draw center circle
        paint.color = ContextCompat.getColor(context, R.color.surface_color)
        paint.style = Paint.Style.FILL
        canvas.drawCircle(centerX, centerY, radius * 0.4f, paint)
    }
}
