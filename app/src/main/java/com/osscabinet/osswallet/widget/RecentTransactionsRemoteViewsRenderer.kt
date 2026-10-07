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
    val TRANSACTIONS_JSON = stringPreferencesKey("rt_transactions_json")
    val PERIOD_LABEL = stringPreferencesKey("rt_period_label")
    val PRIMARY_DISPLAY = stringPreferencesKey("rt_primary_display")
}

private data class RecentTransactionsRowViewIds(
    val container: Int,
    val icon: Int,
    val initial: Int,
    val title: Int,
    val subtitle: Int,
    val amount: Int,
    val time: Int,
    val precedingDivider: Int?
)

/**
 * Builds and pushes the RecentTransactions widget's [RemoteViews] directly — the raw-RemoteViews
 * counterpart to what RecentTransactionsGlanceWidget's @Composable content used to do.
 *
 * Row count follows the old Glance widget's own validated tiering: 3 rows below 200dp tall,
 * 5 rows at 200dp+ — recent_transactions_widget_info.xml has no maxResizeHeight, so this is
 * genuinely reachable. Below [HEADER_HEIGHT_DP] the "RECENT ACTIVITIES" header is hidden: at
 * the widget's true 110dp minimum, the header plus even 2 rows already exceeded the available
 * content height, so the 2nd row was getting squeezed against the bottom edge despite the
 * code claiming 3 rows fit. Still simplified vs the old Glance layout: no width-based detail
 * variants, no account name, and a solid-color initial-letter badge instead of a tinted bitmap
 * render of the actual category icon (there's no equivalent to Compose's CanvasDrawScope icon
 * rendering outside a Composition without a much heavier pipeline).
 */
object RecentTransactionsRemoteViewsRenderer {

    private const val HEADER_HEIGHT_DP = 150
    private const val TALL_HEIGHT_DP = 200

    private val ROW_VIEW_IDS = listOf(
        RecentTransactionsRowViewIds(
            container = R.id.ll_rt_row1, icon = R.id.iv_rt_row1_icon, initial = R.id.tv_rt_row1_initial,
            title = R.id.tv_rt_row1_title, subtitle = R.id.tv_rt_row1_subtitle,
            amount = R.id.tv_rt_row1_amount, time = R.id.tv_rt_row1_time, precedingDivider = null
        ),
        RecentTransactionsRowViewIds(
            container = R.id.ll_rt_row2, icon = R.id.iv_rt_row2_icon, initial = R.id.tv_rt_row2_initial,
            title = R.id.tv_rt_row2_title, subtitle = R.id.tv_rt_row2_subtitle,
            amount = R.id.tv_rt_row2_amount, time = R.id.tv_rt_row2_time, precedingDivider = R.id.v_rt_divider1
        ),
        RecentTransactionsRowViewIds(
            container = R.id.ll_rt_row3, icon = R.id.iv_rt_row3_icon, initial = R.id.tv_rt_row3_initial,
            title = R.id.tv_rt_row3_title, subtitle = R.id.tv_rt_row3_subtitle,
            amount = R.id.tv_rt_row3_amount, time = R.id.tv_rt_row3_time, precedingDivider = R.id.v_rt_divider2
        ),
        RecentTransactionsRowViewIds(
            container = R.id.ll_rt_row4, icon = R.id.iv_rt_row4_icon, initial = R.id.tv_rt_row4_initial,
            title = R.id.tv_rt_row4_title, subtitle = R.id.tv_rt_row4_subtitle,
            amount = R.id.tv_rt_row4_amount, time = R.id.tv_rt_row4_time, precedingDivider = R.id.v_rt_divider3
        ),
        RecentTransactionsRowViewIds(
            container = R.id.ll_rt_row5, icon = R.id.iv_rt_row5_icon, initial = R.id.tv_rt_row5_initial,
            title = R.id.tv_rt_row5_title, subtitle = R.id.tv_rt_row5_subtitle,
            amount = R.id.tv_rt_row5_amount, time = R.id.tv_rt_row5_time, precedingDivider = R.id.v_rt_divider4
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

        val options = appWidgetManager.getAppWidgetOptions(widgetId)
        val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
        val maxRows = if (minHeight >= TALL_HEIGHT_DP) 5 else 3
        val showHeader = minHeight >= HEADER_HEIGHT_DP

        val views = RemoteViews(context.packageName, R.layout.widget_recent_transactions_list)

        views.setViewVisibility(R.id.ll_rt_header, if (showHeader) View.VISIBLE else View.GONE)
        val periodLabel = prefs[RecentTransactionsWidgetKeys.PERIOD_LABEL]
        views.setViewVisibility(R.id.tv_rt_period, if (periodLabel.isNullOrBlank()) View.GONE else View.VISIBLE)
        views.setTextViewText(R.id.tv_rt_period, periodLabel?.uppercase() ?: "")

        val primaryDisplay = prefs[RecentTransactionsWidgetKeys.PRIMARY_DISPLAY] ?: "CATEGORY_FIRST"
        val transactions = try {
            Json.decodeFromString<List<WidgetTransaction>>(
                prefs[RecentTransactionsWidgetKeys.TRANSACTIONS_JSON] ?: "[]"
            )
        } catch (_: Exception) {
            emptyList()
        }

        views.setViewVisibility(R.id.tv_rt_empty, if (transactions.isEmpty()) View.VISIBLE else View.GONE)

        val incomeColor = ContextCompat.getColor(context, R.color.widget_positive)
        val expenseColor = ContextCompat.getColor(context, R.color.widget_negative)
        val showNoteFirst = primaryDisplay == "NOTE_FIRST"

        ROW_VIEW_IDS.forEachIndexed { index, ids ->
            val tx = transactions.getOrNull(index)?.takeIf { index < maxRows }
            if (tx == null) {
                views.setViewVisibility(ids.container, View.GONE)
                ids.precedingDivider?.let { views.setViewVisibility(it, View.GONE) }
                return@forEachIndexed
            }
            views.setViewVisibility(ids.container, View.VISIBLE)
            ids.precedingDivider?.let { views.setViewVisibility(it, View.VISIBLE) }

            val titleText = if (showNoteFirst && tx.note.isNotBlank()) tx.note else tx.category
            val subtitleText = if (showNoteFirst || tx.note.isBlank()) tx.category else tx.note

            views.setTextViewText(ids.title, titleText)
            views.setTextViewText(ids.subtitle, subtitleText)
            views.setTextViewText(ids.time, tx.timestamp)

            val isIncome = tx.type == "income"
            val amountColor = if (isIncome) incomeColor else expenseColor
            val amountPrefix = if (isIncome) "+" else "-"
            views.setTextViewText(ids.amount, amountPrefix + tx.amount)
            views.setTextColor(ids.amount, amountColor)

            val initial = titleText.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
            views.setTextViewText(ids.initial, initial)

            val badgeColor = tx.categoryColor ?: NEUTRAL_BADGE_COLOR
            views.setInt(ids.icon, "setColorFilter", badgeColor)
        }

        appWidgetManager.updateAppWidget(widgetId, views)
    }
}
