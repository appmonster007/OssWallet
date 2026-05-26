package com.osscabinet.ossWallet.ui.screens.transactions.entry

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Tag
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionSplit
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.dto.TransactionPrefillDTO
import com.osscabinet.ossWallet.domain.model.dto.TransactionRequest
import com.osscabinet.ossWallet.domain.repository.AccountRepository
import com.osscabinet.ossWallet.domain.repository.CategoryRepository
import com.osscabinet.ossWallet.domain.repository.ComputationRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.repository.TagRepository
import com.osscabinet.ossWallet.domain.repository.TemplateRepository
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.domain.service.management.TransactionManager
import com.osscabinet.ossWallet.domain.usecase.TransactionUseCases
import com.osscabinet.ossWallet.ui.util.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

/**
 * ViewModel dedicated to transaction entry screen state management.
 * Handles all form state, validation, and transaction operations.
 */
@HiltViewModel
class TransactionEntryViewModel @Inject constructor(
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

    init {
        viewModelScope.launch {
            effectiveShortlistedCurrencies.collect { currencies ->
                _state.update { it.copy(shortlistedCurrencies = currencies) }
            }
        }
    }

    /**
     * Initialize the ViewModel with entry parameters.
     */
    fun initialize(
        transactionId: Long? = null,
        accountId: String? = null,
        initialType: TransactionType = TransactionType.EXPENSE,
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
            val mapsAccess = settingsRepository.mapsAccessEnabled.first()

            val isEditMode = transactionId != null && transactionId != -1L
            val existingTransaction = if (isEditMode) {
                transactionUseCases.getTransactionById(transactionId!!)
            } else null

            val linkedTxs = if (isEditMode) {
                transactionUseCases.getLinkedTransactions(transactionId!!).first()
            } else emptyList()

            // Set default account if provided
            val selectedAccount = accountId?.let { id -> accounts.find { it.id == id } }
                ?: accounts.firstOrNull()

            val defaultCategory = categories.find { it.id == Category.SYSTEM_UNKNOWN } ?: categories.firstOrNull()

            _state.update {
                TransactionEntryState(
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
                    mapsAccessEnabled = mapsAccess,
                    selectedAccount = selectedAccount,
                    selectedCurrency = selectedAccount?.currencyCode ?: "",
                    transactionType = initialType,
                    selectedTemplateName = null,
                    selectedDateMillis = initialTimestamp,
                    selectedCategoryId = defaultCategory?.id ?: Category.SYSTEM_UNKNOWN,
                    shortlistedCurrencies = effectiveShortlistedCurrencies.value,
                    fullProviderCurrencies = CurrencyService.getAllCurrencies()
                )
            }

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

            // Auto-select target account if initialized as transfer
            if (initialType == TransactionType.TRANSFER && existingTransaction == null && prefillDto == null) {
                autoSelectTargetAccount()
            }
        }
    }

    private fun initializeFromTransaction(
        transaction: Transaction,
        accounts: List<Account>,
        linkedTxs: List<Transaction>
    ) {
        val txType = when {
            Category.isTransfer(transaction.categoryId) -> TransactionType.TRANSFER
            else -> transaction.transactionType
        }

        val selectedAcc = accounts.find { it.id == transaction.accountId }

        // Find target account for transfers
        var targetAcc: Account? = null
        if (txType == TransactionType.TRANSFER) {
            val linkedTransfer = linkedTxs.find { tx ->
                Category.isTransfer(tx.categoryId) &&
                        tx.transactionType == TransactionType.TRANSFER &&
                        tx.id != transaction.id &&
                        kotlin.math.abs(tx.timestamp - transaction.timestamp) < 5000
            }
            targetAcc = linkedTransfer?.let { accounts.find { acc -> acc.id == it.accountId } }
        }

        _state.update {
            it.copy(
                amount = transaction.amount.abs().stripTrailingZeros().toPlainString(),
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
                gpsLocation = transaction.gpsLocation,
                location = transaction.location,
                selectedAccount = selectedAcc,
                selectedCurrency = transaction.currency,
                targetAccount = targetAcc,
                targetCurrency = targetAcc?.currencyCode ?: "",
                exchangeRate = BigDecimal.ONE // Transaction entity doesn't have transferRate
            )
        }
    }

    private fun applyPrefill(prefill: TransactionPrefillDTO) {
        val currentState = _state.value
        val accounts = currentState.accounts

        val prefillAcc =
            accounts.find { it.id == prefill.accountId } ?: currentState.selectedAccount

        val txType = prefill.transactionType

        var targetAcc: Account? = null
        var sourceAcc = prefillAcc
        if (txType == TransactionType.TRANSFER) {
            targetAcc = prefill.targetAccountId?.let { id -> accounts.find { acc -> acc.id == id } }
                ?: currentState.targetAccount
            
            // If it's an inflow template (target-only, no source account)
            if (prefill.accountId == null && prefill.categoryId == Category.SYSTEM_TRANSFER_INFLOW) {
                sourceAcc = null
                targetAcc = prefill.targetAccountId?.let { accounts.find { a -> a.id == it } }
            }
        }

        _state.update {
            it.copy(
                amount = prefill.amount.toBigDecimalOrNull()?.abs()?.stripTrailingZeros()
                    ?.toPlainString()
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
                gpsLocation = prefill.gpsLocation,
                location = prefill.location,
                linkedTransactionIds = prefill.clonedFromTransactionId?.let { listOf(it) }
                    ?: prefill.linkedTransactionIds,
                selectedAccount = sourceAcc,
                selectedCurrency = prefill.currency.ifEmpty { sourceAcc?.currencyCode ?: it.selectedCurrency },
                selectedDateMillis = prefill.timestamp ?: it.selectedDateMillis,
                targetAccount = targetAcc,
                targetCurrency = prefill.targetCurrency ?: targetAcc?.currencyCode ?: "",
                exchangeRate = prefill.exchangeRate.toBigDecimalOrNull() ?: BigDecimal.ONE,
                selectedTemplateName = if (prefill.sourceTemplateId != null) prefill.name else it.selectedTemplateName,
                prefillDto = prefill,
                isSplitMode = prefill.isSplitMode,
                showSplitSheet = prefill.isSplitMode,
                splits = if (prefill.isSplitMode) {
                    listOf(
                        TransactionSplit(
                            categoryId = prefill.categoryId,
                            amount = prefill.amount,
                            note = prefill.note
                        ),
                        TransactionSplit(categoryId = null, amount = "", note = "")
                    )
                } else emptyList()
            )
        }

        // Post-prefill logic for transfers
        if (txType == TransactionType.TRANSFER) {
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
            is TransactionEntryEvent.SetAmount -> {
                val inputAmount = event.amount.toBigDecimalOrNull() ?: BigDecimal.ZERO
                if (inputAmount < BigDecimal.ZERO) {
                    _state.update { state ->
                        val newType = when (state.transactionType) {
                            TransactionType.EXPENSE -> TransactionType.INCOME
                            TransactionType.INCOME -> TransactionType.EXPENSE
                            else -> state.transactionType
                        }
                        state.copy(
                            amount = inputAmount.abs().toPlainString(),
                            transactionType = newType
                        )
                    }
                } else {
                    _state.update { it.copy(amount = event.amount) }
                }
            }
            is TransactionEntryEvent.SetCategory -> {
                if (!Category.isInitialBalance(event.categoryId)) update {
                    it.copy(selectedCategoryId = event.categoryId)
                }
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
                val newType = event.type
                
                _state.update { state ->
                    val isTransferToNormal = oldType == TransactionType.TRANSFER && newType != TransactionType.TRANSFER
                    
                    val primaryAccount = if (isTransferToNormal && state.isTransferInflow) {
                        state.targetAccount ?: state.selectedAccount
                    } else {
                        state.selectedAccount
                    }

                    state.copy(
                        transactionType = newType,
                        amount = if (state.amount.isEmpty()) "" else state.amountValue.abs().toPlainString(),

                        selectedAccount = when {
                            newType == TransactionType.TRANSFER && oldType == TransactionType.INCOME -> null
                            else -> primaryAccount
                        },

                        targetAccount = when {
                            newType == TransactionType.TRANSFER && oldType == TransactionType.INCOME -> primaryAccount
                            newType == TransactionType.TRANSFER && oldType == TransactionType.EXPENSE -> null
                            newType == TransactionType.TRANSFER -> state.targetAccount
                            else -> null
                        },

                        // Prohibit transfers in split mode
                        isSplitMode = if (newType == TransactionType.TRANSFER) false else state.isSplitMode,
                        showSplitSheet = if (newType == TransactionType.TRANSFER) false else state.showSplitSheet,
                        selectedCategoryId = when {
                            newType != TransactionType.TRANSFER && Category.isTransfer(state.selectedCategoryId) -> Category.SYSTEM_UNKNOWN
                            newType == TransactionType.TRANSFER && oldType == TransactionType.INCOME -> Category.SYSTEM_TRANSFER_INFLOW
                            newType == TransactionType.TRANSFER && oldType == TransactionType.EXPENSE -> Category.SYSTEM_TRANSFER_OUTFLOW
                            newType == TransactionType.TRANSFER -> state.selectedCategoryId.takeIf { Category.isTransfer(it) } ?: Category.SYSTEM_TRANSFER_OUTFLOW
                            else -> state.selectedCategoryId
                        }
                    )
                }

                // Auto-select target account when switching to transfer
                if (newType == TransactionType.TRANSFER && oldType != TransactionType.TRANSFER) {
                    autoSelectTargetAccount()
                }

                // Recalculate exchange rate if switching to/from transfer
                if (newType == TransactionType.TRANSFER) {
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

            is TransactionEntryEvent.SetLocation -> update {
                it.copy(
                    gpsLocation = event.gpsLocation,
                    location = event.location
                )
            }

            is TransactionEntryEvent.AddLinkedTransaction -> {
                viewModelScope.launch {
                    val tx = transactionUseCases.getTransactionById(event.id)
                    tx?.let { newTx ->
                        update {
                            it.copy(
                                linkedTransactionIds = it.linkedTransactionIds + event.id,
                                linkedTxs = (it.linkedTxs + newTx).distinctBy { t -> t.id }
                            )
                        }
                    }
                }
            }

            is TransactionEntryEvent.RemoveLinkedTransaction -> {
                // Remove from local state. Database update happens on Save.
                update {
                    it.copy(
                        linkedTransactionIds = it.linkedTransactionIds.filter { id -> id != event.linkedTransactionId },
                        linkedTxs = it.linkedTxs.filter { tx -> tx.id != event.linkedTransactionId }
                    )
                }
            }

            is TransactionEntryEvent.SetLinkedTransactionIds -> {
                viewModelScope.launch {
                    val txs = event.ids.mapNotNull { transactionUseCases.getTransactionById(it) }
                    update { it.copy(linkedTransactionIds = event.ids, linkedTxs = txs) }
                }
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
                val targetAcc = if (event.account == null) null else event.account
                update {
                    it.copy(
                        targetAccount = targetAcc,
                        targetCurrency = targetAcc?.currencyCode ?: it.targetCurrency
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

            is TransactionEntryEvent.SwapAccounts -> {
                val s = _state.value
                update {
                    it.copy(
                        selectedAccount = s.targetAccount,
                        targetAccount = s.selectedAccount,
                        selectedCurrency = s.targetAccount?.currencyCode ?: s.selectedCurrency,
                        targetCurrency = s.selectedAccount?.currencyCode ?: "",
                        selectedCategoryId = when (s.selectedCategoryId) {
                            Category.SYSTEM_TRANSFER_INFLOW -> Category.SYSTEM_TRANSFER_OUTFLOW
                            Category.SYSTEM_TRANSFER_OUTFLOW -> Category.SYSTEM_TRANSFER_INFLOW
                            else -> s.selectedCategoryId
                        }
                    )
                }
                recalculateExchangeRate()
            }

            is TransactionEntryEvent.SetTemplate -> update {
                it.copy(selectedTemplateName = event.templateName)
            }

            is TransactionEntryEvent.ApplyTemplate -> applyPrefill(
                TransactionPrefillDTO.fromTemplate(
                    event.template
                )
            )

            is TransactionEntryEvent.ApplyPrefill -> applyPrefill(event.prefill)

            // Splitting
            is TransactionEntryEvent.ToggleSplitMode -> update {
                val newState = it.copy(isSplitMode = event.enabled)
                if (event.enabled && newState.splits.isEmpty()) {
                    // Initialize with 2 splits: 1st with current amount/category, 2nd empty
                    newState.copy(
                        splits = listOf(
                            TransactionSplit(
                                categoryId = it.selectedCategoryId,
                                amount = it.amount,
                                note = it.note
                            ),
                            TransactionSplit(categoryId = null, amount = "", note = "")
                        )
                    )
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
            is TransactionEntryEvent.ShowSplitCategorySheet -> update {
                it.copy(
                    showSplitCategorySheet = event.splitIndex
                )
            }

            is TransactionEntryEvent.ShowNumpad -> update { it.copy(isNumpadVisible = event.show) }
            is TransactionEntryEvent.ShowCurrencySheet -> update { it.copy(showCurrencySheet = event.show) }
            is TransactionEntryEvent.ShowTargetCurrencySheet -> update {
                it.copy(
                    showTargetCurrencySheet = event.show
                )
            }

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
        } else if (selectedAccount != null && accounts.size == 1) {
            // If only one account, target is automatically Outside Wallet
            update {
                it.copy(
                    targetAccount = null,
                    targetCurrency = ""
                )
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
            name = currentState.categories.find { it.id == currentState.selectedCategoryId }?.name
                ?: "Transaction",
            amount = when {
                currentState.isIncome -> currentState.amountValue.abs()
                currentState.isExpense -> currentState.amountValue.abs().negate()
                currentState.isTransfer -> if (currentState.isTransferInflow) currentState.amountValue.abs() else currentState.amountValue.abs().negate()
                else -> currentState.amountValue
            },
            transactionType = when {
                currentState.isIncome -> TransactionType.INCOME
                currentState.isTransfer -> TransactionType.TRANSFER
                else -> TransactionType.EXPENSE
            },
            accountId = currentState.selectedAccount?.id,
            currency = currentState.selectedCurrency,
            categoryId = currentState.selectedCategoryId,
            tags = currentState.selectedTags,
            paymentMethod = currentState.paymentMethod,
            timestamp = finalTimestamp,
            payee = currentState.payee,
            note = currentState.note,
            gpsLocation = currentState.gpsLocation,
            location = currentState.location,
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

            if (currentState.isEditMode && currentState.existingTransaction == null) {
                // Stale edit state — VM was not yet initialized with the existing transaction.
                // Do not proceed to avoid creating a duplicate insert.
                return@launch
            }

            val profileId = settingsRepository.currentProfileId.first()

            if (currentState.isSplitMode) {
                val baseRequest = buildBaseRequest()
                val id = transactionManager.handleSaveSplitTransactions(
                    existingTransactionId = if (currentState.isEditMode) currentState.transactionId else null,
                    splits = currentState.splits,
                    baseRequest = baseRequest,
                    profileId = profileId,
                    categoryNameLookup = { catId ->
                        currentState.categories.find { it.id == catId }?.name ?: "Transaction"
                    }
                )
                onSuccess(id)
            } else {
                val request = buildTransactionRequest() ?: return@launch
                val id = transactionManager.handleSaveTransaction(
                    existingTransaction = if (currentState.isEditMode) currentState.existingTransaction else null,
                    request = request,
                    profileId = profileId,
                    linkedTransactionIds = currentState.linkedTransactionIds,
                    clonedFromId = if (currentState.isEditMode) null else currentState.prefillDto?.clonedFromTransactionId,
                    sourcePlannedPaymentId = currentState.prefillDto?.sourcePlannedPaymentId,
                    scheduledDate = currentState.prefillDto?.timestamp
                        ?: currentState.selectedDateMillis
                )
                onSuccess(id)
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
            name = currentState.categories.find { it.id == currentState.selectedCategoryId }?.name
                ?: "Transaction",
            amount = BigDecimal.ZERO, // To be set by caller
            transactionType = when {
                currentState.isIncome -> TransactionType.INCOME
                currentState.isTransfer -> TransactionType.TRANSFER
                else -> TransactionType.EXPENSE
            },
            accountId = currentState.selectedAccount?.id,
            currency = currentState.selectedCurrency,
            categoryId = currentState.selectedCategoryId,
            tags = currentState.selectedTags,
            paymentMethod = currentState.paymentMethod,
            timestamp = finalTimestamp,
            payee = currentState.payee,
            note = currentState.note,
            gpsLocation = currentState.gpsLocation,
            location = currentState.location,
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

    /**
     * Delete the current transaction.
     */
    fun deleteTransaction(onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            _state.value.transactionId?.let { id ->
                transactionManager.deleteTransaction(id)
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

    /**
     * Search transactions for linking.
     */
    fun searchTransactions(query: String): kotlinx.coroutines.flow.Flow<List<Transaction>> = flow {
        val profileId = settingsRepository.currentProfileId.first()
        emitAll(transactionUseCases.searchTransactions(profileId, query))
    }

    private inline fun update(transform: (TransactionEntryState) -> TransactionEntryState) {
        _state.update(transform)
    }
}
