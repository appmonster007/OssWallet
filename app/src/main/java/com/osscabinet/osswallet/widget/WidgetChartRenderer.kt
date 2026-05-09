package com.osscabinet.ossWallet.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path

object WidgetChartRenderer {
    data class ProgressBarItem(
        val label: String,
        val progress: Float, // 0f to 1f
        val filledColor: Int,
        val trackColor: Int,
        val labelColor: Int
    )

    fun renderProgressBars(
        widthPx: Int,
        heightPx: Int,
        items: List<ProgressBarItem>,
        density: Float
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        if (items.isEmpty()) return bitmap

        val itemHeight = heightPx / items.size.toFloat()
        val barHeight = 8 * density
        
        items.forEachIndexed { index, item ->
            val centerY = index * itemHeight + itemHeight / 2
            val top = centerY - barHeight / 2
            val bottom = centerY + barHeight / 2
            
            // Draw track
            paint.color = item.trackColor
            paint.style = Paint.Style.FILL
            canvas.drawRoundRect(0f, top, widthPx.toFloat(), bottom, barHeight / 2, barHeight / 2, paint)
            
            // Draw progress
            paint.color = item.filledColor
            canvas.drawRoundRect(0f, top, widthPx * item.progress.coerceIn(0f, 1f), bottom, barHeight / 2, barHeight / 2, paint)
        }
        
        return bitmap
    }

    fun renderLineChart(
        widthPx: Int,
        heightPx: Int,
        points: List<Float>,
        lineColor: Int,
        fillColor: Int,
        density: Float
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        
        if (points.size < 2) return bitmap

        val normalized = normalize(points)
        val stepX = widthPx / (points.size - 1).toFloat()
        
        val path = Path()
        normalized.forEachIndexed { index, value ->
            val x = index * stepX
            val y = heightPx * (1f - value)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        // Fill
        val fillPath = Path(path)
        fillPath.lineTo(widthPx.toFloat(), heightPx.toFloat())
        fillPath.lineTo(0f, heightPx.toFloat())
        fillPath.close()
        
        paint.style = Paint.Style.FILL
        paint.color = fillColor
        canvas.drawPath(fillPath, paint)

        // Stroke
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2 * density
        paint.color = lineColor
        canvas.drawPath(path, paint)

        return bitmap
    }

    fun normalize(values: List<Float>): List<Float> {
        val min = values.minOrNull() ?: 0f
        val max = values.maxOrNull() ?: 1f
        val range = max - min
        if (range == 0f) return values.map { 0.5f }
        return values.map { (it - min) / range }
    }
}
