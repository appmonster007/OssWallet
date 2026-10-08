package com.osscabinet.ossWallet.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.Preferences
import com.osscabinet.ossWallet.R
import kotlinx.serialization.json.Json

private fun parseBuckets(json: String?): List<WidgetTrendBucketRow> {
    if (json.isNullOrBlank()) return emptyList()
    return runCatching { Json.decodeFromString<List<WidgetTrendBucketRow>>(json) }.getOrDefault(emptyList())
}

private fun parseLegend(json: String?): List<WidgetTrendLegendRow> {
    if (json.isNullOrBlank()) return emptyList()
    return runCatching { Json.decodeFromString<List<WidgetTrendLegendRow>>(json) }.getOrDefault(emptyList())
}

private data class LegendCellIds(val container: Int, val dot: Int, val name: Int, val tag: Int)

/**
 * Category Spending Trend widget — 4x3 tier. Stacked bar bitmap (WidgetStackedBarChartRenderer)
 * on top, 2-column legend grid below (color swatch + name + MUST/NEED/WANT tag). Tap-inert.
 */
object CategorySpendingTrendRemoteViewsRenderer {

    fun render(context: Context, prefs: Preferences, widgetId: Int, appWidgetManager: AppWidgetManager) {
        val isLoaded = prefs[CategorySpendingTrendWidgetKeys.IS_LOADED] == "true"
        if (!isLoaded) {
            appWidgetManager.updateAppWidget(widgetId, RemoteViews(context.packageName, R.layout.widget_category_spending_trend_loading))
            return
        }
        val buckets = parseBuckets(prefs[CategorySpendingTrendWidgetKeys.BUCKETS_JSON])
        val views = if (buckets.isEmpty()) renderEmpty(context) else renderChart(context, prefs, buckets, widgetId, appWidgetManager)
        appWidgetManager.updateAppWidget(widgetId, views)
    }

    private fun renderEmpty(context: Context): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_category_spending_trend_empty)
        views.setTextViewText(R.id.tv_cst_empty_subtitle, "Category spending over time shows up here.")
        return views
    }

    private fun renderChart(
        context: Context,
        prefs: Preferences,
        buckets: List<WidgetTrendBucketRow>,
        widgetId: Int,
        appWidgetManager: AppWidgetManager
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_category_spending_trend_chart)
        val count = prefs[CategorySpendingTrendWidgetKeys.CATEGORY_COUNT] ?: "0"
        views.setTextViewText(R.id.tv_cst_header, "Category trend · ${prefs[CategorySpendingTrendWidgetKeys.PERIOD_LABEL] ?: ""}")
        views.setTextViewText(R.id.tv_cst_count, "$count categories")

        val options = appWidgetManager.getAppWidgetOptions(widgetId)
        val minWidthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250)
        val density = context.resources.displayMetrics.density
        val widthPx = (minWidthDp * density).toInt().coerceAtLeast(1)
        val heightPx = (90 * density).toInt()

        val stackedBuckets = buckets.map { b -> WidgetStackedBucket(b.label, b.segments.map { WidgetStackedSegment(it.colorHex, it.amount) }) }
        val textMuted = ContextCompat.getColor(context, R.color.widget_text_muted)
        val bitmap = WidgetStackedBarChartRenderer.renderStackedBars(widthPx, heightPx, stackedBuckets, textMuted, density)
        views.setImageViewBitmap(R.id.iv_cst_chart, bitmap)

        val legend = parseLegend(prefs[CategorySpendingTrendWidgetKeys.LEGEND_JSON]).take(8)
        val cells = listOf(
            LegendCellIds(R.id.ll_cst_legend1, R.id.v_cst_legend1_dot, R.id.tv_cst_legend1_name, R.id.tv_cst_legend1_tag),
            LegendCellIds(R.id.ll_cst_legend2, R.id.v_cst_legend2_dot, R.id.tv_cst_legend2_name, R.id.tv_cst_legend2_tag),
            LegendCellIds(R.id.ll_cst_legend3, R.id.v_cst_legend3_dot, R.id.tv_cst_legend3_name, R.id.tv_cst_legend3_tag),
            LegendCellIds(R.id.ll_cst_legend4, R.id.v_cst_legend4_dot, R.id.tv_cst_legend4_name, R.id.tv_cst_legend4_tag),
            LegendCellIds(R.id.ll_cst_legend5, R.id.v_cst_legend5_dot, R.id.tv_cst_legend5_name, R.id.tv_cst_legend5_tag),
            LegendCellIds(R.id.ll_cst_legend6, R.id.v_cst_legend6_dot, R.id.tv_cst_legend6_name, R.id.tv_cst_legend6_tag),
            LegendCellIds(R.id.ll_cst_legend7, R.id.v_cst_legend7_dot, R.id.tv_cst_legend7_name, R.id.tv_cst_legend7_tag),
            LegendCellIds(R.id.ll_cst_legend8, R.id.v_cst_legend8_dot, R.id.tv_cst_legend8_name, R.id.tv_cst_legend8_tag)
        )
        cells.forEachIndexed { index, ids ->
            val row = legend.getOrNull(index)
            if (row == null) {
                views.setViewVisibility(ids.container, View.GONE)
                return@forEachIndexed
            }
            views.setViewVisibility(ids.container, View.VISIBLE)
            views.setInt(ids.dot, "setColorFilter", WidgetSparklineRenderer.parseColorOrDefault(row.colorHex))
            views.setTextViewText(ids.name, row.name)
            views.setTextViewText(ids.tag, row.natureTag.uppercase())
        }
        return views
    }
}
