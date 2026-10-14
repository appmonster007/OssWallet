package com.osscabinet.ossWallet.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.osscabinet.ossWallet.MainActivity
import com.osscabinet.ossWallet.R
import kotlinx.coroutines.runBlocking

/**
 * Backs the Budgets widget's ListView, so every budget is reachable by scrolling. Like Recent
 * Activities' list, the factory loads its rows itself each time the system asks for them (the
 * refresh worker asks via notifyAppWidgetViewDataChanged); the worker's own pass only prepares
 * the header.
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

    private var rows: List<BudgetWidgetRow> = emptyList()

    override fun onCreate() = Unit

    /** Runs on a binder thread, where a blocking read is the documented shape for this contract. */
    override fun onDataSetChanged() {
        val snapshot = runBlocking {
            runCatching {
                BudgetsWidgetFetcher.load(context, widgetId, WidgetDataLoader.loadCommon(context))
            }.getOrNull()
        }
        rows = snapshot?.rows.orEmpty()
    }

    override fun onDestroy() {
        rows = emptyList()
    }

    override fun getCount(): Int = rows.size

    override fun getViewAt(position: Int): RemoteViews {
        val row = rows[position]
        val views = BudgetsRemoteViewsRenderer.buildRow(context, row, palette)
        // Completes the list's pending-intent template (see BudgetsRemoteViewsRenderer).
        views.setOnClickFillInIntent(
            R.id.ll_budget_row,
            Intent().putExtra(MainActivity.EXTRA_WIDGET_ENTITY_ID, row.budgetId.toString())
        )
        return views
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = rows[position].budgetId
    override fun hasStableIds(): Boolean = true
}
