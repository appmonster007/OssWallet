package com.osscabinet.ossWallet.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.widget.RemoteViews
import androidx.datastore.preferences.core.Preferences
import com.osscabinet.ossWallet.R

/**
 * Category Treemap widget — 4x2 tier, same underlying flattened slice data as the Category Pie
 * Chart widget (via [CategoryChartDataMapper]) but rendered via [WidgetTreemapRenderer]'s
 * slice-and-dice layout instead of a donut. Tap-inert.
 */
object CategoryTreemapRemoteViewsRenderer {

    fun render(context: Context, prefs: Preferences, widgetId: Int, appWidgetManager: AppWidgetManager) {
        val isLoaded = prefs[CategoryTreemapWidgetKeys.IS_LOADED] == "true"
        if (!isLoaded) {
            appWidgetManager.updateAppWidget(widgetId, RemoteViews(context.packageName, R.layout.widget_category_treemap_loading))
            return
        }
        val slices = CategoryChartDataMapper.decode(prefs[CategoryTreemapWidgetKeys.SLICES_JSON])
        val views = if (slices.isEmpty()) renderEmpty(context) else renderChart(context, prefs, slices, widgetId, appWidgetManager)
        appWidgetManager.updateAppWidget(widgetId, views)
    }

    private fun renderEmpty(context: Context): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_category_treemap_empty)
        views.setTextViewText(R.id.tv_ctm_empty_subtitle, "Spending by category shows up here.")
        return views
    }

    private fun renderChart(
        context: Context,
        prefs: Preferences,
        slices: List<WidgetCategorySlice>,
        widgetId: Int,
        appWidgetManager: AppWidgetManager
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_category_treemap_chart)
        val total = prefs[CategoryTreemapWidgetKeys.EXPENSE_TOTAL_FORMATTED] ?: ""
        views.setTextViewText(R.id.tv_ctm_header, "Spending · ${prefs[CategoryTreemapWidgetKeys.PERIOD_LABEL] ?: ""} · $total")

        val options = appWidgetManager.getAppWidgetOptions(widgetId)
        val minWidthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250)
        val minHeightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 140)
        val density = context.resources.displayMetrics.density
        val widthPx = (minWidthDp * density).toInt().coerceAtLeast(1)
        val heightPx = ((minHeightDp - 24) * density).toInt().coerceAtLeast((60 * density).toInt())

        val bitmap = WidgetTreemapRenderer.renderTreemap(widthPx, heightPx, slices.take(12), density)
        views.setImageViewBitmap(R.id.iv_ctm_treemap, bitmap)
        return views
    }
}
