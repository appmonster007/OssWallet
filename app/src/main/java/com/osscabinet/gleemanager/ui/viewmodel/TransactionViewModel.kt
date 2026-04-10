package com.osscabinet.gleemanager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.osscabinet.gleemanager.data.local.SettingsDataStore
import com.osscabinet.gleemanager.domain.model.Transaction
import com.osscabinet.gleemanager.domain.usecase.GetTransactionsUseCase
import com.osscabinet.gleemanager.domain.usecase.InsertTransactionUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionViewModel(
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val insertTransactionUseCase: InsertTransactionUseCase,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory = _selectedCategory.asStateFlow()

    private val _selectedAccountId = MutableStateFlow<String?>(null)
    val selectedAccountId = _selectedAccountId.asStateFlow()

    private val _dateRange = MutableStateFlow<Pair<Long?, Long?>>(null to null)
    val dateRange = _dateRange.asStateFlow()

    val transactions: StateFlow<List<Transaction>> = combine(
        settingsDataStore.currentProfileId.flatMapLatest { profileId -> getTransactionsUseCase(profileId) },
        _selectedCategory,
        _selectedAccountId,
        _dateRange
    ) { allTransactions, category, accountId, range ->
        allTransactions.filter { transaction ->
            val matchesCategory = category == null || transaction.category.equals(category, ignoreCase = true)
            val matchesAccount = accountId == null || transaction.accountId == accountId
            val matchesDate = (range.first == null || transaction.timestamp >= range.first!!) &&
                              (range.second == null || transaction.timestamp <= range.second!!)
            matchesCategory && matchesAccount && matchesDate
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setCategoryFilter(category: String?) {
        _selectedCategory.value = category
    }

    fun setAccountFilter(accountId: String?) {
        _selectedAccountId.value = accountId
    }

    fun setDateRangeFilter(start: Long?, end: Long?) {
        _dateRange.value = start to end
    }

    fun clearFilters() {
        _selectedCategory.value = null
        _selectedAccountId.value = null
        _dateRange.value = null to null
    }

    fun addTransaction(
        title: String,
        amount: Double,
        category: String,
        isIncome: Boolean,
        accountId: String,
        categoryId: Long? = null,
        tags: List<String> = emptyList(),
        timestamp: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            val profileId = settingsDataStore.currentProfileId.first()
            val transaction = Transaction(
                title = title,
                amount = amount,
                category = category,
                timestamp = timestamp,
                currency = "USD", // Default currency
                isIncome = isIncome,
                accountId = accountId,
                categoryId = categoryId,
                tags = tags
            )
            insertTransactionUseCase(transaction, profileId)
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            // Need a DeleteTransactionUseCase ideally, but calling repository directly for now if needed
            // Or just add it to the ViewModel if the usecase is missing
        }
    }
}

class TransactionViewModelFactory(
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val insertTransactionUseCase: InsertTransactionUseCase,
    private val settingsDataStore: com.osscabinet.gleemanager.data.local.SettingsDataStore
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return TransactionViewModel(getTransactionsUseCase, insertTransactionUseCase, settingsDataStore) as T
    }
}
