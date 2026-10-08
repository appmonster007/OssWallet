package com.osscabinet.ossWallet.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF

/** One Income/Expense Bar widget bucket, already converted to plain doubles for drawing. */
data class WidgetBarBucket(
    val label: String,
    val income: Double,
    val expense: Double,
    val net: Double,
    val isFuture: Boolean
)

/**
 * Grouped bar chart (income/expense pair per bucket) + a net-flow trend line truncated at
 * `todayBucketIndex`, for the Income vs Expenses widget. Buckets after `todayBucketIndex` are
 * drawn at ~30% alpha (still full height) to mark them as planned/future, per the Phase 3 spec.
 */
object WidgetBarChartRenderer {

    fun renderGroupedBars(
        widthPx: Int,
        heightPx: Int,
        buckets: List<WidgetBarBucket>,
        todayBucketIndex: Int,
        incomeColor: Int,
        expenseColor: Int,
        netLineColor: Int,
        axisLabelColor: Int,
        density: Float
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(widthPx.coerceAtLeast(1), heightPx.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        if (buckets.isEmpty()) return bitmap

        val labelHeight = 14f * density
        val chartBottom = heightPx - labelHeight
        val chartTop = 4f * density
        val maxVal = buckets.flatMap { listOf(it.income, it.expense) }.maxOrNull()?.takeIf { it > 0.0 } ?: 1.0

        val groupWidth = widthPx.toFloat() / buckets.size
        val barGap = 2f * density
        val barWidth = ((groupWidth - barGap * 3) / 2f).coerceAtLeast(1f)

        val barPaint = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL }
        val labelPaint = Paint().apply {
            isAntiAlias = true
            color = axisLabelColor
            textSize = 8.5f * density
            textAlign = Paint.Align.CENTER
        }

        // Thin out x-axis labels if more than ~6 buckets, matching Income/Expense Bar spec.
        val labelStep = if (buckets.size > 6) 2 else 1

        buckets.forEachIndexed { idx, bucket ->
            val groupLeft = groupWidth * idx
            val alpha = if (bucket.isFuture) 77 else 255 // ~30%

            val incomeHeight = (bucket.income / maxVal * (chartBottom - chartTop)).toFloat()
            val incomeRect = RectF(
                groupLeft + barGap, chartBottom - incomeHeight,
                groupLeft + barGap + barWidth, chartBottom
            )
            barPaint.color = incomeColor
            barPaint.alpha = alpha
            canvas.drawRect(incomeRect, barPaint)

            val expenseHeight = (bucket.expense / maxVal * (chartBottom - chartTop)).toFloat()
            val expenseRect = RectF(
                groupLeft + barGap * 2 + barWidth, chartBottom - expenseHeight,
                groupLeft + barGap * 2 + barWidth * 2, chartBottom
            )
            barPaint.color = expenseColor
            barPaint.alpha = alpha
            canvas.drawRect(expenseRect, barPaint)

            if (idx % labelStep == 0) {
                labelPaint.alpha = if (bucket.isFuture) 140 else 255
                canvas.drawText(
                    bucket.label.take(4),
                    groupLeft + groupWidth / 2f,
                    heightPx - 2f * density,
                    labelPaint
                )
            }
        }

        // Net-flow trend line, drawn only through 0..todayBucketIndex inclusive.
        if (todayBucketIndex >= 0) {
            val netMax = buckets.map { kotlin.math.abs(it.net) }.maxOrNull()?.takeIf { it > 0.0 } ?: maxVal
            val linePaint = Paint().apply {
                isAntiAlias = true
                color = netLineColor
                style = Paint.Style.STROKE
                strokeWidth = 2f * density
                strokeCap = Paint.Cap.ROUND
            }
            val path = Path()
            val midY = chartTop + (chartBottom - chartTop) / 2f
            val ampY = (chartBottom - chartTop) / 2.2f
            for (i in 0..todayBucketIndex.coerceAtMost(buckets.size - 1)) {
                val x = groupWidth * i + groupWidth / 2f
                val y = midY - (buckets[i].net / netMax * ampY).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            canvas.drawPath(path, linePaint)
        }

        return bitmap
    }

    fun parseColorOrDefault(hex: String?, fallback: Int = Color.GRAY) =
        WidgetSparklineRenderer.parseColorOrDefault(hex, fallback)
}
