package com.osscabinet.ossWallet.ui.screens.filters.entry

import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.model.Filter
import com.osscabinet.ossWallet.domain.model.PaymentMethod
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import com.osscabinet.ossWallet.domain.repository.AccountRepository
import com.osscabinet.ossWallet.domain.repository.CategoryRepository
import com.osscabinet.ossWallet.domain.repository.FilterRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.repository.TagRepository
import com.osscabinet.ossWallet.domain.repository.TransactionRepository
import com.osscabinet.ossWallet.ui.util.BaseViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class FilterEntryViewModel(
    private val filterRepository: FilterRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
    private val tagRepository: TagRepository,
    settingsRepository: SettingsRepository
) : BaseViewModel(settingsRepository) {

    private val _state = MutableStateFlow(FilterEntryState())
    val state: StateFlow<FilterEntryState> = _state.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            settingsRepository.currentProfileId.flatMapLatest { profileId ->
                combine(
                    accountRepository.getAllAccounts(profileId),
                    categoryRepository.categories,
                    transactionRepository.getAllTransactions(profileId),
                    tagRepository.getAllTags()
                ) { accounts, categories, transactions, tags ->
                    _state.update { 
                        it.copy(
                            accounts = accounts,
                            categories = categories,
                            allTransactions = transactions,
                            availableTags = tags,
                            selectedAccountIds = if (it.isEditMode) it.selectedAccountIds else accounts.map { a -> a.id }.toSet(),
                            selectedCategoryIds = if (it.isEditMode) it.selectedCategoryIds else categories.map { c -> c.id }.toSet(),
                            selectedPaymentMethods = if (it.isEditMode) it.selectedPaymentMethods else PaymentMethod.entries.toSet(),
                            selectedStatuses = if (it.isEditMode) it.selectedStatuses else TransactionStatus.entries.toSet(),
                            selectedCurrencies = if (it.isEditMode) it.selectedCurrencies else transactions.map { t -> t.currency }.distinct().toSet(),
                            selectedTags = if (it.isEditMode) it.selectedTags else tags.map { t -> t.name }.toSet()
                        )
                    }
                }
            }.collect()
        }
    }

    fun initialize(filterId: Long? = null) {
        if (filterId == null || filterId == -1L) {
            _state.update { 
                it.copy(
                    isEditMode = false, 
                    filterId = null,
                    selectedAccountIds = it.accounts.map { a -> a.id }.toSet(),
                    selectedCategoryIds = it.categories.map { c -> c.id }.toSet(),
                    selectedPaymentMethods = PaymentMethod.entries.toSet(),
                    selectedStatuses = TransactionStatus.entries.toSet(),
                    selectedTags = it.availableTags.map { t -> t.name }.toSet()
                ) 
            }
            return
        }

        viewModelScope.launch {
            filterRepository.getFilterById(filterId)?.let { filter ->
                val tf = filter.filter
                _state.update {
                    it.copy(
                        filterId = filterId,
                        isEditMode = true,
                        existingFilter = filter,
                        name = filter.name,
                        transactionType = tf.transactionType,
                        selectedAccountIds = tf.accountIds.toSet(),
                        selectedCategoryIds = tf.categoryIds.toSet(),
                        selectedPaymentMethods = tf.paymentMethods.toSet(),
                        selectedStatuses = tf.statuses.toSet(),
                        linkedTransactionIds = tf.linkedTransactionIds.toSet(),
                        selectedTags = tf.tags.toSet(),
                        selectedCurrencies = tf.currencies.toSet(),
                        queryNote = tf.queryNote ?: "",
                        isNoteRegex = tf.isNoteRegex,
                        queryPayee = tf.queryPayee ?: "",
                        isPayeeRegex = tf.isPayeeRegex,
                        minAmount = tf.minAmount?.toPlainString() ?: "",
                        maxAmount = tf.maxAmount?.toPlainString() ?: "",
                        hasWarranty = tf.hasWarranty,
                        warrantyExpirationStart = tf.warrantyExpirationStart,
                        warrantyExpirationEnd = tf.warrantyExpirationEnd,
                        excludeFuture = tf.excludeFuture
                    )
                }
            }
        }
    }

    fun onEvent(event: FilterEntryEvent) {
        when (event) {
            is FilterEntryEvent.SetName -> _state.update { it.copy(name = event.name) }
            is FilterEntryEvent.SetTransactionType -> _state.update { it.copy(transactionType = event.type) }
            is FilterEntryEvent.SetAccountIds -> _state.update { it.copy(selectedAccountIds = event.ids) }
            is FilterEntryEvent.SetCategoryIds -> _state.update { it.copy(selectedCategoryIds = event.ids) }
            is FilterEntryEvent.SetPaymentMethods -> _state.update { it.copy(selectedPaymentMethods = event.methods) }
            is FilterEntryEvent.SetStatuses -> _state.update { it.copy(selectedStatuses = event.statuses) }
            is FilterEntryEvent.SetLinkedTransactions -> _state.update { it.copy(linkedTransactionIds = event.ids) }
            is FilterEntryEvent.SetTags -> _state.update { it.copy(selectedTags = event.tags) }
            is FilterEntryEvent.SetCurrencies -> _state.update { it.copy(selectedCurrencies = event.currencies) }
            is FilterEntryEvent.SetQueryNote -> _state.update { it.copy(queryNote = event.query) }
            is FilterEntryEvent.SetNoteRegex -> _state.update { it.copy(isNoteRegex = event.isRegex) }
            is FilterEntryEvent.SetQueryPayee -> _state.update { it.copy(queryPayee = event.query) }
            is FilterEntryEvent.SetPayeeRegex -> _state.update { it.copy(isPayeeRegex = event.isRegex) }
            is FilterEntryEvent.SetMinAmount -> _state.update { it.copy(minAmount = event.amount) }
            is FilterEntryEvent.SetMaxAmount -> _state.update { it.copy(maxAmount = event.amount) }
            is FilterEntryEvent.SetHasWarranty -> _state.update { it.copy(hasWarranty = event.hasWarranty) }
            is FilterEntryEvent.SetWarrantyExpirationStart -> _state.update { it.copy(warrantyExpirationStart = event.date) }
            is FilterEntryEvent.SetWarrantyExpirationEnd -> _state.update { it.copy(warrantyExpirationEnd = event.date) }
            is FilterEntryEvent.SetExcludeFuture -> _state.update { it.copy(excludeFuture = event.exclude) }
            is FilterEntryEvent.ShowAccountSheet -> _state.update { it.copy(showAccountSheet = event.show) }
            is FilterEntryEvent.ShowCategorySheet -> _state.update { it.copy(showCategorySheet = event.show) }
            is FilterEntryEvent.ShowPaymentMethodSheet -> _state.update { it.copy(showPaymentMethodSheet = event.show) }
            is FilterEntryEvent.ShowStatusSheet -> _state.update { it.copy(showStatusSheet = event.show) }
            is FilterEntryEvent.ShowLinkSheet -> _state.update { it.copy(showLinkSheet = event.show) }
            is FilterEntryEvent.ShowTagSheet -> _state.update { it.copy(showTagSheet = event.show) }
            is FilterEntryEvent.ShowCurrencySheet -> _state.update { it.copy(showCurrencySheet = event.show) }
            is FilterEntryEvent.ShowTypeSheet -> _state.update { it.copy(showTypeSheet = event.show) }
            is FilterEntryEvent.ShowMinAmountSheet -> _state.update { it.copy(showMinAmountSheet = event.show) }
            is FilterEntryEvent.ShowMaxAmountSheet -> _state.update { it.copy(showMaxAmountSheet = event.show) }
            is FilterEntryEvent.ShowWarrantyStartPicker -> _state.update { it.copy(showWarrantyStartPicker = event.show) }
            is FilterEntryEvent.ShowWarrantyEndPicker -> _state.update { it.copy(showWarrantyEndPicker = event.show) }
            is FilterEntryEvent.TriggerNavigation -> _state.update { it.copy(navTriggered = true) }
            FilterEntryEvent.ResetFilter -> _state.update { it.copy(
                transactionType = null,
                selectedAccountIds = it.accounts.map { a -> a.id }.toSet(),
                selectedCategoryIds = it.categories.map { c -> c.id }.toSet(),
                selectedPaymentMethods = PaymentMethod.entries.toSet(),
                selectedStatuses = TransactionStatus.entries.toSet(),
                linkedTransactionIds = emptySet(),
                selectedTags = it.availableTags.map { t -> t.name }.toSet(),
                selectedCurrencies = it.allTransactions.map { t -> t.currency }.distinct().toSet(),
                queryNote = "",
                isNoteRegex = false,
                queryPayee = "",
                isPayeeRegex = false,
                minAmount = "",
                maxAmount = "",
                hasWarranty = null,
                warrantyExpirationStart = null,
                warrantyExpirationEnd = null,
                excludeFuture = true
            ) }
        }
    }

    fun saveFilter(onSuccess: () -> Unit) {
        val s = _state.value
        if (!s.canSave) return

        viewModelScope.launch {
            val filter = Filter(
                id = s.filterId ?: 0L,
                name = s.name,
                filter = s.toTransactionFilter()
            )
            if (s.isEditMode) {
                filterRepository.updateFilter(filter)
            } else {
                filterRepository.insertFilter(filter)
            }
            onSuccess()
        }
    }

    fun deleteFilter(onSuccess: () -> Unit) {
        val filter = _state.value.existingFilter ?: return
        viewModelScope.launch {
            filterRepository.deleteFilter(filter)
            onSuccess()
        }
    }
}
