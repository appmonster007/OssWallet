package com.osscabinet.ossWallet.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.osscabinet.ossWallet.R

/**
 * Preference keys written by [NetWorthWidgetFetcher]. Plays the same role
 * [NetWorthGlanceWidget.Keys] used to when this widget was Glance-based.
 */
object NetWorthWidgetKeys {
    val IS_LOADED = stringPreferencesKey("nw_is_loaded")
    val NET_WORTH = stringPreferencesKey("nw_net_worth")
    val INC = stringPreferencesKey("nw_inc")
    val EXP = stringPreferencesKey("nw_exp")
    val NET_WORTH_COMPARISON = stringPreferencesKey("nw_comparison")
    val PERIOD_LABEL = stringPreferencesKey("nw_period_label")
    val FONT_SIZE = stringPreferencesKey("nw_font_size")

    // Large-tier (4x3) metric grid — see docs/card-data-summary.md NET_WORTH target model.
    val PERSPECTIVE_LABEL = stringPreferencesKey("nw_perspective_label")
    val INCOME = stringPreferencesKey("nw_income")
    val INCOME_CMP = stringPreferencesKey("nw_income_cmp")
    val EXPENSE = stringPreferencesKey("nw_expense")
    val EXPENSE_CMP = stringPreferencesKey("nw_expense_cmp")
    val TRANSFER = stringPreferencesKey("nw_transfer")
    val TRANSFER_CMP = stringPreferencesKey("nw_transfer_cmp")
    val INFLOW = stringPreferencesKey("nw_inflow")
    val INFLOW_CMP = stringPreferencesKey("nw_inflow_cmp")
    val OUTFLOW = stringPreferencesKey("nw_outflow")
    val OUTFLOW_CMP = stringPreferencesKey("nw_outflow_cmp")
    val NET_FLOW = stringPreferencesKey("nw_net_flow")
    val NET_FLOW_CMP = stringPreferencesKey("nw_net_flow_cmp")
    val BASE_CURRENCY = stringPreferencesKey("nw_base_currency")
}

/**
 * Builds and pushes the NetWorth widget's [RemoteViews] directly — the raw-RemoteViews
 * counterpart to what NetWorthGlanceWidget's @Composable content used to do.
 *
 * Three height tiers across the widget's real 50dp–200dp resizable range (see
 * net_worth_widget_info.xml), reusing the old Glance widget's own validated 130dp
 * strip/card split: micro (just the amount, for 50-79dp) is new since this widget's true
 * minHeight is 50dp, well below where a label+trend line fits without clipping.
 */
object NetWorthRemoteViewsRenderer {

    private const val MICRO_HEIGHT_DP = 80
    private const val STRIP_HEIGHT_DP = 130

    // New "large" tier (design's "large 4x3") — everything the card tier has, plus a
    // 3-column grid of 6 metric tiles. Chosen breakpoint sits well above the existing
    // micro/strip/card thresholds so those are untouched.
    private const val LARGE_HEIGHT_DP = 260

    fun render(
        context: Context,
        prefs: Preferences,
        widgetId: Int,
        appWidgetManager: AppWidgetManager
    ) {
        val isLoaded = prefs[NetWorthWidgetKeys.IS_LOADED] == "true"
        if (!isLoaded) {
            val loading = RemoteViews(context.packageName, R.layout.widget_net_worth)
            appWidgetManager.updateAppWidget(widgetId, loading)
            return
        }

        val options = appWidgetManager.getAppWidgetOptions(widgetId)
        val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)

        val views = when {
            minHeight in 1 until MICRO_HEIGHT_DP -> renderMicro(context, prefs)
            minHeight in MICRO_HEIGHT_DP until STRIP_HEIGHT_DP -> renderStrip(context, prefs)
            minHeight >= LARGE_HEIGHT_DP -> renderLarge(context, prefs)
            else -> renderCard(context, prefs)
        }
        appWidgetManager.updateAppWidget(widgetId, views)
    }

    private fun renderMicro(context: Context, prefs: Preferences): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_net_worth_micro)
        views.setTextViewText(R.id.tv_nw_micro_net_worth, prefs[NetWorthWidgetKeys.NET_WORTH] ?: "")
        return views
    }

    private fun fontSizeSp(fontSize: String?): Float = when (fontSize) {
        "Small" -> 18f
        "Large" -> 24f
        "X-Large" -> 32f
        else -> 20f
    }

    /**
     * [NetWorthWidgetKeys.NET_WORTH_COMPARISON] is a bare "+4.2%"/"-4.2%" (see
     * buildComparisonText) — mirrors NetWorthCard's own "${if (up) "▲" else "▼"} ..." by
     * prepending the arrow and resolving the matching semantic color here.
     */
    private fun formatTrend(context: Context, changeText: String): Pair<String, Int> {
        val isUp = changeText.startsWith("+")
        val arrow = if (isUp) "▲" else "▼"
        val color = ContextCompat.getColor(context, if (isUp) R.color.widget_positive else R.color.widget_negative)
        return "$arrow $changeText" to color
    }

    private fun renderStrip(context: Context, prefs: Preferences): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_net_worth_strip)

        val netWorth = prefs[NetWorthWidgetKeys.NET_WORTH] ?: ""
        val changeText = prefs[NetWorthWidgetKeys.NET_WORTH_COMPARISON]?.takeIf { it.isNotEmpty() }

        views.setTextViewText(R.id.tv_nw_strip_net_worth, netWorth)
        views.setTextViewTextSize(
            R.id.tv_nw_strip_net_worth,
            TypedValue.COMPLEX_UNIT_SP,
            fontSizeSp(prefs[NetWorthWidgetKeys.FONT_SIZE])
        )

        views.setViewVisibility(R.id.tv_nw_strip_change, if (changeText != null) View.VISIBLE else View.GONE)
        if (changeText != null) {
            val (text, color) = formatTrend(context, changeText)
            views.setTextViewText(R.id.tv_nw_strip_change, text)
            views.setTextColor(R.id.tv_nw_strip_change, color)
        }

        return views
    }

    private fun renderCard(context: Context, prefs: Preferences): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_net_worth_card)

        val netWorth = prefs[NetWorthWidgetKeys.NET_WORTH] ?: ""
        val inc = prefs[NetWorthWidgetKeys.INC] ?: ""
        val exp = prefs[NetWorthWidgetKeys.EXP] ?: ""
        val changeText = prefs[NetWorthWidgetKeys.NET_WORTH_COMPARISON]?.takeIf { it.isNotEmpty() }

        views.setViewVisibility(R.id.ll_nw_card_trend, if (changeText != null) View.VISIBLE else View.GONE)
        if (changeText != null) {
            val (text, color) = formatTrend(context, changeText)
            views.setTextViewText(R.id.tv_nw_card_change, text)
            views.setTextColor(R.id.tv_nw_card_change, color)
        }

        views.setTextViewText(R.id.tv_nw_card_net_worth, netWorth)
        views.setTextViewTextSize(
            R.id.tv_nw_card_net_worth,
            TypedValue.COMPLEX_UNIT_SP,
            fontSizeSp(prefs[NetWorthWidgetKeys.FONT_SIZE]) + 4f
        )

        views.setTextViewText(R.id.tv_nw_card_inc, inc)
        views.setTextViewText(R.id.tv_nw_card_exp, exp)

        return views
    }

    private data class MetricTileIds(
        val container: Int,
        val label: Int,
        val value: Int,
        val comparison: Int
    )

    /**
     * 3x2 grid of 6 metric tiles (Income, Expenses, Transfers, Inflow, Outflow, Net flow),
     * Net flow highlighted with the netflow chip bg + accent text — see NET_WORTH target
     * model's metrics[] in docs/card-data-summary.md.
     */
    private fun renderLarge(context: Context, prefs: Preferences): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_net_worth_large)

        val netWorth = prefs[NetWorthWidgetKeys.NET_WORTH] ?: ""
        val changeText = prefs[NetWorthWidgetKeys.NET_WORTH_COMPARISON]?.takeIf { it.isNotEmpty() }
        val periodLabel = prefs[NetWorthWidgetKeys.PERIOD_LABEL] ?: ""
        val perspectiveLabel = prefs[NetWorthWidgetKeys.PERSPECTIVE_LABEL] ?: "Cash Flow"

        views.setTextViewText(R.id.tv_nw_large_net_worth, netWorth)
        views.setTextViewText(R.id.tv_nw_large_subtitle, "$periodLabel · $perspectiveLabel view")

        views.setViewVisibility(R.id.ll_nw_large_trend, if (changeText != null) View.VISIBLE else View.GONE)
        if (changeText != null) {
            val (text, color) = formatTrend(context, changeText)
            views.setTextViewText(R.id.tv_nw_large_change, text)
            views.setTextColor(R.id.tv_nw_large_change, color)
        }

        views.setTextViewText(R.id.tv_nw_large_inc, prefs[NetWorthWidgetKeys.INC] ?: "")
        views.setTextViewText(R.id.tv_nw_large_exp, prefs[NetWorthWidgetKeys.EXP] ?: "")

        val tiles = listOf(
            MetricTileIds(R.id.ll_nw_tile_income, R.id.tv_nw_tile_income_label, R.id.tv_nw_tile_income_value, R.id.tv_nw_tile_income_cmp)
                to (NetWorthWidgetKeys.INCOME to NetWorthWidgetKeys.INCOME_CMP),
            MetricTileIds(R.id.ll_nw_tile_expense, R.id.tv_nw_tile_expense_label, R.id.tv_nw_tile_expense_value, R.id.tv_nw_tile_expense_cmp)
                to (NetWorthWidgetKeys.EXPENSE to NetWorthWidgetKeys.EXPENSE_CMP),
            MetricTileIds(R.id.ll_nw_tile_transfer, R.id.tv_nw_tile_transfer_label, R.id.tv_nw_tile_transfer_value, R.id.tv_nw_tile_transfer_cmp)
                to (NetWorthWidgetKeys.TRANSFER to NetWorthWidgetKeys.TRANSFER_CMP),
            MetricTileIds(R.id.ll_nw_tile_inflow, R.id.tv_nw_tile_inflow_label, R.id.tv_nw_tile_inflow_value, R.id.tv_nw_tile_inflow_cmp)
                to (NetWorthWidgetKeys.INFLOW to NetWorthWidgetKeys.INFLOW_CMP),
            MetricTileIds(R.id.ll_nw_tile_outflow, R.id.tv_nw_tile_outflow_label, R.id.tv_nw_tile_outflow_value, R.id.tv_nw_tile_outflow_cmp)
                to (NetWorthWidgetKeys.OUTFLOW to NetWorthWidgetKeys.OUTFLOW_CMP),
            MetricTileIds(R.id.ll_nw_tile_netflow, R.id.tv_nw_tile_netflow_label, R.id.tv_nw_tile_netflow_value, R.id.tv_nw_tile_netflow_cmp)
                to (NetWorthWidgetKeys.NET_FLOW to NetWorthWidgetKeys.NET_FLOW_CMP)
        )

        tiles.forEach { (ids, keys) ->
            val (valueKey, cmpKey) = keys
            views.setTextViewText(ids.value, prefs[valueKey] ?: "")
            val cmp = prefs[cmpKey]?.takeIf { it.isNotEmpty() }
            views.setViewVisibility(ids.comparison, if (cmp != null) View.VISIBLE else View.GONE)
            if (cmp != null) {
                val (text, color) = formatTrend(context, cmp)
                views.setTextViewText(ids.comparison, text)
                views.setTextColor(ids.comparison, color)
            }
        }

        val baseCurrency = prefs[NetWorthWidgetKeys.BASE_CURRENCY] ?: ""
        views.setTextViewText(
            R.id.tv_nw_large_caption,
            "Amounts in $baseCurrency · vs $periodLabel"
        )

        return views
    }
}
