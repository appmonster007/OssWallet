package com.osscabinet.ossWallet.ui.screens.loans.entry

import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.model.Filter
import com.osscabinet.ossWallet.domain.model.Loan
import com.osscabinet.ossWallet.domain.model.LoanStatus
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.repository.AccountRepository
import com.osscabinet.ossWallet.domain.repository.CategoryRepository
import com.osscabinet.ossWallet.domain.repository.FilterRepository
import com.osscabinet.ossWallet.domain.repository.LoanRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.repository.TagRepository
import com.osscabinet.ossWallet.domain.usecase.TransactionUseCases
import com.osscabinet.ossWallet.ui.util.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LoanEntryViewModel @Inject constructor(
    private val loanRepository: LoanRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val tagRepository: TagRepository,
    private val transactionUseCases: TransactionUseCases,
    private val filterRepository: FilterRepository,
    settingsRepository: SettingsRepository
) : BaseViewModel(settingsRepository) {

    private val _state = MutableStateFlow(LoanEntryState())
    val state: StateFlow<LoanEntryState> = _state.asStateFlow()

    init {
        loadInitialData()
        viewModelScope.launch {
            effectiveShortlistedCurrencies.collect { currencies ->
                _state.update { it.copy(shortlistedCurrencies = currencies) }
            }
        }
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            settingsRepository.currentProfileId.flatMapLatest { profileId ->
                val uniqueNotes = transactionUseCases.getUniqueNotes(profileId)
                val uniquePayees = transactionUseCases.getUniquePayees(profileId)
                combine(
                    accountRepository.getAllAccounts(profileId),
                    categoryRepository.categories,
                    tagRepository.getAllTags(),
                    filterRepository.getAllFilters()
                ) { accounts, categories, tags, filters ->
                    _state.update {
                        it.copy(
                            accounts = accounts,
                            categories = categories,
                            availableTags = tags,
                            uniquePayees = uniquePayees,
                            uniqueNotes = uniqueNotes,
                            currentProfileId = profileId,
                            availableFilters = filters,
                            selectedCategoryIds = if (it.isEditMode) it.selectedCategoryIds else emptySet(),
                            selectedPaymentMethods = if (it.isEditMode) it.selectedPaymentMethods else emptySet(),
                            selectedStatuses = if (it.isEditMode) it.selectedStatuses else emptySet(),
                            selectedTrackTypes = if (it.isEditMode) it.selectedTrackTypes else setOf(
                                TransactionType.EXPENSE,
                                TransactionType.INCOME
                            ),
                            selectedCurrencies = if (it.isEditMode) it.selectedCurrencies else emptySet(),
                            selectedTags = if (it.isEditMode) it.selectedTags else emptySet()
                        )
                    }
                }
            }.collect()
        }
    }

    fun initialize(loanId: Long? = null) {
        if (loanId == null) {
            _state.update {
                it.copy(
                    isEditMode = false,
                    loanId = null,
                    selectedCategoryIds = emptySet(),
                    selectedPaymentMethods = emptySet(),
                    selectedStatuses = emptySet(),
                    selectedTrackTypes = setOf(TransactionType.EXPENSE, TransactionType.INCOME),
                    selectedCurrencies = emptySet(),
                    selectedTags = emptySet(),
                    queryNote = "",
                    queryPayee = "",
                    isNoteRegex = false,
                    isPayeeRegex = false,
                    minAmount = "",
                    maxAmount = "",
                    hasWarranty = null,
                    warrantyExpirationStart = null,
                    warrantyExpirationEnd = null,
                    timelineRange = TimelineRange.AllTime(excludeFuture = true)
                )
            }
            return
        }

        viewModelScope.launch {
            loanRepository.getLoanById(loanId)?.let { loan: Loan ->
                val filter = loan.filterId?.let { id -> filterRepository.getFilterById(id) }
                val tf = filter?.filter ?: TransactionFilter()
                val usageCount =
                    loan.filterId?.let { filterRepository.getFilterUsageCount(it) } ?: 0
                _state.update {
                    it.copy(
                        loanId = loanId,
                        isEditMode = true,
                        existingLoan = loan,
                        name = loan.name,
                        principalAmount = loan.principalAmount.toPlainString(),
                        currency = loan.currency,
                        loanType = loan.loanType,
                        counterparty = loan.lenderOrBorrowerName,
                        interestRate = loan.interestRate?.toString() ?: "",
                        startDate = loan.startDate,
                        hasSettlementDate = loan.expectedSettlementDate != null,
                        settlementDate = loan.expectedSettlementDate ?: System.currentTimeMillis(),
                        linkedAccountId = loan.linkedAccountId,
                        timelineRange = loan.timelineRange,
                        selectedTrackTypes = loan.trackTypes.toSet(),
                        selectedCategoryIds = loan.categoryIds.toSet(),
                        selectedCurrencies = loan.currencies.toSet(),
                        selectedTags = loan.tags.toSet(),
                        selectedPaymentMethods = loan.paymentMethods.toSet(),
                        selectedStatuses = loan.statuses.toSet(),
                        linkedTransactionIds = loan.linkedTransactionIds.toSet(),
                        filterId = loan.filterId,
                        filterUsageCount = usageCount,
                        queryNote = tf.queryNote ?: "",
                        isNoteRegex = tf.isNoteRegex,
                        queryPayee = tf.queryPayee ?: "",
                        isPayeeRegex = tf.isPayeeRegex,
                        minAmount = tf.minAmount?.toPlainString() ?: "",
                        maxAmount = tf.maxAmount?.toPlainString() ?: "",
                        hasWarranty = tf.hasWarranty,
                        warrantyExpirationStart = tf.warrantyExpirationStart,
                        warrantyExpirationEnd = tf.warrantyExpirationEnd,
                        excludeFuture = tf.timelineRange.excludeFuture
                    )
                }
            }
        }
    }

    fun onEvent(event: LoanEntryEvent) {
        when (event) {
            is LoanEntryEvent.SetName -> _state.update { it.copy(name = event.name) }
            is LoanEntryEvent.SetPrincipalAmount -> _state.update { it.copy(principalAmount = event.amount) }
            is LoanEntryEvent.SetCurrency -> _state.update { it.copy(currency = event.currency) }
            is LoanEntryEvent.SetLoanType -> _state.update { it.copy(loanType = event.type) }
            is LoanEntryEvent.SetCounterparty -> _state.update { it.copy(counterparty = event.counterparty) }
            is LoanEntryEvent.SetInterestRate -> _state.update { it.copy(interestRate = event.rate) }
            is LoanEntryEvent.SetNote -> _state.update { it.copy(note = event.note) }
            is LoanEntryEvent.SetTrackTypes -> _state.update { it.copy(selectedTrackTypes = event.types) }
            is LoanEntryEvent.SetCategoryIds -> _state.update { it.copy(selectedCategoryIds = event.ids) }
            is LoanEntryEvent.SetCurrencies -> _state.update { it.copy(selectedCurrencies = event.currencies) }
            is LoanEntryEvent.SetTags -> _state.update { it.copy(selectedTags = event.tags) }
            is LoanEntryEvent.SetQueryNote -> _state.update { it.copy(queryNote = event.query) }
            is LoanEntryEvent.SetNoteRegex -> _state.update { it.copy(isNoteRegex = event.isRegex) }
            is LoanEntryEvent.SetQueryPayee -> _state.update { it.copy(queryPayee = event.query) }
            is LoanEntryEvent.SetPayeeRegex -> _state.update { it.copy(isPayeeRegex = event.isRegex) }
            is LoanEntryEvent.SetMinAmount -> _state.update { it.copy(minAmount = event.amount) }
            is LoanEntryEvent.SetMaxAmount -> _state.update { it.copy(maxAmount = event.amount) }
            is LoanEntryEvent.SetHasWarranty -> _state.update { it.copy(hasWarranty = event.hasWarranty) }
            is LoanEntryEvent.SetWarrantyExpirationStart -> _state.update {
                it.copy(
                    warrantyExpirationStart = event.date
                )
            }

            is LoanEntryEvent.SetWarrantyExpirationEnd -> _state.update {
                it.copy(
                    warrantyExpirationEnd = event.date
                )
            }

            is LoanEntryEvent.SetTimelineRange -> _state.update { it.copy(timelineRange = event.range, excludeFuture = event.range.excludeFuture) }
            is LoanEntryEvent.SetExcludeFuture -> _state.update { 
                val newRange = it.timelineRange.withExcludeFuture(event.exclude)
                it.copy(excludeFuture = event.exclude, timelineRange = newRange) 
            }
            is LoanEntryEvent.SetPaymentMethods -> _state.update { it.copy(selectedPaymentMethods = event.methods) }
            is LoanEntryEvent.SetStatuses -> _state.update { it.copy(selectedStatuses = event.statuses) }
            is LoanEntryEvent.SetLinkedTransactions -> _state.update { it.copy(linkedTransactionIds = event.ids) }
            is LoanEntryEvent.SetStartDate -> _state.update { it.copy(startDate = event.date) }
            is LoanEntryEvent.SetHasSettlementDate -> _state.update { it.copy(hasSettlementDate = event.has) }
            is LoanEntryEvent.SetSettlementDate -> _state.update { it.copy(settlementDate = event.date) }
            is LoanEntryEvent.SetLinkedAccountId -> _state.update { it.copy(linkedAccountId = event.id) }
            is LoanEntryEvent.SetFilterId -> {
                val filter = _state.value.availableFilters.find { it.id == event.filterId }
                if (filter != null) {
                    _state.update { state ->
                        state.copy(
                            selectedCategoryIds = filter.filter.categoryIds.toSet(),
                            selectedCurrencies = filter.filter.currencies.toSet(),
                            selectedTags = filter.filter.tags.toSet(),
                            selectedPaymentMethods = filter.filter.paymentMethods.toSet(),
                            selectedStatuses = filter.filter.statuses.toSet(),
                            linkedTransactionIds = filter.filter.linkedTransactionIds.toSet(),
                            linkedAccountId = filter.filter.accountIds.firstOrNull()
                                ?: state.linkedAccountId,
                            queryNote = filter.filter.queryNote ?: "",
                            isNoteRegex = filter.filter.isNoteRegex,
                            queryPayee = filter.filter.queryPayee ?: "",
                            isPayeeRegex = filter.filter.isPayeeRegex,
                            minAmount = filter.filter.minAmount?.toPlainString() ?: "",
                            maxAmount = filter.filter.maxAmount?.toPlainString() ?: "",
                            hasWarranty = filter.filter.hasWarranty,
                            warrantyExpirationStart = filter.filter.warrantyExpirationStart,
                            warrantyExpirationEnd = filter.filter.warrantyExpirationEnd,
                            timelineRange = filter.filter.timelineRange,
                            excludeFuture = filter.filter.timelineRange.excludeFuture,
                            selectedTrackTypes = if (filter.filter.transactionTypes.isNotEmpty()) {
                                filter.filter.transactionTypes.toSet()
                            } else {
                                state.selectedTrackTypes
                            }
                        )
                    }
                }
            }

            LoanEntryEvent.MakeFilterUnique -> {}
            is LoanEntryEvent.SetDeleteFilterWithLoan -> {}
            is LoanEntryEvent.ShowDeleteDialog -> _state.update { it.copy(showDeleteDialog = event.show) }
            is LoanEntryEvent.ShowStartDatePicker -> _state.update { it.copy(showStartDatePicker = event.show) }
            is LoanEntryEvent.ShowSettlementDatePicker -> _state.update {
                it.copy(
                    showSettlementDatePicker = event.show
                )
            }

            is LoanEntryEvent.ShowNumpad -> _state.update { it.copy(showNumpad = event.show) }
            is LoanEntryEvent.ShowInterestNumpad -> _state.update { it.copy(showInterestNumpad = event.show) }
            is LoanEntryEvent.ShowAccountSheet -> _state.update { it.copy(showAccountSheet = event.show) }
            is LoanEntryEvent.ShowTrackTypeSheet -> _state.update { it.copy(showTrackTypeSheet = event.show) }
            is LoanEntryEvent.ShowTimelineSheet -> _state.update { it.copy(showTimelineSheet = event.show) }
            is LoanEntryEvent.ShowCategorySheet -> _state.update { it.copy(showCategorySheet = event.show) }
            is LoanEntryEvent.ShowTagSheet -> _state.update { it.copy(showTagSheet = event.show) }
            is LoanEntryEvent.ShowCurrencyFilterSheet -> _state.update {
                it.copy(
                    showCurrencyFilterSheet = event.show
                )
            }

            is LoanEntryEvent.ShowPaymentMethodSheet -> _state.update {
                it.copy(
                    showPaymentMethodSheet = event.show
                )
            }

            is LoanEntryEvent.ShowStatusSheet -> _state.update { it.copy(showStatusSheet = event.show) }
            is LoanEntryEvent.ShowLinkSheet -> _state.update { it.copy(showLinkSheet = event.show) }
            is LoanEntryEvent.ShowFilterSheet -> _state.update { it.copy(showFilterSheet = event.show) }
            is LoanEntryEvent.ShowMinAmountSheet -> _state.update { it.copy(showMinAmountSheet = event.show) }
            is LoanEntryEvent.ShowMaxAmountSheet -> _state.update { it.copy(showMaxAmountSheet = event.show) }
            is LoanEntryEvent.ShowWarrantyStartPicker -> _state.update {
                it.copy(
                    showWarrantyStartPicker = event.show
                )
            }

            is LoanEntryEvent.ShowWarrantyEndPicker -> _state.update { it.copy(showWarrantyEndPicker = event.show) }
            is LoanEntryEvent.SetAccountQuery -> _state.update { it.copy(accountQuery = event.query) }
            is LoanEntryEvent.SetCategoryQuery -> _state.update { it.copy(categoryQuery = event.query) }
            is LoanEntryEvent.ShowSharedFilterDialog -> _state.update {
                it.copy(
                    showSharedFilterDialog = event.show
                )
            }

            is LoanEntryEvent.ConfirmSaveWithSharedFilter -> {
                _state.update { it.copy(showSharedFilterDialog = false) }
                saveLoan(
                    onSuccess = { /* Callback handled in saveLoan */ },
                    forceNewFilter = !event.updateShared
                )
            }

            is LoanEntryEvent.TriggerNavigation -> _state.update { it.copy(navTriggered = true) }
        }
    }

    fun saveLoan(forceNewFilter: Boolean = false, onSuccess: () -> Unit) {
        val s = _state.value
        if (!s.canSave) return

        viewModelScope.launch {
            // Check for shared filter if not already handled
            if (s.filterId != null && s.filterId != 0L && s.filterUsageCount > 1 && !forceNewFilter && !s.showSharedFilterDialog) {
                // Check if any filter criteria actually changed compared to the filter
                val currentFilter = s.availableFilters.find { it.id == s.filterId }
                if (currentFilter != null) {
                    val hasChanged = hasFilterChanged(s, currentFilter)
                    if (hasChanged) {
                        _state.update { it.copy(showSharedFilterDialog = true) }
                        return@launch
                    }
                }
            }

            val amount = s.principalAmount.toBigDecimalOrNull() ?: BigDecimal.ZERO

            // 1. Create or Update Filter for this loan
            val filter = Filter(
                id = if (forceNewFilter) 0L else (s.filterId ?: 0L),
                name = "${s.name}-Filter",
                filter = TransactionFilter(
                    categoryIds = s.selectedCategoryIds,
                    accountIds = if (s.linkedAccountId.isBlank()) emptySet() else setOf(s.linkedAccountId),
                    tags = s.selectedTags,
                    paymentMethods = s.selectedPaymentMethods,
                    statuses = s.selectedStatuses,
                    currencies = s.selectedCurrencies,
                    linkedTransactionIds = s.linkedTransactionIds,
                    transactionTypes = s.selectedTrackTypes,
                    queryNote = s.queryNote.takeIf { it.isNotBlank() },
                    isNoteRegex = s.isNoteRegex,
                    queryPayee = s.queryPayee.takeIf { it.isNotBlank() },
                    isPayeeRegex = s.isPayeeRegex,
                    minAmount = s.minAmount.toBigDecimalOrNull(),
                    maxAmount = s.maxAmount.toBigDecimalOrNull(),
                    hasWarranty = s.hasWarranty,
                    warrantyExpirationStart = s.warrantyExpirationStart,
                    warrantyExpirationEnd = s.warrantyExpirationEnd,
                    timelineRange = s.timelineRange
                )
            )

            val filterId = if (filter.id == 0L) {
                filterRepository.insertFilter(filter)
            } else {
                filterRepository.updateFilter(filter)
                filter.id
            }

            // 2. Create or Update Loan
            val loan = (s.existingLoan ?: Loan(
                id = 0,
                name = s.name,
                principalAmount = amount,
                currency = s.currency,
                startDate = s.startDate,
                lenderOrBorrowerName = s.counterparty,
                loanType = s.loanType,
                linkedAccountId = s.linkedAccountId,
                profileId = s.currentProfileId,
                timelineRange = s.timelineRange
            )).copy(
                id = s.loanId ?: 0L,
                name = s.name,
                principalAmount = amount,
                currency = s.currency,
                interestRate = s.interestRate.toFloatOrNull(),
                startDate = s.startDate,
                lenderOrBorrowerName = s.counterparty,
                loanType = s.loanType,
                linkedAccountId = s.linkedAccountId,
                expectedSettlementDate = if (s.hasSettlementDate) s.settlementDate else null,
                status = s.existingLoan?.status ?: LoanStatus.ACTIVE,
                profileId = s.currentProfileId,
                note = s.note,
                categoryIds = s.selectedCategoryIds,
                trackTypes = s.selectedTrackTypes,
                currencies = s.selectedCurrencies,
                tags = s.selectedTags,
                paymentMethods = s.selectedPaymentMethods,
                statuses = s.selectedStatuses,
                linkedTransactionIds = s.linkedTransactionIds,
                timelineRange = s.timelineRange,
                filterId = filterId
            )

            if (s.isEditMode) {
                loanRepository.updateLoan(loan)
            } else {
                loanRepository.insertLoan(loan)
            }
            onSuccess()
        }
    }

    private fun hasFilterChanged(s: LoanEntryState, currentFilter: Filter): Boolean {
        val f = currentFilter.filter
        return s.selectedCategoryIds != f.categoryIds ||
                (if (s.linkedAccountId.isBlank()) emptySet<String>() else setOf(s.linkedAccountId)) != f.accountIds ||
                s.selectedTags != f.tags ||
                s.selectedPaymentMethods != f.paymentMethods ||
                s.selectedStatuses != f.statuses ||
                s.selectedCurrencies != f.currencies ||
                s.linkedTransactionIds != f.linkedTransactionIds ||
                s.selectedTrackTypes != f.transactionTypes ||
                s.queryNote != (f.queryNote ?: "") ||
                s.isNoteRegex != f.isNoteRegex ||
                s.queryPayee != (f.queryPayee ?: "") ||
                s.isPayeeRegex != f.isPayeeRegex ||
                s.minAmount != (f.minAmount?.toString() ?: "") ||
                s.maxAmount != (f.maxAmount?.toString() ?: "") ||
                s.hasWarranty != f.hasWarranty ||
                s.warrantyExpirationStart != f.warrantyExpirationStart ||
                s.warrantyExpirationEnd != f.warrantyExpirationEnd ||
                s.timelineRange != f.timelineRange ||
                "${s.name}-Filter" != currentFilter.name
    }

    fun deleteLoan() {
        val id = _state.value.loanId ?: return
        val filterId = _state.value.filterId

        viewModelScope.launch {
            // Always delete the private filter when deleting a loan
            if (filterId != null) {
                filterRepository.getFilterById(filterId)?.let { filter ->
                    filterRepository.deleteFilter(filter)
                }
            }
            loanRepository.deleteLoan(id)
            _state.update { it.copy(navTriggered = true) }
        }
    }

    /**
     * Search transactions for linking.
     */
    fun searchTransactions(query: String): Flow<List<Transaction>> = flow {
        val profileId = settingsRepository.currentProfileId.first()
        emitAll(transactionUseCases.searchTransactions(profileId, query))
    }
}
