package com.osscabinet.ossWallet.ui.screens.transactions.entry

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Tag
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionSide
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

            val defaultCategory =
                categories.find { it.id == Category.SYSTEM_UNKNOWN } ?: categories.firstOrNull()

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
                    source = TransactionSide(
                        accountId = selectedAccount?.id,
                        currency = selectedAccount?.currencyCode ?: "",
                        categoryId = defaultCategory?.id ?: Category.SYSTEM_UNKNOWN
                    ),
                    transactionType = initialType,
                    selectedTemplateName = null,
                    selectedDateMillis = initialTimestamp,
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

        val isOpenedAsReceiver = Category.isAnyTransferInflow(transaction.categoryId)
        var selectedAcc: Account? = accounts.find { it.id == transaction.accountId }
        var targetAcc: Account? = accounts.find { it.id == transaction.targetAccountId }
        var amountStr = ""
        var targetAmountStr = ""
        var currentCurrency = transaction.currency
        var targetCurrency = transaction.targetCurrency ?: transaction.currency
        var categoryId = transaction.categoryId

        if (txType == TransactionType.TRANSFER) {
            val linkedTransfer = linkedTxs.find { tx ->
                Category.isTransfer(tx.categoryId) &&
                        tx.transactionType == TransactionType.TRANSFER &&
                        tx.id != transaction.id &&
                        kotlin.math.abs(tx.timestamp - transaction.timestamp) < 5000
            }

            if (isOpenedAsReceiver) {
                // Flip for display consistency (Giver above, Receiver below)
                // transaction is the Receiver, so its accountId goes to bottom slot (targetAcc)
                // and its targetAccountId goes to top slot (selectedAcc)
                targetAcc = selectedAcc
                selectedAcc = accounts.find { it.id == transaction.targetAccountId }
                currentCurrency = transaction.targetCurrency ?: transaction.currency
                targetCurrency = transaction.currency
                targetAmountStr = transaction.amount.abs().stripTrailingZeros().toPlainString()
                amountStr = linkedTransfer?.amount?.abs()?.stripTrailingZeros()?.toPlainString()
                    ?: if (transaction.transferRate.signum() != 0) {
                        transaction.amount.abs()
                            .divide(transaction.transferRate, 8, java.math.RoundingMode.HALF_UP)
                            .stripTrailingZeros().toPlainString()
                    } else ""
                categoryId =
                    if (Category.isInitialBalance(transaction.categoryId)) Category.SYSTEM_INITIAL_BALANCE_INFLOW else Category.SYSTEM_TRANSFER_INFLOW
            } else {
                // transaction is the Giver (amount < 0), so its accountId is already in selectedAcc (top slot)
                amountStr = transaction.amount.abs().stripTrailingZeros().toPlainString()
                targetAmountStr =
                    linkedTransfer?.amount?.abs()?.stripTrailingZeros()?.toPlainString()
                        ?: transaction.amount.abs().multiply(transaction.transferRate)
                            .setScale(8, java.math.RoundingMode.HALF_UP).stripTrailingZeros()
                            .toPlainString()
                categoryId =
                    if (Category.isInitialBalance(transaction.categoryId)) Category.SYSTEM_INITIAL_BALANCE_OUTFLOW else Category.SYSTEM_TRANSFER_OUTFLOW
            }
        } else {
            amountStr = transaction.amount.abs().stripTrailingZeros().toPlainString()
        }

        val amountBD = amountStr.toBigDecimalOrNull()
        val targetAmountBD = targetAmountStr.toBigDecimalOrNull()
        val exchangeRate =
            if (amountBD != null && amountBD.signum() != 0 && targetAmountBD != null) {
                targetAmountBD.divide(amountBD, 8, java.math.RoundingMode.HALF_UP)
                    .stripTrailingZeros()
            } else transaction.transferRate

        _state.update {
            it.copy(
                source = TransactionSide(
                    accountId = selectedAcc?.id,
                    amount = amountStr,
                    currency = currentCurrency,
                    categoryId = categoryId
                ),
                destination = TransactionSide(
                    accountId = targetAcc?.id,
                    amount = targetAmountStr,
                    currency = targetCurrency
                ),
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
                exchangeRate = exchangeRate
            )
        }
    }

    private fun applyPrefill(prefill: TransactionPrefillDTO) {
        val currentState = _state.value
        val accounts = currentState.accounts
        val txType = prefill.transactionType

        val isOpenedAsReceiver = Category.isAnyTransferInflow(prefill.categoryId)
        var sourceAcc = accounts.find { it.id == prefill.accountId }
        var targetAcc = prefill.targetAccountId?.let { id -> accounts.find { acc -> acc.id == id } }
        var sourceCurrency =
            prefill.currency.ifEmpty { sourceAcc?.currencyCode ?: currentState.selectedCurrency }
        var targetCurr = prefill.targetCurrency ?: targetAcc?.currencyCode ?: ""
        var amountBD = prefill.amount.toBigDecimalOrNull()?.abs() ?: BigDecimal.ZERO
        val rateBD = prefill.exchangeRate.toBigDecimalOrNull() ?: BigDecimal.ONE
        var targetAmtBD = amountBD.multiply(rateBD).setScale(8, java.math.RoundingMode.HALF_UP)
            .stripTrailingZeros()
        var categoryId = prefill.categoryId

        if (txType == TransactionType.TRANSFER && isOpenedAsReceiver) {
            // Flip for display consistency
            val originalSource = sourceAcc
            sourceAcc = targetAcc
            targetAcc = originalSource

            val originalSourceCurr = sourceCurrency
            sourceCurrency = targetCurr
            targetCurr = originalSourceCurr

            targetAmtBD = amountBD.stripTrailingZeros()
            amountBD = if (rateBD.signum() != 0) {
                targetAmtBD.divide(rateBD, 8, java.math.RoundingMode.HALF_UP).stripTrailingZeros()
            } else targetAmtBD

            categoryId =
                if (Category.isInitialBalance(prefill.categoryId)) Category.SYSTEM_INITIAL_BALANCE_INFLOW else Category.SYSTEM_TRANSFER_INFLOW
        } else if (txType == TransactionType.TRANSFER) {
            categoryId =
                if (Category.isInitialBalance(prefill.categoryId)) Category.SYSTEM_INITIAL_BALANCE_OUTFLOW else Category.SYSTEM_TRANSFER_OUTFLOW
        }

        _state.update {
            it.copy(
                source = TransactionSide(
                    accountId = sourceAcc?.id,
                    amount = amountBD.stripTrailingZeros().toPlainString(),
                    currency = sourceCurrency,
                    categoryId = categoryId
                ),
                destination = TransactionSide(
                    accountId = targetAcc?.id,
                    amount = targetAmtBD.toPlainString(),
                    currency = targetCurr
                ),
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
                selectedDateMillis = prefill.timestamp ?: it.selectedDateMillis,
                exchangeRate = rateBD,
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
                        val finalInput = inputAmount.abs()
                        state.copy(
                            source = state.source.copy(
                                amount = finalInput.stripTrailingZeros().toPlainString()
                            ),
                            destination = state.destination.copy(
                                amount = finalInput.multiply(state.exchangeRate)
                                    .setScale(8, java.math.RoundingMode.HALF_UP).stripTrailingZeros()
                                    .toPlainString()
                            ),
                            transactionType = newType
                        )
                    }
                } else {
                    _state.update {
                        it.copy(
                            source = it.source.copy(amount = event.amount),
                            destination = it.destination.copy(
                                amount = inputAmount.multiply(it.exchangeRate)
                                    .setScale(8, java.math.RoundingMode.HALF_UP).stripTrailingZeros()
                                    .toPlainString()
                            )
                        )
                    }
                }
            }

            is TransactionEntryEvent.SetTargetAmount -> {
                val inputAmount = event.amount.toBigDecimalOrNull() ?: BigDecimal.ZERO
                _state.update { state ->
                    val newSourceAmount = if (state.exchangeRate.signum() != 0) {
                        inputAmount.divide(state.exchangeRate, 8, java.math.RoundingMode.HALF_UP)
                            .stripTrailingZeros().toPlainString()
                    } else state.amount

                    state.copy(
                        destination = state.destination.copy(amount = event.amount),
                        source = state.source.copy(amount = newSourceAmount)
                    )
                }
            }

            is TransactionEntryEvent.SetCategory -> {
                if (!Category.isInitialBalance(event.categoryId)) update {
                    it.copy(source = it.source.copy(categoryId = event.categoryId))
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
                    val isTransferToNormal =
                        oldType == TransactionType.TRANSFER && newType != TransactionType.TRANSFER

                    val primarySide = if (isTransferToNormal && state.isTransferInflow) {
                        state.destination
                    } else {
                        state.source
                    }

                    state.copy(
                        transactionType = newType,
                        source = primarySide.copy(
                            amount = if (primarySide.amount.isEmpty()) "" else primarySide.amountValue.abs()
                                .stripTrailingZeros().toPlainString(),

                            accountId = when {
                                newType == TransactionType.TRANSFER && oldType == TransactionType.INCOME -> null
                                else -> primarySide.accountId
                            },

                            categoryId = when {
                                newType != TransactionType.TRANSFER && Category.isTransfer(state.selectedCategoryId) -> Category.SYSTEM_UNKNOWN
                                newType == TransactionType.TRANSFER && oldType == TransactionType.INCOME -> Category.SYSTEM_TRANSFER_INFLOW
                                newType == TransactionType.TRANSFER && oldType == TransactionType.EXPENSE -> Category.SYSTEM_TRANSFER_OUTFLOW
                                newType == TransactionType.TRANSFER -> state.selectedCategoryId.takeIf {
                                    Category.isTransfer(
                                        it
                                    )
                                } ?: Category.SYSTEM_TRANSFER_OUTFLOW

                                else -> state.selectedCategoryId
                            }
                        ),

                        destination = when {
                            newType == TransactionType.TRANSFER && oldType == TransactionType.INCOME -> primarySide.copy(
                                categoryId = Category.SYSTEM_TRANSFER_INFLOW // Just in case, though destination.categoryId isn't primary
                            )
                            newType == TransactionType.TRANSFER && oldType == TransactionType.EXPENSE -> TransactionSide()
                            newType == TransactionType.TRANSFER -> state.destination
                            else -> TransactionSide()
                        },

                        // Prohibit transfers in split mode
                        isSplitMode = if (newType == TransactionType.TRANSFER) false else state.isSplitMode,
                        showSplitSheet = if (newType == TransactionType.TRANSFER) false else state.showSplitSheet
                    )
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
                        source = it.source.copy(
                            accountId = event.account?.id,
                            currency = event.account?.currencyCode ?: it.selectedCurrency
                        )
                    )
                }
                recalculateExchangeRate()
            }

            is TransactionEntryEvent.SetCurrency -> {
                update { it.copy(source = it.source.copy(currency = event.currency)) }
                recalculateExchangeRate()
            }

            is TransactionEntryEvent.SetTargetAccount -> {
                update {
                    it.copy(
                        destination = it.destination.copy(
                            accountId = event.account?.id,
                            currency = event.account?.currencyCode ?: it.targetCurrency
                        )
                    )
                }
                recalculateExchangeRate()
            }

            is TransactionEntryEvent.SetTargetCurrency -> {
                update { it.copy(destination = it.destination.copy(currency = event.currency)) }
                recalculateExchangeRate()
            }

            is TransactionEntryEvent.SetExchangeRate -> update {
                val inputAmt = it.amountValue
                it.copy(
                    exchangeRate = event.rate,
                    destination = it.destination.copy(
                        amount = inputAmt.multiply(event.rate)
                            .setScale(8, java.math.RoundingMode.HALF_UP).stripTrailingZeros()
                            .toPlainString()
                    )
                )
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

            is TransactionEntryEvent.ShowNumpad -> update {
                it.copy(
                    isNumpadVisible = event.show,
                    activeAmountSide = event.side
                )
            }

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
                update {
                    it.copy(
                        exchangeRate = rate,
                        destination = it.destination.copy(
                            amount = it.amountValue.multiply(rate)
                                .setScale(8, java.math.RoundingMode.HALF_UP).stripTrailingZeros()
                                .toPlainString()
                        )
                    )
                }
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
                currentState.isTransfer -> if (currentState.isTransferInflow) currentState.amountValue.abs() else currentState.amountValue.abs()
                    .negate()

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
                destination = it.destination.copy(
                    accountId = null,
                    currency = ""
                )
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
