package com.osscabinet.ossWallet.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.view.View
import android.widget.RemoteViews
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.osscabinet.ossWallet.R
import kotlinx.serialization.json.Json

object CurrencyBalancesWidgetKeys {
    val IS_LOADED = stringPreferencesKey("cb_is_loaded")
    val TOTAL_FORMATTED = stringPreferencesKey("cb_total_formatted")
    val BASE_CURRENCY = stringPreferencesKey("cb_base_currency")
    val SHOW_BASE_CONVERSION = stringPreferencesKey("cb_show_base_conversion")
    val ROWS_JSON = stringPreferencesKey("cb_rows_json")
}

private fun parseRows(json: String?): List<CurrencyBalanceWidgetRow> {
    if (json.isNullOrBlank()) return emptyList()
    return runCatching { Json.decodeFromString<List<CurrencyBalanceWidgetRow>>(json) }.getOrDefault(emptyList())
}

private data class ChipViewIds(val container: Int, val code: Int, val amount: Int)

/** Currency Balances widget — 4x2 / 2x2 grid tier, up to 4 chips. Cell tap is inert. */
object CurrencyBalancesRemoteViewsRenderer {

    fun render(
        context: Context,
        prefs: Preferences,
        widgetId: Int,
        appWidgetManager: AppWidgetManager
    ) {
        val isLoaded = prefs[CurrencyBalancesWidgetKeys.IS_LOADED] == "true"
        if (!isLoaded) {
            appWidgetManager.updateAppWidget(widgetId, RemoteViews(context.packageName, R.layout.widget_currency_balances_loading))
            return
        }

        val rows = parseRows(prefs[CurrencyBalancesWidgetKeys.ROWS_JSON])
        val views = if (rows.isEmpty()) renderEmpty(context) else renderGrid(context, prefs, rows)
        appWidgetManager.updateAppWidget(widgetId, views)
    }

    private fun renderEmpty(context: Context): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_currency_balances_empty)
        views.setTextViewText(R.id.tv_cb_empty_subtitle, "No balances yet.")
        return views
    }

    private fun renderGrid(context: Context, prefs: Preferences, rows: List<CurrencyBalanceWidgetRow>): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_currency_balances_grid)
        views.setTextViewText(R.id.tv_cb_total, "≈ ${prefs[CurrencyBalancesWidgetKeys.TOTAL_FORMATTED] ?: ""}")

        val baseCurrency = prefs[CurrencyBalancesWidgetKeys.BASE_CURRENCY] ?: ""
        val showBaseConversion = prefs[CurrencyBalancesWidgetKeys.SHOW_BASE_CONVERSION] != "false"

        val cellIds = listOf(
            ChipViewIds(R.id.ll_cb_cell1, R.id.tv_cb_cell1_code, R.id.tv_cb_cell1_amount),
            ChipViewIds(R.id.ll_cb_cell2, R.id.tv_cb_cell2_code, R.id.tv_cb_cell2_amount),
            ChipViewIds(R.id.ll_cb_cell3, R.id.tv_cb_cell3_code, R.id.tv_cb_cell3_amount),
            ChipViewIds(R.id.ll_cb_cell4, R.id.tv_cb_cell4_code, R.id.tv_cb_cell4_amount)
        )

        cellIds.forEachIndexed { index, ids ->
            val row = rows.getOrNull(index)
            if (row == null) {
                views.setViewVisibility(ids.container, View.GONE)
                return@forEachIndexed
            }
            views.setViewVisibility(ids.container, View.VISIBLE)
            views.setTextViewText(ids.amount, row.amountFormatted)

            val showBase = showBaseConversion && row.currency != baseCurrency
            views.setTextViewText(
                ids.code,
                if (showBase) "${row.currency} · ≈ ${row.amountInBaseFormatted}" else row.currency
            )
        }

        return views
    }
}
