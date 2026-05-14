package com.osscabinet.ossWallet.ui.screens.loans.entry

import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.model.*
import com.osscabinet.ossWallet.domain.repository.*
import com.osscabinet.ossWallet.ui.util.BaseViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class LoanEntryViewModel(
    private val loanRepository: LoanRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val tagRepository: TagRepository,
    private val transactionRepository: TransactionRepository,
    private val filterRepository: FilterRepository,
    settingsRepository: SettingsRepository
) : BaseViewModel(settingsRepository) {

    private val _state = MutableStateFlow(LoanEntryState())
    val state: StateFlow<LoanEntryState> = _state.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            settingsRepository.currentProfileId.flatMapLatest { profileId ->
                combine(
                    accountRepository.getAllAccounts(profileId),
                    categoryRepository.categories,
                    tagRepository.getAllTags(),
                    transactionRepository.getAllTransactions(profileId),
                    filterRepository.getAllFilters()
                ) { accounts, categories, tags, transactions, filters ->
                    val uniquePayees = transactions.map { it.payee }.filter { it.isNotBlank() }.distinct().sorted()
                    val uniqueNotes = transactions.map { it.note }.filter { it.isNotBlank() }.distinct().sorted()
                    _state.update {
                        it.copy(
                            accounts = accounts,
                            categories = categories,
                            availableTags = tags.map { t -> t.name },
                            uniquePayees = uniquePayees,
                            uniqueNotes = uniqueNotes,
                            currentProfileId = profileId,
                            allTransactions = transactions,
                            availableFilters = filters,
                            selectedCategoryIds = if (it.isEditMode) it.selectedCategoryIds else categories.map { c -> c.id }.toSet(),
                            selectedPaymentMethods = if (it.isEditMode) it.selectedPaymentMethods else PaymentMethod.entries.toSet(),
                            selectedStatuses = if (it.isEditMode) it.selectedStatuses else TransactionStatus.entries.toSet(),
                            selectedTags = if (it.isEditMode) it.selectedTags else tags.map { it.name }.toSet()
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
                    selectedCategoryIds = it.categories.map { c -> c.id }.toSet(),
                    selectedPaymentMethods = PaymentMethod.entries.toSet(),
                    selectedStatuses = TransactionStatus.entries.toSet(),
                    selectedTags = it.availableTags.toSet()
                ) 
            }
            return
        }

        viewModelScope.launch {
            loanRepository.getLoanById(loanId)?.let { loan: Loan ->
                val filter = loan.filterId?.let { id -> filterRepository.getFilterById(id) }
                val usageCount = loan.filterId?.let { filterRepository.getFilterUsageCount(it) } ?: 0
                _state.update {
                    it.copy(
                        loanId = loanId,
                        isEditMode = true,
                        existingLoan = loan,
                        linkedFilter = filter,
                        filterUsageCount = usageCount,
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
                        selectedCategoryIds = loan.categoryIds.toSet(),
                        selectedTags = loan.tags.toSet(),
                        selectedPaymentMethods = loan.paymentMethods.toSet(),
                        selectedStatuses = loan.statuses.toSet(),
                        linkedTransactionIds = loan.linkedTransactionIds.toSet(),
                        filterId = loan.filterId
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
            is LoanEntryEvent.SetCategoryIds -> _state.update { it.copy(selectedCategoryIds = event.ids) }
            is LoanEntryEvent.SetTags -> _state.update { it.copy(selectedTags = event.tags) }
            is LoanEntryEvent.SetPaymentMethods -> _state.update { it.copy(selectedPaymentMethods = event.methods) }
            is LoanEntryEvent.SetStatuses -> _state.update { it.copy(selectedStatuses = event.statuses) }
            is LoanEntryEvent.SetLinkedTransactions -> _state.update { it.copy(linkedTransactionIds = event.ids) }
            is LoanEntryEvent.SetStartDate -> _state.update { it.copy(startDate = event.date) }
            is LoanEntryEvent.SetHasSettlementDate -> _state.update { it.copy(hasSettlementDate = event.has) }
            is LoanEntryEvent.SetSettlementDate -> _state.update { it.copy(settlementDate = event.date) }
            is LoanEntryEvent.SetLinkedAccountId -> _state.update { it.copy(linkedAccountId = event.id) }
            is LoanEntryEvent.SetFilterId -> {
                val filter = _state.value.availableFilters.find { it.id == event.filterId }
                _state.update { state ->
                    if (filter != null) {
                        state.copy(
                            filterId = event.filterId,
                            linkedFilter = filter,
                            selectedCategoryIds = filter.filter.categoryIds.toSet(),
                            selectedTags = filter.filter.tags.toSet(),
                            selectedPaymentMethods = filter.filter.paymentMethods.toSet(),
                            selectedStatuses = filter.filter.statuses.toSet(),
                            linkedTransactionIds = filter.filter.linkedTransactionIds.toSet(),
                            linkedAccountId = filter.filter.accountIds.firstOrNull() ?: state.linkedAccountId
                        )
                    } else {
                        state.copy(filterId = null, linkedFilter = null, filterUsageCount = 0)
                    }
                }
                if (event.filterId != null) {
                    viewModelScope.launch {
                        val count = filterRepository.getFilterUsageCount(event.filterId)
                        _state.update { it.copy(filterUsageCount = count) }
                    }
                }
            }
            LoanEntryEvent.MakeFilterUnique -> _state.update { it.copy(filterId = null, linkedFilter = null) }
            is LoanEntryEvent.SetDeleteFilterWithLoan -> _state.update { it.copy(deleteFilterWithLoan = event.delete) }
            is LoanEntryEvent.ShowDeleteDialog -> _state.update { it.copy(showDeleteDialog = event.show) }
            is LoanEntryEvent.ShowStartDatePicker -> _state.update { it.copy(showStartDatePicker = event.show) }
            is LoanEntryEvent.ShowSettlementDatePicker -> _state.update { it.copy(showSettlementDatePicker = event.show) }
            is LoanEntryEvent.ShowNumpad -> _state.update { it.copy(showNumpad = event.show) }
            is LoanEntryEvent.ShowInterestNumpad -> _state.update { it.copy(showInterestNumpad = event.show) }
            is LoanEntryEvent.ShowAccountSheet -> _state.update { it.copy(showAccountSheet = event.show) }
            is LoanEntryEvent.ShowCategorySheet -> _state.update { it.copy(showCategorySheet = event.show) }
            is LoanEntryEvent.ShowTagSheet -> _state.update { it.copy(showTagSheet = event.show) }
            is LoanEntryEvent.ShowPaymentMethodSheet -> _state.update { it.copy(showPaymentMethodSheet = event.show) }
            is LoanEntryEvent.ShowStatusSheet -> _state.update { it.copy(showStatusSheet = event.show) }
            is LoanEntryEvent.ShowLinkSheet -> _state.update { it.copy(showLinkSheet = event.show) }
            is LoanEntryEvent.ShowFilterSheet -> _state.update { it.copy(showFilterSheet = event.show) }
            is LoanEntryEvent.SetAccountQuery -> _state.update { it.copy(accountQuery = event.query) }
            is LoanEntryEvent.SetCategoryQuery -> _state.update { it.copy(categoryQuery = event.query) }
            is LoanEntryEvent.TriggerNavigation -> _state.update { it.copy(navTriggered = true) }
        }
    }

    fun saveLoan(onSuccess: () -> Unit) {
        val s = _state.value
        if (!s.canSave) return

        viewModelScope.launch {
            val amount = s.principalAmount.toBigDecimalOrNull() ?: BigDecimal.ZERO
            
            // 1. Create or Update Filter
            val filter = Filter(
                id = s.linkedFilter?.id ?: 0L,
                name = "Filter for Loan: ${s.name}",
                filter = TransactionFilter(
                    categoryIds = s.selectedCategoryIds.toList(),
                    accountIds = if (s.linkedAccountId.isBlank()) emptyList() else listOf(s.linkedAccountId),
                    tags = s.selectedTags.toList(),
                    paymentMethods = s.selectedPaymentMethods.toList(),
                    statuses = s.selectedStatuses.toList(),
                    linkedTransactionIds = s.linkedTransactionIds.toList()
                )
            )

            val filterId = if (filter.id == 0L || s.filterUsageCount > 1) {
                filterRepository.insertFilter(filter.copy(id = 0L))
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
                profileId = s.currentProfileId
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
                profileId = s.currentProfileId,
                note = s.note,
                categoryIds = s.selectedCategoryIds.toList(),
                tags = s.selectedTags.toList(),
                paymentMethods = s.selectedPaymentMethods.toList(),
                statuses = s.selectedStatuses.toList(),
                linkedTransactionIds = s.linkedTransactionIds.toList(),
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

    fun deleteLoan() {
        val id = _state.value.loanId ?: return
        val filterId = _state.value.filterId
        val deleteFilter = _state.value.deleteFilterWithLoan
        
        viewModelScope.launch {
            if (deleteFilter && filterId != null) {
                filterRepository.getFilterById(filterId)?.let { filter ->
                    filterRepository.deleteFilter(filter)
                }
            } else {
                loanRepository.deleteLoan(id)
            }
            _state.update { it.copy(navTriggered = true) }
        }
    }
}
