package com.osscabinet.ossWallet.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

/**
 * Drains [WidgetRefreshCoordinator]'s pending work. Raw RemoteViews (not Glance): each pass loads
 * shared data once, then per widget fetches fresh data into an in-memory Preferences via the
 * widget's [WidgetDataFetcher] and hands it to that widget's RemoteViews renderer.
 *
 * The worker loops until [WidgetRefreshState.beginPass] reports nothing pending, so a write that
 * lands mid-pass is picked up by another pass before the worker exits. Providers render in
 * parallel; a failed pass is aborted and retried by WorkManager with its work still pending.
 *
 * SHELVED (2026-10-08): every widget except Net Worth (three provider registrations, see
 * AndroidManifest.xml), Recent Activities, Budgets and Accounts moved to `shelved-widgets/`; see its README.
 */
class WidgetRefreshWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    private class ProviderSpec(
        val providerClass: Class<out AppWidgetProvider>,
        val fetcher: WidgetDataFetcher,
        val render: (Context, Preferences, Int, AppWidgetManager) -> Unit
    )

    private val providers = listOf(
        netWorthSpec(NetWorthWidgetProviderMicro::class.java),
        netWorthSpec(NetWorthWidgetProvider::class.java),
        netWorthSpec(NetWorthWidgetProviderCard::class.java),
        ProviderSpec(
            RecentActivitiesWidgetProvider::class.java,
            RecentActivitiesWidgetFetcher()
        ) { ctx, prefs, widgetId, mgr ->
            RecentActivitiesRemoteViewsRenderer.render(ctx, prefs, widgetId, mgr)
        },
        ProviderSpec(
            AccountsWidgetProvider::class.java,
            AccountsWidgetFetcher()
        ) { ctx, prefs, widgetId, mgr ->
            AccountsRemoteViewsRenderer.render(ctx, prefs, widgetId, mgr)
        },
        ProviderSpec(
            BudgetsWidgetProvider::class.java,
            BudgetsWidgetFetcher()
        ) { ctx, prefs, widgetId, mgr ->
            BudgetsRemoteViewsRenderer.render(ctx, prefs, widgetId, mgr)
        }
    )

    override suspend fun doWork(): Result {
        val state = WidgetRefreshCoordinator.state
        state.attach()
        var pass: WidgetRefreshState.Pass? = null
        try {
            while (true) {
                pass = state.beginPass() ?: return Result.success().also { Log.d(TAG, "idle, worker retiring") }
                val startedAt = System.currentTimeMillis()
                Log.d(TAG, "pass start: $pass")
                runPass(pass)
                state.complete(pass)
                Log.d(TAG, "pass done in ${System.currentTimeMillis() - startedAt} ms: $pass")
            }
        } catch (e: CancellationException) {
            state.abort(pass)
            throw e
        } catch (t: Throwable) {
            Log.e(TAG, "Widget refresh failed", t)
            state.abort(pass)
            return Result.retry()
        }
    }

    private suspend fun runPass(pass: WidgetRefreshState.Pass) = withContext(Dispatchers.IO) {
        val context = applicationContext
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val common = WidgetDataLoader.loadCommon(context)
        coroutineScope {
            providers.map { spec ->
                async {
                    val ids = appWidgetManager
                        .getAppWidgetIds(ComponentName(context, spec.providerClass))
                        .filter { pass.includes(it) }
                    for (widgetId in ids) {
                        val prefs = mutablePreferencesOf()
                        spec.fetcher.updateState(context, widgetId, common, prefs)
                        spec.render(context, prefs, widgetId, appWidgetManager)
                    }
                }
            }.awaitAll()
        }
    }

    private fun WidgetRefreshState.Pass.includes(widgetId: Int) = when (this) {
        is WidgetRefreshState.Pass.All -> true
        is WidgetRefreshState.Pass.Widgets -> widgetId in ids
    }

    private fun netWorthSpec(providerClass: Class<out AppWidgetProvider>) = ProviderSpec(
        providerClass, NetWorthWidgetFetcher()
    ) { ctx, prefs, widgetId, mgr ->
        NetWorthRemoteViewsRenderer.render(ctx, prefs, widgetId, mgr)
    }

    private companion object {
        const val TAG = "WidgetRefreshWorker"
    }
}
