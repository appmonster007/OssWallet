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
 * Background worker that refreshes all three homescreen widgets. All of them are raw
 * RemoteViews (not Glance): for each, this fetches fresh data into an in-memory Preferences
 * instance via that widget's [WidgetDataFetcher], then hands it to that widget's RemoteViews
 * renderer to build and push the update.
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

                    refreshWidget(
                        context, appWidgetManager, common,
                        CashFlowWidgetProvider::class.java, CashFlowWidgetFetcher()
                    ) { ctx, prefs, widgetId, mgr ->
                        CashFlowRemoteViewsRenderer.render(ctx, prefs, widgetId, mgr)
                    }

                    refreshWidget(
                        context, appWidgetManager, common,
                        RecentTransactionsWidgetProvider::class.java, RecentTransactionsWidgetFetcher()
                    ) { ctx, prefs, widgetId, mgr ->
                        RecentTransactionsRemoteViewsRenderer.render(ctx, prefs, widgetId, mgr)
                    }

                    // Phase 2 widget rebuild — Budgets / Planned Payments / Currency Balances /
                    // Loans / Accounts, same refreshWidget(...) pattern as the three above.
                    refreshWidget(
                        context, appWidgetManager, common,
                        BudgetsWidgetProvider::class.java, BudgetsWidgetFetcher()
                    ) { ctx, prefs, widgetId, mgr ->
                        BudgetsRemoteViewsRenderer.render(ctx, prefs, widgetId, mgr)
                    }

                    refreshWidget(
                        context, appWidgetManager, common,
                        PlannedPaymentsWidgetProvider::class.java, PlannedPaymentsWidgetFetcher()
                    ) { ctx, prefs, widgetId, mgr ->
                        PlannedPaymentsRemoteViewsRenderer.render(ctx, prefs, widgetId, mgr)
                    }

                    refreshWidget(
                        context, appWidgetManager, common,
                        CurrencyBalancesWidgetProvider::class.java, CurrencyBalancesWidgetFetcher()
                    ) { ctx, prefs, widgetId, mgr ->
                        CurrencyBalancesRemoteViewsRenderer.render(ctx, prefs, widgetId, mgr)
                    }

                    refreshWidget(
                        context, appWidgetManager, common,
                        LoansWidgetProvider::class.java, LoansWidgetFetcher()
                    ) { ctx, prefs, widgetId, mgr ->
                        LoansRemoteViewsRenderer.render(ctx, prefs, widgetId, mgr)
                    }

                    refreshWidget(
                        context, appWidgetManager, common,
                        AccountsWidgetProvider::class.java, AccountsWidgetFetcher()
                    ) { ctx, prefs, widgetId, mgr ->
                        AccountsRemoteViewsRenderer.render(ctx, prefs, widgetId, mgr)
                    }

                    // Phase 3 widget rebuild (final phase) — Account Hero / Income vs Expenses /
                    // Category Pie Chart / Category Treemap / Cash Flow Comparison / Category
                    // Spending Trend / Tag UpSet Plot, same refreshWidget(...) pattern as above.
                    refreshWidget(
                        context, appWidgetManager, common,
                        AccountHeroWidgetProvider::class.java, AccountHeroWidgetFetcher()
                    ) { ctx, prefs, widgetId, mgr ->
                        AccountHeroRemoteViewsRenderer.render(ctx, prefs, widgetId, mgr)
                    }

                    refreshWidget(
                        context, appWidgetManager, common,
                        IncomeExpenseBarWidgetProvider::class.java, IncomeExpenseBarWidgetFetcher()
                    ) { ctx, prefs, widgetId, mgr ->
                        IncomeExpenseBarRemoteViewsRenderer.render(ctx, prefs, widgetId, mgr)
                    }

                    refreshWidget(
                        context, appWidgetManager, common,
                        CategoryPieChartWidgetProvider::class.java, CategoryPieChartWidgetFetcher()
                    ) { ctx, prefs, widgetId, mgr ->
                        CategoryPieChartRemoteViewsRenderer.render(ctx, prefs, widgetId, mgr)
                    }

                    refreshWidget(
                        context, appWidgetManager, common,
                        CategoryTreemapWidgetProvider::class.java, CategoryTreemapWidgetFetcher()
                    ) { ctx, prefs, widgetId, mgr ->
                        CategoryTreemapRemoteViewsRenderer.render(ctx, prefs, widgetId, mgr)
                    }

                    refreshWidget(
                        context, appWidgetManager, common,
                        CashFlowComparisonWidgetProvider::class.java, CashFlowComparisonWidgetFetcher()
                    ) { ctx, prefs, widgetId, mgr ->
                        CashFlowComparisonRemoteViewsRenderer.render(ctx, prefs, widgetId, mgr)
                    }

                    refreshWidget(
                        context, appWidgetManager, common,
                        CategorySpendingTrendWidgetProvider::class.java, CategorySpendingTrendWidgetFetcher()
                    ) { ctx, prefs, widgetId, mgr ->
                        CategorySpendingTrendRemoteViewsRenderer.render(ctx, prefs, widgetId, mgr)
                    }

                    refreshWidget(
                        context, appWidgetManager, common,
                        TagUpSetPlotWidgetProvider::class.java, TagUpSetPlotWidgetFetcher()
                    ) { ctx, prefs, widgetId, mgr ->
                        TagUpSetPlotRemoteViewsRenderer.render(ctx, prefs, widgetId, mgr)
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
