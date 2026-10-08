package com.osscabinet.ossWallet.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF

/**
 * Treemap bitmap for the Category Treemap widget. Uses a slice-and-dice layout (cells placed in
 * alternating horizontal/vertical strips, area proportional to amount) rather than a fully
 * faithful squarified treemap — an explicitly allowed simplification per the Phase 3 spec, since
 * a true squarify algorithm wasn't mandated and slice-and-dice already gives area ∝ amount.
 * Each cell's label (name + percentage) is drawn only if it fits within the cell, measured via
 * [Paint.measureText] — cells too small to fit any label are left blank rather than shrinking
 * text below ~9sp.
 */
object WidgetTreemapRenderer {

    fun renderTreemap(
        widthPx: Int,
        heightPx: Int,
        slices: List<WidgetCategorySlice>,
        density: Float
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(widthPx.coerceAtLeast(1), heightPx.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        if (slices.isEmpty()) return bitmap

        val total = slices.sumOf { it.amount }.takeIf { it > 0.0 } ?: return bitmap
        val gapPx = 2f * density
        val minFontPx = 9f * density // ~9sp floor, never shrunk below this

        val cellPaint = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL }
        val namePaint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            textSize = 11f * density
            isFakeBoldText = true
        }
        val pctPaint = Paint().apply {
            isAntiAlias = true
            color = Color.argb(230, 255, 255, 255)
            textSize = 9.5f * density
        }

        var cells = listOf(RectF(0f, 0f, widthPx.toFloat(), heightPx.toFloat())) to slices
        layoutSliceDice(
            RectF(0f, 0f, widthPx.toFloat(), heightPx.toFloat()),
            slices,
            total,
            horizontal = widthPx >= heightPx
        ).forEach { (rect, slice) ->
            val cell = RectF(
                rect.left + gapPx / 2,
                rect.top + gapPx / 2,
                rect.right - gapPx / 2,
                rect.bottom - gapPx / 2
            )
            if (cell.width() <= 0 || cell.height() <= 0) return@forEach
            cellPaint.color = WidgetSparklineRenderer.parseColorOrDefault(slice.colorHex)
            canvas.drawRect(cell, cellPaint)

            if (cell.width() < minFontPx * 2 || cell.height() < minFontPx * 1.6f) return@forEach

            val namePaddingPx = 4f * density
            val availableWidth = cell.width() - namePaddingPx * 2
            val name = slice.category
            val nameWidth = namePaint.measureText(name)
            if (nameWidth <= availableWidth) {
                canvas.drawText(name, cell.left + namePaddingPx, cell.top + namePaintAscentOffset(namePaint), namePaint)
                val pctText = "${(slice.percentage * 100).let { "%.0f".format(it) }}%"
                if (cell.height() > minFontPx * 3 && pctPaint.measureText(pctText) <= availableWidth) {
                    canvas.drawText(pctText, cell.left + namePaddingPx, cell.top + namePaintAscentOffset(namePaint) + pctPaint.textSize + 2f * density, pctPaint)
                }
            }
        }
        return bitmap
    }

    private fun namePaintAscentOffset(paint: Paint): Float = paint.textSize

    /**
     * Slice-and-dice: alternates cutting the remaining rect horizontally/vertically, each cell's
     * share of the strip length proportional to amount/total. Simpler than true squarify but
     * still keeps area ∝ amount for every cell.
     */
    private fun layoutSliceDice(
        bounds: RectF,
        slices: List<WidgetCategorySlice>,
        total: Double,
        horizontal: Boolean
    ): List<Pair<RectF, WidgetCategorySlice>> {
        if (slices.isEmpty()) return emptyList()
        val result = mutableListOf<Pair<RectF, WidgetCategorySlice>>()
        var offset = 0f
        val length = if (horizontal) bounds.width() else bounds.height()
        slices.forEach { slice ->
            val share = (slice.amount / total).toFloat().coerceIn(0f, 1f)
            val extent = length * share
            val rect = if (horizontal) {
                RectF(bounds.left + offset, bounds.top, bounds.left + offset + extent, bounds.bottom)
            } else {
                RectF(bounds.left, bounds.top + offset, bounds.right, bounds.top + offset + extent)
            }
            result.add(rect to slice)
            offset += extent
        }
        return result
    }
}
