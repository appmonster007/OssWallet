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

    val transactions: StateFlow<List<Transaction>> = settingsDataStore.currentProfileId
        .flatMapLatest { profileId ->
            getTransactionsUseCase(profileId)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

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
