package com.osscabinet.ossWallet.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.osscabinet.ossWallet.MainActivity
import com.osscabinet.ossWallet.R
import com.osscabinet.ossWallet.domain.model.BudgetPaceStatus
import kotlinx.coroutines.runBlocking

/**
 * Backs the Budgets widget's ListView, so every budget is reachable by scrolling. Like Recent
 * Activities' list, the factory loads its rows itself each time the system asks for them (the
 * refresh worker asks via notifyAppWidgetViewDataChanged); the worker's own pass only prepares
 * the header. Budgets are grouped by status: a status-header row (OVER BUDGET / NEAR LIMIT /
 * ON TRACK) is inserted ahead of each bucket's first row — see [StatusBucket] and
 * [BudgetsRemoteViewsFactory.bucketFor]. [BudgetsWidgetFetcher] already orders rows most urgent
 * first, so bucket boundaries only ever move forward while walking the list once.
 */
class BudgetsRemoteViewsService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        val widgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
        return BudgetsRemoteViewsFactory(applicationContext, widgetId, WidgetColors.paletteForKey(intent.data?.getQueryParameter("theme")))
    }
}

private class BudgetsRemoteViewsFactory(
    private val context: Context,
    private val widgetId: Int,
    /** The palette the header was rendered with, fixed when the host binds the adapter. */
    private val palette: WidgetPalette?
) : RemoteViewsService.RemoteViewsFactory {

    private var entries: List<ListEntry> = emptyList()

    /** One ListView position: either a status bucket's label or one budget row. */
    private sealed class ListEntry {
        data class Header(val bucket: StatusBucket) : ListEntry()
        data class Item(val row: BudgetWidgetRow) : ListEntry()
    }

    private enum class StatusBucket(val label: String) {
        OVER("OVER BUDGET"),
        NEAR("NEAR LIMIT"),
        ON_TRACK("ON TRACK")
    }

    override fun onCreate() = Unit

    /** Runs on a binder thread, where a blocking read is the documented shape for this contract. */
    override fun onDataSetChanged() {
        val snapshot = runBlocking {
            runCatching {
                BudgetsWidgetFetcher.load(context, widgetId, WidgetDataLoader.loadCommon(context))
            }.getOrNull()
        }
        val rows = snapshot?.rows.orEmpty()

        entries = buildList {
            var lastBucket: StatusBucket? = null
            rows.forEach { row ->
                val bucket = bucketFor(row)
                if (bucket != lastBucket) {
                    add(ListEntry.Header(bucket))
                    lastBucket = bucket
                }
                add(ListEntry.Item(row))
            }
        }
    }

    override fun onDestroy() {
        entries = emptyList()
    }

    override fun getCount(): Int = entries.size

    override fun getViewAt(position: Int): RemoteViews = when (val entry = entries[position]) {
        is ListEntry.Header -> bindHeader(entry.bucket)
        is ListEntry.Item -> bindItem(entry.row)
    }

    private fun bindHeader(bucket: StatusBucket): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_budgets_status_header)
        views.setTextViewText(R.id.tv_budgets_status_header, bucket.label)
        views.setThemedTextColor(R.id.tv_budgets_status_header, palette) {
            when (bucket) {
                StatusBucket.OVER -> it.negative
                StatusBucket.NEAR -> it.warning
                StatusBucket.ON_TRACK -> it.accent
            }
        }
        return views
    }

    private fun bindItem(row: BudgetWidgetRow): RemoteViews {
        val views = BudgetsRemoteViewsRenderer.buildRow(context, row, palette)
        // Completes the list's pending-intent template (see BudgetsRemoteViewsRenderer).
        views.setOnClickFillInIntent(
            R.id.ll_budget_row,
            Intent().putExtra(MainActivity.EXTRA_WIDGET_ENTITY_ID, row.budgetId.toString())
        )
        return views
    }

    private fun bucketFor(row: BudgetWidgetRow): StatusBucket =
        when (runCatching { BudgetPaceStatus.valueOf(row.pace) }.getOrDefault(BudgetPaceStatus.ON_TRACK)) {
            BudgetPaceStatus.OVER -> StatusBucket.OVER
            BudgetPaceStatus.AHEAD_OF_PACE, BudgetPaceStatus.NEAR_LIMIT -> StatusBucket.NEAR
            BudgetPaceStatus.ON_TRACK -> StatusBucket.ON_TRACK
        }

    override fun getLoadingView(): RemoteViews? = null
    // RemoteViewsFactory has no getItemViewType — the host tracks recycling by each returned
    // RemoteViews' own layout id instead, so this just needs to cover the distinct layouts used.
    override fun getViewTypeCount(): Int = 2

    // Headers have no natural row id of their own; a bucket's ordinal is stable across refreshes.
    override fun getItemId(position: Int): Long = when (val entry = entries[position]) {
        is ListEntry.Header -> -(entry.bucket.ordinal.toLong() + 1)
        is ListEntry.Item -> entry.row.budgetId
    }

    override fun hasStableIds(): Boolean = true
}
