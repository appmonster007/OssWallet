package com.osscabinet.ossWallet.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.Preferences
import com.osscabinet.ossWallet.R
import kotlinx.serialization.json.Json

private fun parseTags(json: String?): List<WidgetUpSetTagDto> {
    if (json.isNullOrBlank()) return emptyList()
    return runCatching { Json.decodeFromString<List<WidgetUpSetTagDto>>(json) }.getOrDefault(emptyList())
}

private fun parseIntersections(json: String?): List<WidgetUpSetIntersectionDto> {
    if (json.isNullOrBlank()) return emptyList()
    return runCatching { Json.decodeFromString<List<WidgetUpSetIntersectionDto>>(json) }.getOrDefault(emptyList())
}

private data class TagLegendRowIds(val container: Int, val dot: Int, val name: Int, val amount: Int)

/**
 * Tag UpSet Plot widget — 4x3 tier. Bar chart (one bar per intersection) + dot-matrix membership
 * grid below it (WidgetUpSetPlotRenderer), plus a left-side legend naming each tag row. Capped
 * to top 3 tags / top 6 intersections per the Phase 3 spec's pre-approved simplification.
 * Handles 0/1-tag and empty-intersection edge cases via the empty state. Tap-inert.
 */
object TagUpSetPlotRemoteViewsRenderer {

    fun render(context: Context, prefs: Preferences, widgetId: Int, appWidgetManager: AppWidgetManager) {
        val isLoaded = prefs[TagUpSetPlotWidgetKeys.IS_LOADED] == "true"
        if (!isLoaded) {
            appWidgetManager.updateAppWidget(widgetId, RemoteViews(context.packageName, R.layout.widget_tag_upset_loading))
            return
        }
        val tags = parseTags(prefs[TagUpSetPlotWidgetKeys.TAGS_JSON])
        val intersections = parseIntersections(prefs[TagUpSetPlotWidgetKeys.INTERSECTIONS_JSON])
        val views = if (tags.isEmpty() || intersections.isEmpty()) {
            renderEmpty(context)
        } else {
            renderChart(context, prefs, tags, intersections, widgetId, appWidgetManager)
        }
        appWidgetManager.updateAppWidget(widgetId, views)
    }

    private fun renderEmpty(context: Context): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_tag_upset_empty)
        views.setTextViewText(R.id.tv_tup_empty_subtitle, "Tag combinations show up here once you tag a few transactions.")
        return views
    }

    private fun renderChart(
        context: Context,
        prefs: Preferences,
        tags: List<WidgetUpSetTagDto>,
        intersections: List<WidgetUpSetIntersectionDto>,
        widgetId: Int,
        appWidgetManager: AppWidgetManager
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_tag_upset_chart)
        views.setTextViewText(R.id.tv_tup_header, "Tags · ${prefs[TagUpSetPlotWidgetKeys.PERIOD_LABEL] ?: ""}")
        val total = prefs[TagUpSetPlotWidgetKeys.TOTAL_FORMATTED] ?: ""
        val count = prefs[TagUpSetPlotWidgetKeys.TX_COUNT] ?: "0"
        views.setTextViewText(R.id.tv_tup_subheader, "$total · $count tx")

        val options = appWidgetManager.getAppWidgetOptions(widgetId)
        val minWidthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250)
        val density = context.resources.displayMetrics.density
        val chartWidthPx = ((minWidthDp - 70) * density).toInt().coerceAtLeast((80 * density).toInt())
        val chartHeightPx = (150 * density).toInt()

        val tagRows = tags.map { WidgetUpSetTagRow(it.name, it.colorHex) }
        val columns = intersections.map { WidgetUpSetColumn(it.amount, it.tagIndices.toSet()) }
        val barColor = ContextCompat.getColor(context, R.color.widget_accent_teal)
        val gridLineColor = ContextCompat.getColor(context, R.color.widget_divider_soft)

        val bitmap = WidgetUpSetPlotRenderer.renderUpSet(chartWidthPx, chartHeightPx, tagRows, columns, barColor, gridLineColor, density)
        views.setImageViewBitmap(R.id.iv_tup_chart, bitmap)

        val legendRows = listOf(
            TagLegendRowIds(R.id.ll_tup_legend1, R.id.v_tup_legend1_dot, R.id.tv_tup_legend1_name, R.id.tv_tup_legend1_amount),
            TagLegendRowIds(R.id.ll_tup_legend2, R.id.v_tup_legend2_dot, R.id.tv_tup_legend2_name, R.id.tv_tup_legend2_amount),
            TagLegendRowIds(R.id.ll_tup_legend3, R.id.v_tup_legend3_dot, R.id.tv_tup_legend3_name, R.id.tv_tup_legend3_amount)
        )
        legendRows.forEachIndexed { index, ids ->
            val tag = tags.getOrNull(index)
            if (tag == null) {
                views.setViewVisibility(ids.container, View.GONE)
                return@forEachIndexed
            }
            views.setViewVisibility(ids.container, View.VISIBLE)
            views.setInt(ids.dot, "setColorFilter", WidgetSparklineRenderer.parseColorOrDefault(tag.colorHex))
            views.setTextViewText(ids.name, tag.name)
            views.setTextViewText(ids.amount, tag.totalAmountFormatted)
        }
        return views
    }
}
