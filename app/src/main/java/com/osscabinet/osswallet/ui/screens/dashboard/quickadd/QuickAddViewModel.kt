package com.osscabinet.ossWallet.ui.screens.dashboard.quickadd

import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Template
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.dto.TransactionRequest
import com.osscabinet.ossWallet.domain.repository.AccountRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.service.management.TransactionManager
import com.osscabinet.ossWallet.ui.util.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
    val transactionType: TransactionType = TransactionType.EXPENSE,
    val accounts: List<Account> = emptyList(),
    val isSaving: Boolean = false
)

@HiltViewModel
class QuickAddViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
    private val accountRepository: AccountRepository,
    private val transactionManager: TransactionManager
) : BaseViewModel(settingsRepository) {

    private val _state = MutableStateFlow(QuickAddState())
    val state: StateFlow<QuickAddState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val profileId = settingsRepository.currentProfileId.first()
            val accounts = accountRepository.getAllAccounts(profileId)
                .first()
                .filter { !it.isArchived }
            _state.update { it.copy(accounts = accounts) }
        }
    }

    fun onTemplateSelected(template: Template) {
        val account = _state.value.accounts.find { it.id == template.accountId }
        _state.update {
            it.copy(
                selectedTemplate = template,
                selectedCategoryId = template.categoryId,
                selectedAccountId = template.accountId,
                selectedCurrency = account?.currencyCode ?: template.currency,
                transactionType = template.transactionType,
                amount = template.amount.abs(),
                currentStep = 1
            )
        }
    }

    fun onCategorySelected(category: Category) {
        _state.update {
            it.copy(
                selectedTemplate = null,
                selectedCategoryId = category.id,
                selectedAccountId = null,
                transactionType = TransactionType.EXPENSE,
                amount = BigDecimal.ZERO,
                currentStep = 1
            )
        }
    }

    fun onAmountChanged(amount: BigDecimal) {
        _state.update { it.copy(amount = amount) }
    }

    fun goToStep(step: Int) {
        _state.update { it.copy(currentStep = step) }
    }

    fun onAccountTapped(accountId: String) {
        val account = _state.value.accounts.find { it.id == accountId }
        _state.update {
            it.copy(
                selectedAccountId = accountId,
                selectedCurrency = account?.currencyCode ?: it.selectedCurrency,
                currentStep = 2
            )
        }
    }

    fun onCurrencySelected(currency: String) {
        _state.update { it.copy(selectedCurrency = currency) }
    }

    fun toggleTransactionType() {
        _state.update {
            val next = if (it.transactionType == TransactionType.EXPENSE)
                TransactionType.INCOME else TransactionType.EXPENSE
            it.copy(transactionType = next)
        }
    }

    fun saveTransaction(
        categories: List<Category>,
        onSuccess: () -> Unit
    ) {
        val s = _state.value
        val accountId = s.selectedAccountId ?: return
        val amount = s.amount
        if (amount <= BigDecimal.ZERO) return
        val signedAmount = if (s.transactionType == TransactionType.INCOME) amount else amount.negate()

        val account = s.accounts.find { it.id == accountId } ?: return
        val categoryId = s.selectedCategoryId ?: Category.SYSTEM_OTHERS
        val name = s.selectedTemplate?.name
            ?: categories.find { it.id == categoryId }?.name
            ?: "Quick Transaction"

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            runCatching {
                withProfile { profileId ->
                    transactionManager.handleSaveTransaction(
                        existingTransaction = null,
                        request = TransactionRequest(
                            name = name,
                            amount = signedAmount,
                            transactionType = s.transactionType,
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
                _state.update { it.copy(isSaving = false) }
                onSuccess()
            }.onFailure {
                _state.update { it.copy(isSaving = false) }
            }
        }
    }

    fun reset() {
        val currentAccounts = _state.value.accounts
        _state.value = QuickAddState(accounts = currentAccounts)
    }

    val canSave: Boolean
        get() {
            val s = _state.value
            return s.selectedAccountId != null && s.amount > BigDecimal.ZERO
        }
}
