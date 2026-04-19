package com.osscabinet.ossWallet.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import com.osscabinet.ossWallet.MainActivity
import com.osscabinet.ossWallet.OssWalletApp
import com.osscabinet.ossWallet.R
import com.osscabinet.ossWallet.ui.util.calculateNetWorthFromAccounts
import com.osscabinet.ossWallet.ui.util.convertAmount
import com.osscabinet.ossWallet.ui.util.formatCurrencyRight
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * High-performance Widget Update Manager inspired by Microsoft To-Do architecture.
 * Features:
 * 1. Repository-Driven: Observes data changes via Flows.
 * 2. Debounced: Consolidates multiple rapid updates.
 * 3. Direct RemoteViews: Avoids Glance overhead/locking.
 */
object WidgetUpdateManager {
    private const val TAG = "WidgetUpdateManager"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var updateJob: Job? = null

    // Signal for manual refresh requests
    private val refreshRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    fun startObserving(context: Context) {
        val app = context.applicationContext as OssWalletApp
        val container = app.container

        // Observe the current profile ID first
        container.settingsDataStore.currentProfileId
            .flatMapLatest { profileId ->
                // Then observe data changes for THAT profile
                combine(
                    container.transactionRepository.getAllTransactions(profileId),
                    container.accountRepository.getAllAccounts(profileId),
                    container.settingsDataStore.currency,
                    refreshRequests.onStart { emit(Unit) }
                ) { _, _, _, _ -> Unit }
            }
            .onEach { requestUpdate(context) }
            .launchIn(scope)
    }

    fun requestUpdate(context: Context) {
        updateJob?.cancel()
        updateJob = scope.launch {
            delay(500) // Debounce period: 500ms
            performUpdate(context.applicationContext)
        }
    }

    fun manualRefresh() {
        refreshRequests.tryEmit(Unit)
    }

    private suspend fun performUpdate(context: Context) {
        withContext(Dispatchers.IO) {
            Log.d(TAG, "Performing debounced widget update via RemoteViews...")
            val app = context as OssWalletApp
            val container = app.container
            val appWidgetManager = AppWidgetManager.getInstance(context)

            val profileId = container.settingsDataStore.currentProfileId.first()
            val baseCurrency = container.settingsDataStore.currency.first()
            val precision = container.settingsDataStore.decimalPrecision.first()

            val allAccounts = container.accountRepository.getAllAccountsSync(profileId)
            val summaries = container.transactionRepository.getCurrencySummaries(profileId)
            val exchangeRatesRaw = container.currencyRepository.getExchangeRates(baseCurrency)
            val exchangeRates = exchangeRatesRaw.mapValues { BigDecimal.valueOf(it.value) }

            // Pre-calculate global metrics
            var totalIncome = BigDecimal.ZERO
            var totalExpense = BigDecimal.ZERO
            summaries.forEach { s ->
                totalIncome = totalIncome.add(convertAmount(s.totalIncome, s.currency, baseCurrency, exchangeRates, precision + 4))
                totalExpense = totalExpense.add(convertAmount(s.totalExpense, s.currency, baseCurrency, exchangeRates, precision + 4))
            }
            val netIncome = totalIncome.setScale(precision, RoundingMode.HALF_UP)
            val netExpense = totalExpense.setScale(precision, RoundingMode.HALF_UP)

            // 1. Update NetWorthWidgets
            updateWidget(
                context, 
                appWidgetManager, 
                NetWorthWidgetProvider::class.java, 
                R.layout.widget_net_worth
            ) { views ->
                // Update income/expense badges

                views.setTextViewText(R.id.badge_income, "INCOME: ${formatCurrencyRight(netIncome, baseCurrency, precision)}")
                views.setTextViewText(R.id.badge_expense, "EXPENSE: ${formatCurrencyRight(netExpense, baseCurrency, precision)}")

                views.setTextViewText(R.id.text_amount, formatCurrencyRight(netIncome-netExpense, baseCurrency, precision))

                val intent = Intent(context, MainActivity::class.java)
                val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
                views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
            }

            // 2. Update BalanceWidgets
            updateWidget(
                context, 
                appWidgetManager, 
                BalanceWidgetProvider::class.java, 
                R.layout.widget_balance
            ) { views ->
                val totalBalance = calculateNetWorthFromAccounts(allAccounts, baseCurrency, exchangeRates, precision)
                views.setTextViewText(R.id.text_balance, formatCurrencyRight(totalBalance, baseCurrency, precision))
                
                val intent = Intent(context, MainActivity::class.java)
                val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
                views.setOnClickPendingIntent(R.id.button_quick_add, pendingIntent)
            }
            
            Log.d(TAG, "Widget update completed.")
        }
    }

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        providerClass: Class<*>,
        layoutId: Int,
        binder: (RemoteViews) -> Unit
    ) {
        val provider = ComponentName(context, providerClass)
        val ids = appWidgetManager.getAppWidgetIds(provider)
        if (ids.isEmpty()) return

        ids.forEach { id ->
            val views = RemoteViews(context.packageName, layoutId)
            binder(views)
            appWidgetManager.updateAppWidget(id, views)
        }
    }
}
