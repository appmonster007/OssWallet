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
        return RecentActivitiesRemoteViewsFactory(applicationContext, widgetId, WidgetColors.paletteForKey(intent.data?.getQueryParameter("theme")))
    }
}

private class RecentActivitiesRemoteViewsFactory(
    private val context: Context,
    private val widgetId: Int,
    /** The palette the header was rendered with, fixed when the host binds the adapter. */
    private val palette: WidgetPalette?
) : RemoteViewsService.RemoteViewsFactory {

    private var rows: List<RowData> = emptyList()

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
                        iconName = categoriesById[tx.categoryId]?.iconName,
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

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = rows[position].transactionId
    override fun hasStableIds(): Boolean = true

    private companion object {
        // Matches iv_ra_item_icon's layout size.
        const val ICON_SIZE_DP = 18f
    }
}
