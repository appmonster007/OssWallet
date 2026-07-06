package com.osscabinet.ossWallet.ui.util

import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.repository.ComputationRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.domain.service.management.TransactionEntryService
import com.osscabinet.ossWallet.domain.usecase.EntryFormDataUseCase
import com.osscabinet.ossWallet.ui.components.entry.EntryFormSnapshot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal

/**
 * Base ViewModel for Transaction, PlannedPayment, and Template entry forms.
 *
 * Provides a single [entryFormSnapshot] StateFlow combining profile core data,
 * shortlisted currencies, and exchange rates. Subclasses subscribe in their own
 * `init` and handle any form-specific side effects (e.g. default account selection).
 *
 * Also owns [recalculateExchangeRate] with proper Job cancellation so stale
 * in-flight fetches are never applied. Subclasses expose their state via the
 * six abstract accessors below.
 */
@OptIn(ExperimentalCoroutinesApi::class)
abstract class BaseEntryViewModel(
    protected val entryFormDataUseCase: EntryFormDataUseCase,
    protected val computationRepository: ComputationRepository,
    settingsRepository: SettingsRepository
) : BaseViewModel(settingsRepository) {

    val entryFormSnapshot = combine(
        settingsRepository.currentProfileId
            .flatMapLatest { profileId -> entryFormDataUseCase.observeEntryCoreData(profileId) },
        effectiveShortlistedCurrencies,
        computationRepository.exchangeRates
    ) { data, currencies, rates ->
        EntryFormSnapshot(
            accounts = data.accounts,
            categories = data.categories,
            allTags = data.tags,
            templates = data.templates,
            allPayees = data.payees,
            allNotes = data.notes,
            shortlistedCurrencies = currencies,
            fullProviderCurrencies = CurrencyService.getAllCurrencies(),
            exchangeRates = rates
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), EntryFormSnapshot())

    protected var rateJob: Job? = null

    // Subclasses provide read access to their current state for rate calculation.
    protected abstract fun getSourceCurrency(): String
    protected abstract fun getTargetCurrency(): String
    protected abstract fun getIsTransfer(): Boolean
    protected abstract fun getIsRateFixed(): Boolean
    protected abstract fun getAmountValue(): BigDecimal
    protected abstract fun applyRateUpdate(rate: BigDecimal, targetAmountStr: String)

    protected fun recalculateExchangeRate() {
        rateJob?.cancel()
        rateJob = viewModelScope.launch {
            val srcCurrency = getSourceCurrency()
            val dstCurrency = getTargetCurrency()
            if (!getIsTransfer() || srcCurrency.isEmpty() || dstCurrency.isEmpty()) return@launch

            val rates = computationRepository.exchangeRatesSnapshot.first()

            // Re-read after suspension — abort if state changed or rate was pinned
            if (!getIsTransfer() ||
                getSourceCurrency() != srcCurrency ||
                getTargetCurrency() != dstCurrency ||
                getIsRateFixed()
            ) return@launch

            val rate = TransactionEntryService.getMarketRate(
                fromCurrency = srcCurrency,
                toCurrency = dstCurrency,
                exchangeRates = rates
            )
            applyRateUpdate(rate, TransactionEntryService.calculateTargetAmount(getAmountValue(), rate))
        }
    }
}
