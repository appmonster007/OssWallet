package com.osscabinet.ossWallet.appfunctions

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import androidx.appfunctions.AppFunctionUriGrant
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.io.File
import java.math.BigDecimal

/** One currency's income and spending over time, ready to draw. */
data class ChartSeries(val currency: String, val points: List<ChartPoint>) {
    data class ChartPoint(val label: String, val income: BigDecimal, val expense: BigDecimal)
}

/** Draws the trend as a small bar chart image the assistant can show, without leaving the device until it is shared. */
@Singleton
class AssistantChartRenderer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun render(trend: List<TrendPoint>, title: String): AppFunctionUriGrant? {
        val series = chartSeries(trend) ?: return null
        val file = writeChart(series, title)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return AppFunctionUriGrant(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    private fun writeChart(series: ChartSeries, title: String): File {
        val width = 1000
        val height = 600
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val ink = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(40, 40, 40); textSize = 30f }
        val muted = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(110, 110, 110); textSize = 22f }
        val incomePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(46, 125, 50) }
        val expensePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(198, 40, 40) }
        val axis = Paint().apply { color = Color.rgb(200, 200, 200); strokeWidth = 2f }

        canvas.drawText("Income and spending, $title (${series.currency})", 40f, 50f, ink)

        val left = 40f
        val right = width - 40f
        val top = 110f
        val bottom = height - 90f
        val peak = series.points.maxOf { maxOf(it.income, it.expense) }.toFloat().coerceAtLeast(0.01f)
        val slot = (right - left) / series.points.size
        val barWidth = (slot * 0.38f).coerceAtMost(48f)

        canvas.drawLine(left, bottom, right, bottom, axis)
        val labelEvery = ((series.points.size + 11) / 12).coerceAtLeast(1)
        series.points.forEachIndexed { i, p ->
            val cx = left + slot * i + slot / 2
            val incomeTop = bottom - (p.income.toFloat() / peak) * (bottom - top)
            val expenseTop = bottom - (p.expense.toFloat() / peak) * (bottom - top)
            canvas.drawRect(RectF(cx - barWidth - 2f, incomeTop, cx - 2f, bottom), incomePaint)
            canvas.drawRect(RectF(cx + 2f, expenseTop, cx + barWidth + 2f, bottom), expensePaint)
            if (i % labelEvery == 0) {
                val text = shortLabel(p.label)
                canvas.drawText(text, cx - muted.measureText(text) / 2, bottom + 32f, muted)
            }
        }

        canvas.drawRect(RectF(left, height - 44f, left + 22f, height - 22f), incomePaint)
        canvas.drawText("Income", left + 32f, height - 24f, muted)
        canvas.drawRect(RectF(left + 160f, height - 44f, left + 182f, height - 22f), expensePaint)
        canvas.drawText("Spending", left + 192f, height - 24f, muted)
        canvas.drawText("Tallest bar = ${series.currency} ${peak.toBigDecimal().setScale(0, java.math.RoundingMode.HALF_UP).toPlainString()}", right - 300f, height - 24f, muted)

        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        // Charts are financial detail; don't let old ones pile up in the cache.
        val cutoff = System.currentTimeMillis() - MAX_AGE_MS
        dir.listFiles()?.filter { it.name.startsWith(PREFIX) && it.lastModified() < cutoff }?.forEach { it.delete() }
        val file = File(dir, "$PREFIX${System.currentTimeMillis()}.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        return file
    }

    companion object {
        private const val PREFIX = "assistant_chart_"
        private const val MAX_AGE_MS = 60L * 60 * 1000

        /**
         * The series worth drawing: only the currency with the most activity (bars of different currencies can't share
         * an axis), or null when there is nothing to draw.
         */
        fun chartSeries(trend: List<TrendPoint>): ChartSeries? {
            if (trend.isEmpty()) return null
            val busiest = trend.groupBy { it.currency }
                .maxByOrNull { (_, points) -> points.sumOf { (it.income.toBigDecimalOrNull() ?: BigDecimal.ZERO) + (it.expense.toBigDecimalOrNull() ?: BigDecimal.ZERO) } }
                ?: return null
            val points = busiest.value.sortedBy { it.label }.map {
                ChartSeries.ChartPoint(it.label, it.income.toBigDecimalOrNull() ?: BigDecimal.ZERO, it.expense.toBigDecimalOrNull() ?: BigDecimal.ZERO)
            }
            if (points.all { it.income.signum() == 0 && it.expense.signum() == 0 }) return null
            return ChartSeries(busiest.key, points)
        }

        /** "2026-10" -> "10/26", "2026-10-05" -> "10-05", "2026" -> "2026". */
        fun shortLabel(label: String): String = when (label.count { it == '-' }) {
            1 -> label.substring(5) + "/" + label.substring(2, 4)
            2 -> label.substring(5)
            else -> label
        }
    }
}
