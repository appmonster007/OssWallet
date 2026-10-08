package com.osscabinet.ossWallet.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.graphics.Color
import android.view.View
import android.widget.RemoteViews
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.osscabinet.ossWallet.R
import kotlinx.serialization.json.Json

object BudgetsWidgetKeys {
    val IS_LOADED = stringPreferencesKey("budgets_is_loaded")
    val PERIOD_LABEL = stringPreferencesKey("budgets_period_label")
    val SHOW_COMPARISON = stringPreferencesKey("budgets_show_comparison")
    val ROWS_JSON = stringPreferencesKey("budgets_rows_json")
}

private fun parseBudgetRows(json: String?): List<BudgetWidgetRow> {
    if (json.isNullOrBlank()) return emptyList()
    return runCatching { Json.decodeFromString<List<BudgetWidgetRow>>(json) }.getOrDefault(emptyList())
}

private data class BudgetRowViewIds(
    val container: Int,
    val name: Int,
    val statusRight: Int,
    val barTrack: Int,
    val barFill: Int,
    val subLine: Int,
    val comparison: Int
)

/**
 * Budgets widget — 4x3 "3 rows" tier (primary) with a sub-line per row, and a 4x2 "2 rows,
 * no sub-line" tier dropped at smaller heights. Caps follow the mockup's fixed row budget:
 * 3 rows on the tall tier, 2 on the compact tier (same fixed-row/GONE pattern as Cash Flow's
 * table tier). Row tap is inert — no deep-link infra exists yet for detail cards (Phase 1
 * finding, not re-verified here).
 */
object BudgetsRemoteViewsRenderer {

    private const val COMPACT_HEIGHT_DP = 180

    fun render(
        context: Context,
        prefs: Preferences,
        widgetId: Int,
        appWidgetManager: AppWidgetManager
    ) {
        val isLoaded = prefs[BudgetsWidgetKeys.IS_LOADED] == "true"
        if (!isLoaded) {
            appWidgetManager.updateAppWidget(widgetId, RemoteViews(context.packageName, R.layout.widget_budgets_loading))
            return
        }

        val rows = parseBudgetRows(prefs[BudgetsWidgetKeys.ROWS_JSON])
        val options = appWidgetManager.getAppWidgetOptions(widgetId)
        val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
        val minWidthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 180)
        val isCompact = minHeight in 1 until COMPACT_HEIGHT_DP

        val views = if (rows.isEmpty()) {
            renderEmpty(context)
        } else {
            renderTable(context, prefs, rows, isCompact, minWidthDp)
        }
        appWidgetManager.updateAppWidget(widgetId, views)
    }

    private fun renderEmpty(context: Context): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_budgets_empty)
        views.setTextViewText(R.id.tv_budgets_empty_subtitle, "Create a budget in the app to see progress here.")
        return views
    }

    private fun renderTable(
        context: Context,
        prefs: Preferences,
        rows: List<BudgetWidgetRow>,
        isCompact: Boolean,
        minWidthDp: Int
    ): RemoteViews {
        val layout = if (isCompact) R.layout.widget_budgets_compact else R.layout.widget_budgets_table
        val views = RemoteViews(context.packageName, layout)

        val periodLabel = prefs[BudgetsWidgetKeys.PERIOD_LABEL] ?: ""
        views.setTextViewText(R.id.tv_budgets_header, "Budgets · $periodLabel")

        val showComparison = prefs[BudgetsWidgetKeys.SHOW_COMPARISON] != "false"
        views.setViewVisibility(R.id.tv_budgets_header_comparison, if (showComparison) View.VISIBLE else View.GONE)

        val maxRows = if (isCompact) 2 else 3
        val rowIds = if (isCompact) {
            listOf(
                BudgetRowViewIds(R.id.ll_budget_row1, R.id.tv_budget_row1_name, R.id.tv_budget_row1_status, R.id.v_budget_row1_track, R.id.v_budget_row1_fill, -1, -1),
                BudgetRowViewIds(R.id.ll_budget_row2, R.id.tv_budget_row2_name, R.id.tv_budget_row2_status, R.id.v_budget_row2_track, R.id.v_budget_row2_fill, -1, -1)
            )
        } else {
            listOf(
                BudgetRowViewIds(R.id.ll_budget_row1, R.id.tv_budget_row1_name, R.id.tv_budget_row1_status, R.id.v_budget_row1_track, R.id.v_budget_row1_fill, R.id.tv_budget_row1_sub, R.id.tv_budget_row1_comparison),
                BudgetRowViewIds(R.id.ll_budget_row2, R.id.tv_budget_row2_name, R.id.tv_budget_row2_status, R.id.v_budget_row2_track, R.id.v_budget_row2_fill, R.id.tv_budget_row2_sub, R.id.tv_budget_row2_comparison),
                BudgetRowViewIds(R.id.ll_budget_row3, R.id.tv_budget_row3_name, R.id.tv_budget_row3_status, R.id.v_budget_row3_track, R.id.v_budget_row3_fill, R.id.tv_budget_row3_sub, R.id.tv_budget_row3_comparison)
            )
        }

        rowIds.forEachIndexed { index, ids ->
            val row = rows.getOrNull(index)?.takeIf { index < maxRows }
            if (row == null) {
                views.setViewVisibility(ids.container, View.GONE)
                return@forEachIndexed
            }
            views.setViewVisibility(ids.container, View.VISIBLE)
            views.setTextViewText(ids.name, row.name)

            val overdueColor = Color.parseColor("#A12A20")
            val warningColor = Color.parseColor("#8A5A0B")
            val mutedColor = Color.parseColor("#4C5C59")
            val tealColor = Color.parseColor("#1F6F66")

            when (row.alertLevel) {
                "EXCEEDED" -> {
                    views.setTextViewText(ids.statusRight, "Over by ${row.spentFormatted}")
                    views.setTextColor(ids.statusRight, overdueColor)
                    views.setInt(ids.barFill, "setBackgroundColor", Color.parseColor("#B3261E"))
                }
                "WARNING" -> {
                    views.setTextViewText(ids.statusRight, "${row.percentage.toInt()}% · ${row.remainingFormatted} left")
                    views.setTextColor(ids.statusRight, warningColor)
                    views.setInt(ids.barFill, "setBackgroundColor", Color.parseColor("#B7791F"))
                }
                else -> {
                    views.setTextViewText(ids.statusRight, "${row.remainingFormatted} left")
                    views.setTextColor(ids.statusRight, mutedColor)
                    views.setInt(ids.barFill, "setBackgroundColor", tealColor)
                }
            }

            // Progress bar fill width: RemoteViews has no percentage-width primitive, so the
            // fill View's width is set explicitly in dp (API 31+ setViewLayoutWidth) against the
            // widget's own live minWidth (from OPTION_APPWIDGET_MIN_WIDTH, same source
            // CashFlowRemoteViewsRenderer already reads MIN_HEIGHT from for its own tiering).
            // Pre-31 devices fall back to a full-width track fill — a graceful degradation, not
            // a missing feature, since minSdk 26 still needs to render *something* here.
            val pct = row.percentage.coerceIn(0f, 100f)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val trackWidthDp = (minWidthDp - 32).coerceAtLeast(60)
                val fillWidthDp = trackWidthDp * (pct / 100f)
                views.setViewLayoutWidth(ids.barFill, fillWidthDp, android.util.TypedValue.COMPLEX_UNIT_DIP)
            }

            if (ids.subLine >= 0) {
                val rolloverSuffix = if (row.hasRollover) " incl. ${row.rolloverAmountFormatted} rollover" else ""
                views.setTextViewText(ids.subLine, "${row.spentFormatted} / ${row.limitFormatted}$rolloverSuffix")
            }
            if (ids.comparison >= 0) {
                val comparison = row.comparisonText
                if (comparison != null) {
                    val arrow = if (comparison.startsWith("+")) "▲" else "▼"
                    views.setTextViewText(ids.comparison, "$arrow $comparison")
                    views.setViewVisibility(ids.comparison, View.VISIBLE)
                } else {
                    views.setViewVisibility(ids.comparison, View.GONE)
                }
            }
        }

        return views
    }
}
