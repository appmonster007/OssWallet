package com.osscabinet.ossWallet.ui.widget

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import androidx.work.*
import com.osscabinet.ossWallet.OssWalletApp
import com.osscabinet.ossWallet.data.local.dao.CurrencySummary
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Budget
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit
import java.util.concurrent.ConcurrentHashMap

data class WidgetDataSnapshot(
    val accounts: List<Account>,
    val currencySummaries: List<CurrencySummary>,
    val budgets: List<Budget>,
    val exchangeRates: Map<String, Double>,
    val timestamp: Long = System.currentTimeMillis()
)

object WidgetDataCache {
    private val snapshots = ConcurrentHashMap<Int, WidgetDataSnapshot>()
    private const val CACHE_VALIDITY_MS = 10_000L // 10 seconds

    fun update(profileId: Int, snapshot: WidgetDataSnapshot) {
        snapshots[profileId] = snapshot
    }

    fun get(profileId: Int): WidgetDataSnapshot? {
        val snapshot = snapshots[profileId]
        if (snapshot != null && System.currentTimeMillis() - snapshot.timestamp < CACHE_VALIDITY_MS) {
            return snapshot
        }
        return null
    }
}

object WidgetUpdateHelper {
    private const val TAG = "WidgetUpdateHelper"
    private const val UPDATE_WORK_NAME = "consolidated_widget_update"

    fun triggerUpdate(context: Context) {
        val workManager = WorkManager.getInstance(context.applicationContext)
        val updateRequest = OneTimeWorkRequestBuilder<WidgetUpdateWorker>()
//            .setInitialDelay(500, TimeUnit.MILLISECONDS)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 2, TimeUnit.SECONDS)
            .addTag(TAG)
            .build()

        workManager.enqueueUniqueWork(
            UPDATE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            updateRequest
        )
    }

    suspend fun updateAllWidgets(context: Context) {
        performUpdate(context)
    }

    suspend fun performUpdate(context: Context) {
        withContext(NonCancellable + Dispatchers.IO) {
            val app = context.applicationContext as OssWalletApp
            val container = app.container
            
            try {
                Log.d(TAG, "Centrally fetching data for widgets...")
                val profileId = container.settingsDataStore.currentProfileId.first()
                val baseCurrency = container.settingsDataStore.currency.first()
                
                // Fetch everything combinedly
                val accounts = container.accountRepository.getAllAccountsSync(profileId)
                val summaries = container.transactionRepository.getCurrencySummaries(profileId)
                val budgets = container.budgetRepository.getBudgets().first()
                val exchangeRates = container.currencyRepository.getExchangeRates(baseCurrency)

                val snapshot = WidgetDataSnapshot(
                    accounts = accounts,
                    currencySummaries = summaries,
                    budgets = budgets,
                    exchangeRates = exchangeRates
                )
                
                WidgetDataCache.update(profileId, snapshot)
                Log.d(TAG, "Snapshot cached for profile $profileId")

                // Now refresh widgets
                val widgets = listOf(
                    NetWorthWidget(),
                    BalanceWidget(),
                    TransactionHistoryWidget(),
                    BudgetWidget(),
                    CashFlowWidget()
                )

                val manager = GlanceAppWidgetManager(context)
                widgets.forEach { widget ->
                    val ids = manager.getGlanceIds(widget::class.java)
                    if (ids.isNotEmpty()) {
                        widget.updateAll(context)
                        delay(150)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Central fetch failed", e)
            }
        }
    }
}
