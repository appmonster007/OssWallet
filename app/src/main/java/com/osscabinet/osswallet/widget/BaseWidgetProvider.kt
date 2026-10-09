package com.osscabinet.ossWallet.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.Bundle
import android.widget.RemoteViews
import com.osscabinet.ossWallet.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Shared shape for the plain (non-Glance) homescreen widget providers — raw RemoteViews,
 * built by each widget's own renderer.
 *
 * Refresh path: TransactionManager -> WidgetRefreshTrigger -> WorkManagerWidgetRefreshTrigger
 *   -> WidgetRefreshWorker.enqueueNow() -> that widget's fetcher updates an in-memory Preferences
 *   -> that widget's RemoteViews renderer -> AppWidgetManager.updateAppWidget().
 *
 * [onUpdate] pushes a cheap loading placeholder SYNCHRONOUSLY before enqueueing the real async
 * refresh. This isn't just belt-and-suspenders: Android's own widget-picker "generated preview"
 * flow (and some OEM launchers, e.g. One UI's "Couldn't add widget" failure we hit) binds a real
 * throwaway widget instance and waits for an actual `updateAppWidget()` call to render its
 * thumbnail — it doesn't necessarily fall back to `android:initialLayout` the way placing a real
 * widget does. Since our real data fetch goes through a background WorkManager job (hits the
 * database, builds the Hilt graph on a cold start), that round trip can be too slow for a
 * preview-generation timeout even though it's fine for an actually-placed widget. Pushing
 * [R.layout.widget_net_worth] immediately here gives the system *something* to render at once,
 * every time, with the async job replacing it with real data moments later as before.
 */
abstract class BaseWidgetProvider(
    private val createPrefsStore: (Context) -> WidgetPrefsStore<*>,
    private val loadingLayoutResId: Int = R.layout.widget_net_worth
) : AppWidgetProvider() {

    private val cleanupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { widgetId ->
            runCatching {
                appWidgetManager.updateAppWidget(widgetId, RemoteViews(context.packageName, loadingLayoutResId))
            }
        }
        WidgetRefreshWorker.enqueueNow(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        WidgetRefreshWorker.enqueueNow(context)
        onResized(context, appWidgetManager, appWidgetId)
    }

    /**
     * Override point for a provider whose content includes a ListView
     * (RecentActivitiesWidgetProvider) — a resize needs an explicit
     * notifyAppWidgetViewDataChanged too, not just the header re-render
     * WidgetRefreshWorker.enqueueNow above already schedules.
     */
    protected open fun onResized(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) = Unit

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val prefsStore = createPrefsStore(context)
        appWidgetIds.forEach { widgetId ->
            cleanupScope.launch {
                runCatching { prefsStore.delete(widgetId) }
            }
        }
    }
}
