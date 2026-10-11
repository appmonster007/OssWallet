package com.osscabinet.ossWallet.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.osscabinet.ossWallet.R
import kotlinx.serialization.json.Json

/**
 * Preference keys written by [RecentTransactionsWidgetFetcher]. Plays the same role
 * [RecentTransactionsGlanceWidget.Keys] used to when this widget was Glance-based.
 */
object RecentTransactionsWidgetKeys {
    val IS_LOADED = stringPreferencesKey("rt_is_loaded")
    val ROWS_JSON = stringPreferencesKey("rt_rows_json")
    val PERIOD_LABEL = stringPreferencesKey("rt_period_label")
    val PRIMARY_DISPLAY = stringPreferencesKey("rt_primary_display")
}

private data class RecentTransactionsRowViewIds(
    val container: Int,
    val dateCap: Int,
    val icon: Int,
    val initial: Int,
    val title: Int,
    val pill: Int,
    val subtitle: Int,
    val amount: Int,
    val amountBase: Int,
    val precedingDivider: Int?
)

/**
 * Builds and pushes the Recent Activities widget's [RemoteViews] directly — the
 * raw-RemoteViews counterpart to what RecentTransactionsGlanceWidget's @Composable
 * content used to do.
 *
 * Three height tiers (see RECENT_ACTIVITIES target model, docs/card-data-summary.md):
 *  - compact (4x2, < [COMPACT_HEIGHT_DP]): single line per row — name + status/date + amount.
 *  - note-first (4x3, [COMPACT_HEIGHT_DP]..[HEADER_HEIGHT_DP)): note (falling back to
 *    categoryName) as line 1, "categoryName · timestamp · status" as line 2.
 *  - list (4x4, >= [HEADER_HEIGHT_DP]): full categoryName-first row — avatar, pill,
 *    account+time/planned/transfer subtitle, trailing amount + "≈ base". Row count still
 *    follows the old 3-rows-below-200dp / 5-rows-at-200dp+ tiering.
 *
 * Simplifications vs the old Glance version, carried over: a solid-color initial-letter
 * badge instead of a tinted bitmap render of the actual category icon (no equivalent to
 * Compose's CanvasDrawScope icon rendering outside a Composition without a much heavier
 * pipeline), and at most one priority pill per row (Overdue > Pending > first tag) rather
 * than a full tag chip row, since RemoteViews has no flexbox-style wrapping layout.
 */
object RecentTransactionsRemoteViewsRenderer {

    private const val COMPACT_HEIGHT_DP = 130
    private const val HEADER_HEIGHT_DP = 150
    private const val TALL_HEIGHT_DP = 200

    private val ROW_VIEW_IDS = listOf(
        RecentTransactionsRowViewIds(
            container = R.id.ll_rt_row1, dateCap = R.id.tv_rt_row1_datecap,
            icon = R.id.iv_rt_row1_icon, initial = R.id.tv_rt_row1_initial,
            title = R.id.tv_rt_row1_title, pill = R.id.tv_rt_row1_pill, subtitle = R.id.tv_rt_row1_subtitle,
            amount = R.id.tv_rt_row1_amount, amountBase = R.id.tv_rt_row1_amount_base, precedingDivider = null
        ),
        RecentTransactionsRowViewIds(
            container = R.id.ll_rt_row2, dateCap = R.id.tv_rt_row2_datecap,
            icon = R.id.iv_rt_row2_icon, initial = R.id.tv_rt_row2_initial,
            title = R.id.tv_rt_row2_title, pill = R.id.tv_rt_row2_pill, subtitle = R.id.tv_rt_row2_subtitle,
            amount = R.id.tv_rt_row2_amount, amountBase = R.id.tv_rt_row2_amount_base, precedingDivider = R.id.v_rt_divider1
        ),
        RecentTransactionsRowViewIds(
            container = R.id.ll_rt_row3, dateCap = R.id.tv_rt_row3_datecap,
            icon = R.id.iv_rt_row3_icon, initial = R.id.tv_rt_row3_initial,
            title = R.id.tv_rt_row3_title, pill = R.id.tv_rt_row3_pill, subtitle = R.id.tv_rt_row3_subtitle,
            amount = R.id.tv_rt_row3_amount, amountBase = R.id.tv_rt_row3_amount_base, precedingDivider = R.id.v_rt_divider2
        ),
        RecentTransactionsRowViewIds(
            container = R.id.ll_rt_row4, dateCap = R.id.tv_rt_row4_datecap,
            icon = R.id.iv_rt_row4_icon, initial = R.id.tv_rt_row4_initial,
            title = R.id.tv_rt_row4_title, pill = R.id.tv_rt_row4_pill, subtitle = R.id.tv_rt_row4_subtitle,
            amount = R.id.tv_rt_row4_amount, amountBase = R.id.tv_rt_row4_amount_base, precedingDivider = R.id.v_rt_divider3
        ),
        RecentTransactionsRowViewIds(
            container = R.id.ll_rt_row5, dateCap = R.id.tv_rt_row5_datecap,
            icon = R.id.iv_rt_row5_icon, initial = R.id.tv_rt_row5_initial,
            title = R.id.tv_rt_row5_title, pill = R.id.tv_rt_row5_pill, subtitle = R.id.tv_rt_row5_subtitle,
            amount = R.id.tv_rt_row5_amount, amountBase = R.id.tv_rt_row5_amount_base, precedingDivider = R.id.v_rt_divider4
        )
    )

    private const val NEUTRAL_BADGE_COLOR = 0xFF8A8370.toInt()

    fun render(
        context: Context,
        prefs: Preferences,
        widgetId: Int,
        appWidgetManager: AppWidgetManager
    ) {
        val isLoaded = prefs[RecentTransactionsWidgetKeys.IS_LOADED] == "true"
        if (!isLoaded) {
            val loading = RemoteViews(context.packageName, R.layout.widget_recent_transactions)
            appWidgetManager.updateAppWidget(widgetId, loading)
            return
        }

        val rows = try {
            Json.decodeFromString<List<RecentActivityWidgetRow>>(
                prefs[RecentTransactionsWidgetKeys.ROWS_JSON] ?: "[]"
            )
        } catch (_: Exception) {
            emptyList()
        }

        val options = appWidgetManager.getAppWidgetOptions(widgetId)
        val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)

        val views = when {
            minHeight in 1 until COMPACT_HEIGHT_DP -> renderCompact(context, rows)
            minHeight in COMPACT_HEIGHT_DP until HEADER_HEIGHT_DP -> renderNoteFirst(context, prefs, rows)
            else -> {
                val maxRows = if (minHeight >= TALL_HEIGHT_DP) 5 else 3
                renderList(context, prefs, rows, maxRows, showHeader = true)
            }
        }
        appWidgetManager.updateAppWidget(widgetId, views)
    }

    private fun isIncome(row: RecentActivityWidgetRow) = row.isIncome
    private fun isTransfer(row: RecentActivityWidgetRow) = row.linkedTransactionIds.isNotEmpty()

    private fun amountColor(context: Context, row: RecentActivityWidgetRow): Int = when {
        isTransfer(row) -> ContextCompat.getColor(context, R.color.widget_on_surface_variant)
        isIncome(row) -> ContextCompat.getColor(context, R.color.widget_positive)
        else -> ContextCompat.getColor(context, R.color.widget_negative)
    }

    /** Compact tier (4x2): name + status/date + amount only, up to 3 rows. */
    private fun renderCompact(context: Context, rows: List<RecentActivityWidgetRow>): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_recent_transactions_compact)
        views.setViewVisibility(R.id.tv_rtc_empty, if (rows.isEmpty()) View.VISIBLE else View.GONE)

        val ids = listOf(
            Triple(R.id.ll_rtc_row1, R.id.tv_rtc_row1_title, R.id.tv_rtc_row1_status) to R.id.tv_rtc_row1_amount,
            Triple(R.id.ll_rtc_row2, R.id.tv_rtc_row2_title, R.id.tv_rtc_row2_status) to R.id.tv_rtc_row2_amount,
            Triple(R.id.ll_rtc_row3, R.id.tv_rtc_row3_title, R.id.tv_rtc_row3_status) to R.id.tv_rtc_row3_amount
        )
        ids.forEachIndexed { index, (triple, amountId) ->
            val (containerId, titleId, statusId) = triple
            val row = rows.getOrNull(index)
            if (row == null) {
                views.setViewVisibility(containerId, View.GONE)
                return@forEachIndexed
            }
            views.setViewVisibility(containerId, View.VISIBLE)
            views.setTextViewText(titleId, row.categoryName)
            val statusText = when {
                row.kind == "planned" && row.isOverdue -> "Overdue"
                row.kind == "planned" -> row.timestampFormatted
                else -> row.timestampFormatted
            }
            views.setTextViewText(statusId, statusText)
            val prefix = if (isIncome(row) && !isTransfer(row)) "+" else "-"
            views.setTextViewText(amountId, prefix + row.amountFormatted)
            views.setTextColor(amountId, amountColor(context, row))
        }
        return views
    }

    /** Note-first tier (4x3): line1 = note (fallback categoryName), line2 = categoryName · time · status. */
    private fun renderNoteFirst(
        context: Context,
        prefs: Preferences,
        rows: List<RecentActivityWidgetRow>
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_recent_transactions_list)
        views.setViewVisibility(R.id.ll_rt_header, View.VISIBLE)
        val periodLabel = prefs[RecentTransactionsWidgetKeys.PERIOD_LABEL]
        views.setViewVisibility(R.id.tv_rt_period, if (periodLabel.isNullOrBlank()) View.GONE else View.VISIBLE)
        views.setTextViewText(R.id.tv_rt_period, periodLabel?.uppercase() ?: "")
        views.setViewVisibility(R.id.tv_rt_empty, if (rows.isEmpty()) View.VISIBLE else View.GONE)

        val incomeColor = ContextCompat.getColor(context, R.color.widget_positive)
        val expenseColor = ContextCompat.getColor(context, R.color.widget_negative)

        var lastDateLabel: String? = null
        ROW_VIEW_IDS.forEachIndexed { index, ids ->
            val row = rows.getOrNull(index)?.takeIf { index < 3 }
            if (row == null) {
                hideRow(views, ids)
                return@forEachIndexed
            }
            showRow(views, ids)
            bindDateCap(context, views, ids, row, lastDateLabel)
            lastDateLabel = row.dateLabel

            views.setViewVisibility(ids.pill, View.GONE)

            val title = row.note?.takeIf { it.isNotBlank() } ?: row.categoryName
            val statusLabel = row.status?.let { statusLabel(it) } ?: if (row.kind == "planned") "Planned" else ""
            views.setTextViewText(ids.title, title)
            views.setTextViewText(ids.subtitle, "${row.categoryName} · ${row.timestampFormatted}${if (statusLabel.isNotBlank()) " · $statusLabel" else ""}")

            bindAvatar(context, views, ids, row)
            bindAmount(context, views, ids, row, incomeColor, expenseColor)
        }
        return views
    }

    /** List tier (4x4): full categoryName-first row. */
    private fun renderList(
        context: Context,
        prefs: Preferences,
        rows: List<RecentActivityWidgetRow>,
        maxRows: Int,
        showHeader: Boolean
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_recent_transactions_list)

        views.setViewVisibility(R.id.ll_rt_header, if (showHeader) View.VISIBLE else View.GONE)
        val periodLabel = prefs[RecentTransactionsWidgetKeys.PERIOD_LABEL]
        views.setViewVisibility(R.id.tv_rt_period, if (periodLabel.isNullOrBlank()) View.GONE else View.VISIBLE)
        views.setTextViewText(R.id.tv_rt_period, periodLabel?.uppercase() ?: "")
        views.setViewVisibility(R.id.tv_rt_empty, if (rows.isEmpty()) View.VISIBLE else View.GONE)

        val incomeColor = ContextCompat.getColor(context, R.color.widget_positive)
        val expenseColor = ContextCompat.getColor(context, R.color.widget_negative)
        val overdueColor = ContextCompat.getColor(context, R.color.widget_overdue_text)
        val pendingColor = ContextCompat.getColor(context, R.color.widget_pending_outline)
        val tagTextColor = ContextCompat.getColor(context, R.color.widget_text_muted)

        var lastDateLabel: String? = null
        ROW_VIEW_IDS.forEachIndexed { index, ids ->
            val row = rows.getOrNull(index)?.takeIf { index < maxRows }
            if (row == null) {
                hideRow(views, ids)
                return@forEachIndexed
            }
            showRow(views, ids)
            bindDateCap(context, views, ids, row, lastDateLabel)
            lastDateLabel = row.dateLabel

            views.setTextViewText(ids.title, row.categoryName)

            // One priority pill: Overdue (planned) > Pending status (confirmed) > first tag.
            when {
                row.kind == "planned" && row.isOverdue -> {
                    views.setViewVisibility(ids.pill, View.VISIBLE)
                    views.setTextViewText(ids.pill, "Overdue")
                    views.setTextColor(ids.pill, overdueColor)
                    views.setInt(ids.pill, "setBackgroundResource", R.drawable.widget_pill_overdue_bg)
                }
                row.kind == "confirmed" && row.status == "UNCLEARED" -> {
                    views.setViewVisibility(ids.pill, View.VISIBLE)
                    views.setTextViewText(ids.pill, "Pending")
                    views.setTextColor(ids.pill, pendingColor)
                    views.setInt(ids.pill, "setBackgroundResource", R.drawable.widget_pill_pending_bg)
                }
                row.tags.isNotEmpty() -> {
                    views.setViewVisibility(ids.pill, View.VISIBLE)
                    views.setTextViewText(ids.pill, row.tags.first().name)
                    views.setTextColor(ids.pill, tagTextColor)
                    views.setInt(ids.pill, "setBackgroundResource", R.drawable.widget_pill_tag_bg)
                }
                else -> views.setViewVisibility(ids.pill, View.GONE)
            }

            val subtitle = when {
                row.kind == "planned" -> "Planned · ${row.dateLabel} · ${row.accountName}"
                isTransfer(row) -> "Transfer · ${row.accountName} · ${row.timestampFormatted}"
                else -> "${row.accountName} · ${row.timestampFormatted}"
            }
            views.setTextViewText(ids.subtitle, subtitle)

            bindAvatar(context, views, ids, row)
            bindAmount(context, views, ids, row, incomeColor, expenseColor)
        }

        return views
    }

    private fun statusLabel(status: String): String = when (status) {
        "CLEARED" -> "Cleared"
        "RECONCILE" -> "Reconcile"
        "UNCLEARED" -> "Pending"
        else -> status
    }

    private fun hideRow(views: RemoteViews, ids: RecentTransactionsRowViewIds) {
        views.setViewVisibility(ids.container, View.GONE)
        views.setViewVisibility(ids.dateCap, View.GONE)
        ids.precedingDivider?.let { views.setViewVisibility(it, View.GONE) }
    }

    private fun showRow(views: RemoteViews, ids: RecentTransactionsRowViewIds) {
        views.setViewVisibility(ids.container, View.VISIBLE)
        ids.precedingDivider?.let { views.setViewVisibility(it, View.VISIBLE) }
    }

    private fun bindDateCap(
        context: Context,
        views: RemoteViews,
        ids: RecentTransactionsRowViewIds,
        row: RecentActivityWidgetRow,
        lastDateLabel: String?
    ) {
        val show = row.dateLabel.isNotBlank() && row.dateLabel != lastDateLabel
        views.setViewVisibility(ids.dateCap, if (show) View.VISIBLE else View.GONE)
        if (show) {
            views.setTextViewText(ids.dateCap, row.dateLabel.uppercase())
            views.setTextColor(
                ids.dateCap,
                if (row.isOverdueGroup) {
                    ContextCompat.getColor(context, R.color.widget_overdue_text)
                } else {
                    ContextCompat.getColor(context, R.color.widget_text_muted)
                }
            )
        }
    }

    private fun bindAvatar(context: Context, views: RemoteViews, ids: RecentTransactionsRowViewIds, row: RecentActivityWidgetRow) {
        val isOverdue = row.kind == "planned" && row.isOverdue
        views.setImageViewResource(
            ids.icon,
            if (isOverdue) R.drawable.widget_circle_badge_dashed else R.drawable.widget_circle_badge
        )
        val badgeColor = if (isOverdue) {
            ContextCompat.getColor(context, R.color.widget_overdue_text)
        } else {
            row.categoryColor.takeIf { it != 0 } ?: NEUTRAL_BADGE_COLOR
        }
        views.setInt(ids.icon, "setColorFilter", badgeColor)

        val initial = if (isTransfer(row)) {
            "⇄"
        } else {
            row.categoryName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
        }
        views.setTextViewText(ids.initial, initial)
    }

    private fun bindAmount(
        context: Context,
        views: RemoteViews,
        ids: RecentTransactionsRowViewIds,
        row: RecentActivityWidgetRow,
        incomeColor: Int,
        expenseColor: Int
    ) {
        val isIncomeRow = isIncome(row) && !isTransfer(row)
        val amountColor = when {
            isTransfer(row) -> ContextCompat.getColor(context, R.color.widget_text_muted)
            isIncomeRow -> incomeColor
            else -> expenseColor
        }
        val amountPrefix = if (isIncomeRow) "+" else "-"
        views.setTextViewText(ids.amount, amountPrefix + row.amountFormatted)
        views.setTextColor(ids.amount, amountColor)

        val base = row.amountInBaseFormatted
        views.setViewVisibility(ids.amountBase, if (base != null) View.VISIBLE else View.GONE)
        if (base != null) {
            views.setTextViewText(ids.amountBase, "≈ $base")
        }
    }
}
