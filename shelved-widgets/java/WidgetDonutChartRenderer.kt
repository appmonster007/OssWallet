package com.osscabinet.ossWallet.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF

/**
 * Donut (ring, not filled pie) bitmap for the Category Pie Chart widget — one `drawArc` sweep per
 * slice, proportional to [WidgetCategorySlice.percentage]. No center text: the center amount is
 * drawn by a separate overlay TextView in the layout (crisper, localizable) per the Phase 3 spec.
 */
object WidgetDonutChartRenderer {

    fun renderDonut(
        widthPx: Int,
        heightPx: Int,
        slices: List<WidgetCategorySlice>,
        strokeWidthPx: Float,
        trackColor: Int = Color.parseColor("#E6EEEC")
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(widthPx.coerceAtLeast(1), heightPx.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val inset = strokeWidthPx / 2f + 2f
        val size = minOf(widthPx, heightPx) - inset * 2
        val left = (widthPx - size) / 2f
        val top = (heightPx - size) / 2f
        val bounds = RectF(left, top, left + size, top + size)

        val trackPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = strokeWidthPx
            color = trackColor
        }
        canvas.drawOval(bounds, trackPaint)

        if (slices.isEmpty()) return bitmap

        var startAngle = -90f
        val arcPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = strokeWidthPx
            strokeCap = Paint.Cap.BUTT
        }
        slices.forEach { slice ->
            val sweep = (slice.percentage.coerceIn(0f, 1f)) * 360f
            if (sweep <= 0f) return@forEach
            arcPaint.color = WidgetSparklineRenderer.parseColorOrDefault(slice.colorHex)
            // Small gap between slices for legibility.
            val gap = if (slices.size > 1) 1.5f else 0f
            canvas.drawArc(bounds, startAngle + gap, (sweep - gap).coerceAtLeast(0f), false, arcPaint)
            startAngle += sweep
        }
        return bitmap
    }
}
