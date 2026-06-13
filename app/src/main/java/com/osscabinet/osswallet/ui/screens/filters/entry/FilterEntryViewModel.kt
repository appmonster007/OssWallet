package com.osscabinet.ossWallet.ui.screens.filters.entry

import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.model.Filter
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.repository.AccountRepository
import com.osscabinet.ossWallet.domain.repository.CategoryRepository
import com.osscabinet.ossWallet.domain.repository.FilterRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.repository.TagRepository
import com.osscabinet.ossWallet.domain.service.category.CategoryHierarchyCache
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
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FilterEntryViewModel @Inject constructor(
    private val filterRepository: FilterRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val transactionUseCases: TransactionUseCases,
    private val tagRepository: TagRepository,
    settingsRepository: SettingsRepository
) : BaseViewModel(settingsRepository) {

    private val _state = MutableStateFlow(FilterEntryState())
    val state: StateFlow<FilterEntryState> = _state.asStateFlow()

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
                combine(
                    accountRepository.getAllAccounts(profileId),
                    categoryRepository.categories,
                    tagRepository.getAllTags()
                ) { accounts, categories, tags ->
                    _state.update {
                        it.copy(
                            accounts = accounts,
                            categories = categories,
                            availableTags = tags
                        )
                    }
                }
            }.collect()
        }
    }

    fun initialize(filterId: Long? = null, initialFilter: TransactionFilter? = null) {
        if (initialFilter != null) {
            _state.update {
                it.copy(
                    filterId = null,
                    isEditMode = false,
                    name = "New Filter",
                    selectedTransactionTypes = initialFilter.transactionTypes.toSet(),
                    selectedAccountIds = initialFilter.accountIds.toSet(),
                    selectedCategoryIds = initialFilter.categoryIds.toSet(),
                    selectedPaymentMethods = initialFilter.paymentMethods.toSet(),
                    selectedStatuses = initialFilter.statuses.toSet(),
                    linkedTransactionIds = initialFilter.linkedTransactionIds.toSet(),
                    selectedTags = initialFilter.tags.toSet(),
                    matchAllTags = initialFilter.matchAllTags,
                    selectedCurrencies = initialFilter.currencies.toSet(),
                    queryNote = initialFilter.queryNote ?: "",
                    isNoteRegex = initialFilter.isNoteRegex,
                    queryPayee = initialFilter.queryPayee ?: "",
                    isPayeeRegex = initialFilter.isPayeeRegex,
                    minAmount = initialFilter.minAmount?.toPlainString() ?: "",
                    maxAmount = initialFilter.maxAmount?.toPlainString() ?: "",
                    hasWarranty = initialFilter.hasWarranty,
                    warrantyExpirationStart = initialFilter.warrantyExpirationStart,
                    warrantyExpirationEnd = initialFilter.warrantyExpirationEnd,
                    timelineRange = initialFilter.timelineRange
                )
            }
            return
        }

        if (filterId == null || filterId == -1L) {
            _state.update {
                it.copy(
                    isEditMode = false,
                    filterId = null,
                    selectedAccountIds = emptySet(),
                    selectedCategoryIds = emptySet(),
                    selectedPaymentMethods = emptySet(),
                    selectedStatuses = emptySet(),
                    selectedTransactionTypes = emptySet(),
                    selectedCurrencies = emptySet(),
                    selectedTags = emptySet(),
                    matchAllTags = true
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
                        selectedTransactionTypes = tf.transactionTypes.toSet(),
                        selectedAccountIds = tf.accountIds.toSet(),
                        selectedCategoryIds = CategoryHierarchyCache.build(_state.value.categories)
                            .expand(tf.categoryIds),
                        selectedPaymentMethods = tf.paymentMethods.toSet(),
                        selectedStatuses = tf.statuses.toSet(),
                        linkedTransactionIds = tf.linkedTransactionIds.toSet(),
                        selectedTags = tf.tags.toSet(),
                        matchAllTags = tf.matchAllTags,
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
                        timelineRange = tf.timelineRange
                    )
                }
            }
        }
    }

    fun onEvent(event: FilterEntryEvent) {
        when (event) {
            is FilterEntryEvent.SetName -> _state.update { it.copy(name = event.name) }
            is FilterEntryEvent.SetTransactionTypes -> _state.update {
                it.copy(
                    selectedTransactionTypes = event.types
                )
            }

            is FilterEntryEvent.SetAccountIds -> _state.update { it.copy(selectedAccountIds = event.ids) }
            is FilterEntryEvent.SetCategoryIds -> {
                val expanded =
                    CategoryHierarchyCache.build(_state.value.categories).expand(event.ids)
                _state.update { it.copy(selectedCategoryIds = expanded) }
            }

            is FilterEntryEvent.SetPaymentMethods -> _state.update { it.copy(selectedPaymentMethods = event.methods) }
            is FilterEntryEvent.SetStatuses -> _state.update { it.copy(selectedStatuses = event.statuses) }
            is FilterEntryEvent.SetLinkedTransactions -> _state.update {
                it.copy(
                    linkedTransactionIds = event.ids
                )
            }

            is FilterEntryEvent.SetTags -> _state.update { it.copy(selectedTags = event.tags) }
            is FilterEntryEvent.SetMatchAllTags -> _state.update { it.copy(matchAllTags = event.matchAll) }
            is FilterEntryEvent.SetCurrencies -> _state.update { it.copy(selectedCurrencies = event.currencies) }
            is FilterEntryEvent.SetQueryNote -> _state.update { it.copy(queryNote = event.query) }
            is FilterEntryEvent.SetNoteRegex -> _state.update { it.copy(isNoteRegex = event.isRegex) }
            is FilterEntryEvent.SetQueryPayee -> _state.update { it.copy(queryPayee = event.query) }
            is FilterEntryEvent.SetPayeeRegex -> _state.update { it.copy(isPayeeRegex = event.isRegex) }
            is FilterEntryEvent.SetMinAmount -> _state.update { it.copy(minAmount = event.amount) }
            is FilterEntryEvent.SetMaxAmount -> _state.update { it.copy(maxAmount = event.amount) }
            is FilterEntryEvent.SetTimelineRange -> _state.update { it.copy(timelineRange = event.range) }
            is FilterEntryEvent.SetHasWarranty -> _state.update { it.copy(hasWarranty = event.hasWarranty) }
            is FilterEntryEvent.SetWarrantyExpirationStart -> _state.update {
                it.copy(
                    warrantyExpirationStart = event.date
                )
            }

            is FilterEntryEvent.SetWarrantyExpirationEnd -> _state.update {
                it.copy(
                    warrantyExpirationEnd = event.date
                )
            }

            is FilterEntryEvent.ShowAccountSheet -> _state.update { it.copy(showAccountSheet = event.show) }
            is FilterEntryEvent.ShowCategorySheet -> _state.update { it.copy(showCategorySheet = event.show) }
            is FilterEntryEvent.ShowPaymentMethodSheet -> _state.update {
                it.copy(
                    showPaymentMethodSheet = event.show
                )
            }

            is FilterEntryEvent.ShowStatusSheet -> _state.update { it.copy(showStatusSheet = event.show) }
            is FilterEntryEvent.ShowLinkSheet -> _state.update { it.copy(showLinkSheet = event.show) }
            is FilterEntryEvent.ShowTagSheet -> _state.update { it.copy(showTagSheet = event.show) }
            is FilterEntryEvent.ShowCurrencySheet -> _state.update { it.copy(showCurrencySheet = event.show) }
            is FilterEntryEvent.ShowTypeSheet -> _state.update { it.copy(showTypeSheet = event.show) }
            is FilterEntryEvent.ShowMinAmountSheet -> _state.update { it.copy(showMinAmountSheet = event.show) }
            is FilterEntryEvent.ShowMaxAmountSheet -> _state.update { it.copy(showMaxAmountSheet = event.show) }
            is FilterEntryEvent.ShowTimelineSheet -> _state.update { it.copy(showTimelineSheet = event.show) }
            is FilterEntryEvent.ShowWarrantyStartPicker -> _state.update {
                it.copy(
                    showWarrantyStartPicker = event.show
                )
            }

            is FilterEntryEvent.ShowWarrantyEndPicker -> _state.update {
                it.copy(
                    showWarrantyEndPicker = event.show
                )
            }

            is FilterEntryEvent.TriggerNavigation -> _state.update { it.copy(navTriggered = true) }
            FilterEntryEvent.ResetFilter -> _state.update {
                it.copy(
                    selectedTransactionTypes = emptySet(),
                    selectedAccountIds = emptySet(),
                    selectedCategoryIds = emptySet(),
                    selectedPaymentMethods = emptySet(),
                    selectedStatuses = emptySet(),
                    linkedTransactionIds = emptySet(),
                    selectedTags = emptySet(),
                    matchAllTags = true,
                    selectedCurrencies = emptySet(),
                    queryNote = "",
                    isNoteRegex = false,
                    queryPayee = "",
                    isPayeeRegex = false,
                    minAmount = "",
                    maxAmount = "",
                    hasWarranty = null,
                    warrantyExpirationStart = null,
                    warrantyExpirationEnd = null,
                    timelineRange = TimelineRange.AllTime(excludeFuture = true)
                )
            }
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

    fun deleteFilter() {
        val filter = _state.value.existingFilter ?: return
        viewModelScope.launch {
            filterRepository.deleteFilter(filter)
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
