package com.osscabinet.ossWallet.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.SizeF
import android.view.View
import android.widget.RemoteViews
import androidx.datastore.preferences.core.Preferences
import com.osscabinet.ossWallet.MainActivity
import com.osscabinet.ossWallet.R
import com.osscabinet.ossWallet.domain.model.BudgetPaceStatus
import kotlinx.serialization.json.Json

/**
 * Builds and pushes the Budgets widget's [RemoteViews]: the shared header shell (with a status
 * chip in its extra slot) plus one [R.layout.widget_budgets_row] per budget, most urgent first.
 * The number of rows follows the widget's live height, up to [BudgetsWidgetConfig.MAX_ROWS]: on
 * API 31+ one RemoteViews per row count is handed to the host, which picks the one that fits;
 * older devices use the reported minimum height.
 *
 * Progress is drawn with a ProgressBar rather than a resized View: setViewLayoutWidth is API 31+
 * and minSdk is 26. A ProgressBar's colour comes from its drawable, so each row carries three
 * bars (ok / warning / exceeded) and shows one. The pace tick is each bar's secondary progress.
 */
object BudgetsRemoteViewsRenderer {

    /** Height the header shell takes, and what one row takes (10dp gap included), in dp. */
    private const val HEADER_HEIGHT_DP = 30
    private const val ROW_HEIGHT_DP = 56
    /** A launcher's reported height runs slightly under the real one; the 4x2 floor is 110dp. */
    private const val ROW_SLACK_DP = 12

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
        val rows = prefs[BudgetsWidgetKeys.ROWS_JSON]
            ?.let { runCatching { Json.decodeFromString<List<BudgetWidgetRow>>(it) }.getOrNull() }
            .orEmpty()
        val overCount = prefs[BudgetsWidgetKeys.OVER_COUNT]?.toIntOrNull() ?: 0
        val nearCount = prefs[BudgetsWidgetKeys.NEAR_COUNT]?.toIntOrNull() ?: 0

        val views = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // The host picks the largest entry that fits the widget's live size on every layout
            // pass. OPTION_APPWIDGET_MIN_HEIGHT under-reports on some launchers (92dp for a
            // 172dp widget on the emulator), so it can't be used to count rows.
            RemoteViews(
                (1..BudgetsWidgetConfig.MAX_ROWS).associate { n ->
                    SizeF(0f, minHeightForRows(n)) to buildViews(context, widgetId, rows, n, overCount, nearCount, palette)
                }
            )
        } else {
            val minHeightDp = appWidgetManager.getAppWidgetOptions(widgetId)
                .getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
            val n = (1..BudgetsWidgetConfig.MAX_ROWS).lastOrNull { minHeightDp >= minHeightForRows(it) } ?: 1
            buildViews(context, widgetId, rows, n, overCount, nearCount, palette)
        }
        appWidgetManager.updateAppWidget(widgetId, views)
    }

    /** Smallest widget height, in dp, at which [rows] rows fit; one row is the floor. */
    private fun minHeightForRows(rows: Int): Float =
        if (rows <= 1) 0f else (HEADER_HEIGHT_DP + ROW_HEIGHT_DP * rows - ROW_SLACK_DP).toFloat()

    private fun buildViews(
        context: Context,
        widgetId: Int,
        rows: List<BudgetWidgetRow>,
        visibleRows: Int,
        overCount: Int,
        nearCount: Int,
        palette: WidgetPalette?
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_budgets_main)
        views.setTextViewText(R.id.tv_widget_header_title, "BUDGETS")
        if (palette != null) {
            views.setInt(R.id.ll_widget_root, "setBackgroundColor", palette.surface)
            views.setTextColor(R.id.tv_widget_header_title, palette.onSurfaceVariant)
            views.setInt(R.id.iv_widget_header_settings, "setColorFilter", palette.onSurfaceVariant)
            views.setInt(R.id.v_widget_header_divider, "setBackgroundColor", palette.outline)
            views.setTextColor(R.id.tv_budgets_empty_text, palette.onSurfaceVariant)
        }

        views.removeAllViews(R.id.ll_widget_header_extra)
        if (rows.isNotEmpty()) {
            if (overCount > 0) addChip(context, views, "$overCount OVER", R.drawable.widget_budget_chip_negative, R.color.widget_negative, palette?.negative)
            if (nearCount > 0) addChip(context, views, "$nearCount NEAR", R.drawable.widget_budget_chip_warning, R.color.widget_warning, palette?.warning)
            if (overCount == 0 && nearCount == 0) {
                addChip(context, views, "ON TRACK", R.drawable.widget_budget_chip_neutral, R.color.widget_on_surface_variant, palette?.onSurfaceVariant)
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

        val openBudgets = PendingIntent.getActivity(
            context, widgetId * 10 + 1,
            Intent(context, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_WIDGET_NAV, "budgets")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.ll_budgets_body, openBudgets)

        views.removeAllViews(R.id.ll_budgets_rows)
        if (rows.isEmpty()) {
            views.setViewVisibility(R.id.ll_budgets_rows, View.GONE)
            views.setViewVisibility(R.id.tv_budgets_empty_text, View.VISIBLE)
        } else {
            views.setViewVisibility(R.id.ll_budgets_rows, View.VISIBLE)
            views.setViewVisibility(R.id.tv_budgets_empty_text, View.GONE)
            rows.take(visibleRows).forEach { row ->
                views.addView(R.id.ll_budgets_rows, buildRow(context, row, palette))
            }
        }

        return views
    }

    private fun addChip(
        context: Context,
        parent: RemoteViews,
        text: String,
        backgroundRes: Int,
        textColorRes: Int,
        forcedTextColor: Int?
    ) {
        val chip = RemoteViews(context.packageName, R.layout.widget_budgets_chip)
        chip.setTextViewText(R.id.tv_budgets_chip, text)
        chip.setInt(R.id.tv_budgets_chip, "setBackgroundResource", backgroundRes)
        chip.setTextColor(R.id.tv_budgets_chip, forcedTextColor ?: context.getColor(textColorRes))
        parent.addView(R.id.ll_widget_header_extra, chip)
    }

    private fun buildRow(context: Context, row: BudgetWidgetRow, palette: WidgetPalette?): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_budgets_row)
        views.setTextViewText(R.id.tv_budget_name, row.name)

        val pace = runCatching { BudgetPaceStatus.valueOf(row.pace) }.getOrDefault(BudgetPaceStatus.ON_TRACK)
        val over = pace == BudgetPaceStatus.OVER
        val warn = pace == BudgetPaceStatus.AHEAD_OF_PACE || pace == BudgetPaceStatus.NEAR_LIMIT

        // The amount you act on: what is left, or by how much you are over.
        views.setTextViewText(R.id.tv_budget_amount, if (over) row.overByFormatted else row.remainingFormatted)
        views.setTextViewText(R.id.tv_budget_unit, if (over) "OVER" else "LEFT")
        views.setTextColor(
            R.id.tv_budget_amount,
            when {
                over -> palette?.negative ?: context.getColor(R.color.widget_negative)
                warn -> palette?.warning ?: context.getColor(R.color.widget_warning)
                else -> palette?.onSurface ?: context.getColor(R.color.widget_on_surface)
            }
        )

        val progress = row.percentage.toInt().coerceIn(0, 100)
        val pacePercent = row.elapsedPercent ?: 0
        views.setViewVisibility(R.id.pb_budget_ok, if (!over && !warn) View.VISIBLE else View.GONE)
        views.setViewVisibility(R.id.pb_budget_warn, if (warn) View.VISIBLE else View.GONE)
        views.setViewVisibility(R.id.pb_budget_over, if (over) View.VISIBLE else View.GONE)
        listOf(R.id.pb_budget_ok, R.id.pb_budget_warn, R.id.pb_budget_over).forEach { bar ->
            views.setProgressBar(bar, 100, progress, false)
            // The pace tick. 0 draws nothing, which is what an All Time budget wants.
            views.setInt(bar, "setSecondaryProgress", pacePercent)
        }

        val rollover = if (row.hasRollover) " +${row.rolloverAmountFormatted}" else ""
        views.setTextViewText(R.id.tv_budget_sub, "${row.spentFormatted} of ${row.limitFormatted}$rollover")
        val meta = if (pace == BudgetPaceStatus.AHEAD_OF_PACE) "ahead of pace" else row.periodLabel
        views.setTextViewText(R.id.tv_budget_meta, "${row.percentage.toInt().coerceAtLeast(0)}% · $meta")

        palette?.let {
            views.setTextColor(R.id.tv_budget_name, it.onSurface)
            views.setTextColor(R.id.tv_budget_unit, it.onSurfaceVariant)
            views.setTextColor(R.id.tv_budget_sub, it.onSurfaceVariant)
            views.setTextColor(R.id.tv_budget_meta, it.onSurfaceVariant)
        }
        return views
    }
}
