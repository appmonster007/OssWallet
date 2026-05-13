package com.osscabinet.ossWallet.ui.screens.loans.entry

import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.model.Loan
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
                    transactionRepository.getAllTransactions(profileId)
                ) { accounts, categories, transactions ->
                    val uniquePayees = transactions.map { it.payee }.filter { it.isNotBlank() }.distinct().sorted()
                    val uniqueNotes = transactions.map { it.note }.filter { it.isNotBlank() }.distinct().sorted()
                    val availableTags = transactions.flatMap { it.tags }.distinct().sorted()
                    
                    _state.update {
                        it.copy(
                            accounts = accounts,
                            categories = categories,
                            uniquePayees = uniquePayees,
                            uniqueNotes = uniqueNotes,
                            availableTags = availableTags,
                            currentProfileId = profileId
                        )
                    }
                }
            }.collect()
        }
    }

    fun initialize(loanId: Long? = null) {
        if (loanId == null) {
            _state.update { it.copy(isEditMode = false, loanId = null) }
            return
        }

        viewModelScope.launch {
            loanRepository.getLoanById(loanId)?.let { loan ->
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
                        note = loan.note ?: "",
                        selectedCategoryIds = loan.categoryIds.toSet(),
                        selectedTags = loan.tags.toSet(),
                        startDate = loan.startDate,
                        hasSettlementDate = loan.expectedSettlementDate != null,
                        settlementDate = loan.expectedSettlementDate ?: System.currentTimeMillis(),
                        linkedAccountId = loan.linkedAccountId ?: ""
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
            is LoanEntryEvent.SetStartDate -> _state.update { it.copy(startDate = event.date) }
            is LoanEntryEvent.SetHasSettlementDate -> _state.update { it.copy(hasSettlementDate = event.has) }
            is LoanEntryEvent.SetSettlementDate -> _state.update { it.copy(settlementDate = event.date) }
            is LoanEntryEvent.SetLinkedAccountId -> _state.update { it.copy(linkedAccountId = event.id) }
            is LoanEntryEvent.ShowStartDatePicker -> _state.update { it.copy(showStartDatePicker = event.show) }
            is LoanEntryEvent.ShowSettlementDatePicker -> _state.update { it.copy(showSettlementDatePicker = event.show) }
            is LoanEntryEvent.ShowNumpad -> _state.update { it.copy(showNumpad = event.show) }
            is LoanEntryEvent.ShowInterestNumpad -> _state.update { it.copy(showInterestNumpad = event.show) }
            is LoanEntryEvent.ShowAccountSheet -> _state.update { it.copy(showAccountSheet = event.show) }
            is LoanEntryEvent.ShowCategorySheet -> _state.update { it.copy(showCategorySheet = event.show) }
            is LoanEntryEvent.ShowTagSheet -> _state.update { it.copy(showTagSheet = event.show) }
            is LoanEntryEvent.SetAccountQuery -> _state.update { it.copy(accountQuery = event.query) }
            is LoanEntryEvent.SetCategoryQuery -> _state.update { it.copy(categoryQuery = event.query) }
            is LoanEntryEvent.TriggerNavigation -> _state.update { it.copy(navTriggered = true) }
        }
    }

    fun saveLoan(onSuccess: () -> Unit) {
        val s = _state.value
        if (!s.canSave) return

        viewModelScope.launch {
            val loan = (s.existingLoan ?: Loan(
                id = 0,
                name = s.name,
                principalAmount = s.principalAmount.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                currency = s.currency,
                loanType = s.loanType,
                lenderOrBorrowerName = s.counterparty,
                startDate = s.startDate,
                linkedAccountId = s.linkedAccountId,
                profileId = s.currentProfileId
            )).copy(
                id = s.loanId ?: 0L,
                name = s.name,
                principalAmount = s.principalAmount.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                currency = s.currency,
                loanType = s.loanType,
                lenderOrBorrowerName = s.counterparty,
                interestRate = s.interestRate.toFloatOrNull(),
                note = s.note,
                startDate = s.startDate,
                expectedSettlementDate = if (s.hasSettlementDate) s.settlementDate else null,
                linkedAccountId = s.linkedAccountId,
                categoryIds = s.selectedCategoryIds.toList(),
                tags = s.selectedTags.toList(),
                profileId = s.currentProfileId
            )

            if (s.isEditMode) {
                loanRepository.updateLoan(loan)
            } else {
                loanRepository.insertLoan(loan)
            }
            onSuccess()
        }
    }

    fun deleteLoan(onSuccess: () -> Unit) {
        val id = _state.value.loanId ?: return
        viewModelScope.launch {
            loanRepository.deleteLoan(id)
            onSuccess()
        }
    }
}
