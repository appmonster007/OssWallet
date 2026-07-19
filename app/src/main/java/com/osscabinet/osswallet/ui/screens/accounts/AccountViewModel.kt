package com.osscabinet.ossWallet.ui.screens.accounts

import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.repository.AccountRepository
import com.osscabinet.ossWallet.domain.repository.ComputationRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.repository.TransactionRepository
import com.osscabinet.ossWallet.domain.service.aggregation.TransactionAggregationService
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.domain.service.currency.ExchangeRateSnapshot
import com.osscabinet.ossWallet.domain.service.query.DatabaseQueryService
import com.osscabinet.ossWallet.ui.util.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AccountViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val computationRepository: ComputationRepository,
    private val queryService: DatabaseQueryService,
    settingsRepository: SettingsRepository,
) : BaseViewModel(settingsRepository) {

    val accounts: StateFlow<List<Account>> = settingsRepository.currentProfileId
        .flatMapLatest { profileId -> accountRepository.getAllAccounts(profileId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    private data class AccountFinancialSnapshot(
        val accounts: List<Account>,
        val version: Long,
        val rates: ExchangeRateSnapshot
    )

    val accountBalances: StateFlow<Map<String, BigDecimal>> = settingsRepository.currentProfileId
        .flatMapLatest { profileId ->
            combine(
                accountRepository.getAllAccounts(profileId),
                transactionRepository.getLatestTransactionVersion(profileId),
                computationRepository.exchangeRatesSnapshot
            ) { accounts, version, rates ->
                AccountFinancialSnapshot(accounts, version ?: 0L, rates)
            }.distinctUntilChanged()
                .map { snapshot ->
                    val summaries = TransactionAggregationService.getSummaryByAccountAndCurrency(
                        queryService,
                        profileId
                    ).getOrDefault(emptyMap())

                    snapshot.accounts.associate { account ->
                        val accountSummaries = summaries[account.id] ?: emptyMap()
                        account.id to CurrencyService.calculateTotalFinancialSummary(
                            accountSummaries,
                            account.currencyCode,
                            snapshot.rates
                        ).totalAmount
                    }
                }.flowOn(Dispatchers.Default)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyMap())

    fun deleteAccount(accountId: String) {
        viewModelScope.launch {
            withProfile { profileId ->
                accountRepository.deleteAccount(accountId, profileId)
            }
        }
    }

    fun updateAccountsOrder(updatedAccounts: List<Account>) {
        viewModelScope.launch {
            withProfile { profileId ->
                val accountsWithOrder = updatedAccounts.mapIndexed { index, account ->
                    account.copy(displayOrder = index)
                }
                accountRepository.updateAccounts(accountsWithOrder, profileId)
            }
        }
    }

    fun toggleArchiveAccount(account: Account) {
        viewModelScope.launch {
            withProfile { profileId ->
                accountRepository.insertAccount(
                    account.copy(isArchived = !account.isArchived),
                    profileId
                )
            }
        }
    }
}
