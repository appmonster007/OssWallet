package com.osscabinet.ossWallet.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import com.osscabinet.ossWallet.R

/**
 * Plain (non-Glance) AppWidgetProvider for the Recent Activities widget — raw RemoteViews (a
 * ListView, not a single static tree like Net Worth), built by
 * [RecentActivitiesRemoteViewsRenderer]/[RecentActivitiesRemoteViewsService]. See
 * [BaseWidgetProvider] for the shared refresh/cleanup shape.
 */
class RecentActivitiesWidgetProvider : BaseWidgetProvider(
    createPrefsStore = { RecentActivitiesWidgetPrefsStore(it) },
    loadingLayoutResId = R.layout.widget_recent_activities_loading
) {
    override fun onResized(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.lv_ra_list)
    }
}
