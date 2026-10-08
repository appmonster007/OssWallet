package com.osscabinet.ossWallet.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.view.View
import android.widget.RemoteViews
import androidx.datastore.preferences.core.Preferences
import com.osscabinet.ossWallet.R

private data class LegendRowIds(val container: Int, val dot: Int, val name: Int, val pct: Int)

/**
 * Category Pie Chart widget — 4x2 tier, donut bitmap (left) + up to 5 legend rows (top 4 slices
 * + one aggregate "Other" row) on the right. The center amount overlay is a plain TextView on
 * top of the donut ImageView (crisper/localizable than drawing text into the bitmap). Tap-inert.
 */
object CategoryPieChartRemoteViewsRenderer {

    private const val DONUT_SIZE_DP = 84

    fun render(context: Context, prefs: Preferences, widgetId: Int, appWidgetManager: AppWidgetManager) {
        val isLoaded = prefs[CategoryPieChartWidgetKeys.IS_LOADED] == "true"
        if (!isLoaded) {
            appWidgetManager.updateAppWidget(widgetId, RemoteViews(context.packageName, R.layout.widget_category_pie_loading))
            return
        }
        val slices = CategoryChartDataMapper.decode(prefs[CategoryPieChartWidgetKeys.SLICES_JSON])
        val views = if (slices.isEmpty()) renderEmpty(context) else renderChart(context, prefs, slices)
        appWidgetManager.updateAppWidget(widgetId, views)
    }

    private fun renderEmpty(context: Context): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_category_pie_empty)
        views.setTextViewText(R.id.tv_cpc_empty_subtitle, "Spending by category shows up here.")
        return views
    }

    private fun renderChart(context: Context, prefs: Preferences, slices: List<WidgetCategorySlice>): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_category_pie_chart)
        views.setTextViewText(R.id.tv_cpc_header, "Spending · ${prefs[CategoryPieChartWidgetKeys.PERIOD_LABEL] ?: ""}")
        views.setTextViewText(R.id.tv_cpc_center_amount, prefs[CategoryPieChartWidgetKeys.EXPENSE_TOTAL_FORMATTED] ?: "")

        val density = context.resources.displayMetrics.density
        val sizePx = (DONUT_SIZE_DP * density).toInt()
        val strokeWidthPx = 14f * density
        val bitmap = WidgetDonutChartRenderer.renderDonut(sizePx, sizePx, slices, strokeWidthPx)
        views.setImageViewBitmap(R.id.iv_cpc_donut, bitmap)

        val top4 = slices.take(4)
        val otherAmount = slices.drop(4).sumOf { it.amount }
        val otherPct = slices.drop(4).sumOf { it.percentage.toDouble() }.toFloat()

        val legendRows = listOf(
            LegendRowIds(R.id.ll_cpc_legend1, R.id.v_cpc_legend1_dot, R.id.tv_cpc_legend1_name, R.id.tv_cpc_legend1_pct),
            LegendRowIds(R.id.ll_cpc_legend2, R.id.v_cpc_legend2_dot, R.id.tv_cpc_legend2_name, R.id.tv_cpc_legend2_pct),
            LegendRowIds(R.id.ll_cpc_legend3, R.id.v_cpc_legend3_dot, R.id.tv_cpc_legend3_name, R.id.tv_cpc_legend3_pct),
            LegendRowIds(R.id.ll_cpc_legend4, R.id.v_cpc_legend4_dot, R.id.tv_cpc_legend4_name, R.id.tv_cpc_legend4_pct),
            LegendRowIds(R.id.ll_cpc_legend5, R.id.v_cpc_legend5_dot, R.id.tv_cpc_legend5_name, R.id.tv_cpc_legend5_pct)
        )

        legendRows.forEachIndexed { index, ids ->
            if (index < top4.size) {
                val slice = top4[index]
                views.setViewVisibility(ids.container, View.VISIBLE)
                views.setInt(ids.dot, "setColorFilter", WidgetSparklineRenderer.parseColorOrDefault(slice.colorHex))
                views.setTextViewText(ids.name, slice.category)
                views.setTextViewText(ids.pct, "${(slice.percentage * 100).let { "%.0f".format(it) }}%")
            } else if (index == 4 && otherAmount > 0.0) {
                views.setViewVisibility(ids.container, View.VISIBLE)
                views.setInt(ids.dot, "setColorFilter", android.graphics.Color.parseColor("#9AA8A5"))
                views.setTextViewText(ids.name, "Other")
                views.setTextViewText(ids.pct, "${(otherPct * 100).let { "%.0f".format(it) }}%")
            } else {
                views.setViewVisibility(ids.container, View.GONE)
            }
        }
        return views
    }
}
