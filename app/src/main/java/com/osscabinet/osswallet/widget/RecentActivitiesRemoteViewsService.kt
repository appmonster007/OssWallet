package com.osscabinet.ossWallet.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import androidx.core.content.ContextCompat
import com.osscabinet.ossWallet.R
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.ui.util.CurrencyReprUi
import com.osscabinet.ossWallet.ui.util.TimeUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * Backs the Recent Activities widget's ListView. Unlike every other widget here (a single
 * RemoteViews tree pushed by a renderer reading pre-fetched prefs), a ListView's rows are
 * supplied on demand by this service's [RemoteViewsFactory] — the system binds to it and calls
 * [RemoteViewsFactory.getViewAt] per visible row, which is what makes true scrolling possible in
 * a widget (a plain RemoteViews tree can't scroll or lazily render). [RecentActivitiesWidgetFetcher]
 * only prepares the header (title); row data is queried here, directly, each time the system
 * asks for it.
 */
class RecentActivitiesRemoteViewsService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        val widgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
        return RecentActivitiesRemoteViewsFactory(applicationContext, widgetId)
    }
}

private class RecentActivitiesRemoteViewsFactory(
    private val context: Context,
    private val widgetId: Int
) : RemoteViewsService.RemoteViewsFactory {

    private var rows: List<RowData> = emptyList()
    private var palette: WidgetPalette? = null

    private data class RowData(
        val transactionId: Long,
        val title: String,
        val initial: String,
        val subtitle: String,
        val timeText: String,
        val amountText: String,
        val isPositive: Boolean,
        val categoryColor: Int?
    )

    override fun onCreate() = Unit

    /**
     * Runs synchronously on a background binder thread — RemoteViewsFactory is Android's own
     * (pre-coroutine) contract for this, so a blocking DB round trip here is the documented,
     * expected shape, not a shortcut.
     */
    override fun onDataSetChanged() {
        rows = runBlocking {
            runCatching {
                val config = RecentActivitiesWidgetPrefsStore(context).loadSafe(widgetId)
                palette = WidgetColors.resolve(config.theme)
                val common = WidgetDataLoader.loadCommon(context)
                val transactions = common.databaseQueryService.observeFilteredTransactions(
                    profileId = common.profileId,
                    filter = config.filter,
                    limit = 50
                ).first().sortedWith(compareByDescending<com.osscabinet.ossWallet.domain.model.Transaction> { it.timestamp }.thenByDescending { it.id })

                val accountsById = common.accountRepository.getAllAccountsSync(common.profileId)
                    .associateBy { it.id }
                val categoriesById = common.categoryRepository.getAllCategoriesSync()
                    .associateBy { it.id }

                val formatter = CurrencyReprUi.CurrencyNumberFormatter()
                val now = System.currentTimeMillis()

                transactions.map { tx ->
                    val title = tx.name.ifBlank { tx.note.ifBlank { tx.transactionType.displayName } }
                    // Account + category only — time moved out to its own line under the
                    // amount on the right (design pick: canvas "Row B" with time relocated).
                    val subtitleParts = buildList {
                        if (config.showAccount) accountsById[tx.accountId]?.name?.let { add(it) }
                        if (config.showCategory) categoriesById[tx.categoryId]?.name?.let { add(it) }
                    }
                    val isPositive = tx.transactionType == TransactionType.INCOME ||
                        tx.transactionType == TransactionType.TRANSFER_INFLOW
                    RowData(
                        transactionId = tx.id,
                        title = title,
                        initial = title.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                        subtitle = subtitleParts.joinToString(" · "),
                        timeText = if (config.showTimestamp) TimeUtils.formatRelativeDate(tx.timestamp, now) else "",
                        amountText = formatter.format(tx.amount, tx.currency),
                        isPositive = isPositive,
                        categoryColor = categoriesById[tx.categoryId]?.colorHex
                    )
                }
            }.getOrDefault(emptyList())
        }
    }

    override fun onDestroy() {
        rows = emptyList()
    }

    override fun getCount(): Int = rows.size

    override fun getViewAt(position: Int): RemoteViews {
        val row = rows[position]
        val views = RemoteViews(context.packageName, R.layout.widget_recent_activities_item)

        views.setTextViewText(R.id.tv_ra_item_title, row.title)
        views.setTextViewText(R.id.tv_ra_item_subtitle, row.subtitle)
        views.setViewVisibility(
            R.id.tv_ra_item_subtitle,
            if (row.subtitle.isEmpty()) android.view.View.GONE else android.view.View.VISIBLE
        )
        views.setTextViewText(R.id.tv_ra_item_amount, row.amountText)
        views.setTextViewText(R.id.tv_ra_item_time, row.timeText)
        views.setViewVisibility(
            R.id.tv_ra_item_time,
            if (row.timeText.isEmpty()) android.view.View.GONE else android.view.View.VISIBLE
        )

        val palette = this.palette
        val color = if (palette != null) {
            if (row.isPositive) palette.positive else palette.negative
        } else {
            ContextCompat.getColor(context, if (row.isPositive) R.color.widget_positive else R.color.widget_negative)
        }
        views.setTextColor(R.id.tv_ra_item_amount, color)
        if (palette != null) {
            views.setTextColor(R.id.tv_ra_item_title, palette.onSurface)
            views.setTextColor(R.id.tv_ra_item_subtitle, palette.onSurfaceVariant)
            views.setTextColor(R.id.tv_ra_item_time, palette.onSurfaceVariant)
            views.setInt(R.id.v_ra_item_divider, "setBackgroundColor", palette.outline)
        }

        // TextView has no setColorFilter (ImageView/Drawable only), so swap the pre-tinted
        // drawable by sign instead — same pattern as cell3's highlight toggle in NetWorthRenderer.
        views.setTextViewText(R.id.tv_ra_item_indicator, row.initial)
        val iconColor = row.categoryColor ?: color
        views.setTextColor(R.id.tv_ra_item_indicator, iconColor)
        if (row.categoryColor != null) {
            views.setInt(R.id.iv_ra_item_badge_bg, "setImageResource", R.drawable.widget_oval_bg)
            val containerColor = (row.categoryColor and 0x00FFFFFF) or 0x28000000
            views.setInt(R.id.iv_ra_item_badge_bg, "setColorFilter", containerColor)
        } else {
            val ovalBg = if (palette != null) {
                if (row.isPositive) palette.ovalPositiveBg else palette.ovalNegativeBg
            } else {
                if (row.isPositive) R.drawable.widget_oval_positive_bg else R.drawable.widget_oval_negative_bg
            }
            views.setInt(R.id.iv_ra_item_badge_bg, "setImageResource", ovalBg)
            views.setInt(R.id.iv_ra_item_badge_bg, "setColorFilter", 0)
        }

        // Completes the ListView's setPendingIntentTemplate (see
        // RecentActivitiesRemoteViewsRenderer) — tapping this row launches MainActivity with
        // this transaction's id, same as tapping a row in RecentActivitiesCard opens it.
        val fillInIntent = Intent().putExtra(
            RecentActivitiesRemoteViewsRenderer.EXTRA_TRANSACTION_ID,
            row.transactionId
        )
        views.setOnClickFillInIntent(R.id.ll_ra_item_root, fillInIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = rows[position].transactionId
    override fun hasStableIds(): Boolean = true
}
