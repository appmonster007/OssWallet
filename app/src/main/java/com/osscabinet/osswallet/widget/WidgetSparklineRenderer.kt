package com.osscabinet.ossWallet.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF

/**
 * Minimal bitmap-drawn line sparkline for RemoteViews, which has no native chart primitive and
 * cannot render Canvas-based Compose charts (see CLAUDE.md Glance/RemoteViews chart note). Used
 * today only by the Accounts widget's per-account balance-history sparkline
 * ([CardRenderData.Accounts] -> `AccountSummaryItem.balanceHistory`). Deliberately minimal — just
 * a stroked polyline, no axes/grid/fill — later chart-widget phases (bar/pie/treemap/etc.) should
 * get their own dedicated renderer rather than this being grown into a general chart library.
 */
object WidgetSparklineRenderer {

    /**
     * Renders [points] (already-normalized or raw Double values — only relative shape matters,
     * this function normalizes internally) as a single stroked polyline, no fill, no axes.
     *
     * @param widthPx / [heightPx] target bitmap size in pixels (caller converts dp -> px using
     *   its own [density], matching the `WidgetChartRenderer` convention this mirrors).
     * @param strokeColor ARGB color int, typically the account's own `colorHex` parsed via
     *   [Color.parseColor].
     */
    fun renderLineSparkline(
        widthPx: Int,
        heightPx: Int,
        points: List<Double>,
        strokeColor: Int,
        strokeWidthPx: Float
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(
            widthPx.coerceAtLeast(1),
            heightPx.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)

        if (points.size < 2) {
            return bitmap
        }

        val min = points.min()
        val max = points.max()
        val range = (max - min).takeIf { it > 0.0 } ?: 1.0

        val paint = Paint().apply {
            color = strokeColor
            style = Paint.Style.STROKE
            strokeWidth = strokeWidthPx
            isAntiAlias = true
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        // Inset by half the stroke width so the line doesn't clip at the bitmap edges.
        val inset = strokeWidthPx / 2f
        val bounds = RectF(inset, inset, widthPx - inset, heightPx - inset)
        val stepX = if (points.size > 1) bounds.width() / (points.size - 1) else 0f

        var prevX = bounds.left
        var prevY = bounds.bottom - ((points[0] - min) / range * bounds.height()).toFloat()

        for (i in 1 until points.size) {
            val x = bounds.left + stepX * i
            val y = bounds.bottom - ((points[i] - min) / range * bounds.height()).toFloat()
            canvas.drawLine(prevX, prevY, x, y, paint)
            prevX = x
            prevY = y
        }

        return bitmap
    }

    /** Parses a `colorHex` string (e.g. "#1F6F66"), falling back to [fallback] on any failure. */
    fun parseColorOrDefault(colorHex: String?, fallback: Int = Color.parseColor("#1F6F66")): Int =
        runCatching { Color.parseColor(colorHex) }.getOrDefault(fallback)
}
