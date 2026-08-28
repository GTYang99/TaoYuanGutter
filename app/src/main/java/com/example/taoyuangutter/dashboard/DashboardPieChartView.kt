package com.example.taoyuangutter.dashboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.example.taoyuangutter.R
import com.example.taoyuangutter.api.DashboardSlice
import kotlin.math.min

class DashboardPieChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val slicePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.BUTT
    }
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = resources.displayMetrics.density * 14f
        color = ContextCompat.getColor(context, R.color.map_borders_grey)
    }
    private val holePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.white)
    }

    private var slices: List<DashboardSlice> = emptyList()
    private val rect = RectF()

    fun setSlices(slices: List<DashboardSlice>) {
        this.slices = slices
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val total = slices.fold(0) { acc, slice -> acc + slice.value }
        val size = min(width, height).toFloat()
        val inset = ringPaint.strokeWidth / 2f + resources.displayMetrics.density * 8f
        rect.set(
            (width - size) / 2f + inset,
            (height - size) / 2f + inset,
            (width + size) / 2f - inset,
            (height + size) / 2f - inset
        )

        canvas.drawOval(rect, ringPaint)
        if (total <= 0) return

        slicePaint.strokeWidth = ringPaint.strokeWidth
        var startAngle = -90f
        slices.forEach { slice ->
            if (slice.value <= 0) return@forEach
            val sweep = 360f * slice.value / total
            slicePaint.color = slice.color
            canvas.drawArc(rect, startAngle, sweep, false, slicePaint)
            startAngle += sweep
        }

        val holeRadius = (rect.width().coerceAtMost(rect.height()) / 2f) - slicePaint.strokeWidth / 2f
        canvas.drawCircle(width / 2f, height / 2f, holeRadius, holePaint)
    }
}
