package com.osscabinet.ossWallet.ui.screens.transactions.entry

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Tag
import com.osscabinet.ossWallet.domain.model.Template
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import com.osscabinet.ossWallet.domain.model.dto.TransactionPrefillDTO
import com.osscabinet.ossWallet.domain.model.dto.TransactionRequest
import com.osscabinet.ossWallet.domain.repository.AccountRepository
import com.osscabinet.ossWallet.domain.repository.CategoryRepository
import com.osscabinet.ossWallet.domain.repository.TagRepository
import com.osscabinet.ossWallet.domain.repository.TemplateRepository
import com.osscabinet.ossWallet.domain.repository.ComputationRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.usecase.TransactionUseCases
import com.osscabinet.ossWallet.domain.service.CurrencyService
import com.osscabinet.ossWallet.domain.service.TransactionManager
import com.osscabinet.ossWallet.ui.viewmodel.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.math.BigDecimal

/**
 * ViewModel dedicated to transaction entry screen state management.
 * Handles all form state, validation, and transaction operations.
 */
class TransactionEntryViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val transactionUseCases: TransactionUseCases,
    private val transactionManager: TransactionManager,
    settingsRepository: SettingsRepository,
    private val computationRepository: ComputationRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val tagRepository: TagRepository,
    private val templateRepository: TemplateRepository
) : BaseViewModel(settingsRepository) {

    private val _state = MutableStateFlow(TransactionEntryState())
    val state: StateFlow<TransactionEntryState> = _state.asStateFlow()

    private var initialTimestamp: Long = System.currentTimeMillis()

    /**
     * Initialize the ViewModel with entry parameters.
     */
    fun initialize(
        transactionId: Long? = null,
        accountId: String? = null,
        initialType: Int = 0,
        initialTemplate: String? = null,
        prefillDto: TransactionPrefillDTO? = null
    ) {
        viewModelScope.launch {
            initialTimestamp = System.currentTimeMillis()

            // Load initial data
            val profileId = settingsRepository.currentProfileId.first()
            val accounts = accountRepository.getAllAccounts(profileId).first()
            val categories = categoryRepository.categories.first()
            val tags = tagRepository.getAllTags().first()
            val templates = templateRepository.getAllTemplates().first()
            val precision = settingsRepository.decimalPrecision.first()
            val transactions = if (transactionId != null) {
                transactionUseCases.getTransactions(profileId).first()
            } else emptyList()

            val isEditMode = transactionId != null && transactionId != -1L
            val existingTransaction = if (isEditMode) {
                transactions.find { it.id == transactionId }
            } else null

            val linkedTxs = if (isEditMode && transactionId != null) {
                transactionUseCases.getLinkedTransactions(transactionId).first()
            } else emptyList()

            // Set default account if provided
            val selectedAccount = accountId?.let { id -> accounts.find { it.id == id } }
                ?: accounts.firstOrNull()

            _state.value = TransactionEntryState(
                transactionId = transactionId,
                isEditMode = isEditMode,
                existingTransaction = existingTransaction,
                linkedTxs = linkedTxs,
                prefillDto = prefillDto,
                accounts = accounts,
                categories = categories,
                allTags = tags,
                templates = templates,
                decimalPrecision = precision,
                transactions = transactions,
                selectedAccount = selectedAccount,
                selectedCurrency = selectedAccount?.currencyCode ?: "",
                transactionType = initialType,
                selectedTemplateName = initialTemplate,
                selectedDateMillis = initialTimestamp
            )

            // Initialize from existing transaction or prefill DTO
            if (isEditMode && existingTransaction != null) {
                initializeFromTransaction(existingTransaction, accounts, linkedTxs)
            } else if (prefillDto != null) {
                initializeFromPrefill(prefillDto, accounts)
            }
        }
    }

    private fun initializeFromTransaction(
        transaction: Transaction,
        accounts: List<Account>,
        linkedTxs: List<Transaction>
    ) {
        val txType = when {
            transaction.category == "Transfer" -> 2
            transaction.transactionType == "income" -> 1
            else -> 0
        }

        val selectedAcc = accounts.find { it.id == transaction.accountId }

        // Find target account for transfers
        var targetAcc: Account? = null
        if (txType == 2) {
            val linkedTransfer = linkedTxs.find { tx ->
                tx.category == "Transfer" &&
                tx.transactionType == "income" &&
                kotlin.math.abs(tx.timestamp - transaction.timestamp) < 1000
            }
            targetAcc = linkedTransfer?.let { accounts.find { acc -> acc.id == it.accountId } }
        }

        _state.value = _state.value.copy(
            title = transaction.title,
            amount = transaction.amount.abs().toPlainString(),
            category = transaction.category,
            selectedCategoryId = transaction.categoryId,
            selectedTags = transaction.tags,
            paymentMethod = transaction.paymentMethod,
            status = transaction.status,
            transactionType = txType,
            selectedDateMillis = transaction.timestamp,
            payee = transaction.payee,
            note = transaction.note,
            warrantyExpirationMillis = transaction.warrantyExpiration ?: System.currentTimeMillis() + 31536000000L,
            isWarrantyEnabled = transaction.warrantyExpiration != null,
            locationCoords = transaction.latitude?.let { lat ->
                transaction.longitude?.let { lng -> lat to lng }
            },
            selectedAccount = selectedAcc,
            selectedCurrency = transaction.currency,
            targetAccount = targetAcc,
            targetCurrency = targetAcc?.currencyCode ?: "",
            exchangeRate = BigDecimal.ONE // Transaction entity doesn't have transferRate
        )
    }

    private fun initializeFromPrefill(prefillDto: TransactionPrefillDTO, accounts: List<Account>) {
        val prefillAcc = accounts.find { it.id == prefillDto.accountId }
        val txType = when (prefillDto.transactionType) {
            "transfer" -> 2
            "income" -> 1
            else -> 0
        }

        var targetAcc: Account? = null
        if (txType == 2) {
            targetAcc = prefillDto.targetAccountId?.let { accounts.find { acc -> acc.id == it } }
        }

        _state.value = _state.value.copy(
            title = prefillDto.title,
            amount = prefillDto.amount.toBigDecimalOrNull()?.abs()?.toPlainString() ?: prefillDto.amount,
            category = prefillDto.category,
            selectedCategoryId = prefillDto.categoryId,
            selectedTags = prefillDto.tags,
            paymentMethod = prefillDto.paymentMethod,
            status = prefillDto.status,
            transactionType = txType,
            selectedDateMillis = System.currentTimeMillis(),
            payee = prefillDto.payee,
            note = prefillDto.note,
            warrantyExpirationMillis = prefillDto.warrantyExpiration ?: System.currentTimeMillis() + 31536000000L,
            isWarrantyEnabled = prefillDto.warrantyExpiration != null,
            locationCoords = prefillDto.latitude?.let { lat ->
                prefillDto.longitude?.let { lng -> lat to lng }
            },
            linkedTransactionIds = prefillDto.clonedFromTransactionId?.let { listOf(it) } ?: emptyList(),
            selectedAccount = prefillAcc,
            selectedCurrency = prefillDto.currency,
            targetAccount = targetAcc,
            targetCurrency = targetAcc?.currencyCode ?: "",
            exchangeRate = prefillDto.exchangeRate.toBigDecimalOrNull() ?: BigDecimal.ONE
        )
    }

    /**
     * Handle state modification events.
     */
    fun onEvent(event: TransactionEntryEvent) {
        when (event) {
            is TransactionEntryEvent.SetTitle -> update { it.copy(title = event.title) }
            is TransactionEntryEvent.SetAmount -> update { it.copy(amount = event.amount) }
            is TransactionEntryEvent.SetCategory -> update {
                it.copy(category = event.category, selectedCategoryId = event.categoryId)
            }
            is TransactionEntryEvent.SetTags -> update { it.copy(selectedTags = event.tags) }
            is TransactionEntryEvent.AddTag -> update {
                it.copy(selectedTags = it.selectedTags + event.tag)
            }
            is TransactionEntryEvent.RemoveTag -> update {
                it.copy(selectedTags = it.selectedTags - event.tag)
            }
            is TransactionEntryEvent.SetPaymentMethod -> update {
                it.copy(paymentMethod = event.method)
            }
            is TransactionEntryEvent.SetStatus -> update { it.copy(status = event.status) }
            is TransactionEntryEvent.SetTransactionType -> {
                val oldType = _state.value.transactionType
                update { it.copy(transactionType = event.type) }

                // Auto-select target account when switching to transfer
                if (event.type == 2 && oldType != 2) {
                    autoSelectTargetAccount()
                }

                // Recalculate exchange rate if switching to/from transfer
                if (event.type == 2) {
                    recalculateExchangeRate()
                }
            }
            is TransactionEntryEvent.SetDate -> update { it.copy(selectedDateMillis = event.millis) }
            is TransactionEntryEvent.SetPayee -> update { it.copy(payee = event.payee) }
            is TransactionEntryEvent.SetNote -> update { it.copy(note = event.note) }
            is TransactionEntryEvent.SetWarrantyEnabled -> update {
                it.copy(isWarrantyEnabled = event.enabled)
            }
            is TransactionEntryEvent.SetWarrantyExpiration -> update {
                it.copy(warrantyExpirationMillis = event.millis)
            }
            is TransactionEntryEvent.SetLocation -> update { it.copy(locationCoords = event.coords) }
            is TransactionEntryEvent.AddLinkedTransaction -> update {
                it.copy(linkedTransactionIds = it.linkedTransactionIds + event.id)
            }
            is TransactionEntryEvent.RemoveLinkedTransaction -> update {
                it.copy(linkedTransactionIds = it.linkedTransactionIds - event.id)
            }
            is TransactionEntryEvent.SetLinkedTransactionIds -> update {
                it.copy(linkedTransactionIds = event.ids)
            }
            is TransactionEntryEvent.SetAccount -> {
                update {
                    it.copy(
                        selectedAccount = event.account,
                        selectedCurrency = event.account?.currencyCode ?: it.selectedCurrency
                    )
                }
                recalculateExchangeRate()
            }
            is TransactionEntryEvent.SetCurrency -> {
                update { it.copy(selectedCurrency = event.currency) }
                recalculateExchangeRate()
            }
            is TransactionEntryEvent.SetTargetAccount -> {
                update {
                    it.copy(
                        targetAccount = event.account,
                        targetCurrency = event.account?.currencyCode ?: it.targetCurrency
                    )
                }
                recalculateExchangeRate()
            }
            is TransactionEntryEvent.SetTargetCurrency -> {
                update { it.copy(targetCurrency = event.currency) }
                recalculateExchangeRate()
            }
            is TransactionEntryEvent.SetExchangeRate -> update {
                it.copy(exchangeRate = event.rate)
            }
            is TransactionEntryEvent.SetTemplate -> update {
                it.copy(selectedTemplateName = event.templateName)
            }
            is TransactionEntryEvent.ApplyTemplate -> applyTemplate(event.template)

            // Sheet visibility
            is TransactionEntryEvent.ShowDatePicker -> update { it.copy(showDatePicker = event.show) }
            is TransactionEntryEvent.ShowCategorySheet -> update { it.copy(showCategorySheet = event.show) }
            is TransactionEntryEvent.ShowAccountSheet -> update { it.copy(showAccountSheet = event.show) }
            is TransactionEntryEvent.ShowTargetAccountSheet -> update {
                it.copy(showTargetAccountSheet = event.show)
            }
            is TransactionEntryEvent.ShowExchangeRateDialog -> update {
                it.copy(showExchangeRateDialog = event.show)
            }
            is TransactionEntryEvent.ShowTemplateSheet -> update { it.copy(showTemplateSheet = event.show) }
            is TransactionEntryEvent.ShowLinkedTransactionSheet -> update {
                it.copy(showLinkedTransactionSheet = event.show)
            }
            is TransactionEntryEvent.ShowLabelSheet -> update { it.copy(showLabelSheet = event.show) }
            is TransactionEntryEvent.ShowPaymentMethodSheet -> update {
                it.copy(showPaymentMethodSheet = event.show)
            }
            is TransactionEntryEvent.ShowStatusSheet -> update { it.copy(showStatusSheet = event.show) }
            is TransactionEntryEvent.ShowWarrantyDatePicker -> update {
                it.copy(showWarrantyDatePicker = event.show)
            }
            is TransactionEntryEvent.ShowLocationSheet -> update { it.copy(showLocationSheet = event.show) }
            is TransactionEntryEvent.ShowNumpad -> update { it.copy(isNumpadVisible = event.show) }

            // Navigation
            is TransactionEntryEvent.TriggerNavigation -> update { it.copy(navTriggered = true) }
            is TransactionEntryEvent.ClearNavigationTrigger -> update { it.copy(navTriggered = false) }
        }
    }

    private fun applyTemplate(template: Template) {
        viewModelScope.launch {
            val categories = categoryRepository.categories.first()
            val categoryId = categories.find { it.name == template.category }?.id

            _state.value = _state.value.copy(
                title = template.name,
                category = template.category,
                selectedCategoryId = categoryId,
                selectedTags = template.tags,
                paymentMethod = template.paymentMethod ?: "Cash",
                payee = template.payee ?: "",
                note = template.note ?: "",
                selectedTemplateName = template.name
            )
        }
    }

    /**
     * Auto-select a different account as target when switching to transfer.
     * If only one account exists, target remains null (Outside of wallet).
     */
    private fun autoSelectTargetAccount() {
        val currentState = _state.value
        val selectedAccount = currentState.selectedAccount
        val accounts = currentState.accounts

        if (selectedAccount != null && accounts.size > 1) {
            // Find a different account to auto-select as target
            val targetAccount = accounts.find { it.id != selectedAccount.id }
                ?: accounts.firstOrNull()

            if (targetAccount != null && targetAccount.id != selectedAccount.id) {
                update {
                    it.copy(
                        targetAccount = targetAccount,
                        targetCurrency = targetAccount.currencyCode
                    )
                }
                recalculateExchangeRate()
            }
        }
    }

    private fun recalculateExchangeRate() {
        viewModelScope.launch {
            val currentState = _state.value
            if (currentState.isTransfer &&
                currentState.selectedCurrency.isNotEmpty() &&
                currentState.targetCurrency.isNotEmpty()) {

                val rates = computationRepository.exchangeRates.first()
                val rate = CurrencyService.convertAmount(
                    amount = BigDecimal.ONE,
                    fromCurrency = currentState.selectedCurrency,
                    toCurrency = currentState.targetCurrency,
                    exchangeRates = rates
                )
                update { it.copy(exchangeRate = rate) }
            }
        }
    }

    /**
     * Build a TransactionRequest from current state.
     */
    fun buildTransactionRequest(): TransactionRequest? {
        val currentState = _state.value
        if (!currentState.canSave) return null

        val finalTimestamp = if (!currentState.isEditMode &&
            currentState.selectedDateMillis == initialTimestamp) {
            System.currentTimeMillis()
        } else {
            currentState.selectedDateMillis
        }

        return TransactionRequest(
            title = currentState.title.ifBlank { currentState.category },
            amount = if (currentState.isIncome || currentState.isTransfer) {
                currentState.amountValue
            } else {
                currentState.amountValue.negate()
            },
            category = currentState.category,
            transactionType = when {
                currentState.isIncome -> "income"
                currentState.isTransfer -> "expense" // Transfer is stored as expense with link
                else -> "expense"
            },
            accountId = currentState.selectedAccount?.id ?: "",
            currency = currentState.selectedCurrency,
            categoryId = currentState.selectedCategoryId,
            tags = currentState.selectedTags,
            paymentMethod = currentState.paymentMethod,
            timestamp = finalTimestamp,
            payee = currentState.payee,
            note = currentState.note,
            latitude = currentState.locationCoords?.first,
            longitude = currentState.locationCoords?.second,
            warrantyExpiration = if (currentState.isWarrantyEnabled) {
                currentState.warrantyExpirationMillis
            } else null,
            status = currentState.status,
            isTransfer = currentState.isTransfer,
            targetAccountId = currentState.targetAccount?.id,
            targetCurrency = currentState.targetCurrency,
            transferRate = if (currentState.isTransfer) currentState.exchangeRate else BigDecimal.ONE
        )
    }

    /**
     * Save the transaction (create new or update existing).
     */
    fun saveTransaction(
        onSuccess: (transactionId: Long) -> Unit = {}
    ) {
        viewModelScope.launch {
            val request = buildTransactionRequest() ?: return@launch
            val currentState = _state.value
            val profileId = settingsRepository.currentProfileId.first()

            if (currentState.isEditMode && currentState.transactionId != null) {
                val existing = currentState.existingTransaction ?: return@launch
                val id = transactionManager.handleSaveTransaction(existing, request, profileId)
                updateLinkedTransactions(currentState.transactionId)
                onSuccess(id)
            } else {
                val newId = transactionManager.handleSaveTransaction(null, request, profileId)
                // Add links for cloned transaction
                currentState.prefillDto?.clonedFromTransactionId?.let {
                    transactionUseCases.addTransactionLink(newId, it)
                }
                // Add any additional links
                currentState.linkedTransactionIds.forEach { linkedId ->
                    transactionUseCases.addTransactionLink(newId, linkedId)
                }
                onSuccess(newId)
            }
        }
    }

    /**
     * Set target account to null for outside-of-wallet transfers.
     */
    fun clearTargetAccount() {
        update {
            it.copy(
                targetAccount = null,
                targetCurrency = ""
            )
        }
    }

    private suspend fun updateLinkedTransactions(transactionId: Long) {
        val currentIds = _state.value.linkedTxs.map { it.id }.toSet()
        val selectedIds = _state.value.linkedTransactionIds.toSet()

        (currentIds - selectedIds).forEach { id ->
            transactionUseCases.removeTransactionLink(transactionId, id)
        }
        (selectedIds - currentIds).forEach { id ->
            transactionUseCases.addTransactionLink(transactionId, id)
        }
    }

    /**
     * Delete the current transaction.
     */
    fun deleteTransaction(onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            _state.value.transactionId?.let { id ->
                transactionUseCases.deleteTransaction(id)
                onDeleted()
            }
        }
    }

    /**
     * Add a new tag to the repository.
     */
    fun addTag(name: String) {
        viewModelScope.launch {
            tagRepository.insertTag(com.osscabinet.ossWallet.domain.model.Tag(name = name))
        }
    }

    /**
     * Clear prefill DTO state.
     */
    fun clearPrefillDto() {
        // Handled by the DisposableEffect in the UI
    }

    /**
     * Reset navigation trigger on resume.
     */
    fun onScreenResume() {
        onEvent(TransactionEntryEvent.ClearNavigationTrigger)
    }

    private inline fun update(transform: (TransactionEntryState) -> TransactionEntryState) {
        _state.value = transform(_state.value)
    }
}
