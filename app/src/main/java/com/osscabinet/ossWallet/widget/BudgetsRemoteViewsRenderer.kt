package com.osscabinet.ossWallet.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import androidx.datastore.preferences.core.Preferences
import com.osscabinet.ossWallet.MainActivity
import com.osscabinet.ossWallet.R
import com.osscabinet.ossWallet.domain.card.CardType
import com.osscabinet.ossWallet.domain.model.BudgetPaceStatus
import kotlinx.serialization.json.Json

/**
 * Builds and pushes the Budgets widget's [RemoteViews]: the shared header shell (with a status
 * chip in its extra slot) and a scrolling ListView of budgets, most urgent first, supplied by
 * [BudgetsRemoteViewsService]. Each row opens that budget.
 *
 * Progress is drawn with a ProgressBar rather than a resized View: setViewLayoutWidth is API 31+
 * and minSdk is 26. A ProgressBar's colour comes from its drawable, so each row carries three
 * bars (ok / warning / exceeded) and shows one. The pace tick is each bar's secondary progress.
 */
object BudgetsRemoteViewsRenderer {

    fun render(
        context: Context,
        prefs: Preferences,
        widgetId: Int,
        appWidgetManager: AppWidgetManager
    ) {
        if (prefs[BudgetsWidgetKeys.IS_LOADED] != "true") {
            appWidgetManager.updateAppWidget(widgetId, RemoteViews(context.packageName, R.layout.widget_budgets_loading))
            return
        }

        val theme = prefs[BudgetsWidgetKeys.THEME]?.let { runCatching { WidgetTheme.valueOf(it) }.getOrNull() }
            ?: WidgetTheme.SYSTEM
        val palette = WidgetColors.resolve(theme)
        val overCount = prefs[BudgetsWidgetKeys.OVER_COUNT]?.toIntOrNull() ?: 0
        val nearCount = prefs[BudgetsWidgetKeys.NEAR_COUNT]?.toIntOrNull() ?: 0

        val views = RemoteViews(context.packageName, WidgetColors.layoutFor(palette, R.layout.widget_budgets_main_t0, R.layout.widget_budgets_main_t1, R.layout.widget_budgets_main_t2))
        views.setTextViewText(R.id.tv_widget_header_title, "BUDGETS")
        WidgetHeader.bindIcon(context, views, CardType.BUDGETS, palette)
        WidgetHeader.applyTheme(views, palette, prefs[BudgetsWidgetKeys.GLASS] == "true")
        views.setThemedTextColor(R.id.tv_budgets_empty_text, palette) { it.onSurfaceVariant }

        WidgetHeader.clearExtras(views)
        // The chip describes the budgets, so it waits for them: an empty list shows only the empty text.
        val hasBudgets = (prefs[BudgetsWidgetKeys.COUNT]?.toIntOrNull() ?: 0) > 0
        if (hasBudgets) {
            if (overCount > 0) WidgetHeader.addChip(context, views, "$overCount OVER", WidgetHeader.ChipStyle.NEGATIVE, palette)
            if (nearCount > 0) WidgetHeader.addChip(context, views, "$nearCount NEAR", WidgetHeader.ChipStyle.WARNING, palette)
            if (overCount == 0 && nearCount == 0) {
                WidgetHeader.addChip(context, views, "ON TRACK", WidgetHeader.ChipStyle.NEUTRAL, palette)
            }
        }

        views.setOnClickPendingIntent(
            R.id.iv_widget_header_settings,
            PendingIntent.getActivity(
                context, widgetId * 10,
                Intent(context, BudgetsWidgetConfigActivity::class.java)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
        // The title opens the whole Budgets list, like "VIEW ALL" on Recent Activities.
        views.setOnClickPendingIntent(
            R.id.tv_widget_header_title,
            PendingIntent.getActivity(
                context, widgetId * 10 + 1,
                Intent(context, MainActivity::class.java)
                    .putExtra(MainActivity.EXTRA_WIDGET_NAV, "budgets")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
        // Each row's fill-in Intent (BudgetsRemoteViewsFactory) adds the budget id to this template.
        views.setPendingIntentTemplate(
            R.id.lv_budgets_list,
            PendingIntent.getActivity(
                context, widgetId * 10 + 2,
                Intent(context, MainActivity::class.java)
                    .putExtra(MainActivity.EXTRA_WIDGET_NAV, "budget_detail")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
        )

        val adapterIntent = Intent(context, BudgetsRemoteViewsService::class.java)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            .apply { data = Uri.parse("remoteviews://budgets/$widgetId?theme=${WidgetColors.themeKey(theme)}") }
        views.setRemoteAdapter(R.id.lv_budgets_list, adapterIntent)
        views.setEmptyView(R.id.lv_budgets_list, R.id.tv_budgets_empty_text)

        appWidgetManager.updateAppWidget(widgetId, views)
        // setRemoteAdapter alone does not re-run the factory's onDataSetChanged().
        appWidgetManager.notifyAppWidgetViewDataChanged(widgetId, R.id.lv_budgets_list)
    }

    fun buildRow(context: Context, row: BudgetWidgetRow, palette: WidgetPalette?): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_budgets_row)
        views.setTextViewText(R.id.tv_budget_name, row.name)

        val pace = runCatching { BudgetPaceStatus.valueOf(row.pace) }.getOrDefault(BudgetPaceStatus.ON_TRACK)
        val over = pace == BudgetPaceStatus.OVER
        val warn = pace == BudgetPaceStatus.AHEAD_OF_PACE || pace == BudgetPaceStatus.NEAR_LIMIT

        // The amount you act on: what is left, or by how much you are over.
        views.setTextViewText(R.id.tv_budget_amount, if (over) row.overByFormatted else row.remainingFormatted)
        views.setTextViewText(R.id.tv_budget_unit, if (over) "OVER" else "LEFT")
        views.setThemedTextColor(R.id.tv_budget_amount, palette) {
            when {
                over -> it.negative
                warn -> it.warning
                else -> it.onSurface
            }
        }

        // Exactly one bar shows: this row's state, in the variant that matches the theme.
        val variant = when (palette) {
            null -> 0
            WidgetColors.LIGHT -> 1
            else -> 2
        }
        val bars = arrayOf(
            intArrayOf(R.id.pb_budget_ok, R.id.pb_budget_ok_light, R.id.pb_budget_ok_dark),
            intArrayOf(R.id.pb_budget_warn, R.id.pb_budget_warn_light, R.id.pb_budget_warn_dark),
            intArrayOf(R.id.pb_budget_over, R.id.pb_budget_over_light, R.id.pb_budget_over_dark)
        )
        val shownState = if (over) 2 else if (warn) 1 else 0
        val progress = row.percentage.toInt().coerceIn(0, 100)
        val pacePercent = row.elapsedPercent ?: 0
        bars.forEachIndexed { state, ids ->
            ids.forEachIndexed { v, bar ->
                val shown = state == shownState && v == variant
                views.setViewVisibility(bar, if (shown) View.VISIBLE else View.GONE)
                if (shown) {
                    views.setProgressBar(bar, 100, progress, false)
                    // The pace tick. 0 draws nothing, which is what an All Time budget wants.
                    views.setInt(bar, "setSecondaryProgress", pacePercent)
                }
            }
        }

        val rollover = if (row.hasRollover) " +${row.rolloverAmountFormatted}" else ""
        views.setTextViewText(R.id.tv_budget_sub, "${row.spentFormatted} of ${row.limitFormatted}$rollover")
        val meta = if (pace == BudgetPaceStatus.AHEAD_OF_PACE) "ahead of pace" else row.periodLabel
        views.setTextViewText(R.id.tv_budget_meta, "${row.percentage.toInt().coerceAtLeast(0)}% · $meta")

        views.setThemedTextColor(R.id.tv_budget_name, palette) { it.onSurface }
        views.setThemedTextColor(R.id.tv_budget_unit, palette) { it.onSurfaceVariant }
        views.setThemedTextColor(R.id.tv_budget_sub, palette) { it.onSurfaceVariant }
        views.setThemedTextColor(R.id.tv_budget_meta, palette) { it.onSurfaceVariant }
        return views
    }
}
