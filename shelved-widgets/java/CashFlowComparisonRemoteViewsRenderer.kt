package com.osscabinet.ossWallet.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.Preferences
import com.osscabinet.ossWallet.R
import kotlinx.serialization.json.Json

private fun parseSeries(json: String?): WidgetCashFlowSeriesDto {
    if (json.isNullOrBlank()) return WidgetCashFlowSeriesDto(emptyList())
    return runCatching { Json.decodeFromString<WidgetCashFlowSeriesDto>(json) }.getOrDefault(WidgetCashFlowSeriesDto(emptyList()))
}

/**
 * Cash Flow Comparison widget — 4x2 tier. Three cumulative net-flow lines (current/previous/
 * lastYear) distinguished by line style, a single dashed "Today" marker, and a 3-item line-style
 * legend. Tap-inert.
 */
object CashFlowComparisonRemoteViewsRenderer {

    fun render(context: Context, prefs: Preferences, widgetId: Int, appWidgetManager: AppWidgetManager) {
        val isLoaded = prefs[CashFlowComparisonWidgetKeys.IS_LOADED] == "true"
        if (!isLoaded) {
            appWidgetManager.updateAppWidget(widgetId, RemoteViews(context.packageName, R.layout.widget_cashflow_comparison_loading))
            return
        }
        val current = parseSeries(prefs[CashFlowComparisonWidgetKeys.CURRENT_SERIES_JSON])
        val views = if (current.points.isEmpty()) renderEmpty(context) else renderChart(context, prefs, widgetId, appWidgetManager)
        appWidgetManager.updateAppWidget(widgetId, views)
    }

    private fun renderEmpty(context: Context): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_cashflow_comparison_empty)
        views.setTextViewText(R.id.tv_cfc_empty_subtitle, "Net flow trends show up here.")
        return views
    }

    private fun renderChart(
        context: Context,
        prefs: Preferences,
        widgetId: Int,
        appWidgetManager: AppWidgetManager
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_cashflow_comparison_chart)
        views.setTextViewText(R.id.tv_cfc_header, "Net flow · cumulative")
        views.setTextViewText(R.id.tv_cfc_total, prefs[CashFlowComparisonWidgetKeys.CURRENT_TOTAL_FORMATTED] ?: "")

        val current = parseSeries(prefs[CashFlowComparisonWidgetKeys.CURRENT_SERIES_JSON])
        val previous = parseSeries(prefs[CashFlowComparisonWidgetKeys.PREVIOUS_SERIES_JSON])
        val lastYear = parseSeries(prefs[CashFlowComparisonWidgetKeys.LAST_YEAR_SERIES_JSON])
        val totalBuckets = (prefs[CashFlowComparisonWidgetKeys.CURRENT_TOTAL_BUCKETS]?.toIntOrNull() ?: 0)
            .coerceAtLeast(current.points.size)

        val options = appWidgetManager.getAppWidgetOptions(widgetId)
        val minWidthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250)
        val minHeightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 140)
        val density = context.resources.displayMetrics.density
        val widthPx = (minWidthDp * density).toInt().coerceAtLeast(1)
        val heightPx = ((minHeightDp - 56) * density).toInt().coerceAtLeast((60 * density).toInt())

        val accentColor = ContextCompat.getColor(context, R.color.widget_accent_teal)
        val grayColor = ContextCompat.getColor(context, R.color.widget_series_gray)
        val mutedColor = ContextCompat.getColor(context, R.color.widget_text_muted)

        val series = buildList {
            if (current.points.isNotEmpty()) add(WidgetLineSeries(current.points, accentColor, WidgetLineStyle.SOLID_THICK))
            if (previous.points.isNotEmpty()) add(WidgetLineSeries(previous.points, grayColor, WidgetLineStyle.DASHED_MEDIUM))
            if (lastYear.points.isNotEmpty()) add(WidgetLineSeries(lastYear.points, mutedColor, WidgetLineStyle.DOTTED_THIN))
        }

        val bitmap = WidgetLineAreaChartRenderer.renderMultiLine(
            widthPx, heightPx, series, totalBuckets.coerceAtLeast(1), current.points.size, mutedColor, density
        )
        views.setImageViewBitmap(R.id.iv_cfc_chart, bitmap)
        return views
    }
}
