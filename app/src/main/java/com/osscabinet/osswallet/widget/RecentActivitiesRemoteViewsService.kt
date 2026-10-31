package com.osscabinet.ossWallet.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.osscabinet.ossWallet.R
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.ui.components.getIconByName
import com.osscabinet.ossWallet.ui.util.CurrencyReprUi
import com.osscabinet.ossWallet.ui.util.TimeUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Backs the Recent Activities widget's ListView. Unlike every other widget here (a single
 * RemoteViews tree pushed by a renderer reading pre-fetched prefs), a ListView's rows are
 * supplied on demand by this service's [RemoteViewsFactory] — the system binds to it and calls
 * [RemoteViewsFactory.getViewAt] per visible row, which is what makes true scrolling possible in
 * a widget (a plain RemoteViews tree can't scroll or lazily render). Transactions are grouped by
 * calendar day: a date-header row (TODAY / YESTERDAY / "MON, OCT 20") is inserted ahead of each
 * day's first transaction, so the per-row time line only needs the time of day, not the date —
 * see [RecentActivitiesRemoteViewsFactory.groupLabel].
 */
class RecentActivitiesRemoteViewsService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        val widgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
        return RecentActivitiesRemoteViewsFactory(applicationContext, widgetId, WidgetColors.paletteForKey(intent.data?.getQueryParameter("theme")))
    }
}

private class RecentActivitiesRemoteViewsFactory(
    private val context: Context,
    private val widgetId: Int,
    /** The palette the header was rendered with, fixed when the host binds the adapter. */
    private val palette: WidgetPalette?
) : RemoteViewsService.RemoteViewsFactory {

    private var entries: List<ListEntry> = emptyList()

    // Rendered category icons, keyed by icon name + tint + size. Rows repeat the same few categories,
    // so the cache stays small, and the key fully determines the bitmap so it can never go stale.
    private val iconCache = HashMap<String, android.graphics.Bitmap>()

    private data class RowData(
        val transactionId: Long,
        val title: String,
        val initial: String,
        val iconName: String?,
        val subtitle: String,
        val timeText: String,
        val amountText: String,
        val isPositive: Boolean,
        val categoryColor: Int?
    )

    /** One ListView position: either a day's group label or one transaction row. */
    private sealed class ListEntry {
        data class Header(val label: String) : ListEntry()
        data class Item(val row: RowData) : ListEntry()
    }

    override fun onCreate() = Unit

    /**
     * Runs synchronously on a background binder thread — RemoteViewsFactory is Android's own
     * (pre-coroutine) contract for this, so a blocking DB round trip here is the documented,
     * expected shape, not a shortcut.
     */
    override fun onDataSetChanged() {
        entries = runBlocking {
            runCatching {
                val config = RecentActivitiesWidgetPrefsStore(context).loadSafe(widgetId)
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

                val rows = transactions.map { tx ->
                    val title = tx.name.ifBlank { tx.note.ifBlank { tx.transactionType.displayName } }
                    // Account + category only — the date now lives in this row's group header;
                    // time-of-day sits under the amount on the right (see RowData.timeText).
                    val subtitleParts = buildList {
                        if (config.showAccount) accountsById[tx.accountId]?.name?.let { add(it) }
                        if (config.showCategory) categoriesById[tx.categoryId]?.name?.let { add(it) }
                    }
                    val isPositive = tx.transactionType == TransactionType.INCOME ||
                        tx.transactionType == TransactionType.TRANSFER_INFLOW
                    tx.timestamp to RowData(
                        transactionId = tx.id,
                        title = title,
                        initial = title.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                        iconName = categoriesById[tx.categoryId]?.iconName,
                        subtitle = subtitleParts.joinToString(" · "),
                        timeText = if (config.showTimestamp) TimeUtils.formatTime(tx.timestamp) else "",
                        amountText = formatter.format(tx.amount, tx.currency),
                        isPositive = isPositive,
                        categoryColor = categoriesById[tx.categoryId]?.colorHex
                    )
                }

                buildList {
                    var lastLabel: String? = null
                    rows.forEach { (timestamp, row) ->
                        val label = groupLabel(timestamp, now)
                        if (label != lastLabel) {
                            add(ListEntry.Header(label))
                            lastLabel = label
                        }
                        add(ListEntry.Item(row))
                    }
                }
            }.getOrDefault(emptyList())
        }
    }

    override fun onDestroy() {
        entries = emptyList()
    }

    override fun getCount(): Int = entries.size

    override fun getViewAt(position: Int): RemoteViews = when (val entry = entries[position]) {
        is ListEntry.Header -> bindHeader(entry)
        is ListEntry.Item -> bindItem(entry.row)
    }

    private fun bindHeader(header: ListEntry.Header): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_recent_activities_date_header)
        views.setTextViewText(R.id.tv_ra_date_header, header.label)
        views.setThemedTextColor(R.id.tv_ra_date_header, palette) { it.onSurfaceVariant }
        return views
    }

    private fun bindItem(row: RowData): RemoteViews {
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
        views.setThemedTextColor(R.id.tv_ra_item_amount, palette) { if (row.isPositive) it.positive else it.negative }
        views.setThemedTextColor(R.id.tv_ra_item_title, palette) { it.onSurface }
        views.setThemedTextColor(R.id.tv_ra_item_subtitle, palette) { it.onSurfaceVariant }
        views.setThemedTextColor(R.id.tv_ra_item_time, palette) { it.onSurfaceVariant }
        views.setThemedBackgroundColor(R.id.v_ra_item_divider, palette) { it.outline }

        // TextView has no setColorFilter (ImageView/Drawable only), so swap the pre-tinted
        // drawable by sign instead — same pattern as cell3's highlight toggle in NetWorthRenderer.
        // Baked into a bitmap, so it can't follow a later night-mode change on its own.
        val color = (palette ?: WidgetColors.systemPalette()).let { if (row.isPositive) it.positive else it.negative }
        val iconColor = row.categoryColor ?: color
        if (row.iconName != null) {
            // Category icon, as on the dashboard cards; the initial is only the fallback below.
            views.setImageViewBitmap(R.id.iv_ra_item_icon, categoryIcon(row.iconName, iconColor))
            views.setViewVisibility(R.id.iv_ra_item_icon, android.view.View.VISIBLE)
            views.setViewVisibility(R.id.tv_ra_item_indicator, android.view.View.GONE)
        } else {
            views.setViewVisibility(R.id.iv_ra_item_icon, android.view.View.GONE)
            views.setViewVisibility(R.id.tv_ra_item_indicator, android.view.View.VISIBLE)
            views.setTextViewText(R.id.tv_ra_item_indicator, row.initial)
            views.setTextColor(R.id.tv_ra_item_indicator, iconColor)
        }
        if (row.categoryColor != null) {
            views.setInt(R.id.iv_ra_item_badge_bg, "setImageResource", R.drawable.widget_oval_bg)
            // Opaque colour at low alpha so it tints the surface behind it in light and dark.
            views.setInt(R.id.iv_ra_item_badge_bg, "setColorFilter", row.categoryColor or 0xFF000000.toInt())
            views.setInt(R.id.iv_ra_item_badge_bg, "setImageAlpha", 0x28)
        } else {
            val ovalBg = if (palette != null) {
                if (row.isPositive) palette.ovalPositiveBg else palette.ovalNegativeBg
            } else {
                if (row.isPositive) R.drawable.widget_oval_positive_bg else R.drawable.widget_oval_negative_bg
            }
            views.setInt(R.id.iv_ra_item_badge_bg, "setImageResource", ovalBg)
            views.setInt(R.id.iv_ra_item_badge_bg, "setColorFilter", 0)
            views.setInt(R.id.iv_ra_item_badge_bg, "setImageAlpha", 0xFF)
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

    private fun categoryIcon(iconName: String, tint: Int): android.graphics.Bitmap {
        val sizePx = (ICON_SIZE_DP * context.resources.displayMetrics.density).toInt()
        return iconCache.getOrPut("$iconName:$tint:$sizePx") {
            WidgetIconRenderer.render(getIconByName(iconName), sizePx, tint)
        }
    }

    /** TODAY / YESTERDAY / "MON, OCT 20" — matches the per-row amount's uppercase, mono styling. */
    private fun groupLabel(dateMillis: Long, now: Long): String {
        val zone = ZoneId.systemDefault()
        val target = Instant.ofEpochMilli(dateMillis).atZone(zone).toLocalDate()
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        return when (target) {
            today -> "TODAY"
            today.minusDays(1) -> "YESTERDAY"
            else -> DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())
                .format(target)
                .uppercase(Locale.getDefault())
        }
    }

    override fun getLoadingView(): RemoteViews? = null
    // RemoteViewsFactory has no getItemViewType — the host tracks recycling by each returned
    // RemoteViews' own layout id instead, so this just needs to cover the distinct layouts used.
    override fun getViewTypeCount(): Int = 2

    // Headers have no natural row id of their own; their label hash is stable across refreshes
    // as long as the same calendar day keeps the same label, which it always does.
    override fun getItemId(position: Int): Long = when (val entry = entries[position]) {
        is ListEntry.Header -> -(entry.label.hashCode().toLong())
        is ListEntry.Item -> entry.row.transactionId
    }

    override fun hasStableIds(): Boolean = true

    private companion object {
        // Matches iv_ra_item_icon's layout size.
        const val ICON_SIZE_DP = 18f
    }
}
