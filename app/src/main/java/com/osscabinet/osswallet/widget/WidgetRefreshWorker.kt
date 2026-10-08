package com.osscabinet.ossWallet.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/**
 * Background worker that refreshes the homescreen widget(s). Raw RemoteViews (not Glance):
 * fetches fresh data into an in-memory Preferences instance via that widget's
 * [WidgetDataFetcher], then hands it to that widget's RemoteViews renderer to build and push
 * the update.
 *
 * SHELVED (2026-10-08): Cash Flow, Recent Transactions, and every Phase 2/3 widget
 * (Budgets/Planned Payments/Currency Balances/Loans/Accounts/Account Hero/Income-Expense Bar/
 * Category Pie Chart/Category Treemap/Cash Flow Comparison/Category Spending Trend/Tag UpSet
 * Plot) were pulled out of the active build and moved to `shelved-widgets/` at the repo root —
 * see `shelved-widgets/README.md` and `shelved-widgets/restore.patch` to bring them back. Only
 * Net Worth is wired below.
 */
class WidgetRefreshWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            withContext(NonCancellable) {
                val context = applicationContext
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val common = withContext(Dispatchers.IO) {
                    WidgetDataLoader.loadCommon(context)
                }

                withContext(Dispatchers.IO) {
                    refreshWidget(
                        context, appWidgetManager, common,
                        NetWorthWidgetProvider::class.java, NetWorthWidgetFetcher()
                    ) { ctx, prefs, widgetId, mgr ->
                        NetWorthRemoteViewsRenderer.render(ctx, prefs, widgetId, mgr)
                    }
                }
            }
            Result.success()
        } catch (t: Throwable) {
            Log.e(TAG, "Widget refresh failed", t)
            Result.retry()
        }
    }

    private suspend fun refreshWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        common: WidgetDataLoader.CommonData,
        providerClass: Class<out android.appwidget.AppWidgetProvider>,
        fetcher: WidgetDataFetcher,
        render: (Context, androidx.datastore.preferences.core.Preferences, Int, AppWidgetManager) -> Unit
    ) {
        val widgetIds = appWidgetManager.getAppWidgetIds(ComponentName(context, providerClass))
        for (widgetId in widgetIds) {
            val prefs = mutablePreferencesOf()
            fetcher.updateState(context, widgetId, common, prefs)
            render(context, prefs, widgetId, appWidgetManager)
        }
    }

    companion object {
        private const val TAG = "WidgetRefreshWorker"
        const val WORK_NAME = "net_worth_widget_refresh"

        fun enqueueNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<WidgetRefreshWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
        }
    }
}
