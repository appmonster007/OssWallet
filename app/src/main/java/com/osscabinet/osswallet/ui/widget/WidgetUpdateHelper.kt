package com.osscabinet.ossWallet.ui.widget

import android.content.Context
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.work.*
import com.osscabinet.ossWallet.OssWalletApp
import com.osscabinet.ossWallet.data.local.dao.CurrencySummary
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Budget
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.settings.NetWorthSettings
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.ui.util.FilterUtils
import com.osscabinet.ossWallet.ui.util.calculateNetWorth
import com.osscabinet.ossWallet.ui.util.calculateNetWorthFromAccounts
import com.osscabinet.ossWallet.ui.util.convertAmount
import com.osscabinet.ossWallet.ui.widget.config.NetWorthWidgetConfigActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import java.util.concurrent.ConcurrentHashMap

data class NetWorthWidgetResult(
    val netWorth: BigDecimal,
    val netIncome: BigDecimal,
    val netExpense: BigDecimal,
    val baseCurrency: String,
    val precision: Int,
    val fontSize: String,
    val timelineLabel: String
)

data class BalanceWidgetResult(
    val totalBalance: BigDecimal,
    val currency: String,
    val precision: Int
)

data class TransactionHistoryWidgetResult(
    val balance: BigDecimal,
    val transactions: List<Transaction>,
    val currency: String,
    val precision: Int
)

data class BudgetWidgetResult(
    val budgets: List<Budget>,
    val currency: String,
    val precision: Int
)

data class CashFlowWidgetResult(
    val income: BigDecimal,
    val expense: BigDecimal,
    val currency: String,
    val precision: Int
)

data class WidgetPrecomputedState(
    val netWorthResults: Map<String, NetWorthWidgetResult> = emptyMap(),
    val balanceResult: BalanceWidgetResult? = null,
    val historyResult: TransactionHistoryWidgetResult? = null,
    val budgetResult: BudgetWidgetResult? = null,
    val cashFlowResult: CashFlowWidgetResult? = null,
    val timestamp: Long = System.currentTimeMillis()
)

object WidgetDataCache {
    private val precomputedStates = ConcurrentHashMap<Int, WidgetPrecomputedState>()
    private const val CACHE_VALIDITY_MS = 60_000L // 60 seconds

    fun update(profileId: Int, state: WidgetPrecomputedState) {
        precomputedStates[profileId] = state
    }

    fun get(profileId: Int): WidgetPrecomputedState? {
        val state = precomputedStates[profileId]
        if (state != null && System.currentTimeMillis() - state.timestamp < CACHE_VALIDITY_MS) {
            return state
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
            val manager = GlanceAppWidgetManager(context)
            
            try {
                Log.d(TAG, "Starting central precomputation for all widgets...")
                val startTime = System.currentTimeMillis()
                
                val profileId = container.settingsDataStore.currentProfileId.first()
                val globalBaseCurrency = container.settingsDataStore.currency.first()
                val globalPrecision = container.settingsDataStore.decimalPrecision.first()
                
                // Fetch basic data needed for all computations
                val allAccounts = container.accountRepository.getAllAccountsSync(profileId)
                val allSummaries = container.transactionRepository.getCurrencySummaries(profileId)
                val exchangeRatesRaw = container.currencyRepository.getExchangeRates(globalBaseCurrency)
                val exchangeRates = exchangeRatesRaw.mapValues { BigDecimal.valueOf(it.value) }
                
                // --- Precompute NetWorthWidget ---
                val nwIds = manager.getGlanceIds(NetWorthWidget::class.java)
                val nwResults = mutableMapOf<String, NetWorthWidgetResult>()
                nwIds.forEach { glanceId ->
                    try {
                        val prefs = getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId)
                        val settingsJson = prefs[NetWorthWidgetConfigActivity.KEY_SETTINGS]
                        val widgetSettings = settingsJson?.let {
                            try { Json.decodeFromString<NetWorthSettings>(it) } catch (e: Exception) { null }
                        } ?: NetWorthSettings(
                            timelineRange = prefs[NetWorthWidgetConfigActivity.KEY_TIMELINE]?.let {
                                try { Json.decodeFromString<TimelineRange>(it) } catch (e: Exception) { TimelineRange.AllTime }
                            } ?: TimelineRange.AllTime,
                            fontSize = prefs[NetWorthWidgetConfigActivity.KEY_FONT_SIZE] ?: "Medium"
                        )
                        
                        val instanceAccounts = allAccounts.filter { 
                            widgetSettings.selectedAccountIds == null || it.id in widgetSettings.selectedAccountIds
                        }
                        
                        nwResults[glanceId.toString()] = computeNetWorthResult(
                            timelineRange = widgetSettings.timelineRange,
                            accounts = instanceAccounts,
                            allSummaries = allSummaries,
                            baseCurrency = globalBaseCurrency,
                            exchangeRates = exchangeRates,
                            precision = globalPrecision,
                            fontSize = widgetSettings.fontSize,
                            transactionRepository = container.transactionRepository,
                            profileId = profileId,
                            selectedAccountIds = widgetSettings.selectedAccountIds
                        )
                    } catch (e: Exception) { Log.e(TAG, "NW precompute failed for $glanceId", e) }
                }

                // --- Precompute BalanceWidget ---
                val totalBalance = calculateNetWorthFromAccounts(allAccounts, globalBaseCurrency, exchangeRates, globalPrecision)
                val balanceResult = BalanceWidgetResult(totalBalance, globalBaseCurrency, globalPrecision)

                // --- Precompute TransactionHistoryWidget ---
                val recentTransactions = container.transactionRepository.getAllTransactionsSync(profileId)
                    .sortedByDescending { it.timestamp }
                    .take(4)
                val historyResult = TransactionHistoryWidgetResult(totalBalance, recentTransactions, globalBaseCurrency, globalPrecision)

                // --- Precompute BudgetWidget ---
                val budgets = container.budgetRepository.getBudgets().first()
                val budgetResult = BudgetWidgetResult(budgets, globalBaseCurrency, globalPrecision)

                // --- Precompute CashFlowWidget ---
                var totalIncome = BigDecimal.ZERO
                var totalExpense = BigDecimal.ZERO
                allSummaries.forEach { s ->
                    totalIncome = totalIncome.add(convertAmount(s.totalIncome, s.currency, globalBaseCurrency, exchangeRates, globalPrecision + 2))
                    totalExpense = totalExpense.add(convertAmount(s.totalExpense, s.currency, globalBaseCurrency, exchangeRates, globalPrecision + 2))
                }
                val cashFlowResult = CashFlowWidgetResult(
                    totalIncome.setScale(globalPrecision, RoundingMode.HALF_UP),
                    totalExpense.setScale(globalPrecision, RoundingMode.HALF_UP),
                    globalBaseCurrency,
                    globalPrecision
                )

                // Cache final state
                WidgetDataCache.update(profileId, WidgetPrecomputedState(
                    netWorthResults = nwResults,
                    balanceResult = balanceResult,
                    historyResult = historyResult,
                    budgetResult = budgetResult,
                    cashFlowResult = cashFlowResult
                ))
                
                Log.d(TAG, "Precomputation done in ${System.currentTimeMillis() - startTime}ms")

                // Update widgets
                val widgets = listOf(NetWorthWidget(), BalanceWidget(), TransactionHistoryWidget(), BudgetWidget(), CashFlowWidget())
                widgets.forEach { widget ->
                    val ids = manager.getGlanceIds(widget::class.java)
                    if (ids.isNotEmpty()) {
                        widget.updateAll(context)
                        delay(100)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Central fetch failed", e)
            }
        }
    }

    private suspend fun computeNetWorthResult(
        timelineRange: TimelineRange,
        accounts: List<Account>,
        allSummaries: List<CurrencySummary>,
        baseCurrency: String,
        exchangeRates: Map<String, BigDecimal>,
        precision: Int,
        fontSize: String,
        transactionRepository: com.osscabinet.ossWallet.domain.repository.TransactionRepository,
        profileId: Int,
        selectedAccountIds: List<String>?
    ): NetWorthWidgetResult {
        val (netWorth, netIncome, netExpense) = if (timelineRange is TimelineRange.AllTime) {
            val nw = calculateNetWorthFromAccounts(accounts, baseCurrency, exchangeRates, precision)
            var totalIncome = BigDecimal.ZERO
            var totalExpense = BigDecimal.ZERO
            allSummaries.forEach { s ->
                totalIncome = totalIncome.add(convertAmount(s.totalIncome, s.currency, baseCurrency, exchangeRates, precision + 4))
                totalExpense = totalExpense.add(convertAmount(s.totalExpense, s.currency, baseCurrency, exchangeRates, precision + 4))
            }
            Triple(nw, totalIncome.setScale(precision, RoundingMode.HALF_UP), totalExpense.setScale(precision, RoundingMode.HALF_UP))
        } else {
            val (start, end) = when (timelineRange) {
                is TimelineRange.ThisPeriod -> FilterUtils.getRangeForThis(timelineRange.period)
                is TimelineRange.LastPeriod -> FilterUtils.getRangeForLast(timelineRange.period)
                is TimelineRange.CustomRange -> Pair(timelineRange.start, timelineRange.end)
                else -> Pair(0L, Long.MAX_VALUE)
            }
            val summariesInRange = transactionRepository.getCurrencySummariesInRange(profileId, start, end)
            var totalIncome = BigDecimal.ZERO
            var totalExpense = BigDecimal.ZERO
            summariesInRange.forEach { s ->
                totalIncome = totalIncome.add(convertAmount(s.totalIncome, s.currency, baseCurrency, exchangeRates, precision + 4))
                totalExpense = totalExpense.add(convertAmount(s.totalExpense, s.currency, baseCurrency, exchangeRates, precision + 4))
            }
            val filter = TransactionFilter(startDate = start, endDate = end)
            val transactions = transactionRepository.getFilteredTransactionsSync(profileId, filter)
                .filter { selectedAccountIds == null || it.accountId in selectedAccountIds }
            val nw = calculateNetWorth(accounts, transactions, baseCurrency, exchangeRates, precision, includeInitialBalance = false)
            Triple(nw, totalIncome.setScale(precision, RoundingMode.HALF_UP), totalExpense.setScale(precision, RoundingMode.HALF_UP))
        }

        val timelineLabel = when (timelineRange) {
            is TimelineRange.AllTime -> "ALL TIME"
            is TimelineRange.ThisPeriod -> timelineRange.period.label.uppercase()
            is TimelineRange.LastPeriod -> timelineRange.period.label.uppercase()
            is TimelineRange.CustomRange -> {
                val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy").withZone(ZoneId.systemDefault())
                val start = formatter.format(Instant.ofEpochMilli(timelineRange.start))
                val end = formatter.format(Instant.ofEpochMilli(timelineRange.end))
                "$start - $end".uppercase()
            }
            else -> "ALL TIME"
        }
        return NetWorthWidgetResult(netWorth, netIncome, netExpense, baseCurrency, precision, fontSize, timelineLabel)
    }
}
