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
 *
 * Unified 3-slot cell model, used by every tier from strip (2 cells only) up: cell1/cell2 are
 * Inflow/Outflow (CashFlow) or Income/Expenses (Accounting); cell3 is Net flow (CashFlow,
 * highlighted) or Transfer (Accounting, plain). See NetWorthWidgetFetcher for the exact
 * visibility/value rules per perspective.
 */
object NetWorthWidgetKeys {
    val IS_LOADED = stringPreferencesKey("nw_is_loaded")
    val NET_WORTH = stringPreferencesKey("nw_net_worth")
    val NET_WORTH_COMPARISON = stringPreferencesKey("nw_comparison")
    val PERIOD_LABEL = stringPreferencesKey("nw_period_label")
    val FONT_SIZE = stringPreferencesKey("nw_font_size")
    val BASE_CURRENCY = stringPreferencesKey("nw_base_currency")
    val PERSPECTIVE_LABEL = stringPreferencesKey("nw_perspective_label")

    val CELL1_LABEL = stringPreferencesKey("nw_cell1_label")
    val CELL1_VALUE = stringPreferencesKey("nw_cell1_value")
    val CELL1_CMP = stringPreferencesKey("nw_cell1_cmp")
    val CELL1_VS = stringPreferencesKey("nw_cell1_vs")
    val CELL1_VISIBLE = stringPreferencesKey("nw_cell1_visible")

    val CELL2_LABEL = stringPreferencesKey("nw_cell2_label")
    val CELL2_VALUE = stringPreferencesKey("nw_cell2_value")
    val CELL2_CMP = stringPreferencesKey("nw_cell2_cmp")
    val CELL2_VS = stringPreferencesKey("nw_cell2_vs")
    val CELL2_VISIBLE = stringPreferencesKey("nw_cell2_visible")

    val CELL3_LABEL = stringPreferencesKey("nw_cell3_label")
    val CELL3_VALUE = stringPreferencesKey("nw_cell3_value")
    val CELL3_CMP = stringPreferencesKey("nw_cell3_cmp")
    val CELL3_VS = stringPreferencesKey("nw_cell3_vs")
    val CELL3_VISIBLE = stringPreferencesKey("nw_cell3_visible")
    val CELL3_HIGHLIGHT = stringPreferencesKey("nw_cell3_highlight")
}

/**
 * Builds and pushes the NetWorth widget's [RemoteViews] directly.
 *
 * Four height tiers across the widget's real 50dp-300dp resizable range (see
 * net_worth_widget_info*.xml): micro (just header+amount, <80dp), strip (+ a compact 2-cell
 * row, 80-129dp), card (+ a 3rd cell, 130-259dp), large (same 3-cell shape as card, plus a
 * "vs prior" line per cell since there's room, >=260dp — there is no separate "even taller"
 * tier beyond this, it just gets more breathing room inside the same flex:1 rows).
 */
object NetWorthRemoteViewsRenderer {

    private const val MICRO_HEIGHT_DP = 80
    private const val STRIP_HEIGHT_DP = 130
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
            minHeight >= LARGE_HEIGHT_DP -> renderCellTier(context, prefs, R.layout.widget_net_worth_large, withVsLine = true)
            else -> renderCellTier(context, prefs, R.layout.widget_net_worth_card, withVsLine = false)
        }
        appWidgetManager.updateAppWidget(widgetId, views)
    }

    private fun renderMicro(context: Context, prefs: Preferences): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_net_worth_micro)
        bindHeader(views, prefs, R.id.tv_nw_micro_label, R.id.tv_nw_micro_change, context)
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
     * [NetWorthWidgetKeys.NET_WORTH_COMPARISON] (and every per-cell *_CMP key) is a bare
     * "+4.2%"/"-4.2%" (see buildComparisonText) — mirrors NetWorthCard's own
     * "${if (up) "▲" else "▼"} ..." by prepending the arrow and resolving the matching
     * semantic color here.
     */
    private fun formatTrend(context: Context, changeText: String): Pair<String, Int> {
        val isUp = changeText.startsWith("+")
        val arrow = if (isUp) "▲" else "▼"
        val color = ContextCompat.getColor(context, if (isUp) R.color.widget_positive else R.color.widget_negative)
        return "$arrow $changeText" to color
    }

    /** Label (with perspective) + comparison pill, shared by every tier's header row. */
    private fun bindHeader(views: RemoteViews, prefs: Preferences, labelId: Int, pillId: Int, context: Context) {
        val perspectiveLabel = prefs[NetWorthWidgetKeys.PERSPECTIVE_LABEL] ?: "Cash Flow"
        views.setTextViewText(labelId, "NET WORTH · ${perspectiveLabel.uppercase()}")

        val changeText = prefs[NetWorthWidgetKeys.NET_WORTH_COMPARISON]?.takeIf { it.isNotEmpty() }
        views.setViewVisibility(pillId, if (changeText != null) View.VISIBLE else View.GONE)
        if (changeText != null) {
            val (text, color) = formatTrend(context, changeText)
            views.setTextViewText(pillId, text)
            views.setTextColor(pillId, color)
        }
    }

    private fun renderStrip(context: Context, prefs: Preferences): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_net_worth_strip)

        bindHeader(views, prefs, R.id.tv_nw_strip_label, R.id.tv_nw_strip_change, context)

        views.setTextViewText(R.id.tv_nw_strip_net_worth, prefs[NetWorthWidgetKeys.NET_WORTH] ?: "")
        views.setTextViewTextSize(
            R.id.tv_nw_strip_net_worth,
            TypedValue.COMPLEX_UNIT_SP,
            fontSizeSp(prefs[NetWorthWidgetKeys.FONT_SIZE])
        )

        bindCell(
            views, prefs, context,
            labelKey = NetWorthWidgetKeys.CELL1_LABEL, valueKey = NetWorthWidgetKeys.CELL1_VALUE,
            cmpKey = NetWorthWidgetKeys.CELL1_CMP, vsKey = null, visibleKey = NetWorthWidgetKeys.CELL1_VISIBLE,
            labelId = R.id.tv_nw_cell1_label, valueId = R.id.tv_nw_cell1_value,
            cmpId = R.id.tv_nw_cell1_cmp, vsId = null, containerId = R.id.ll_nw_cell1
        )
        bindCell(
            views, prefs, context,
            labelKey = NetWorthWidgetKeys.CELL2_LABEL, valueKey = NetWorthWidgetKeys.CELL2_VALUE,
            cmpKey = NetWorthWidgetKeys.CELL2_CMP, vsKey = null, visibleKey = NetWorthWidgetKeys.CELL2_VISIBLE,
            labelId = R.id.tv_nw_cell2_label, valueId = R.id.tv_nw_cell2_value,
            cmpId = R.id.tv_nw_cell2_cmp, vsId = null, containerId = R.id.ll_nw_cell2
        )

        return views
    }

    /**
     * Card (130-259dp) and large (>=260dp) tiers share this exact same 3-cell shape — only the
     * layout resource and whether the "vs prior" line is present differ.
     */
    private fun renderCellTier(context: Context, prefs: Preferences, layoutResId: Int, withVsLine: Boolean): RemoteViews {
        val views = RemoteViews(context.packageName, layoutResId)

        val netWorth = prefs[NetWorthWidgetKeys.NET_WORTH] ?: ""
        val changeText = prefs[NetWorthWidgetKeys.NET_WORTH_COMPARISON]?.takeIf { it.isNotEmpty() }
        val periodLabel = prefs[NetWorthWidgetKeys.PERIOD_LABEL] ?: ""
        val perspectiveLabel = prefs[NetWorthWidgetKeys.PERSPECTIVE_LABEL] ?: "Cash Flow"

        val netWorthId = if (withVsLine) R.id.tv_nw_large_net_worth else R.id.tv_nw_card_net_worth
        val changeId = if (withVsLine) R.id.tv_nw_large_change else R.id.tv_nw_card_change
        val trendId = if (withVsLine) R.id.ll_nw_large_trend else R.id.ll_nw_card_trend
        val subtitleId = if (withVsLine) R.id.tv_nw_large_subtitle else R.id.tv_nw_card_subtitle

        views.setTextViewText(netWorthId, netWorth)
        if (!withVsLine) {
            views.setTextViewTextSize(
                netWorthId,
                TypedValue.COMPLEX_UNIT_SP,
                fontSizeSp(prefs[NetWorthWidgetKeys.FONT_SIZE]) + 4f
            )
        }
        views.setTextViewText(subtitleId, "$periodLabel · $perspectiveLabel view")

        views.setViewVisibility(trendId, if (changeText != null) View.VISIBLE else View.GONE)
        if (changeText != null) {
            val (text, color) = formatTrend(context, changeText)
            views.setTextViewText(changeId, text)
            views.setTextColor(changeId, color)
        }

        val vs1 = if (withVsLine) R.id.tv_nw_cell1_vs else null
        val vs2 = if (withVsLine) R.id.tv_nw_cell2_vs else null
        val vs3 = if (withVsLine) R.id.tv_nw_cell3_vs else null

        bindCell(
            views, prefs, context,
            labelKey = NetWorthWidgetKeys.CELL1_LABEL, valueKey = NetWorthWidgetKeys.CELL1_VALUE,
            cmpKey = NetWorthWidgetKeys.CELL1_CMP, vsKey = NetWorthWidgetKeys.CELL1_VS, visibleKey = NetWorthWidgetKeys.CELL1_VISIBLE,
            labelId = R.id.tv_nw_cell1_label, valueId = R.id.tv_nw_cell1_value,
            cmpId = R.id.tv_nw_cell1_cmp, vsId = vs1, containerId = R.id.ll_nw_cell1
        )
        bindCell(
            views, prefs, context,
            labelKey = NetWorthWidgetKeys.CELL2_LABEL, valueKey = NetWorthWidgetKeys.CELL2_VALUE,
            cmpKey = NetWorthWidgetKeys.CELL2_CMP, vsKey = NetWorthWidgetKeys.CELL2_VS, visibleKey = NetWorthWidgetKeys.CELL2_VISIBLE,
            labelId = R.id.tv_nw_cell2_label, valueId = R.id.tv_nw_cell2_value,
            cmpId = R.id.tv_nw_cell2_cmp, vsId = vs2, containerId = R.id.ll_nw_cell2
        )
        bindCell(
            views, prefs, context,
            labelKey = NetWorthWidgetKeys.CELL3_LABEL, valueKey = NetWorthWidgetKeys.CELL3_VALUE,
            cmpKey = NetWorthWidgetKeys.CELL3_CMP, vsKey = NetWorthWidgetKeys.CELL3_VS, visibleKey = NetWorthWidgetKeys.CELL3_VISIBLE,
            labelId = R.id.tv_nw_cell3_label, valueId = R.id.tv_nw_cell3_value,
            cmpId = R.id.tv_nw_cell3_cmp, vsId = vs3, containerId = R.id.ll_nw_cell3
        )

        // Cell3 swaps between the highlighted netflow-chip style (CashFlow's Net flow) and the
        // plain metric-tile style + default text colors (Accounting's Transfer) — everything
        // else about cell3 is identical between perspectives.
        val highlighted = prefs[NetWorthWidgetKeys.CELL3_HIGHLIGHT] != "false"
        val cell3Bg = if (highlighted) R.drawable.widget_netflow_pill_bg else R.drawable.widget_metric_tile_bg
        views.setInt(R.id.ll_nw_cell3, "setBackgroundResource", cell3Bg)
        val cell3LabelColor = ContextCompat.getColor(
            context,
            if (highlighted) R.color.widget_accent else R.color.widget_on_surface_variant
        )
        val cell3ValueColor = ContextCompat.getColor(
            context,
            if (highlighted) R.color.widget_accent else R.color.widget_on_surface
        )
        views.setTextColor(R.id.tv_nw_cell3_label, cell3LabelColor)
        views.setTextColor(R.id.tv_nw_cell3_value, cell3ValueColor)
        if (vs3 != null) {
            views.setTextColor(vs3, if (highlighted) cell3LabelColor else ContextCompat.getColor(context, R.color.widget_on_surface_variant))
        }
        // tv_nw_cell3_cmp keeps whatever color formatTrend gave it (positive/negative), except
        // its highlighted-accent tint when there's no comparison to show (cmp view is GONE then
        // anyway, so this only matters while visible — formatTrend already handles that case).

        if (withVsLine) {
            val baseCurrency = prefs[NetWorthWidgetKeys.BASE_CURRENCY] ?: ""
            views.setTextViewText(
                R.id.tv_nw_large_caption,
                "Amounts in $baseCurrency · vs $periodLabel"
            )
        }

        return views
    }

    /**
     * Binds one of the 3 shared cell slots (see [NetWorthWidgetKeys]'s header comment).
     * [vsKey]/[vsId] are null on tiers with no "vs prior" line (strip, card).
     */
    private fun bindCell(
        views: RemoteViews,
        prefs: Preferences,
        context: Context,
        labelKey: Preferences.Key<String>,
        valueKey: Preferences.Key<String>,
        cmpKey: Preferences.Key<String>,
        vsKey: Preferences.Key<String>?,
        visibleKey: Preferences.Key<String>,
        labelId: Int,
        valueId: Int,
        cmpId: Int,
        vsId: Int?,
        containerId: Int
    ) {
        views.setTextViewText(labelId, prefs[labelKey] ?: "")
        views.setTextViewText(valueId, prefs[valueKey] ?: "")

        val cmp = prefs[cmpKey]?.takeIf { it.isNotEmpty() }
        views.setViewVisibility(cmpId, if (cmp != null) View.VISIBLE else View.GONE)
        if (cmp != null) {
            val (text, color) = formatTrend(context, cmp)
            views.setTextViewText(cmpId, text)
            views.setTextColor(cmpId, color)
        }

        if (vsId != null && vsKey != null) {
            val vsValue = prefs[vsKey]?.takeIf { it.isNotEmpty() }
            views.setViewVisibility(vsId, if (vsValue != null) View.VISIBLE else View.GONE)
            if (vsValue != null) {
                views.setTextViewText(vsId, "vs $vsValue")
            }
        }

        views.setViewVisibility(containerId, if (prefs[visibleKey] != "false") View.VISIBLE else View.GONE)
    }
}
