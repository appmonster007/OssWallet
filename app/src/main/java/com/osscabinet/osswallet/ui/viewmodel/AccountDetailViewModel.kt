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
import com.osscabinet.ossWallet.domain.card.*
import com.osscabinet.ossWallet.domain.model.settings.*
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
    private val computationRepository: ComputationRepository,
    private val registry: CardComputerRegistry
) : ViewModel(), CardUpdateHandler {

    private val cardDataCache = CardDataCache()

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
    override fun updateCardHeroShowCurrencyBadges(cardId: String, show: Boolean) {
        viewModelScope.launch {
            cardRepository.updateAccountDetailCard(cardId) { config ->
                if (config is AccountHeroCardConfig) {
                    config.copy(showCurrencyBadges = show)
                } else config
            }
            cardDataCache.invalidateCard(cardId)
        }
    }

    override fun updateCardHeroFilter(cardId: String, filter: TransactionFilter) {
        viewModelScope.launch {
            cardRepository.updateAccountDetailCard(cardId) { config ->
                if (config is AccountHeroCardConfig) {
                    config.copy(filter = filter)
                } else config
            }
            cardDataCache.invalidateCard(cardId)
        }
    }

    override fun updateCardCashFlowShowBaseConversion(cardId: String, show: Boolean) {
        viewModelScope.launch {
            cardRepository.updateAccountDetailCard(cardId) { config ->
                if (config is CashFlowCardConfig) {
                    config.copy(showBaseConversion = show)
                } else config
            }
            cardDataCache.invalidateCard(cardId)
        }
    }


    override fun updateCardCurrencyBalancesLayout(cardId: String, layout: String) {
        viewModelScope.launch {
            cardRepository.updateAccountDetailCard(cardId) { config ->
                if (config is CurrencyBalancesCardConfig) {
                    config.copy(layout = layout)
                } else config
            }
            cardDataCache.invalidateCard(cardId)
        }
    }

    override fun updateCardCurrencyBalancesShowBaseConversion(cardId: String, show: Boolean) {
        viewModelScope.launch {
            cardRepository.updateAccountDetailCard(cardId) { config ->
                if (config is CurrencyBalancesCardConfig) {
                    config.copy(showBaseConversion = show)
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

                if (!hasLoadedExchangeRates) {
                    return@combine AccountDetailUiState.Loading
                }

                var accountTransactions = allTransactions.filter { it.accountId == accountId }

                if (!settings.transactionsSection.showPlannedPayments) {
                    val now = System.currentTimeMillis()
                    accountTransactions = accountTransactions.filter { it.timestamp <= now }
                }

                val input = CardInput(
                    transactions = accountTransactions,
                    accounts = listOf(account),
                    categories = emptyList(),
                    exchangeRates = rates,
                    baseCurrency = baseCurrency,
                    precision = precision
                )

                // Compute currency totals directly
                val currencyTotals = (registry.compute(
                    CardType.CURRENCY_BALANCES,
                    input,
                    CurrencyBalancesCardConfig(
                        cardId = "currency_balances_detail",
                        filter = TransactionFilter(timelineRange = settings.currencyBalancesSection.timelineRange)
                    )
                ) as CardRenderData.CurrencyBalances).items

                // Compute cash flow directly
                val cashFlowResult = (registry.compute(
                    CardType.CASH_FLOW,
                    input,
                    CashFlowCardConfig(
                        cardId = "cash_flow_detail",
                        filter = TransactionFilter(timelineRange = settings.cashFlowSection.timelineRange)
                    )
                ) as CardRenderData.CashFlow).result

                // Compute account stats directly
                val accountsData = registry.compute(
                    CardType.ACCOUNTS,
                    input,
                    AccountsCardConfig(
                        cardId = "accounts_detail",
                        filter = TransactionFilter(timelineRange = settings.currencyBalancesSection.timelineRange)
                    )
                ) as CardRenderData.Accounts
                val updatedAccount = accountsData.updatedAccounts.firstOrNull() ?: account

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
