package com.osscabinet.ossWallet.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.card.*
import com.osscabinet.ossWallet.domain.model.*
import com.osscabinet.ossWallet.domain.model.analytics.*
import com.osscabinet.ossWallet.domain.model.settings.*
import com.osscabinet.ossWallet.domain.repository.*
import com.osscabinet.ossWallet.domain.service.*
import com.osscabinet.ossWallet.domain.usecase.TransactionUseCases
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.*

@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsViewModel(
    private val transactionUseCases: TransactionUseCases,
    private val settingsRepository: SettingsRepository,
    private val computationRepository: ComputationRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val registry: CardComputerRegistry,
    override val cardRepository: CardRepository
) : ViewModel(), CardUpdateHandler {

    override val cardUpdateScope = viewModelScope
    override val cardScreenContext = ScreenContext.ANALYTICS

    // Analytics cards config - now using cardRepository
    val cardsConfig: StateFlow<DashboardCardsConfig> = cardRepository.getCardsConfig(cardScreenContext)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardCardsConfig())

    val uiState: StateFlow<AnalyticsUiState> = settingsRepository.currentProfileId
        .flatMapLatest { profileId ->
            combine(
                transactionUseCases.getTransactions(profileId),
                accountRepository.getAllAccounts(profileId),
                categoryRepository.categories,
                computationRepository.exchangeRates,
                computationRepository.selectedCurrency,
                computationRepository.hasLoadedExchangeRates,
                settingsRepository.decimalPrecision
            ) { args: Array<Any> ->
                @Suppress("UNCHECKED_CAST")
                val transactions = args[0] as List<Transaction>
                @Suppress("UNCHECKED_CAST")
                val accounts = args[1] as List<Account>
                @Suppress("UNCHECKED_CAST")
                val categories = args[2] as List<Category>
                @Suppress("UNCHECKED_CAST")
                val rates = args[3] as Map<String, BigDecimal>
                val baseCurrency = args[4] as String
                val hasLoadedRates = args[5] as Boolean
                val precision = args[6] as Int

                if (!hasLoadedRates) {
                    return@combine AnalyticsUiState.Loading
                }

                // Invalidate registry cache when data changes to ensure cards refresh
                registry.clearCache()

                AnalyticsUiState.Success(
                    allTransactions = transactions,
                    accounts = accounts,
                    allCategories = categories,
                    exchangeRates = rates,
                    selectedCurrency = baseCurrency,
                    decimalPrecision = precision
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AnalyticsUiState.Loading
        )
}

sealed class AnalyticsUiState {
    object Loading : AnalyticsUiState()
    data class Success(
        val allTransactions: List<Transaction> = emptyList(),
        val accounts: List<Account> = emptyList(),
        val allCategories: List<Category> = emptyList(),
        val selectedCurrency: String = "USD",
        val exchangeRates: Map<String, BigDecimal> = emptyMap(),
        val decimalPrecision: Int = 2
    ) : AnalyticsUiState()
    data class Error(val message: String) : AnalyticsUiState()
}
