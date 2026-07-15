package com.osscabinet.ossWallet.ui.screens.transactions.entry

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.AccountRef
import com.osscabinet.ossWallet.domain.model.AmountEditingSide
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.EntryMode
import com.osscabinet.ossWallet.domain.model.Tag
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionSide
import com.osscabinet.ossWallet.domain.model.TransactionSplit
import com.osscabinet.ossWallet.domain.model.dto.TransactionPrefillDTO
import com.osscabinet.ossWallet.domain.model.dto.TransactionRequest
import com.osscabinet.ossWallet.domain.model.isOutside
import com.osscabinet.ossWallet.domain.model.isTransfer
import com.osscabinet.ossWallet.domain.model.toDbTransferSide
import com.osscabinet.ossWallet.domain.model.toEntryMode
import com.osscabinet.ossWallet.domain.repository.ComputationRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.repository.TagRepository
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.domain.service.currency.ExchangeRateSnapshot
import com.osscabinet.ossWallet.domain.service.management.TransactionEntryService
import com.osscabinet.ossWallet.domain.service.management.TransactionManager
import com.osscabinet.ossWallet.domain.usecase.EntryFormDataUseCase
import com.osscabinet.ossWallet.domain.usecase.TransactionUseCases
import com.osscabinet.ossWallet.ui.util.BaseEntryViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal

/**
 * ViewModel dedicated to transaction entry screen state management.
 * Handles all form state, validation, and transaction operations.
 */
@HiltViewModel
class TransactionEntryViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val transactionUseCases: TransactionUseCases,
    entryFormDataUseCase: EntryFormDataUseCase,
    private val transactionManager: TransactionManager,
    settingsRepository: SettingsRepository,
    computationRepository: ComputationRepository,
    private val tagRepository: TagRepository
) : BaseEntryViewModel(entryFormDataUseCase, computationRepository, settingsRepository) {

    private val _state = MutableStateFlow(TransactionEntryState())
    val state: StateFlow<TransactionEntryState> = _state.asStateFlow()

    private var initialTimestamp: Long = System.currentTimeMillis()
    private var lastInitParams: Any? = null

    // BaseEntryViewModel abstract overrides
    override fun getSourceCurrency() = _state.value.source.currency
    override fun getTargetCurrency() = _state.value.destination.currency
    override fun getIsTransfer() = _state.value.isTransfer
    override fun getIsRateFixed() = _state.value.isRateFixed
    override fun getAmountValue() = _state.value.amountValue
    override fun applyRateUpdate(rate: BigDecimal, targetAmountStr: String) {
        update {
            it.copy(
                exchangeRate = rate,
                destination = it.destination.copy(amount = targetAmountStr)
            )
        }
    }

    init {
        viewModelScope.launch {
            entryFormSnapshot.collect { snapshot ->
                _state.update {
                    it.copy(
                        accounts = snapshot.accounts,
                        categories = snapshot.categories,
                        allTags = snapshot.allTags,
                        templates = snapshot.templates,
                        allPayees = snapshot.allPayees,
                        allNotes = snapshot.allNotes,
                        shortlistedCurrencies = snapshot.shortlistedCurrencies,
                        fullProviderCurrencies = snapshot.fullProviderCurrencies,
                        exchangeRates = snapshot.exchangeRates
                    )
                }
            }
        }
    }

    /**
     * Initialize the ViewModel with entry parameters.
     */
    fun initialize(
        transactionId: Long? = null,
        accountId: String? = null,
        initialType: EntryMode = EntryMode.EXPENSE,
        initialTemplateId: Long? = null,
        prefillDto: TransactionPrefillDTO? = null
    ) {
        val params = listOf(transactionId, accountId, initialType, initialTemplateId, prefillDto)
        if (lastInitParams == params) return
        if (prefillDto == null && (lastInitParams as? List<*>)?.get(4) != null) return
        val isFirstInit = lastInitParams == null
        lastInitParams = params

        viewModelScope.launch {
            initialTimestamp = System.currentTimeMillis()
            val profileId = settingsRepository.currentProfileId.first()
            val baseCurrency = settingsRepository.currency.first()

            // Shared Data Loading
            entryFormDataUseCase.observeEntryCoreData(profileId).first().let { data ->
                val mapsAccess = settingsRepository.mapsAccessEnabled.first()

                val isEditMode = transactionId != null && transactionId != -1L
                val existingTransaction = if (isEditMode) {
                    transactionUseCases.getTransactionById(transactionId, profileId)
                } else null

                val linkedTxs = if (isEditMode) {
                    transactionUseCases.getLinkedTransactions(transactionId).first()
                } else emptyList()

                val selectedAccount = accountId?.let { id -> data.accounts.find { it.id == id } }
                    ?: data.accounts.firstOrNull()

                val defaultCategory =
                    data.categories.find { it.id == Category.SYSTEM_UNKNOWN }
                        ?: data.categories.firstOrNull()

                update {
                    it.copy(
                        transactionId = transactionId,
                        isEditMode = isEditMode,
                        existingTransaction = existingTransaction,
                        linkedTxs = linkedTxs,
                        linkedTransactionIds = linkedTxs.map { it.id },
                        prefillDto = prefillDto,
                        accounts = data.accounts,
                        categories = data.categories,
                        allTags = data.tags,
                        templates = data.templates,
                        allPayees = data.payees,
                        allNotes = data.notes,
                        baseCurrency = baseCurrency,
                        mapsAccessEnabled = mapsAccess,
                        source = TransactionSide(
                            account = selectedAccount?.id?.let { AccountRef.fromStored(it) }
                                ?: AccountRef.Unselected,
                            currency = selectedAccount?.currencyCode ?: "",
                            categoryId = defaultCategory?.id ?: Category.SYSTEM_UNKNOWN
                        ),
                        entryMode = initialType,
                        selectedTemplateName = null,
                        selectedDateMillis = initialTimestamp,
                        shortlistedCurrencies = effectiveShortlistedCurrencies.value,
                        fullProviderCurrencies = CurrencyService.getAllCurrencies(),
                        isNumpadVisible = isFirstInit && !isEditMode && prefillDto == null && initialTemplateId == null
                    )
                }

                if (initialTemplateId != null && initialTemplateId != -1L) {
                    data.templates.find { it.id == initialTemplateId }?.let { template ->
                        applyPrefill(TransactionPrefillDTO.fromTemplate(template))
                    }
                }

                if (isEditMode && existingTransaction != null) {
                    initializeFromTransaction(existingTransaction, data.accounts, linkedTxs)
                } else if (prefillDto != null) {
                    applyPrefill(prefillDto)
                }
            }
        }
    }

    private fun initializeFromTransaction(
        transaction: Transaction,
        accounts: List<Account>,
        linkedTxs: List<Transaction>
    ) {
        val txType = when {
            Category.isTransfer(transaction.categoryId) -> EntryMode.TRANSFER
            else -> transaction.transactionType.toEntryMode()
        }

        val isOpenedAsReceiver =
            transaction.transactionType == com.osscabinet.ossWallet.domain.model.TransactionType.TRANSFER_INFLOW
        var selectedAcc: Account? = accounts.find { it.id == transaction.accountId }
        var targetAcc: Account? = accounts.find { it.id == transaction.targetAccountId }
        var amountStr = "0"
        var targetAmountStr = "0"
        var currentCurrency = transaction.currency
        var targetCurrency = transaction.targetCurrency ?: transaction.currency
        var categoryId = transaction.categoryId

        if (txType == EntryMode.TRANSFER) {
            val linkedTransfer = linkedTxs.find { tx ->
                Category.isTransfer(tx.categoryId) &&
                        tx.transactionType.isTransfer &&
                        tx.id != transaction.id &&
                        kotlin.math.abs(tx.timestamp - transaction.timestamp) < 5000
            }

            if (isOpenedAsReceiver) {
                // Flip for display consistency: Giver on top, Receiver on bottom
                targetAcc = selectedAcc
                selectedAcc = accounts.find { it.id == transaction.targetAccountId }
                currentCurrency = transaction.targetCurrency ?: transaction.currency
                targetCurrency = transaction.currency
                targetAmountStr = transaction.amount.abs().stripTrailingZeros().toPlainString()
                amountStr = linkedTransfer?.amount?.abs()?.stripTrailingZeros()?.toPlainString()
                    ?: TransactionEntryService.calculateSourceAmount(
                        transaction.amount.abs(),
                        transaction.transferRate
                    )
            } else {
                amountStr = transaction.amount.abs().stripTrailingZeros().toPlainString()
                targetAmountStr =
                    linkedTransfer?.amount?.abs()?.stripTrailingZeros()?.toPlainString()
                        ?: TransactionEntryService.calculateTargetAmount(
                            transaction.amount.abs(),
                            transaction.transferRate
                        )
            }
            // Semantic Normalization: Unified category for both sides
            categoryId = if (Category.isInitialBalance(transaction.categoryId))
                Category.SYSTEM_INITIAL_BALANCE else Category.SYSTEM_TRANSFER
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

        // Decode each side using the type-safe AccountRef. For TRANSFER rows, a null/blank
        // id is the persistence convention for "Outside Wallet" (not "Unselected"); for
        // non-transfer rows the source is always a real wallet.
        val sourceRef: AccountRef = if (txType == EntryMode.TRANSFER) {
            selectedAcc?.let { AccountRef.fromStored(it.id) } ?: AccountRef.Outside
        } else {
            selectedAcc?.let { AccountRef.fromStored(it.id) } ?: AccountRef.Unselected
        }
        val destinationRef: AccountRef = if (txType == EntryMode.TRANSFER) {
            targetAcc?.let { AccountRef.fromStored(it.id) } ?: AccountRef.Outside
        } else {
            targetAcc?.let { AccountRef.fromStored(it.id) } ?: AccountRef.Unselected
        }

        _state.update {
            it.copy(
                source = TransactionSide(
                    account = sourceRef,
                    amount = amountStr,
                    currency = currentCurrency,
                    categoryId = categoryId
                ),
                destination = TransactionSide(
                    account = destinationRef,
                    amount = targetAmountStr,
                    currency = targetCurrency
                ),
                selectedTags = transaction.tags,
                paymentMethod = transaction.paymentMethod,
                transactionStatus = transaction.status,
                entryMode = txType,
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
        val txType = prefill.entryMode

        val isOpenedAsReceiver =
            prefill.transactionType == com.osscabinet.ossWallet.domain.model.TransactionType.TRANSFER_INFLOW

        // Use prefill data if available, otherwise fallback to current selection.
        // In TRANSFER mode: null id means Outside Wallet (DB convention) for all prefill sources.
        // In non-TRANSFER mode: null id means "no preference", keep current selection.
        fun decode(prefId: String?, fallback: AccountRef): AccountRef = when {
            txType != EntryMode.TRANSFER -> if (prefId != null) AccountRef.fromStored(prefId) else fallback
            prefId != null -> AccountRef.fromTransferSide(prefId)
            else -> AccountRef.Outside
        }

        var sourceRef = decode(prefill.accountId, currentState.source.account)
        var targetRef = decode(prefill.targetAccountId, currentState.destination.account)
        var sourceAccId: String? = (sourceRef as? AccountRef.Wallet)?.id
        var targetAccId: String? = (targetRef as? AccountRef.Wallet)?.id

        var sourceCurrency =
            prefill.currency.ifEmpty {
                accounts.find { it.id == sourceAccId }?.currencyCode
                    ?: currentState.selectedCurrency
            }
        var targetCurr =
            prefill.targetCurrency ?: accounts.find { it.id == targetAccId }?.currencyCode ?: ""

        var amountBD = prefill.amount.toBigDecimalOrNull()?.abs() ?: BigDecimal.ZERO
        val rateBD = prefill.exchangeRate.toBigDecimalOrNull() ?: BigDecimal.ONE
        var targetAmtBD = amountBD.multiply(rateBD).setScale(8, java.math.RoundingMode.HALF_UP)
            .stripTrailingZeros()
        var categoryId = prefill.categoryId

        if (txType == EntryMode.TRANSFER && isOpenedAsReceiver) {
            // Flip for display consistency
            val originalSourceId = sourceAccId
            sourceAccId = targetAccId
            targetAccId = originalSourceId

            val originalSourceRef = sourceRef
            sourceRef = targetRef
            targetRef = originalSourceRef

            val originalSourceCurr = sourceCurrency
            sourceCurrency = targetCurr
            targetCurr = originalSourceCurr

            // Swap amounts: pre-flip source becomes destination, pre-flip destination becomes source.
            // Capture both before either is overwritten.
            val preFlipSource = amountBD.stripTrailingZeros()
            val preFlipTarget = targetAmtBD.stripTrailingZeros()
            amountBD = preFlipTarget
            targetAmtBD = preFlipSource

            categoryId = if (Category.isInitialBalance(prefill.categoryId))
                Category.SYSTEM_INITIAL_BALANCE else Category.SYSTEM_TRANSFER
        } else if (txType == EntryMode.TRANSFER) {
            categoryId = if (Category.isInitialBalance(prefill.categoryId))
                Category.SYSTEM_INITIAL_BALANCE else Category.SYSTEM_TRANSFER
        }

        // After a receiver flip the stored rate is the inflow inverse; invert it so it reads
        // source→destination from the form's now-normalised perspective.
        val effectiveRate =
            if (txType == EntryMode.TRANSFER && isOpenedAsReceiver && rateBD.signum() != 0)
                BigDecimal.ONE.divide(rateBD, 8, java.math.RoundingMode.HALF_UP)
                    .stripTrailingZeros()
            else rateBD

        _state.update {
            it.copy(
                source = TransactionSide(
                    account = sourceRef,
                    amount = if (prefill.amount.isNotEmpty()) amountBD.stripTrailingZeros()
                        .toPlainString() else it.amount,
                    currency = sourceCurrency,
                    categoryId = categoryId
                ),
                destination = TransactionSide(
                    account = targetRef,
                    amount = if (prefill.amount.isNotEmpty()) targetAmtBD.toPlainString() else it.destination.amount,
                    currency = targetCurr
                ),
                selectedTags = prefill.tags,
                paymentMethod = prefill.paymentMethod,
                transactionStatus = prefill.status,
                entryMode = txType,
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
                exchangeRate = effectiveRate,
                selectedTemplateName = if (prefill.sourceTemplateId != null) prefill.name else it.selectedTemplateName,
                prefillDto = prefill,
                isSplitMode = prefill.isSplitMode,
                showSplitSheet = prefill.isSplitMode,
                splits = emptyList()
            )
        }

        // Recalculate live rate only when no custom rate was provided (rate == 1 is the default).
        // Use numeric comparison: BigDecimal("1").compareTo avoids "1" vs "1.0" string mismatch.
        if (txType == EntryMode.TRANSFER && rateBD.compareTo(BigDecimal.ONE) == 0) {
            recalculateExchangeRate()
        }
    }

    /**
     * Handle state modification events.
     */
    fun onEvent(event: TransactionEntryEvent) {
        when (event) {
            is TransactionEntryEvent.SetName -> update { it.copy(name = event.name) }

            is TransactionEntryEvent.SetAmount -> update { s ->
                s.applyUpdate(
                    TransactionEntryService.handleAmountChange(
                        amountStr = event.amount,
                        currentState = s.toUpdateResult(),
                        exchangeRates = ExchangeRateSnapshot(
                            anchor = s.selectedCurrency,
                            rates = s.exchangeRates.ifEmpty { mapOf(s.targetCurrency to s.exchangeRate) }
                        ),
                        isSplitMode = s.isSplitMode
                    )
                )
            }

            is TransactionEntryEvent.SetTargetAmount -> update { s ->
                s.applyUpdate(
                    TransactionEntryService.handleTargetAmountChange(
                        amountStr = event.amount,
                        currentState = s.toUpdateResult(),
                        exchangeRates = ExchangeRateSnapshot(
                            anchor = s.selectedCurrency,
                            rates = s.exchangeRates.ifEmpty { mapOf(s.targetCurrency to s.exchangeRate) }
                        )
                    )
                )
            }

            is TransactionEntryEvent.SetCategoryId -> {
                if (!Category.isInitialBalance(event.id)) update {
                    it.copy(source = it.source.copy(categoryId = event.id))
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

            is TransactionEntryEvent.SetTransactionStatus -> update { it.copy(transactionStatus = event.status) }
            is TransactionEntryEvent.SetTransactionType -> {
                val oldMode = _state.value.entryMode
                val newMode = event.type

                _state.update { state ->
                    val result =
                        TransactionEntryService.transitionType(
                            oldMode = oldMode,
                            newMode = newMode,
                            currentSource = state.source,
                            currentDestination = state.destination,
                            isSplitMode = state.isSplitMode
                        )

                    state.copy(
                        entryMode = result.newMode,
                        source = result.source,
                        destination = result.destination,
                        isSplitMode = result.isSplitModeAllowed,
                        showSplitSheet = if (result.newMode == EntryMode.TRANSFER) false else state.showSplitSheet,
                        isRateFixed = false
                    )
                }

                // Recalculate exchange rate if switching to/from transfer
                if (newMode == EntryMode.TRANSFER) {
                    recalculateExchangeRate()
                }
            }

            is TransactionEntryEvent.SetDate -> update { it.copy(selectedDateMillis = event.millis) }
            is TransactionEntryEvent.SetPayee -> update { it.copy(payee = event.payee) }
            is TransactionEntryEvent.SetNote -> {
                update { it.copy(note = event.note) }
            }

            is TransactionEntryEvent.SetWarrantyEnabled -> update {
                it.copy(isWarrantyEnabled = event.enabled)
            }

            is TransactionEntryEvent.SetWarrantyExpiration -> update {
                it.copy(warrantyExpirationMillis = event.millis, showWarrantyDatePicker = false)
            }

            is TransactionEntryEvent.SetLocation -> update {
                it.copy(
                    gpsLocation = event.gpsLocation,
                    location = event.location
                )
            }

            is TransactionEntryEvent.AddLinkedTransaction -> {
                viewModelScope.launch {
                    withProfile { profileId ->
                        val tx = transactionUseCases.getTransactionById(event.id, profileId)
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
                    withProfile { profileId ->
                        val txs = event.ids.mapNotNull {
                            transactionUseCases.getTransactionById(
                                it,
                                profileId
                            )
                        }
                        update { it.copy(linkedTransactionIds = event.ids, linkedTxs = txs) }
                    }
                }
            }

            is TransactionEntryEvent.SetAccountId -> {
                update { s ->
                    val result = TransactionEntryService.handleAccountSelection(
                        accountId = event.id,
                        isSource = true,
                        currentState = s.toUpdateResult(),
                        accounts = s.accounts,
                        baseCurrency = s.baseCurrency
                    )
                    s.applyUpdate(result)
                }
                if (!_state.value.isRateFixed) recalculateExchangeRate()
            }

            is TransactionEntryEvent.SetCurrency -> {
                update { s ->
                    val nextTarget = if (s.isTransfer && s.destination.account.isOutside) {
                        event.currency
                    } else s.targetCurrency

                    s.copy(
                        source = s.source.copy(currency = event.currency),
                        destination = s.destination.copy(currency = nextTarget)
                    )
                }
                if (!_state.value.isRateFixed) recalculateExchangeRate()
            }

            is TransactionEntryEvent.SetTargetAccountId -> {
                update { s ->
                    val result = TransactionEntryService.handleAccountSelection(
                        accountId = event.id,
                        isSource = false,
                        currentState = s.toUpdateResult(),
                        accounts = s.accounts,
                        baseCurrency = s.baseCurrency
                    )
                    s.applyUpdate(result)
                }
                if (!_state.value.isRateFixed) recalculateExchangeRate()
            }

            is TransactionEntryEvent.SetTargetCurrency -> {
                update { s ->
                    val nextSource = if (s.isTransfer && s.source.account.isOutside) {
                        event.currency
                    } else s.selectedCurrency

                    s.copy(
                        destination = s.destination.copy(currency = event.currency),
                        source = s.source.copy(currency = nextSource)
                    )
                }
                if (!_state.value.isRateFixed) recalculateExchangeRate()
            }

            is TransactionEntryEvent.SetExchangeRate -> update {
                val sync = TransactionEntryService.syncTransferState(
                    sourceAmount = it.source.amountValue,
                    targetAmount = TransactionEntryService.calculateTargetAmountValue(
                        it.source.amountValue,
                        event.rate
                    ),
                    sourceCurrency = it.source.currency,
                    targetCurrency = it.destination.currency,
                    isFixed = true, // Manually setting rate always fixes it
                    activeSide = AmountEditingSide.SOURCE,
                    exchangeRates = ExchangeRateSnapshot(
                        it.source.currency,
                        it.exchangeRates
                    )
                )

                it.copy(
                    isRateFixed = true,
                    exchangeRate = event.rate,
                    destination = it.destination.copy(
                        amount = sync.targetAmount.stripTrailingZeros().toPlainString()
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
                it.copy(
                    isSplitMode = event.enabled,
                    splits = if (!event.enabled) emptyList() else it.splits
                )
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
            is TransactionEntryEvent.ShowSplitSheet -> update {
                val shouldRevert = !event.show && it.splits.isEmpty()
                it.copy(
                    showSplitSheet = event.show,
                    isSplitMode = if (shouldRevert) false else it.isSplitMode
                )
            }
            is TransactionEntryEvent.ShowSplitCategorySheet -> update {
                it.copy(
                    showSplitCategorySheet = event.splitIndex
                )
            }

            is TransactionEntryEvent.ShowSplitNumpad -> update {
                it.copy(
                    showSplitNumpad = event.splitIndex
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

            is TransactionEntryEvent.DismissAllSheets -> update {
                it.copy(
                    showDatePicker = false,
                    showAccountSheet = false,
                    showTargetAccountSheet = false,
                    showCategorySheet = false,
                    showTemplateSheet = false,
                    showTagSheet = false,
                    showPaymentMethodSheet = false,
                    showStatusSheet = false,
                    showWarrantyDatePicker = false,
                    showLocationSheet = false,
                    showExchangeRateDialog = false,
                    showCurrencySheet = false,
                    showTargetCurrencySheet = false,
                    showLinkedTransactionSheet = false,
                    showSplitSheet = false,
                    showSplitCategorySheet = null,
                    showSplitNumpad = null,
                ).let { s ->
                    if (s.splits.isEmpty()) s.copy(isSplitMode = false) else s
                }
            }

            is TransactionEntryEvent.SwapAccounts -> {
                update { s ->
                    if (!s.isTransfer) return@update s
                    val (newSrc, newDst, newRate) = TransactionEntryService.swapAccounts(
                        source = s.source,
                        destination = s.destination,
                        transferRate = s.exchangeRate
                    )
                    s.copy(source = newSrc, destination = newDst, exchangeRate = newRate)
                }
                if (!_state.value.isRateFixed) recalculateExchangeRate()
            }

            is TransactionEntryEvent.ToggleRateMode -> {
                update { s ->
                    val nextFixed = !s.isRateFixed
                    s.copy(isRateFixed = nextFixed)
                }
                if (!_state.value.isRateFixed) {
                    recalculateExchangeRate()
                }
            }

            // Navigation
            is TransactionEntryEvent.TriggerNavigation -> update { it.copy(navTriggered = true) }
            is TransactionEntryEvent.ClearNavigationTrigger -> update { it.copy(navTriggered = false) }
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

        return TransactionEntryService.createRequest(
            name = currentState.name.ifBlank {
                currentState.selectedCategory?.name ?: "Transaction"
            },
            amount = currentState.amountValue,
            entryMode = currentState.entryMode,
            sourceAccount = currentState.source.account,
            sourceCurrency = currentState.selectedCurrency,
            targetAccount = currentState.destination.account,
            targetCurrency = currentState.targetCurrency,
            categoryId = currentState.selectedCategoryId,
            note = currentState.note,
            payee = currentState.payee,
            tags = currentState.selectedTags,
            status = currentState.transactionStatus,
            targetAmountOverride = if (currentState.isTransfer) currentState.destination.amountValue else null,
            exchangeRates = ExchangeRateSnapshot(
                anchor = "USD",
                rates = currentState.exchangeRates
            ),
            accounts = currentState.accounts,
            baseCurrency = currentState.baseCurrency,
            timestamp = finalTimestamp
        ).copy(
            gpsLocation = currentState.gpsLocation,
            location = currentState.location,
            warrantyExpiration = if (currentState.isWarrantyEnabled) currentState.warrantyExpirationMillis else null,
            paymentMethod = currentState.paymentMethod,
        )
    }

    /**
     * Save the transaction (create new or update existing).
     */
    fun saveTransaction(
        onSuccess: (transactionId: Long) -> Unit = {}
    ) {
        if (_state.value.isSaving) return
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                val currentState = _state.value
                if (!currentState.canSave) return@launch

                if (currentState.isEditMode && currentState.existingTransaction == null) {
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
                        scheduledDate = currentState.prefillDto?.plannedScheduledDate
                            ?: currentState.selectedDateMillis
                    )
                    onSuccess(id)
                }
            } finally {
                _state.update { it.copy(isSaving = false) }
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
            name = currentState.name.ifBlank {
                currentState.selectedCategory?.name ?: "Transaction"
            },
            amount = BigDecimal.ZERO, // To be set by caller
            entryMode = when {
                currentState.isIncome -> EntryMode.INCOME
                currentState.isTransfer -> EntryMode.TRANSFER
                else -> EntryMode.EXPENSE
            },
            accountId = currentState.source.account.toDbTransferSide(),
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
            status = currentState.transactionStatus,
            sourcePlannedPaymentId = currentState.prefillDto?.sourcePlannedPaymentId,
            targetAccountId = currentState.destination.account.toDbTransferSide(),
            targetCurrency = currentState.targetCurrency,
            transferRate = if (currentState.isTransfer) currentState.exchangeRate else BigDecimal.ONE
        )
    }

    /**
     * Delete the current transaction.
     */
    fun deleteTransaction(onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            _state.value.transactionId?.let { id ->
                withProfile { profileId ->
                    transactionManager.deleteTransaction(id, profileId)
                }
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
        update { it.copy(prefillDto = null) }
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
