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
 * Backs the Accounts widget's ListView, so every account is reachable by scrolling. Like Recent
 * Activities' list, the factory loads its rows itself each time the system asks for them (the
 * refresh worker asks via notifyAppWidgetViewDataChanged); the worker's own pass only prepares
 * the header.
 */
class AccountsRemoteViewsService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        val widgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
        return AccountsRemoteViewsFactory(applicationContext, widgetId)
    }
}

private class AccountsRemoteViewsFactory(
    private val context: Context,
    private val widgetId: Int
) : RemoteViewsService.RemoteViewsFactory {

    private var rows: List<AccountWidgetRow> = emptyList()
    private var palette: WidgetPalette? = null

    override fun onCreate() = Unit

    /** Runs on a binder thread, where a blocking read is the documented shape for this contract. */
    override fun onDataSetChanged() {
        val snapshot = runBlocking {
            runCatching {
                AccountsWidgetFetcher.load(context, widgetId, WidgetDataLoader.loadCommon(context))
            }.getOrNull()
        }
        rows = snapshot?.rows.orEmpty()
        palette = snapshot?.let { WidgetColors.resolve(it.theme) }
    }

    override fun onDestroy() {
        rows = emptyList()
    }

    override fun getCount(): Int = rows.size

    override fun getViewAt(position: Int): RemoteViews {
        val row = rows[position]
        val views = AccountsRemoteViewsRenderer.buildRow(context, row, palette)
        // Completes the list's pending-intent template (see AccountsRemoteViewsRenderer).
        views.setOnClickFillInIntent(
            R.id.ll_account_row,
            Intent().putExtra(MainActivity.EXTRA_WIDGET_ENTITY_ID, row.accountId)
        )
        return views
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = row(position).accountId.hashCode().toLong()
    override fun hasStableIds(): Boolean = true

    private fun row(position: Int) = rows[position]
}
