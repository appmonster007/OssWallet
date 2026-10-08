package com.osscabinet.ossWallet.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

/** One UpSet tag row (top 3 by total amount, per the Phase 3 spec cap). */
data class WidgetUpSetTagRow(val name: String, val colorHex: String)

/** One UpSet intersection column (top 6 by amount, per the Phase 3 spec cap). */
data class WidgetUpSetColumn(val amount: Double, val memberTagIndices: Set<Int>)

/**
 * Simplified UpSet plot bitmap: a bar chart on top (one bar per intersection, height ∝ amount)
 * with a dot-matrix membership grid below it (one column per intersection, one row per tag;
 * filled dot = tag is a member of that intersection), connected by a vertical line between the
 * topmost and bottommost filled dot in each column — the standard UpSet visual language.
 *
 * Simplifications explicitly taken per the Phase 3 spec (this chart was called out as the most
 * intricate one, with simplification pre-approved): capped to top 3 tags / top 6 intersections
 * (enforced by the caller before this is invoked), and the connecting line is a plain straight
 * line rather than a rendered "whisker" — no further reduction (e.g. dropping the line entirely)
 * was needed since both axes already fit comfortably at a 4x3 widget's bitmap size.
 */
object WidgetUpSetPlotRenderer {

    fun renderUpSet(
        widthPx: Int,
        heightPx: Int,
        tags: List<WidgetUpSetTagRow>,
        columns: List<WidgetUpSetColumn>,
        barColor: Int,
        gridLineColor: Int,
        density: Float
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(widthPx.coerceAtLeast(1), heightPx.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        if (tags.isEmpty() || columns.isEmpty()) return bitmap

        val barAreaHeight = heightPx * 0.55f
        val gridAreaTop = barAreaHeight + 4f * density
        val rowHeight = (heightPx - gridAreaTop) / tags.size.coerceAtLeast(1)
        val colWidth = widthPx.toFloat() / columns.size

        // Bars
        val maxAmount = columns.maxOf { it.amount }.takeIf { it > 0.0 } ?: 1.0
        val barPaint = Paint().apply { isAntiAlias = true; color = barColor; style = Paint.Style.FILL }
        val barGap = 3f * density
        columns.forEachIndexed { colIdx, col ->
            val barHeight = (col.amount / maxAmount * (barAreaHeight - 4f * density)).toFloat()
            val left = colWidth * colIdx + barGap / 2f
            val right = colWidth * (colIdx + 1) - barGap / 2f
            canvas.drawRect(left, barAreaHeight - barHeight, right, barAreaHeight, barPaint)
        }

        // Dot matrix
        val dotRadius = (minOf(rowHeight, colWidth) * 0.22f).coerceAtMost(5f * density)
        val filledPaint = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL }
        val emptyPaint = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL; color = gridLineColor }
        val linePaint = Paint().apply { isAntiAlias = true; style = Paint.Style.STROKE; strokeWidth = 2f * density }

        columns.forEachIndexed { colIdx, col ->
            val cx = colWidth * colIdx + colWidth / 2f
            val memberYs = mutableListOf<Float>()
            tags.forEachIndexed { rowIdx, tag ->
                val cy = gridAreaTop + rowHeight * rowIdx + rowHeight / 2f
                val isMember = rowIdx in col.memberTagIndices
                if (isMember) {
                    memberYs.add(cy)
                }
            }
            if (memberYs.size >= 2) {
                linePaint.color = WidgetSparklineRenderer.parseColorOrDefault(tags.getOrNull(0)?.colorHex, Color.DKGRAY)
                canvas.drawLine(cx, memberYs.min(), cx, memberYs.max(), linePaint)
            }
            tags.forEachIndexed { rowIdx, tag ->
                val cy = gridAreaTop + rowHeight * rowIdx + rowHeight / 2f
                val isMember = rowIdx in col.memberTagIndices
                if (isMember) {
                    filledPaint.color = WidgetSparklineRenderer.parseColorOrDefault(tag.colorHex)
                    canvas.drawCircle(cx, cy, dotRadius, filledPaint)
                } else {
                    canvas.drawCircle(cx, cy, dotRadius * 0.6f, emptyPaint)
                }
            }
        }
        return bitmap
    }
}
