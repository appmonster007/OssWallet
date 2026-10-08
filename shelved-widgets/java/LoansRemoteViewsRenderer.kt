package com.osscabinet.ossWallet.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.view.View
import android.widget.RemoteViews
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.osscabinet.ossWallet.R
import kotlinx.serialization.json.Json

object LoansWidgetKeys {
    val IS_LOADED = stringPreferencesKey("loans_is_loaded")
    val ROWS_JSON = stringPreferencesKey("loans_rows_json")
}

private fun parseRows(json: String?): List<LoanWidgetRow> {
    if (json.isNullOrBlank()) return emptyList()
    return runCatching { Json.decodeFromString<List<LoanWidgetRow>>(json) }.getOrDefault(emptyList())
}

private data class ActiveLoanRowIds(
    val container: Int,
    val name: Int,
    val typePill: Int,
    val remaining: Int,
    val track: Int,
    val fill: Int,
    val caption: Int,
    val interest: Int
)

/**
 * Loans widget — 4x3 tier, ACTIVE loans first (up to 2, with progress bars) then a single
 * compact SETTLED summary row if one exists and space allows. Row tap is inert.
 */
object LoansRemoteViewsRenderer {

    fun render(
        context: Context,
        prefs: Preferences,
        widgetId: Int,
        appWidgetManager: AppWidgetManager
    ) {
        val isLoaded = prefs[LoansWidgetKeys.IS_LOADED] == "true"
        if (!isLoaded) {
            appWidgetManager.updateAppWidget(widgetId, RemoteViews(context.packageName, R.layout.widget_loans_loading))
            return
        }

        val rows = parseRows(prefs[LoansWidgetKeys.ROWS_JSON])
        val options = appWidgetManager.getAppWidgetOptions(widgetId)
        val minWidthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 180)

        val views = if (rows.isEmpty()) renderEmpty(context) else renderTable(context, rows, minWidthDp)
        appWidgetManager.updateAppWidget(widgetId, views)
    }

    private fun renderEmpty(context: Context): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_loans_empty)
        views.setTextViewText(R.id.tv_loans_empty_subtitle, "Track money lent or borrowed from the app.")
        return views
    }

    private fun renderTable(context: Context, rows: List<LoanWidgetRow>, minWidthDp: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_loans_table)

        val active = rows.filter { it.status == "ACTIVE" }.take(2)
        val settled = rows.firstOrNull { it.status == "SETTLED" }

        val activeIds = listOf(
            ActiveLoanRowIds(R.id.ll_loan_row1, R.id.tv_loan_row1_name, R.id.tv_loan_row1_type, R.id.tv_loan_row1_remaining, R.id.v_loan_row1_track, R.id.v_loan_row1_fill, R.id.tv_loan_row1_caption, R.id.tv_loan_row1_interest),
            ActiveLoanRowIds(R.id.ll_loan_row2, R.id.tv_loan_row2_name, R.id.tv_loan_row2_type, R.id.tv_loan_row2_remaining, R.id.v_loan_row2_track, R.id.v_loan_row2_fill, R.id.tv_loan_row2_caption, R.id.tv_loan_row2_interest)
        )

        activeIds.forEachIndexed { index, ids ->
            val loan = active.getOrNull(index)
            if (loan == null) {
                views.setViewVisibility(ids.container, View.GONE)
                return@forEachIndexed
            }
            views.setViewVisibility(ids.container, View.VISIBLE)
            views.setTextViewText(ids.name, loan.name)
            views.setTextViewText(ids.typePill, loan.loanType)
            views.setTextViewText(ids.remaining, loan.remainingBalanceFormatted)

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val trackWidthDp = (minWidthDp - 32).coerceAtLeast(60)
                val fillWidthDp = trackWidthDp * loan.paidFraction
                views.setViewLayoutWidth(ids.fill, fillWidthDp, android.util.TypedValue.COMPLEX_UNIT_DIP)
            }

            val verb = if (loan.loanType == "Lent") "paid" else "repaid"
            views.setTextViewText(ids.caption, "${loan.totalPaidFormatted} $verb of ${loan.principalFormatted}")

            if (loan.hasInterest) {
                views.setTextViewText(ids.interest, "Interest ${loan.interestAccruedFormatted}")
            } else {
                views.setTextViewText(ids.interest, "No interest")
            }
        }

        views.setViewVisibility(R.id.ll_loan_settled, if (settled != null) View.VISIBLE else View.GONE)
        if (settled != null) {
            views.setTextViewText(R.id.tv_loan_settled_name, "${settled.name} · ${settled.lenderOrBorrowerName}")
        }

        return views
    }
}
