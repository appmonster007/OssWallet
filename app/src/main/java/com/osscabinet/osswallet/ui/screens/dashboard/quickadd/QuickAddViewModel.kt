package com.osscabinet.ossWallet.ui.screens.dashboard.quickadd

import com.osscabinet.ossWallet.domain.model.AccountStructureType
import com.osscabinet.ossWallet.domain.repository.TransactionRepository
import com.osscabinet.ossWallet.domain.service.aggregation.AggregationService
import com.osscabinet.ossWallet.domain.repository.ComputationRepository
import com.osscabinet.ossWallet.domain.model.EntryMode
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Template
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.dto.TransactionRequest
import com.osscabinet.ossWallet.domain.repository.AccountRepository
import com.osscabinet.ossWallet.domain.repository.CategoryRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.repository.TemplateRepository
import com.osscabinet.ossWallet.domain.service.management.TransactionManager
import com.osscabinet.ossWallet.ui.util.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

data class QuickAddState(
    val currentStep: Int = 0,
    val amount: BigDecimal = BigDecimal.ZERO,
    val selectedTemplate: Template? = null,
    val selectedCategoryId: Long? = null,
    val selectedAccountId: String? = null,
    val selectedCurrency: String = "",
    val entryMode: EntryMode = EntryMode.EXPENSE,
    val accounts: List<Account> = emptyList(),
    val structureTypes: List<AccountStructureType> = emptyList(),
    val accountBalances: Map<String, BigDecimal> = emptyMap(),
    val exchangeRates: Map<String, BigDecimal> = emptyMap(),
    val baseCurrency: String = "USD",
    val isSaving: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class QuickAddViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    settingsRepository: SettingsRepository,
    private val accountRepository: AccountRepository,
    private val transactionManager: TransactionManager,
    private val transactionRepository: TransactionRepository,
    private val computationRepository: ComputationRepository,
    templateRepository: TemplateRepository,
    categoryRepository: CategoryRepository,
) : BaseViewModel(settingsRepository) {

    companion object {
        const val EXTRA_TYPE = "type"
    }

    private val initialType: EntryMode = savedStateHandle
        .get<String>(EXTRA_TYPE)
        ?.let { runCatching { EntryMode.valueOf(it) }.getOrNull() }
        ?: EntryMode.EXPENSE

    val templates: StateFlow<List<Template>> = templateRepository.getAllTemplates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList<Template>())

    val categories: StateFlow<List<Category>> = categoryRepository.categories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList<Category>())

    private val _uiState = MutableStateFlow(QuickAddState(entryMode = initialType))
    
    val state: StateFlow<QuickAddState> = settingsRepository.currentProfileId
        .flatMapLatest { profileId ->
            combine(
                _uiState,
                accountRepository.getAllAccounts(profileId),
                transactionRepository.getAllTransactions(profileId),
                settingsRepository.currency,
                computationRepository.exchangeRates
            ) { uiState, allAccounts, transactions, baseCurrency, rates ->
                val activeAccounts = allAccounts.filter { !it.isArchived }
                val structureTypes = allAccounts.map { it.structureType }
                    .distinct()
                    .sortedBy { it.displayName }
                
                val balances = activeAccounts.associate { acc ->
                    acc.id to AggregationService.calculateAccountBalance(
                        acc.id,
                        transactions,
                        baseCurrency,
                        rates
                    )
                }
                
                uiState.copy(
                    accounts = activeAccounts,
                    structureTypes = structureTypes,
                    accountBalances = balances,
                    exchangeRates = rates,
                    baseCurrency = baseCurrency
                )
            }
        }.stateIn(
            viewModelScope, 
            SharingStarted.WhileSubscribed(5000), 
            QuickAddState(entryMode = initialType)
        )

    fun onTemplateSelected(template: Template) {
        val account = state.value.accounts.find { it.id == template.accountId }
        _uiState.update {
            it.copy(
                selectedTemplate = template,
                selectedCategoryId = template.categoryId,
                selectedAccountId = template.accountId,
                selectedCurrency = account?.currencyCode ?: template.currency,
                entryMode = template.entryMode,
                amount = template.amount.abs(),
                currentStep = 1
            )
        }
    }

    fun onCategorySelected(category: Category) {
        _uiState.update {
            it.copy(
                selectedTemplate = null,
                selectedCategoryId = category.id,
                selectedAccountId = null,
                entryMode = EntryMode.EXPENSE,
                amount = BigDecimal.ZERO,
                currentStep = 1
            )
        }
    }

    fun onAmountChanged(amount: BigDecimal) {
        _uiState.update { it.copy(amount = amount) }
    }

    fun goToStep(step: Int) {
        _uiState.update { it.copy(currentStep = step) }
    }

    fun onAccountTapped(accountId: String) {
        val account = state.value.accounts.find { it.id == accountId }
        _uiState.update {
            it.copy(
                selectedAccountId = accountId,
                selectedCurrency = account?.currencyCode ?: it.selectedCurrency,
                currentStep = 2
            )
        }
    }

    fun onCurrencySelected(currency: String) {
        _uiState.update { it.copy(selectedCurrency = currency) }
    }

    fun toggleTransactionType() {
        _uiState.update {
            val next = if (it.entryMode == EntryMode.EXPENSE)
                EntryMode.INCOME else EntryMode.EXPENSE
            it.copy(entryMode = next)
        }
    }

    fun saveTransaction(onSuccess: () -> Unit) {
        val s = state.value
        val accountId = s.selectedAccountId ?: return
        val amount = s.amount
        if (amount <= BigDecimal.ZERO) return
        val signedAmount = if (s.entryMode == EntryMode.INCOME) amount else amount.negate()

        val account = s.accounts.find { it.id == accountId } ?: return
        val categoryId = s.selectedCategoryId ?: Category.SYSTEM_OTHERS
        val name = s.selectedTemplate?.name
            ?: categories.value.find { it.id == categoryId }?.name
            ?: "Quick Transaction"

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            runCatching {
                withProfile { profileId ->
                    transactionManager.handleSaveTransaction(
                        existingTransaction = null,
                        request = TransactionRequest(
                            name = name,
                            amount = signedAmount,
                            entryMode = s.entryMode,
                            accountId = accountId,
                            currency = s.selectedCurrency.ifBlank { account.currencyCode },
                            categoryId = categoryId,
                            note = s.selectedTemplate?.note ?: "",
                            payee = s.selectedTemplate?.payee ?: "",
                            status = s.selectedTemplate?.transactionStatus ?: TransactionStatus.CLEARED,
                            tags = s.selectedTemplate?.tags ?: emptySet()
                        ),
                        profileId = profileId
                    )
                }
            }.onSuccess {
                _uiState.update { it.copy(isSaving = false) }
                onSuccess()
            }.onFailure {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun reset() {
        val currentAccounts = state.value.accounts
        _uiState.value = QuickAddState(accounts = currentAccounts, entryMode = initialType)
    }

    val canSave: Boolean
        get() {
            val s = state.value
            return s.selectedAccountId != null && s.amount > BigDecimal.ZERO
        }
}
