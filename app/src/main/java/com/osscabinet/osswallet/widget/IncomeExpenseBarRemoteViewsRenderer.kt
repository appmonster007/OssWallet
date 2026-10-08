package com.osscabinet.ossWallet.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.graphics.Color
import android.widget.RemoteViews
import androidx.datastore.preferences.core.Preferences
import androidx.core.content.ContextCompat
import com.osscabinet.ossWallet.R
import kotlinx.serialization.json.Json

private fun parseBuckets(json: String?): List<WidgetIncomeExpenseBucketRow> {
    if (json.isNullOrBlank()) return emptyList()
    return runCatching { Json.decodeFromString<List<WidgetIncomeExpenseBucketRow>>(json) }.getOrDefault(emptyList())
}

/**
 * Income vs Expenses widget — 4x2 tier. Grouped bar bitmap (WidgetBarChartRenderer) with a
 * net-flow trend line truncated at today, faded future buckets, and a legend/footer caption.
 * Tap-inert.
 */
object IncomeExpenseBarRemoteViewsRenderer {

    fun render(context: Context, prefs: Preferences, widgetId: Int, appWidgetManager: AppWidgetManager) {
        val isLoaded = prefs[IncomeExpenseBarWidgetKeys.IS_LOADED] == "true"
        if (!isLoaded) {
            appWidgetManager.updateAppWidget(widgetId, RemoteViews(context.packageName, R.layout.widget_income_expense_bar_loading))
            return
        }
        val buckets = parseBuckets(prefs[IncomeExpenseBarWidgetKeys.BUCKETS_JSON])
        val views = if (buckets.isEmpty()) renderEmpty(context) else renderChart(context, prefs, buckets, widgetId, appWidgetManager)
        appWidgetManager.updateAppWidget(widgetId, views)
    }

    private fun renderEmpty(context: Context): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_income_expense_bar_empty)
        views.setTextViewText(R.id.tv_ieb_empty_subtitle, "Income and expenses show up here.")
        return views
    }

    private fun renderChart(
        context: Context,
        prefs: Preferences,
        buckets: List<WidgetIncomeExpenseBucketRow>,
        widgetId: Int,
        appWidgetManager: AppWidgetManager
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_income_expense_bar_chart)
        views.setTextViewText(R.id.tv_ieb_header, "Income vs expenses · ${prefs[IncomeExpenseBarWidgetKeys.PERIOD_LABEL] ?: ""}")
        views.setTextViewText(R.id.tv_ieb_caption, "Net so far ${prefs[IncomeExpenseBarWidgetKeys.NET_SO_FAR_FORMATTED] ?: ""}")

        val options = appWidgetManager.getAppWidgetOptions(widgetId)
        val minWidthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250)
        val minHeightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 140)
        val density = context.resources.displayMetrics.density
        val widthPx = (minWidthDp * density).toInt().coerceAtLeast(1)
        val heightPx = ((minHeightDp - 56) * density).toInt().coerceAtLeast((60 * density).toInt())

        val todayIdx = prefs[IncomeExpenseBarWidgetKeys.TODAY_BUCKET_INDEX]?.toIntOrNull() ?: -1
        val barBuckets = buckets.map { WidgetBarBucket(it.label, it.income, it.expense, it.net, it.isFuture) }

        val incomeColor = ContextCompat.getColor(context, R.color.widget_accent_teal)
        val expenseColor = Color.parseColor("#D49A55")
        val textMuted = ContextCompat.getColor(context, R.color.widget_text_muted)
        val netLineColor = ContextCompat.getColor(context, R.color.widget_text_primary)

        val bitmap = WidgetBarChartRenderer.renderGroupedBars(
            widthPx, heightPx, barBuckets, todayIdx, incomeColor, expenseColor, netLineColor, textMuted, density
        )
        views.setImageViewBitmap(R.id.iv_ieb_chart, bitmap)
        return views
    }
}
