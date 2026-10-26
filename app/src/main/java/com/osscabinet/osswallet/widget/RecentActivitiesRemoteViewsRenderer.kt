package com.osscabinet.ossWallet.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.datastore.preferences.core.Preferences
import com.osscabinet.ossWallet.MainActivity
import com.osscabinet.ossWallet.R

/**
 * Builds and pushes the Recent Activities widget's [RemoteViews]. Unlike NetWorthRemoteViewsRenderer,
 * there's only ever one layout here (widget_recent_activities_main) — no height tiers, since the
 * content is a genuinely scrolling ListView rather than a fixed set of rows chosen by size; it
 * grows with the widget (more visible rows) or scrolls (more content than fits) either way, with
 * no upper bound the way Net Worth's card tier is capped at 180dp.
 */
object RecentActivitiesRemoteViewsRenderer {

    /** Long extra on the fill-in Intent each ListView row sets — see RecentActivitiesRemoteViewsService. */
    const val EXTRA_TRANSACTION_ID = "ra_transaction_id"

    fun render(
        context: Context,
        prefs: Preferences,
        widgetId: Int,
        appWidgetManager: AppWidgetManager
    ) {
        val isLoaded = prefs[RecentActivitiesWidgetKeys.IS_LOADED] == "true"
        if (!isLoaded) {
            val loading = RemoteViews(context.packageName, R.layout.widget_recent_activities_loading)
            appWidgetManager.updateAppWidget(widgetId, loading)
            return
        }

        val theme = prefs[RecentActivitiesWidgetKeys.THEME]?.let { runCatching { WidgetTheme.valueOf(it) }.getOrNull() }
            ?: WidgetTheme.SYSTEM
        val glass = prefs[RecentActivitiesWidgetKeys.GLASS] == "true"
        val palette = WidgetColors.resolve(theme, glass)

        val views = RemoteViews(context.packageName, WidgetColors.layoutFor(palette, R.layout.widget_recent_activities_main_t0, R.layout.widget_recent_activities_main_t1, R.layout.widget_recent_activities_main_t2))
        views.applyRootTheme(palette, glass)
        views.setThemedColorFilter(R.id.iv_ra_empty_icon, palette) { it.outline }
        views.setThemedTextColor(R.id.tv_ra_empty_text, palette) { it.onSurfaceVariant }
        views.setThemedBackgroundColor(R.id.v_ra_footer_divider, palette) { it.outline }

        // "VIEW ALL" opens Records pre-filtered — same target as RecentActivitiesCard's own
        // onSeeAll/onRecordsClick(config.filter). Lives in the footer below the list now that
        // there's no header row to carry it.
        views.setThemedTextColor(R.id.tv_ra_view_all, palette) { it.accent }
        views.setThemedBackgroundResource(R.id.tv_ra_view_all, palette, { it.pillBg }, R.drawable.widget_netflow_pill_bg)
        val viewAllIntent = Intent(context, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_WIDGET_NAV, "records")
            .putExtra(MainActivity.EXTRA_WIDGET_FILTER, prefs[RecentActivitiesWidgetKeys.VIEW_ALL_FILTER] ?: "")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        views.setOnClickPendingIntent(
            R.id.tv_ra_view_all,
            PendingIntent.getActivity(
                context, widgetId * 10 + 1, viewAllIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )

        // Tapping a row opens that transaction — same target as RecentActivitiesCard's
        // onTransactionClick. The template intent is completed per-row by
        // RecentActivitiesRemoteViewsFactory.getViewAt()'s fill-in Intent (EXTRA_TRANSACTION_ID).
        val itemTemplateIntent = Intent(context, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_WIDGET_NAV, "transaction_entry")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        views.setPendingIntentTemplate(
            R.id.lv_ra_list,
            PendingIntent.getActivity(
                context, widgetId * 10 + 2, itemTemplateIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
        )

        val adapterIntent = Intent(context, RecentActivitiesRemoteViewsService::class.java)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            .apply { data = android.net.Uri.parse("remoteviews://ra/$widgetId?theme=${WidgetColors.themeKey(theme, glass)}") }
        views.setRemoteAdapter(R.id.lv_ra_list, adapterIntent)
        views.setEmptyView(R.id.lv_ra_list, R.id.ll_ra_empty)

        appWidgetManager.updateAppWidget(widgetId, views)
        // setRemoteAdapter alone doesn't re-run the factory's onDataSetChanged() — this is the
        // separate call that actually triggers it (see RecentActivitiesRemoteViewsFactory).
        appWidgetManager.notifyAppWidgetViewDataChanged(widgetId, R.id.lv_ra_list)
    }
}
