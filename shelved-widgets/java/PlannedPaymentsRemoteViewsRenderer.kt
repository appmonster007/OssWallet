package com.osscabinet.ossWallet.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.osscabinet.ossWallet.R
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object PlannedPaymentsWidgetKeys {
    val IS_LOADED = stringPreferencesKey("pp_is_loaded")
    val LOOKAHEAD_DAYS = stringPreferencesKey("pp_lookahead_days")
    val ROWS_JSON = stringPreferencesKey("pp_rows_json")
}

private fun parseRows(json: String?): List<PlannedPaymentWidgetRow> {
    if (json.isNullOrBlank()) return emptyList()
    return runCatching { Json.decodeFromString<List<PlannedPaymentWidgetRow>>(json) }.getOrDefault(emptyList())
}

private data class PlannedPaymentRowViewIds(
    val container: Int,
    val dateDay: Int,
    val dateMonth: Int,
    val dateBadgeBg: Int,
    val name: Int,
    val amount: Int,
    val skipButton: Int,
    val confirmButton: Int
)

/**
 * Planned Payments widget — 4x3 tier, sorted by scheduledDate, capped to 4 rows. The only
 * widget in this phase with real tap-to-mutate PendingIntents (Confirm/Skip ->
 * [PlannedPaymentsWidgetActionReceiver]); row body tap stays inert like every other card in
 * this phase (no deep-link infra exists yet, per Phase 1's finding).
 */
object PlannedPaymentsRemoteViewsRenderer {

    private val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())

    fun render(
        context: Context,
        prefs: Preferences,
        widgetId: Int,
        appWidgetManager: AppWidgetManager
    ) {
        val isLoaded = prefs[PlannedPaymentsWidgetKeys.IS_LOADED] == "true"
        if (!isLoaded) {
            appWidgetManager.updateAppWidget(widgetId, RemoteViews(context.packageName, R.layout.widget_planned_payments_loading))
            return
        }

        val rows = parseRows(prefs[PlannedPaymentsWidgetKeys.ROWS_JSON])
        val lookaheadDays = prefs[PlannedPaymentsWidgetKeys.LOOKAHEAD_DAYS]?.toIntOrNull() ?: 14

        val views = if (rows.isEmpty()) {
            renderEmpty(context, lookaheadDays)
        } else {
            renderTable(context, rows, lookaheadDays)
        }
        appWidgetManager.updateAppWidget(widgetId, views)
    }

    private fun renderEmpty(context: Context, lookaheadDays: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_planned_payments_empty)
        views.setTextViewText(
            R.id.tv_pp_empty_subtitle,
            "Nothing planned for the next $lookaheadDays days."
        )
        return views
    }

    private fun renderTable(
        context: Context,
        rows: List<PlannedPaymentWidgetRow>,
        lookaheadDays: Int
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_planned_payments_table)
        views.setTextViewText(R.id.tv_pp_header_right, "Next $lookaheadDays days")

        val rowIds = listOf(
            PlannedPaymentRowViewIds(R.id.ll_pp_row1, R.id.tv_pp_row1_day, R.id.tv_pp_row1_month, R.id.v_pp_row1_badge, R.id.tv_pp_row1_name, R.id.tv_pp_row1_amount, R.id.btn_pp_row1_skip, R.id.btn_pp_row1_confirm),
            PlannedPaymentRowViewIds(R.id.ll_pp_row2, R.id.tv_pp_row2_day, R.id.tv_pp_row2_month, R.id.v_pp_row2_badge, R.id.tv_pp_row2_name, R.id.tv_pp_row2_amount, R.id.btn_pp_row2_skip, R.id.btn_pp_row2_confirm),
            PlannedPaymentRowViewIds(R.id.ll_pp_row3, R.id.tv_pp_row3_day, R.id.tv_pp_row3_month, R.id.v_pp_row3_badge, R.id.tv_pp_row3_name, R.id.tv_pp_row3_amount, R.id.btn_pp_row3_skip, R.id.btn_pp_row3_confirm),
            PlannedPaymentRowViewIds(R.id.ll_pp_row4, R.id.tv_pp_row4_day, R.id.tv_pp_row4_month, R.id.v_pp_row4_badge, R.id.tv_pp_row4_name, R.id.tv_pp_row4_amount, R.id.btn_pp_row4_skip, R.id.btn_pp_row4_confirm)
        )

        rowIds.forEachIndexed { index, ids ->
            val row = rows.getOrNull(index)
            if (row == null) {
                views.setViewVisibility(ids.container, View.GONE)
                return@forEachIndexed
            }
            views.setViewVisibility(ids.container, View.VISIBLE)

            val cal = Calendar.getInstance().apply { timeInMillis = row.scheduledDate }
            views.setTextViewText(ids.dateDay, cal.get(Calendar.DAY_OF_MONTH).toString())
            views.setTextViewText(ids.dateMonth, monthFormat.format(Date(row.scheduledDate)))

            views.setInt(
                ids.dateBadgeBg, "setBackgroundResource",
                if (row.isOverdue) R.drawable.widget_pill_overdue_bg else R.drawable.widget_metric_tile_bg
            )
            views.setTextColor(ids.dateDay, if (row.isOverdue) android.graphics.Color.parseColor("#A12A20") else android.graphics.Color.parseColor("#14201E"))
            views.setTextColor(ids.dateMonth, if (row.isOverdue) android.graphics.Color.parseColor("#A12A20") else android.graphics.Color.parseColor("#4C5C59"))

            views.setTextViewText(ids.name, row.name)
            val amountText = when {
                row.isOverdue -> "Overdue · ${row.amountFormatted}"
                row.isIncome -> row.amountFormatted
                else -> row.amountFormatted
            }
            views.setTextViewText(ids.amount, amountText)
            val amountColor = when {
                row.isOverdue -> android.graphics.Color.parseColor("#A12A20")
                row.isIncome -> android.graphics.Color.parseColor("#1E9B4F")
                else -> android.graphics.Color.parseColor("#4C5C59")
            }
            views.setTextColor(ids.amount, amountColor)

            // First/overdue row gets the urgent styling (filled confirm, outlined skip); the
            // rest use the idle styling per the mockup. Purely cosmetic — both rows wire the
            // same real PendingIntents below.
            val isUrgent = index == 0 || row.isOverdue
            views.setInt(
                ids.confirmButton, "setBackgroundResource",
                if (isUrgent) R.drawable.widget_accent_pill_bg else R.drawable.widget_pill_pending_bg
            )

            views.setOnClickPendingIntent(
                ids.confirmButton,
                actionPendingIntent(context, PlannedPaymentsWidgetActionReceiver.ACTION_CONFIRM, row.paymentId, row.scheduledDate)
            )
            views.setOnClickPendingIntent(
                ids.skipButton,
                actionPendingIntent(context, PlannedPaymentsWidgetActionReceiver.ACTION_SKIP, row.paymentId, row.scheduledDate)
            )
        }

        return views
    }

    private fun actionPendingIntent(context: Context, action: String, paymentId: Long, scheduledDate: Long): PendingIntent {
        val intent = Intent(context, PlannedPaymentsWidgetActionReceiver::class.java).apply {
            this.action = action
            putExtra(PlannedPaymentsWidgetActionReceiver.EXTRA_PAYMENT_ID, paymentId)
            putExtra(PlannedPaymentsWidgetActionReceiver.EXTRA_SCHEDULED_DATE, scheduledDate)
        }
        // Unique request code per (action, paymentId, scheduledDate) so confirm/skip on
        // different rows (or the same row across refreshes) don't collide and overwrite
        // each other's PendingIntent extras.
        val requestCode = (action.hashCode() * 31 + paymentId.hashCode()) * 31 + scheduledDate.hashCode()
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
