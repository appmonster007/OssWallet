package com.osscabinet.ossWallet.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path

/** One Cash Flow Comparison line series — cumulative net-flow values, plain doubles for drawing. */
data class WidgetLineSeries(val points: List<Double>, val color: Int, val style: WidgetLineStyle)

enum class WidgetLineStyle { SOLID_THICK, DASHED_MEDIUM, DOTTED_THIN }

/**
 * Multi-line chart for the Cash Flow Comparison widget — three cumulative net-flow series
 * (current/previous/lastYear), distinguished by line STYLE (not just color) per the Phase 3
 * spec's accessibility note. Draws a single dashed "Today" marker at the current series' last
 * real point when it ends before the period's full bucket count — the spec explicitly allows
 * simplifying to one marker instead of two when a precise pair proves fiddly, which this takes.
 */
object WidgetLineAreaChartRenderer {

    fun renderMultiLine(
        widthPx: Int,
        heightPx: Int,
        series: List<WidgetLineSeries>,
        totalBuckets: Int,
        currentSeriesLength: Int,
        markerColor: Int,
        density: Float
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(widthPx.coerceAtLeast(1), heightPx.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val allValues = series.flatMap { it.points }
        if (allValues.isEmpty() || totalBuckets <= 1) return bitmap

        val minV = allValues.min()
        val maxV = allValues.max()
        val range = (maxV - minV).takeIf { it > 0.0 } ?: 1.0
        val inset = 4f * density
        val top = inset
        val bottom = heightPx - inset
        val stepX = widthPx.toFloat() / (totalBuckets - 1).coerceAtLeast(1)

        fun yFor(v: Double): Float = (bottom - ((v - minV) / range * (bottom - top))).toFloat()

        series.forEach { s ->
            if (s.points.size < 2) return@forEach
            val paint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.STROKE
                color = s.color
                strokeCap = Paint.Cap.ROUND
                when (s.style) {
                    WidgetLineStyle.SOLID_THICK -> strokeWidth = 3f * density
                    WidgetLineStyle.DASHED_MEDIUM -> {
                        strokeWidth = 2f * density
                        pathEffect = DashPathEffect(floatArrayOf(8f * density, 5f * density), 0f)
                    }
                    WidgetLineStyle.DOTTED_THIN -> {
                        strokeWidth = 1.5f * density
                        pathEffect = DashPathEffect(floatArrayOf(2f * density, 4f * density), 0f)
                    }
                }
            }
            val path = Path()
            s.points.forEachIndexed { i, v ->
                val x = stepX * i
                val y = yFor(v)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            canvas.drawPath(path, paint)

            if (s.style == WidgetLineStyle.SOLID_THICK) {
                val lastX = stepX * (s.points.size - 1)
                val lastY = yFor(s.points.last())
                val dotPaint = Paint().apply { isAntiAlias = true; color = s.color; style = Paint.Style.FILL }
                canvas.drawCircle(lastX, lastY, 3.5f * density, dotPaint)
            }
        }

        // Single "Today" marker at the current series' last real point, only if it ends before
        // the full period (otherwise the period is already complete and a marker is redundant).
        if (currentSeriesLength in 1 until totalBuckets) {
            val x = stepX * (currentSeriesLength - 1)
            val markerPaint = Paint().apply {
                isAntiAlias = true
                color = markerColor
                style = Paint.Style.STROKE
                strokeWidth = 1.5f * density
                pathEffect = DashPathEffect(floatArrayOf(4f * density, 3f * density), 0f)
            }
            canvas.drawLine(x, 0f, x, heightPx.toFloat(), markerPaint)
        }

        return bitmap
    }

    /**
     * Filled-area + stroked line + end-point dot sparkline, used by the Account Hero widget's
     * balance-history mini-chart (fill at ~12% opacity per the Phase 3 spec). Sibling to
     * [WidgetSparklineRenderer.renderLineSparkline] — kept here rather than growing that file
     * since this variant draws a fill path too, not just a stroked polyline.
     */
    fun renderAreaSparkline(
        widthPx: Int,
        heightPx: Int,
        points: List<Double>,
        strokeColor: Int,
        strokeWidthPx: Float
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(widthPx.coerceAtLeast(1), heightPx.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        if (points.size < 2) return bitmap

        val min = points.min()
        val max = points.max()
        val range = (max - min).takeIf { it > 0.0 } ?: 1.0
        val inset = strokeWidthPx / 2f
        val left = inset
        val right = widthPx - inset
        val top = inset
        val bottom = heightPx - inset
        val stepX = (right - left) / (points.size - 1)

        fun yFor(v: Double): Float = bottom - ((v - min) / range * (bottom - top)).toFloat()

        val linePath = Path()
        val fillPath = Path()
        points.forEachIndexed { i, v ->
            val x = left + stepX * i
            val y = yFor(v)
            if (i == 0) {
                linePath.moveTo(x, y)
                fillPath.moveTo(x, bottom)
                fillPath.lineTo(x, y)
            } else {
                linePath.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }
        fillPath.lineTo(left + stepX * (points.size - 1), bottom)
        fillPath.close()

        val fillPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            color = strokeColor
            alpha = 31 // ~12%
        }
        canvas.drawPath(fillPath, fillPaint)

        val linePaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = strokeWidthPx
            color = strokeColor
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        canvas.drawPath(linePath, linePaint)

        val dotPaint = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL; color = strokeColor }
        canvas.drawCircle(left + stepX * (points.size - 1), yFor(points.last()), strokeWidthPx, dotPaint)

        return bitmap
    }
}
