package com.osscabinet.ossWallet.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF

/** One stacked-bar segment, pre-converted to a plain double for drawing. */
data class WidgetStackedSegment(val colorHex: String, val amount: Double)

/** One Category Spending Trend bucket. */
data class WidgetStackedBucket(val label: String, val segments: List<WidgetStackedSegment>)

/**
 * Stacked bar chart for the Category Spending Trend widget — one bar-group per bucket, each
 * vertically stacked by category amount using each segment's own color.
 */
object WidgetStackedBarChartRenderer {

    fun renderStackedBars(
        widthPx: Int,
        heightPx: Int,
        buckets: List<WidgetStackedBucket>,
        axisLabelColor: Int,
        density: Float
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(widthPx.coerceAtLeast(1), heightPx.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        if (buckets.isEmpty()) return bitmap

        val labelHeight = 14f * density
        val chartBottom = heightPx - labelHeight
        val chartTop = 4f * density
        val maxTotal = buckets.maxOf { b -> b.segments.sumOf { it.amount } }.takeIf { it > 0.0 } ?: 1.0

        val barGap = 3f * density
        val groupWidth = widthPx.toFloat() / buckets.size
        val barWidth = (groupWidth - barGap).coerceAtLeast(1f)

        val segPaint = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL }
        val labelPaint = Paint().apply {
            isAntiAlias = true
            color = axisLabelColor
            textSize = 8.5f * density
            textAlign = Paint.Align.CENTER
        }
        val labelStep = if (buckets.size > 6) 2 else 1

        buckets.forEachIndexed { idx, bucket ->
            val left = groupWidth * idx + barGap / 2f
            var cursorY = chartBottom
            bucket.segments.forEach { seg ->
                val segHeight = (seg.amount / maxTotal * (chartBottom - chartTop)).toFloat()
                if (segHeight <= 0f) return@forEach
                segPaint.color = WidgetSparklineRenderer.parseColorOrDefault(seg.colorHex)
                canvas.drawRect(RectF(left, cursorY - segHeight, left + barWidth, cursorY), segPaint)
                cursorY -= segHeight
            }
            if (idx % labelStep == 0) {
                canvas.drawText(bucket.label.take(4), left + barWidth / 2f, heightPx - 2f * density, labelPaint)
            }
        }
        return bitmap
    }
}
