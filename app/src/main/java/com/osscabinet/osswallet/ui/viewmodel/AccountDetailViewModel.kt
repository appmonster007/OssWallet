package com.osscabinet.ossWallet.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.repository.AccountRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.repository.CardRepository
import com.osscabinet.ossWallet.domain.repository.ComputationRepository
import com.osscabinet.ossWallet.domain.usecase.*
import com.osscabinet.ossWallet.domain.model.analytics.*
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.model.analytics.ComputationContext
import com.osscabinet.ossWallet.domain.util.CardDataCache
import com.osscabinet.ossWallet.domain.util.CachedCardData
import com.osscabinet.ossWallet.domain.service.TransactionFilterService
import com.osscabinet.ossWallet.domain.service.AggregationService
import com.osscabinet.ossWallet.domain.service.CurrencyConversionService
import com.osscabinet.ossWallet.domain.service.FinancialSummary
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import java.math.RoundingMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class AccountDetailViewModel(
    private val accountId: String,
    private val accountRepository: AccountRepository,
    private val transactionUseCases: TransactionUseCases,
    private val settingsRepository: SettingsRepository,
    private val cardRepository: CardRepository,
    private val computationRepository: ComputationRepository
) : ViewModel() {

    private val cardDataCache = CardDataCache()

    // Direct implementations using split services
    suspend fun computeCashFlowCard(
        cardConfig: com.osscabinet.ossWallet.domain.model.settings.CashFlowCardConfig,
        transactions: List<Transaction>,
        baseCurrency: String,
        exchangeRates: Map<String, BigDecimal>,
        precision: Int
    ): CashFlowResult {
        val cacheKey = "cashflow_${cardConfig.cardId}_${cardConfig.timelineRange}"
        return cardDataCache.getOrCompute(cacheKey) {
            val filtered = TransactionFilterService.filter(transactions, cardConfig.filter)
            val breakdown = AggregationService.calculateIncomeExpenseBreakdown(
                transactions = filtered,
                exchangeRates = exchangeRates,
                baseCurrency = baseCurrency
            )
            val cashFlowBreakdown = (breakdown.incomeCurrencyBreakdown.keys + breakdown.expenseCurrencyBreakdown.keys)
                .distinct().sorted().map { curr ->
                    val inc = breakdown.incomeCurrencyBreakdown[curr] ?: FinancialSummary.EMPTY
                    val exp = breakdown.expenseCurrencyBreakdown[curr] ?: FinancialSummary.EMPTY
                    CashFlowBreakdownItem(
                        currency = curr,
                        income = inc.totalAmount,
                        incomeInBase = CurrencyConversionService.convertToBaseCurrency(inc.totalAmount, curr, baseCurrency, exchangeRates),
                        expense = exp.totalAmount,
                        expenseInBase = CurrencyConversionService.convertToBaseCurrency(exp.totalAmount, curr, baseCurrency, exchangeRates)
                    )
                }
            CashFlowResult(
                totalIncomeInBase = breakdown.totalIncome.setScale(precision, java.math.RoundingMode.HALF_UP),
                totalExpensesInBase = breakdown.totalExpense.setScale(precision, java.math.RoundingMode.HALF_UP),
                netInBase = breakdown.netFlow.setScale(precision, java.math.RoundingMode.HALF_UP),
                breakdown = cashFlowBreakdown,
                filteredTransactions = filtered
            )
        }
    }

    suspend fun computeCurrencyBalancesCard(
        cardConfig: com.osscabinet.ossWallet.domain.model.settings.CurrencyBalancesCardConfig,
        accounts: List<Account>,
        transactions: List<Transaction>,
        baseCurrency: String,
        exchangeRates: Map<String, BigDecimal>,
        precision: Int
    ): List<CurrencyAggregationItem> {
        val cacheKey = "currency_${cardConfig.cardId}_${cardConfig.timelineRange}"
        return cardDataCache.getOrCompute(cacheKey) {
            val filteredTx = TransactionFilterService.filter(transactions, cardConfig.filter)
            val currencySummaries = AggregationService.calculateSummaryByCurrency(filteredTx)
            currencySummaries.map { (curr, summary) ->
                CurrencyAggregationItem(
                    currency = curr,
                    amount = summary.totalAmount,
                    amountInBase = CurrencyConversionService.convertToBaseCurrency(summary.totalAmount, curr, baseCurrency, exchangeRates)
                )
            }.sortedByDescending { it.amountInBase.abs() }
        }
    }

    fun updateCurrencyBalancesLayout(layout: String) {
        viewModelScope.launch {
            settingsRepository.updateAccountDetailSettings { it.copy(currencyBalancesSection = it.currencyBalancesSection.copy(layout = layout)) }
        }
    }

    fun updateShowBaseConversion(show: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateAccountDetailSettings { it.copy(currencyBalancesSection = it.currencyBalancesSection.copy(showBaseConversion = show)) }
        }
    }

    fun updateCurrencyBalancesTimelineRange(range: TimelineRange) {
        viewModelScope.launch {
            settingsRepository.updateAccountDetailSettings { it.copy(currencyBalancesSection = it.currencyBalancesSection.copy(timelineRange = range)) }
        }
    }

    fun updateCashFlowShowBaseConversion(show: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateAccountDetailSettings { it.copy(cashFlowSection = it.cashFlowSection.copy(showBaseConversion = show)) }
        }
    }

    fun updateCashFlowTimelineRange(range: TimelineRange) {
        viewModelScope.launch {
            settingsRepository.updateAccountDetailSettings { it.copy(cashFlowSection = it.cashFlowSection.copy(timelineRange = range)) }
        }
    }

    fun updateShowPlannedPayments(show: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateAccountDetailSettings { it.copy(transactionsSection = it.transactionsSection.copy(showPlannedPayments = show)) }
        }
    }

    fun updateUseNoteAsHeader(use: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateAccountDetailSettings { it.copy(transactionsSection = it.transactionsSection.copy(useNoteAsHeader = use)) }
        }
    }

    // Card-specific update methods
    fun updateCardHeroShowCurrencyBadges(cardId: String, show: Boolean) {
        viewModelScope.launch {
            cardRepository.updateAccountDetailCard(cardId) { config ->
                if (config is com.osscabinet.ossWallet.domain.model.settings.AccountHeroCardConfig) {
                    config.copy(showCurrencyBadges = show)
                } else config
            }
            cardDataCache.invalidateCard(cardId)
        }
    }

    fun updateCardCashFlowShowBaseConversion(cardId: String, show: Boolean) {
        viewModelScope.launch {
            cardRepository.updateAccountDetailCard(cardId) { config ->
                if (config is com.osscabinet.ossWallet.domain.model.settings.CashFlowCardConfig) {
                    config.copy(showBaseConversion = show)
                } else config
            }
            cardDataCache.invalidateCard(cardId)
        }
    }

    fun updateCardCashFlowTimelineRange(cardId: String, range: TimelineRange) {
        viewModelScope.launch {
            cardRepository.updateAccountDetailCard(cardId) { config ->
                if (config is com.osscabinet.ossWallet.domain.model.settings.CashFlowCardConfig) {
                    config.copy(timelineRange = range)
                } else config
            }
            cardDataCache.invalidateCard(cardId)
        }
    }

    fun updateCardCurrencyBalancesLayout(cardId: String, layout: String) {
        viewModelScope.launch {
            cardRepository.updateAccountDetailCard(cardId) { config ->
                if (config is com.osscabinet.ossWallet.domain.model.settings.CurrencyBalancesCardConfig) {
                    config.copy(layout = layout)
                } else config
            }
            cardDataCache.invalidateCard(cardId)
        }
    }

    fun updateCardCurrencyBalancesShowBaseConversion(cardId: String, show: Boolean) {
        viewModelScope.launch {
            cardRepository.updateAccountDetailCard(cardId) { config ->
                if (config is com.osscabinet.ossWallet.domain.model.settings.CurrencyBalancesCardConfig) {
                    config.copy(showBaseConversion = show)
                } else config
            }
            cardDataCache.invalidateCard(cardId)
        }
    }

    fun updateCardCurrencyBalancesTimelineRange(cardId: String, range: TimelineRange) {
        viewModelScope.launch {
            cardRepository.updateAccountDetailCard(cardId) { config ->
                if (config is com.osscabinet.ossWallet.domain.model.settings.CurrencyBalancesCardConfig) {
                    config.copy(timelineRange = range)
                } else config
            }
            cardDataCache.invalidateCard(cardId)
        }
    }

    fun removeAccountDetailCard(cardId: String) {
        viewModelScope.launch {
            cardRepository.updateAccountDetailCard(cardId) { it } // Mock update to trigger re-flow if needed, but repository should have a remove method
            // cardRepository.removeAccountDetailCard(cardId) // Assuming this exists or using update to remove
            cardDataCache.invalidateCard(cardId)
        }
    }

    val uiState: StateFlow<AccountDetailUiState> = settingsRepository.currentProfileId
        .flatMapLatest { profileId ->
            combine(
                accountRepository.getAllAccounts(profileId),
                transactionUseCases.getTransactions(profileId),
                computationRepository.exchangeRates,
                computationRepository.selectedCurrency,
                computationRepository.hasLoadedExchangeRates,
                settingsRepository.decimalPrecision,
                settingsRepository.accountDetailSettings
            ) { args: Array<*> ->
                @Suppress("UNCHECKED_CAST")
                val accounts = args[0] as List<Account>
                @Suppress("UNCHECKED_CAST")
                val allTransactions = args[1] as List<Transaction>
                @Suppress("UNCHECKED_CAST")
                val rates = args[2] as Map<String, BigDecimal>
                val baseCurrency = args[3] as String
                val hasLoadedExchangeRates = args[4] as Boolean
                val precision = args[5] as Int
                val settings = args[6] as com.osscabinet.ossWallet.data.local.AccountDetailSettings

                // Invalidate cache when transactions change
                cardDataCache.invalidateAll()
                
                val account = accounts.find { it.id == accountId } ?: return@combine AccountDetailUiState.Error("Account not found")

                val computationContext = ComputationContext(baseCurrency, rates, precision)

                var accountTransactions = allTransactions.filter { it.accountId == accountId }

                if (!hasLoadedExchangeRates) {
                    return@combine AccountDetailUiState.Loading
                }

                if (!settings.transactionsSection.showPlannedPayments) {
                    val now = System.currentTimeMillis()
                    accountTransactions = accountTransactions.filter { it.timestamp <= now }
                }

                // Compute currency totals directly
                val filteredTxForCurrency = TransactionFilterService.filter(accountTransactions, com.osscabinet.ossWallet.domain.model.TransactionFilter(timelineRange = settings.currencyBalancesSection.timelineRange))
                val currencySummaries = AggregationService.calculateSummaryByCurrency(filteredTxForCurrency)
                val currencyTotals = currencySummaries.map { (curr, summary) ->
                    CurrencyAggregationItem(
                        currency = curr,
                        amount = summary.totalAmount,
                        amountInBase = CurrencyConversionService.convertToBaseCurrency(summary.totalAmount, curr, baseCurrency, rates)
                    )
                }.sortedByDescending { it.amountInBase.abs() }

                // Compute cash flow directly
                val filteredTxForCashFlow = TransactionFilterService.filter(accountTransactions, com.osscabinet.ossWallet.domain.model.TransactionFilter(timelineRange = settings.cashFlowSection.timelineRange))
                val cfBreakdown = AggregationService.calculateIncomeExpenseBreakdown(
                    transactions = filteredTxForCashFlow,
                    exchangeRates = rates,
                    baseCurrency = baseCurrency
                )
                val cfCashFlowBreakdown = (cfBreakdown.incomeCurrencyBreakdown.keys + cfBreakdown.expenseCurrencyBreakdown.keys)
                    .distinct().sorted().map { curr ->
                        val inc = cfBreakdown.incomeCurrencyBreakdown[curr] ?: FinancialSummary.EMPTY
                        val exp = cfBreakdown.expenseCurrencyBreakdown[curr] ?: FinancialSummary.EMPTY
                        CashFlowBreakdownItem(
                            currency = curr,
                            income = inc.totalAmount,
                            incomeInBase = CurrencyConversionService.convertToBaseCurrency(inc.totalAmount, curr, baseCurrency, rates),
                            expense = exp.totalAmount,
                            expenseInBase = CurrencyConversionService.convertToBaseCurrency(exp.totalAmount, curr, baseCurrency, rates)
                        )
                    }
                val cashFlowResult = CashFlowResult(
                    totalIncomeInBase = cfBreakdown.totalIncome.setScale(precision, java.math.RoundingMode.HALF_UP),
                    totalExpensesInBase = cfBreakdown.totalExpense.setScale(precision, java.math.RoundingMode.HALF_UP),
                    netInBase = cfBreakdown.netFlow.setScale(precision, java.math.RoundingMode.HALF_UP),
                    breakdown = cfCashFlowBreakdown,
                    filteredTransactions = filteredTxForCashFlow
                )

                // Compute account stats directly
                val filterForAccounts = com.osscabinet.ossWallet.domain.model.TransactionFilter(timelineRange = settings.currencyBalancesSection.timelineRange)
                val filteredTxForAccounts = TransactionFilterService.filter(accountTransactions, filterForAccounts)
                val accountBreakdowns = AggregationService.breakdownByAccount(
                    transactions = filteredTxForAccounts,
                    accounts = listOf(account),
                    exchangeRates = rates,
                    baseCurrency = baseCurrency
                )
                val b = accountBreakdowns.firstOrNull()
                val updatedAccount = if (b != null) {
                    val totalInAccountCurrency = b.currencyBreakdown.map { (curr, summary) ->
                        CurrencyConversionService.convertToBaseCurrency(summary.totalAmount, curr, account.currencyCode, rates)
                    }.fold(BigDecimal.ZERO) { acc, amount -> acc.add(amount) }
                    account.copy(balance = totalInAccountCurrency)
                } else account

                AccountDetailUiState.Success(
                    account = updatedAccount,
                    allAccountTransactions = accountTransactions,
                    currencyTotals = currencyTotals,
                    baseCurrency = baseCurrency,
                    exchangeRates = rates,
                    decimalPrecision = precision,
                    currencyBalancesLayout = settings.currencyBalancesSection.layout,
                    showBaseConversion = settings.currencyBalancesSection.showBaseConversion,
                    currencyBalancesTimelineRange = settings.currencyBalancesSection.timelineRange,
                    cashFlowShowBaseConversion = settings.cashFlowSection.showBaseConversion,
                    cashFlowTimelineRange = settings.cashFlowSection.timelineRange,
                    showPlannedPayments = settings.transactionsSection.showPlannedPayments,
                    useNoteAsHeader = settings.transactionsSection.useNoteAsHeader,
                    cashFlowResult = cashFlowResult
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AccountDetailUiState.Loading
        )
}

sealed class AccountDetailUiState {
    object Loading : AccountDetailUiState()
    data class Success(
        val account: Account,
        val allAccountTransactions: List<Transaction>,
        val currencyTotals: List<CurrencyAggregationItem> = emptyList(),
        val baseCurrency: String,
        val exchangeRates: Map<String, BigDecimal>,
        val decimalPrecision: Int,
        val currencyBalancesLayout: String,
        val showBaseConversion: Boolean,
        val currencyBalancesTimelineRange: TimelineRange,
        val cashFlowShowBaseConversion: Boolean,
        val cashFlowTimelineRange: TimelineRange,
        val showPlannedPayments: Boolean = false,
        val useNoteAsHeader: Boolean = false,
        val cashFlowResult: CashFlowResult? = null
    ) : AccountDetailUiState()
    data class Error(val message: String) : AccountDetailUiState()
}
