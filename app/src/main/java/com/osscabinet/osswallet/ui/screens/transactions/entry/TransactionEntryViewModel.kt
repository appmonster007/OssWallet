package com.osscabinet.ossWallet.ui.screens.transactions.entry

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.OccurrenceStatus
import com.osscabinet.ossWallet.domain.model.PlannedPaymentOccurrence
import com.osscabinet.ossWallet.domain.model.PlannedPaymentStatus
import com.osscabinet.ossWallet.domain.model.Tag
import com.osscabinet.ossWallet.domain.model.Template
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.dto.TransactionPrefillDTO
import com.osscabinet.ossWallet.domain.model.dto.TransactionRequest
import com.osscabinet.ossWallet.domain.repository.AccountRepository
import com.osscabinet.ossWallet.domain.repository.CategoryRepository
import com.osscabinet.ossWallet.domain.repository.ComputationRepository
import com.osscabinet.ossWallet.domain.repository.PlannedPaymentRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.repository.TagRepository
import com.osscabinet.ossWallet.domain.repository.TemplateRepository
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.domain.service.management.PlannedPaymentService
import com.osscabinet.ossWallet.domain.service.management.TransactionManager
import com.osscabinet.ossWallet.domain.usecase.TransactionUseCases
import com.osscabinet.ossWallet.ui.util.BaseViewModel
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
    private val templateRepository: TemplateRepository,
    private val plannedPaymentRepository: PlannedPaymentRepository
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
        initialTemplateId: Long? = null,
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
            val payees = transactionUseCases.getUniquePayees(profileId)
            val notes = transactionUseCases.getUniqueNotes(profileId)
            val precision = settingsRepository.decimalPrecision.first()
            val use24Hour = settingsRepository.use24HourFormat.first()
            val mapsAccess = settingsRepository.mapsAccessEnabled.first()
            val transactions = transactionUseCases.getTransactions(profileId).first()

            val isEditMode = transactionId != null && transactionId != -1L
            val existingTransaction = if (isEditMode) {
                transactions.find { it.id == transactionId }
            } else null

            val linkedTxs = if (isEditMode) {
                transactionUseCases.getLinkedTransactions(transactionId!!).first()
            } else emptyList()

            // Merge linkedTxs into transactions list to ensure they are available for display
            val allRelevantTransactions = (transactions + linkedTxs).distinctBy { it.id }

            // Set default account if provided
            val selectedAccount = accountId?.let { id -> accounts.find { it.id == id } }
                ?: accounts.firstOrNull()
            
            val defaultCategory = categories.find { it.id == 1002L } ?: categories.firstOrNull()

            _state.value = TransactionEntryState(
                transactionId = transactionId,
                isEditMode = isEditMode,
                existingTransaction = existingTransaction,
                linkedTxs = linkedTxs,
                linkedTransactionIds = linkedTxs.map { it.id },
                prefillDto = prefillDto,
                accounts = accounts,
                categories = categories,
                allTags = tags,
                templates = templates,
                allPayees = payees,
                allNotes = notes,
                decimalPrecision = precision,
                use24HourFormat = use24Hour,
                mapsAccessEnabled = mapsAccess,
                transactions = allRelevantTransactions,
                selectedAccount = selectedAccount,
                selectedCurrency = selectedAccount?.currencyCode ?: "",
                transactionType = initialType,
                selectedTemplateName = null, // Will be set by applyPrefill
                selectedDateMillis = initialTimestamp,
                selectedCategoryId = defaultCategory?.id ?: 1002L
            )

            // Initialize from template ID if provided
            if (initialTemplateId != null && initialTemplateId != -1L) {
                templates.find { it.id == initialTemplateId }?.let { template ->
                    applyPrefill(TransactionPrefillDTO.fromTemplate(template))
                }
            }

            // Initialize from existing transaction or prefill DTO
            if (isEditMode && existingTransaction != null) {
                initializeFromTransaction(existingTransaction, accounts, linkedTxs)
            } else if (prefillDto != null) {
                applyPrefill(prefillDto)
            }
        }
    }

    private fun initializeFromTransaction(
        transaction: Transaction,
        accounts: List<Account>,
        linkedTxs: List<Transaction>
    ) {
        val txType = when {
            transaction.categoryId == 1003L -> 2
            transaction.transactionType == "income" -> 1
            else -> 0
        }

        val selectedAcc = accounts.find { it.id == transaction.accountId }

        // Find target account for transfers
        var targetAcc: Account? = null
        if (txType == 2) {
            val linkedTransfer = linkedTxs.find { tx ->
                tx.categoryId == 1003L &&
                        tx.transactionType == "income" &&
                        kotlin.math.abs(tx.timestamp - transaction.timestamp) < 1000
            }
            targetAcc = linkedTransfer?.let { accounts.find { acc -> acc.id == it.accountId } }
        }

        _state.value = _state.value.copy(
            name = transaction.name,
            amount = transaction.amount.abs().toPlainString(),
            selectedCategoryId = transaction.categoryId,
            selectedTags = transaction.tags,
            paymentMethod = transaction.paymentMethod,
            status = transaction.status,
            transactionType = txType,
            selectedDateMillis = transaction.timestamp,
            payee = transaction.payee,
            note = transaction.note,
            warrantyExpirationMillis = transaction.warrantyExpiration
                ?: (System.currentTimeMillis() + 31536000000L),
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

    private fun applyPrefill(prefill: TransactionPrefillDTO) {
        val currentState = _state.value
        val accounts = currentState.accounts
        
        val prefillAcc = accounts.find { it.id == prefill.accountId } ?: currentState.selectedAccount
        
        val txType = when (prefill.transactionType) {
            "transfer" -> 2
            "income" -> 1
            else -> 0
        }

        var targetAcc: Account? = null
        if (txType == 2) {
            targetAcc = prefill.targetAccountId?.let { id -> accounts.find { acc -> acc.id == id } }
                ?: currentState.targetAccount
        }

        _state.value = currentState.copy(
            name = prefill.name,
            amount = prefill.amount.toBigDecimalOrNull()?.abs()?.stripTrailingZeros()?.toPlainString()
                ?: prefill.amount,
            selectedCategoryId = prefill.categoryId,
            selectedTags = prefill.tags,
            paymentMethod = prefill.paymentMethod,
            status = prefill.status,
            transactionType = txType,
            payee = prefill.payee,
            note = prefill.note,
            warrantyExpirationMillis = prefill.warrantyExpiration
                ?: System.currentTimeMillis() + 31536000000L,
            isWarrantyEnabled = prefill.warrantyExpiration != null,
            locationCoords = prefill.latitude?.let { lat ->
                prefill.longitude?.let { lng -> lat to lng }
            },
            linkedTransactionIds = prefill.clonedFromTransactionId?.let { listOf(it) }
                ?: prefill.linkedTransactionIds,
            selectedAccount = prefillAcc,
            selectedCurrency = prefill.currency.ifEmpty { prefillAcc?.currencyCode ?: "" },
            selectedDateMillis = prefill.timestamp ?: currentState.selectedDateMillis,
            targetAccount = targetAcc,
            targetCurrency = prefill.targetCurrency ?: targetAcc?.currencyCode ?: "",
            exchangeRate = prefill.exchangeRate.toBigDecimalOrNull() ?: BigDecimal.ONE,
            selectedTemplateName = if (prefill.sourceTemplateId != null) prefill.name else currentState.selectedTemplateName,
            prefillDto = prefill,
            isSplitMode = prefill.isSplitMode,
            showSplitSheet = prefill.isSplitMode,
            splits = if (prefill.isSplitMode) {
                listOf(
                    TransactionSplit(categoryId = prefill.categoryId, amount = prefill.amount, note = prefill.note),
                    TransactionSplit(categoryId = null, amount = "", note = "")
                )
            } else emptyList()
        )

        // Post-prefill logic for transfers
        if (txType == 2) {
            if (_state.value.targetAccount == null) {
                autoSelectTargetAccount()
            }
            recalculateExchangeRate()
        }
    }

    /**
     * Handle state modification events.
     */
    fun onEvent(event: TransactionEntryEvent) {
        when (event) {
            is TransactionEntryEvent.SetName -> update { it.copy(name = event.name) }
            is TransactionEntryEvent.SetAmount -> update { it.copy(amount = event.amount) }
            is TransactionEntryEvent.SetCategory -> update {
                it.copy(selectedCategoryId = event.categoryId)
            }

            is TransactionEntryEvent.SetTags -> update { it.copy(selectedTags = event.tags) }
            is TransactionEntryEvent.AddTag -> {
                update { it.copy(selectedTags = it.selectedTags + event.tag) }
                viewModelScope.launch {
                    tagRepository.insertTag(Tag(name = event.tag, colorHex = event.color ?: -1))
                }
            }

            is TransactionEntryEvent.DeleteTag -> {
                update { it.copy(selectedTags = it.selectedTags - event.tag.name) }
                viewModelScope.launch {
                    tagRepository.deleteTagAndRemoveFromTransactions(event.tag)
                }
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

            is TransactionEntryEvent.RemoveLinkedTransaction -> {
                // Remove from local state. Database update happens on Save.
                update { 
                    it.copy(linkedTransactionIds = it.linkedTransactionIds.filter { id -> id != event.linkedTransactionId })
                }
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

            is TransactionEntryEvent.ApplyTemplate -> applyPrefill(TransactionPrefillDTO.fromTemplate(event.template))
            is TransactionEntryEvent.ApplyPrefill -> applyPrefill(event.prefill)

            // Splitting
            is TransactionEntryEvent.ToggleSplitMode -> update {
                val newState = it.copy(isSplitMode = event.enabled)
                if (event.enabled && newState.splits.isEmpty()) {
                    // Initialize with 2 splits: 1st with current amount/category, 2nd empty
                    newState.copy(splits = listOf(
                        TransactionSplit(categoryId = it.selectedCategoryId, amount = it.amount, note = it.note),
                        TransactionSplit(categoryId = null, amount = "", note = "")
                    ))
                } else newState
            }
            is TransactionEntryEvent.AddSplit -> update {
                it.copy(splits = it.splits + TransactionSplit(categoryId = it.selectedCategoryId))
            }
            is TransactionEntryEvent.RemoveSplit -> update {
                val newSplits = it.splits.filterIndexed { index, _ -> index != event.index }
                if (newSplits.size <= 1) {
                    it.copy(
                        isSplitMode = false,
                        showSplitSheet = false,
                        splits = emptyList()
                    )
                } else {
                    it.copy(splits = newSplits)
                }
            }
            is TransactionEntryEvent.UpdateSplit -> update {
                val newSplits = it.splits.toMutableList()
                if (event.index in newSplits.indices) {
                    val current = newSplits[event.index]
                    newSplits[event.index] = current.copy(
                        categoryId = event.categoryId ?: current.categoryId,
                        amount = event.amount ?: current.amount,
                        note = event.note ?: current.note
                    )
                }
                it.copy(splits = newSplits)
            }

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

            is TransactionEntryEvent.ShowTagSheet -> update { it.copy(showTagSheet = event.show) }
            is TransactionEntryEvent.ShowPaymentMethodSheet -> update {
                it.copy(showPaymentMethodSheet = event.show)
            }

            is TransactionEntryEvent.ShowStatusSheet -> update { it.copy(showStatusSheet = event.show) }
            is TransactionEntryEvent.ShowWarrantyDatePicker -> update {
                it.copy(showWarrantyDatePicker = event.show)
            }

            is TransactionEntryEvent.ShowLocationSheet -> update { it.copy(showLocationSheet = event.show) }
            is TransactionEntryEvent.ShowSplitSheet -> update { it.copy(showSplitSheet = event.show) }
            is TransactionEntryEvent.ShowSplitCategorySheet -> update { it.copy(showSplitCategorySheet = event.splitIndex) }
            is TransactionEntryEvent.ShowNumpad -> update { it.copy(isNumpadVisible = event.show) }
            is TransactionEntryEvent.ShowCurrencySheet -> update { it.copy(showCurrencySheet = event.show) }
            is TransactionEntryEvent.ShowTargetCurrencySheet -> update { it.copy(showTargetCurrencySheet = event.show) }

            // Navigation
            is TransactionEntryEvent.TriggerNavigation -> update { it.copy(navTriggered = true) }
            is TransactionEntryEvent.ClearNavigationTrigger -> update { it.copy(navTriggered = false) }
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
                currentState.targetCurrency.isNotEmpty()
            ) {

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
            currentState.selectedDateMillis == initialTimestamp
        ) {
            System.currentTimeMillis()
        } else {
            currentState.selectedDateMillis
        }

        return TransactionRequest(
            name = currentState.name.ifBlank {
                currentState.categories.find { it.id == currentState.selectedCategoryId }?.name ?: "Transaction"
            },
            amount = if (currentState.isIncome || currentState.isTransfer) {
                currentState.amountValue
            } else {
                currentState.amountValue.negate()
            },
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
            sourcePlannedPaymentId = currentState.prefillDto?.sourcePlannedPaymentId,
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
            val currentState = _state.value
            if (!currentState.canSave) return@launch
            
            val profileId = settingsRepository.currentProfileId.first()

            if (currentState.isSplitMode) {
                // Handle Split Transactions
                val splitTransactionIds = mutableListOf<Long>()
                
                // If editing, delete original
                if (currentState.isEditMode && currentState.transactionId != null) {
                    transactionUseCases.deleteTransaction(currentState.transactionId)
                }

                currentState.splits.forEach { split ->
                    val splitAmount = split.amount.toBigDecimalOrNull() ?: BigDecimal.ZERO
                    val request = buildBaseRequest().copy(
                        amount = if (currentState.isIncome) splitAmount else splitAmount.negate(),
                        categoryId = split.categoryId ?: currentState.selectedCategoryId,
                        note = split.note,
                        name = currentState.name.ifBlank {
                            currentState.categories.find { it.id == (split.categoryId ?: currentState.selectedCategoryId) }?.name ?: "Transaction"
                        }
                    )
                    
                    val newId = transactionManager.handleSaveTransaction(null, request, profileId)
                    splitTransactionIds.add(newId)
                }
                
                // Link all split transactions together
                if (splitTransactionIds.size > 1) {
                    for (i in splitTransactionIds.indices) {
                        for (j in i + 1 until splitTransactionIds.size) {
                            transactionUseCases.addTransactionLink(splitTransactionIds[i], splitTransactionIds[j])
                        }
                    }
                }
                
                onSuccess(splitTransactionIds.firstOrNull() ?: -1L)
            } else {
                val request = buildTransactionRequest() ?: return@launch
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

                    // Handle Planned Payment side effects
                    currentState.prefillDto?.sourcePlannedPaymentId?.let { paymentId ->
                        val payment = plannedPaymentRepository.getById(paymentId)
                        if (payment != null) {
                            // Mark occurrence as confirmed
                            val occurrence = PlannedPaymentOccurrence(
                                id = 0,
                                plannedPaymentId = paymentId,
                                scheduledDate = payment.schedule.nextDueDate, // Assume we are confirming the next one
                                status = OccurrenceStatus.CONFIRMED,
                                generatedTransactionId = newId
                            )
                            plannedPaymentRepository.upsertOccurrence(occurrence)

                            // Update planned payment next due date
                            val nextDue = PlannedPaymentService.calculateNextDueDate(payment, payment.schedule.nextDueDate)
                            if (nextDue == null) {
                                plannedPaymentRepository.update(payment.copy(status = PlannedPaymentStatus.COMPLETED))
                            } else {
                                plannedPaymentRepository.update(
                                    payment.copy(
                                        schedule = payment.schedule.copy(
                                            nextDueDate = nextDue,
                                            lastTriggeredDate = payment.schedule.nextDueDate
                                        )
                                    )
                                )
                            }
                        }
                    }

                    // Add any additional links
                    currentState.linkedTransactionIds.forEach { linkedId ->
                        transactionUseCases.addTransactionLink(newId, linkedId)
                    }
                    onSuccess(newId)
                }
            }
        }
    }

    private fun buildBaseRequest(): TransactionRequest {
        val currentState = _state.value
        val finalTimestamp = if (!currentState.isEditMode &&
            currentState.selectedDateMillis == initialTimestamp
        ) {
            System.currentTimeMillis()
        } else {
            currentState.selectedDateMillis
        }

        return TransactionRequest(
            name = currentState.name,
            amount = BigDecimal.ZERO, // To be set by caller
            transactionType = when {
                currentState.isIncome -> "income"
                currentState.isTransfer -> "expense"
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
            sourcePlannedPaymentId = currentState.prefillDto?.sourcePlannedPaymentId,
            isTransfer = currentState.isTransfer,
            targetAccountId = currentState.targetAccount?.id,
            targetCurrency = currentState.targetCurrency,
            transferRate = if (currentState.isTransfer) currentState.exchangeRate else BigDecimal.ONE
        )
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
    fun addTag(name: String, color: Int? = null) {
        viewModelScope.launch {
            tagRepository.insertTag(Tag(name = name, colorHex = color ?: -1))
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
