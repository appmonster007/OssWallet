package com.osscabinet.ossWallet.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.view.View
import android.widget.RemoteViews
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.osscabinet.ossWallet.R
import org.json.JSONArray
import org.json.JSONException

/**
 * Preference keys written by [CashFlowWidgetFetcher]. Plays the same role
 * [CashFlowGlanceWidget.Keys] used to when this widget was Glance-based.
 */
object CashFlowWidgetKeys {
    val IS_LOADED = stringPreferencesKey("cf_is_loaded")
    val INCOME_TOTAL = stringPreferencesKey("cf_income_total")
    val EXPENSE_TOTAL = stringPreferencesKey("cf_expense_total")
    val COMPARISON = stringPreferencesKey("cf_comparison")
    val PERIOD_LABEL = stringPreferencesKey("cf_period_label")
    val BASE_CURRENCY = stringPreferencesKey("cf_base_currency")
    val SHOW_BASE_CONVERSION = stringPreferencesKey("cf_show_base_conversion")
    val SHOW_TOTALS = stringPreferencesKey("cf_show_totals")
    val CURRENCY_LIST = stringPreferencesKey("cf_currency_list")

    // Target-model additions (CASH_FLOW section, docs/card-data-summary.md): full
    // FinancialSummary (net flow / transfer / inflow / outflow), not just income/expense,
    // plus the showNetColumn toggle for the table tier's 4th column.
    val NET_FLOW_TOTAL = stringPreferencesKey("cf_net_flow_total")
    val TRANSFER_TOTAL = stringPreferencesKey("cf_transfer_total")
    val INFLOW_TOTAL = stringPreferencesKey("cf_inflow_total")
    val OUTFLOW_TOTAL = stringPreferencesKey("cf_outflow_total")
    val SHOW_NET_COLUMN = stringPreferencesKey("cf_show_net_column")
}

private data class CashFlowRowViewIds(
    val container: Int,
    val code: Int,
    val income: Int,
    val incomeBase: Int,
    val expense: Int,
    val expenseBase: Int,
    val net: Int,
    val netBase: Int
)

internal data class CashFlowCurrencyRow(
    val code: String,
    val income: String,
    val incomeBase: String,
    val expense: String,
    val expenseBase: String,
    val netFlow: String,
    val netFlowBase: String,
    val transfer: String
)

internal fun parseCashFlowCurrencyList(json: String?): List<CashFlowCurrencyRow> {
    if (json.isNullOrBlank()) return emptyList()
    return try {
        val arr = JSONArray(json)
        (0 until arr.length()).mapNotNull { i ->
            val obj = arr.getJSONObject(i)
            val code = obj.optString("code", "")
            if (code.isBlank()) return@mapNotNull null
            CashFlowCurrencyRow(
                code = code,
                income = obj.optString("income", ""),
                incomeBase = obj.optString("incomeBase", ""),
                expense = obj.optString("expense", ""),
                expenseBase = obj.optString("expenseBase", ""),
                netFlow = obj.optString("netFlow", ""),
                netFlowBase = obj.optString("netFlowBase", ""),
                transfer = obj.optString("transfer", "")
            )
        }
    } catch (_: JSONException) {
        emptyList()
    }
}

/**
 * Builds and pushes the Cash Flow widget's [RemoteViews] directly — the raw-RemoteViews
 * counterpart to what CashFlowGlanceWidget's @Composable content used to do. Reads the exact
 * same preference keys [CashFlowWidgetFetcher] already writes, so that fetcher needed no changes.
 *
 * Row count follows the old Glance widget's own validated tiering: 3 rows below 200dp tall,
 * 5 rows at 200dp+ (cash_flow_widget_info.xml has no maxResizeHeight, so this is genuinely
 * reachable, unlike the compact layout below). No width-based INCOME/INC label switch or
 * transfer column (CashFlowWidgetFetcher never populates transfer data either way).
 *
 * Below [HEADER_HEIGHT_DP] the "CASH FLOW · {period}" header line is hidden: at the widget's
 * true 110dp minimum, that line plus the column headers and Σ row already consumed enough of
 * the ~90dp content area that a 2nd currency row didn't actually fit, even though the code
 * claimed 3 rows always did. Dropping it trades the period label for making the currency
 * data — the actual point of the widget — fit at the sizes users can really place it at.
 */
object CashFlowRemoteViewsRenderer {

    private val COMPACT_HEIGHT_DP = 100
    private val HEADER_HEIGHT_DP = 150
    private val TALL_HEIGHT_DP = 200

    fun render(
        context: Context,
        prefs: Preferences,
        widgetId: Int,
        appWidgetManager: AppWidgetManager
    ) {
        val isLoaded = prefs[CashFlowWidgetKeys.IS_LOADED] == "true"
        if (!isLoaded) {
            val loading = RemoteViews(context.packageName, R.layout.widget_cash_flow)
            appWidgetManager.updateAppWidget(widgetId, loading)
            return
        }

        val options = appWidgetManager.getAppWidgetOptions(widgetId)
        val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
        val isCompact = minHeight in 1 until COMPACT_HEIGHT_DP
        val rows = parseCashFlowCurrencyList(prefs[CashFlowWidgetKeys.CURRENCY_LIST])

        val views = when {
            rows.isEmpty() && !isCompact -> renderEmpty(context, prefs)
            isCompact -> renderCompact(context, prefs)
            else -> {
                val maxRows = if (minHeight >= TALL_HEIGHT_DP) 5 else 3
                val showHeader = minHeight >= HEADER_HEIGHT_DP
                renderTable(context, prefs, rows, maxRows, showHeader)
            }
        }
        appWidgetManager.updateAppWidget(widgetId, views)
    }

    /**
     * Compact tier (design's "compact"): period label, net% trend chip, 3-col grid
     * (Income/Expenses/Net flow), a summary chip row (In/Out/Transfers), and a caption.
     */
    private fun renderCompact(context: Context, prefs: Preferences): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_cash_flow_compact)

        val periodLabel = prefs[CashFlowWidgetKeys.PERIOD_LABEL] ?: ""
        views.setTextViewText(R.id.tv_cf_compact_period, periodLabel)

        val comparison = prefs[CashFlowWidgetKeys.COMPARISON]?.takeIf { it.isNotEmpty() }
        views.setViewVisibility(R.id.tv_cf_compact_trend, if (comparison != null) View.VISIBLE else View.GONE)
        if (comparison != null) {
            val isUp = comparison.startsWith("+")
            val arrow = if (isUp) "▲" else "▼"
            views.setTextViewText(R.id.tv_cf_compact_trend, "$arrow $comparison")
        }

        views.setTextViewText(R.id.tv_cf_compact_income, prefs[CashFlowWidgetKeys.INCOME_TOTAL] ?: "")
        views.setTextViewText(R.id.tv_cf_compact_expense, prefs[CashFlowWidgetKeys.EXPENSE_TOTAL] ?: "")
        views.setTextViewText(R.id.tv_cf_compact_netflow, prefs[CashFlowWidgetKeys.NET_FLOW_TOTAL] ?: "")

        views.setTextViewText(R.id.tv_cf_compact_chip_in, prefs[CashFlowWidgetKeys.INFLOW_TOTAL] ?: "")
        views.setTextViewText(R.id.tv_cf_compact_chip_out, prefs[CashFlowWidgetKeys.OUTFLOW_TOTAL] ?: "")
        views.setTextViewText(R.id.tv_cf_compact_chip_transfers, prefs[CashFlowWidgetKeys.TRANSFER_TOTAL] ?: "")

        val baseCurrency = prefs[CashFlowWidgetKeys.BASE_CURRENCY] ?: ""
        views.setTextViewText(R.id.tv_cf_compact_caption, "Amounts in $baseCurrency")

        return views
    }

    /** Empty state (new tier): shown when [CashFlowWidgetKeys.CURRENCY_LIST] has no rows. */
    private fun renderEmpty(context: Context, prefs: Preferences): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_cash_flow_empty)
        val periodLabel = prefs[CashFlowWidgetKeys.PERIOD_LABEL] ?: ""
        views.setTextViewText(
            R.id.tv_cf_empty_subtitle,
            "$periodLabel income and expenses show up here."
        )
        return views
    }

    private fun renderTable(
        context: Context,
        prefs: Preferences,
        rows: List<CashFlowCurrencyRow>,
        maxRows: Int,
        showHeader: Boolean
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_cash_flow_table)

        views.setViewVisibility(R.id.ll_cf_header, if (showHeader) View.VISIBLE else View.GONE)
        val periodLabel = prefs[CashFlowWidgetKeys.PERIOD_LABEL] ?: ""
        val comparison = prefs[CashFlowWidgetKeys.COMPARISON]?.takeIf { it.isNotEmpty() }
        views.setTextViewText(R.id.tv_cf_subtitle, comparison ?: periodLabel)

        val showNetColumn = prefs[CashFlowWidgetKeys.SHOW_NET_COLUMN] != "false"
        views.setViewVisibility(R.id.tv_cf_header_net, if (showNetColumn) View.VISIBLE else View.GONE)

        val showTotals = prefs[CashFlowWidgetKeys.SHOW_TOTALS] != "false"
        views.setViewVisibility(R.id.ll_cf_totals, if (showTotals) View.VISIBLE else View.GONE)
        views.setTextViewText(R.id.tv_cf_income_total, prefs[CashFlowWidgetKeys.INCOME_TOTAL] ?: "")
        views.setTextViewText(R.id.tv_cf_expense_total, prefs[CashFlowWidgetKeys.EXPENSE_TOTAL] ?: "")
        views.setViewVisibility(R.id.tv_cf_net_total, if (showNetColumn) View.VISIBLE else View.GONE)
        views.setTextViewText(R.id.tv_cf_net_total, prefs[CashFlowWidgetKeys.NET_FLOW_TOTAL] ?: "")

        val baseCurrency = prefs[CashFlowWidgetKeys.BASE_CURRENCY] ?: ""
        val showBaseConversion = prefs[CashFlowWidgetKeys.SHOW_BASE_CONVERSION] != "false"

        val rowViewIds = listOf(
            CashFlowRowViewIds(
                container = R.id.ll_cf_row1, code = R.id.tv_cf_row1_code,
                income = R.id.tv_cf_row1_income, incomeBase = R.id.tv_cf_row1_income_base,
                expense = R.id.tv_cf_row1_expense, expenseBase = R.id.tv_cf_row1_expense_base,
                net = R.id.tv_cf_row1_net, netBase = R.id.tv_cf_row1_net_base
            ),
            CashFlowRowViewIds(
                container = R.id.ll_cf_row2, code = R.id.tv_cf_row2_code,
                income = R.id.tv_cf_row2_income, incomeBase = R.id.tv_cf_row2_income_base,
                expense = R.id.tv_cf_row2_expense, expenseBase = R.id.tv_cf_row2_expense_base,
                net = R.id.tv_cf_row2_net, netBase = R.id.tv_cf_row2_net_base
            ),
            CashFlowRowViewIds(
                container = R.id.ll_cf_row3, code = R.id.tv_cf_row3_code,
                income = R.id.tv_cf_row3_income, incomeBase = R.id.tv_cf_row3_income_base,
                expense = R.id.tv_cf_row3_expense, expenseBase = R.id.tv_cf_row3_expense_base,
                net = R.id.tv_cf_row3_net, netBase = R.id.tv_cf_row3_net_base
            ),
            CashFlowRowViewIds(
                container = R.id.ll_cf_row4, code = R.id.tv_cf_row4_code,
                income = R.id.tv_cf_row4_income, incomeBase = R.id.tv_cf_row4_income_base,
                expense = R.id.tv_cf_row4_expense, expenseBase = R.id.tv_cf_row4_expense_base,
                net = R.id.tv_cf_row4_net, netBase = R.id.tv_cf_row4_net_base
            ),
            CashFlowRowViewIds(
                container = R.id.ll_cf_row5, code = R.id.tv_cf_row5_code,
                income = R.id.tv_cf_row5_income, incomeBase = R.id.tv_cf_row5_income_base,
                expense = R.id.tv_cf_row5_expense, expenseBase = R.id.tv_cf_row5_expense_base,
                net = R.id.tv_cf_row5_net, netBase = R.id.tv_cf_row5_net_base
            )
        )

        rowViewIds.forEachIndexed { index, ids ->
            val row = rows.getOrNull(index)?.takeIf { index < maxRows }
            if (row == null) {
                views.setViewVisibility(ids.container, View.GONE)
                return@forEachIndexed
            }
            views.setViewVisibility(ids.container, View.VISIBLE)
            views.setTextViewText(ids.code, row.code)
            views.setTextViewText(ids.income, row.income)
            views.setTextViewText(ids.expense, row.expense)

            views.setViewVisibility(ids.net, if (showNetColumn) View.VISIBLE else View.GONE)
            views.setViewVisibility(ids.netBase, if (showNetColumn) View.VISIBLE else View.GONE)
            if (showNetColumn) {
                views.setTextViewText(ids.net, row.netFlow)
            }

            val showConversion = showBaseConversion && row.code != baseCurrency
            val conversionVisibility = if (showConversion) View.VISIBLE else View.GONE
            views.setViewVisibility(ids.incomeBase, conversionVisibility)
            views.setViewVisibility(ids.expenseBase, conversionVisibility)
            if (showConversion) {
                views.setTextViewText(ids.incomeBase, "≈ ${row.incomeBase}")
                views.setTextViewText(ids.expenseBase, "≈ ${row.expenseBase}")
            }
            if (showNetColumn) {
                views.setViewVisibility(ids.netBase, if (showConversion) View.VISIBLE else View.GONE)
                if (showConversion) {
                    views.setTextViewText(ids.netBase, "≈ ${row.netFlowBase}")
                }
            }
        }

        return views
    }
}
