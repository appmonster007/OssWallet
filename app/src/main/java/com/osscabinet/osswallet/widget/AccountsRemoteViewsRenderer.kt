package com.osscabinet.ossWallet.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.view.View
import android.widget.RemoteViews
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.osscabinet.ossWallet.R
import kotlinx.serialization.json.Json

object AccountsWidgetKeys {
    val IS_LOADED = stringPreferencesKey("accounts_is_loaded")
    val PERIOD_LABEL = stringPreferencesKey("accounts_period_label")
    val TOTAL_FORMATTED = stringPreferencesKey("accounts_total_formatted")
    val ROWS_JSON = stringPreferencesKey("accounts_rows_json")
}

private fun parseRows(json: String?): List<AccountWidgetRow> {
    if (json.isNullOrBlank()) return emptyList()
    return runCatching { Json.decodeFromString<List<AccountWidgetRow>>(json) }.getOrDefault(emptyList())
}

private data class AccountRowIds(
    val container: Int,
    val dot: Int,
    val name: Int,
    val caption: Int,
    val sparkline: Int,
    val balance: Int
)

/**
 * Accounts widget — 4x3 tier, by displayOrder, up to 3 rows plus a "Total" footer. Each row's
 * sparkline is a bitmap drawn via [WidgetSparklineRenderer] at render time (RemoteViews has no
 * native chart primitive — same constraint CLAUDE.md's Glance/RemoteViews note describes). Row
 * tap is inert.
 */
object AccountsRemoteViewsRenderer {

    private const val SPARKLINE_WIDTH_DP = 56
    private const val SPARKLINE_HEIGHT_DP = 22

    fun render(
        context: Context,
        prefs: Preferences,
        widgetId: Int,
        appWidgetManager: AppWidgetManager
    ) {
        val isLoaded = prefs[AccountsWidgetKeys.IS_LOADED] == "true"
        if (!isLoaded) {
            appWidgetManager.updateAppWidget(widgetId, RemoteViews(context.packageName, R.layout.widget_accounts_loading))
            return
        }

        val rows = parseRows(prefs[AccountsWidgetKeys.ROWS_JSON])
        val views = if (rows.isEmpty()) renderEmpty(context) else renderTable(context, prefs, rows)
        appWidgetManager.updateAppWidget(widgetId, views)
    }

    private fun renderEmpty(context: Context): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_accounts_empty)
        views.setTextViewText(R.id.tv_accounts_empty_subtitle, "Add an account in the app to see it here.")
        return views
    }

    private fun renderTable(context: Context, prefs: Preferences, rows: List<AccountWidgetRow>): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_accounts_table)
        views.setTextViewText(R.id.tv_accounts_header_right, prefs[AccountsWidgetKeys.PERIOD_LABEL] ?: "")
        views.setTextViewText(R.id.tv_accounts_total, "≈ ${prefs[AccountsWidgetKeys.TOTAL_FORMATTED] ?: ""}")

        val density = context.resources.displayMetrics.density
        val widthPx = (SPARKLINE_WIDTH_DP * density).toInt()
        val heightPx = (SPARKLINE_HEIGHT_DP * density).toInt()
        val strokeWidthPx = 2f * density

        val rowIds = listOf(
            AccountRowIds(R.id.ll_account_row1, R.id.v_account_row1_dot, R.id.tv_account_row1_name, R.id.tv_account_row1_caption, R.id.iv_account_row1_sparkline, R.id.tv_account_row1_balance),
            AccountRowIds(R.id.ll_account_row2, R.id.v_account_row2_dot, R.id.tv_account_row2_name, R.id.tv_account_row2_caption, R.id.iv_account_row2_sparkline, R.id.tv_account_row2_balance),
            AccountRowIds(R.id.ll_account_row3, R.id.v_account_row3_dot, R.id.tv_account_row3_name, R.id.tv_account_row3_caption, R.id.iv_account_row3_sparkline, R.id.tv_account_row3_balance)
        )

        rowIds.forEachIndexed { index, ids ->
            val row = rows.getOrNull(index)
            if (row == null) {
                views.setViewVisibility(ids.container, View.GONE)
                return@forEachIndexed
            }
            views.setViewVisibility(ids.container, View.VISIBLE)

            val dotColor = WidgetSparklineRenderer.parseColorOrDefault(row.colorHex)
            views.setInt(ids.dot, "setColorFilter", dotColor)
            views.setTextViewText(ids.name, row.name)
            views.setTextViewText(ids.caption, row.netCaption)
            views.setTextViewText(ids.balance, row.balanceFormatted)

            if (row.sparklinePoints.size >= 2) {
                val bitmap = WidgetSparklineRenderer.renderLineSparkline(
                    widthPx = widthPx,
                    heightPx = heightPx,
                    points = row.sparklinePoints,
                    strokeColor = dotColor,
                    strokeWidthPx = strokeWidthPx
                )
                views.setImageViewBitmap(ids.sparkline, bitmap)
                views.setViewVisibility(ids.sparkline, View.VISIBLE)
            } else {
                views.setViewVisibility(ids.sparkline, View.GONE)
            }
        }

        return views
    }
}
