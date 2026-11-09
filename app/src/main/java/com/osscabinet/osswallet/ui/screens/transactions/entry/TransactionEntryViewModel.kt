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
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.dto.TransactionPrefillDTO
import com.osscabinet.ossWallet.domain.model.dto.TransactionRequest
import com.osscabinet.ossWallet.domain.model.isOutside
import com.osscabinet.ossWallet.domain.model.isTransfer
import com.osscabinet.ossWallet.domain.model.toDbTransferSide
import com.osscabinet.ossWallet.domain.model.toEntryMode
import com.osscabinet.ossWallet.domain.repository.AttachmentProcessor
import com.osscabinet.ossWallet.domain.repository.AttachmentRepository
import com.osscabinet.ossWallet.domain.repository.ComputationRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.repository.TagRepository
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.domain.service.currency.ExchangeRateSnapshot
import com.osscabinet.ossWallet.domain.service.management.DuplicateInitialBalanceException
import com.osscabinet.ossWallet.domain.service.management.TransactionEntryService
import com.osscabinet.ossWallet.domain.service.management.TransactionManager
import com.osscabinet.ossWallet.domain.service.management.attachmentTargets
import com.osscabinet.ossWallet.domain.usecase.EntryFormDataUseCase
import com.osscabinet.ossWallet.domain.usecase.TransactionUseCases
import com.osscabinet.ossWallet.ui.util.BaseEntryViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs

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
    private val tagRepository: TagRepository,
    private val attachmentRepository: AttachmentRepository,
    attachmentProcessor: AttachmentProcessor
) : BaseEntryViewModel(entryFormDataUseCase, computationRepository, settingsRepository) {

    private val _state = MutableStateFlow(TransactionEntryState())
    val state: StateFlow<TransactionEntryState> = _state.asStateFlow()

    private val attachmentStaging = AttachmentStaging(attachmentRepository, attachmentProcessor, viewModelScope)
    val attachments: StateFlow<AttachmentsUiState> = attachmentStaging.state

    private var initialTimestamp: Long = System.currentTimeMillis()
    private var lastInitParams: InitParams? = null

    private data class InitParams(
        val transactionId: Long?,
        val accountId: String?,
        val initialType: EntryMode,
        val initialTemplateId: Long?,
        val prefillDto: TransactionPrefillDTO?
    )

    init {
        viewModelScope.launch {
            _state.map { it.transactionId }.distinctUntilChanged().collect { id ->
                attachmentStaging.load(id, settingsRepository.currentProfileId.first())
            }
        }
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

    // region BaseEntryViewModel hooks

    override fun getSourceCurrency() = _state.value.source.currency
    override fun getTargetCurrency() = _state.value.destination.currency
    override fun getIsTransfer() = _state.value.isTransfer
    override fun getIsRateFixed() = _state.value.isRateFixed
    override fun getAmountValue() = _state.value.amountValue
    override fun applyRateUpdate(rate: BigDecimal, targetAmountStr: String) {
        update { it.copy(exchangeRate = rate, destination = it.destination.copy(amount = targetAmountStr)) }
    }

    // endregion

    // region Attachments

    fun onAttachmentPicked(source: String) = attachmentStaging.add(source)
    fun removeAttachment(item: AttachmentItem) = attachmentStaging.remove(item)
    fun clearAttachmentError() = attachmentStaging.clearError()
    suspend fun readAttachment(item: AttachmentItem): ByteArray? =
        item.pending?.bytes ?: item.attachment?.let { attachmentRepository.readBytes(it) }

    // endregion

    // region Initialisation

    /** Initialize the ViewModel with entry parameters. */
    fun initialize(
        transactionId: Long? = null,
        accountId: String? = null,
        initialType: EntryMode = EntryMode.EXPENSE,
        initialTemplateId: Long? = null,
        prefillDto: TransactionPrefillDTO? = null
    ) {
        val params = InitParams(transactionId, accountId, initialType, initialTemplateId, prefillDto)
        val previous = lastInitParams
        if (previous == params) return
        if (prefillDto == null && previous?.prefillDto != null) return
        lastInitParams = params

        viewModelScope.launch {
            initialTimestamp = System.currentTimeMillis()
            val profileId = settingsRepository.currentProfileId.first()
            val data = entryFormDataUseCase.observeEntryCoreData(profileId).first()

            val isEditMode = transactionId != null && transactionId != NO_TRANSACTION_ID
            val existingTransaction = if (isEditMode) {
                transactionUseCases.getTransactionById(transactionId, profileId)
            } else null
            val linkedTxs = if (isEditMode) {
                transactionUseCases.getLinkedTransactions(transactionId).first()
            } else emptyList()

            val selectedAccount = accountId?.let { id -> data.accounts.find { it.id == id } }
                ?: data.accounts.firstOrNull()
            val defaultCategoryId = (data.categories.find { it.id == Category.SYSTEM_UNKNOWN }
                ?: data.categories.firstOrNull())?.id ?: Category.SYSTEM_UNKNOWN
            val baseCurrency = settingsRepository.currency.first()
            val mapsAccessEnabled = settingsRepository.mapsAccessEnabled.first()

            update {
                it.copy(
                    transactionId = transactionId,
                    isEditMode = isEditMode,
                    existingTransaction = existingTransaction,
                    linkedTxs = linkedTxs,
                    linkedTransactionIds = linkedTxs.map { tx -> tx.id },
                    prefillDto = prefillDto,
                    accounts = data.accounts,
                    categories = data.categories,
                    allTags = data.tags,
                    templates = data.templates,
                    allPayees = data.payees,
                    allNotes = data.notes,
                    baseCurrency = baseCurrency,
                    mapsAccessEnabled = mapsAccessEnabled,
                    source = TransactionSide(
                        account = selectedAccount?.id?.let { id -> AccountRef.fromStored(id) } ?: AccountRef.Unselected,
                        currency = selectedAccount?.currencyCode ?: "",
                        categoryId = if (initialType == EntryMode.TRANSFER) Category.SYSTEM_TRANSFER else defaultCategoryId
                    ),
                    entryMode = initialType,
                    selectedTemplateName = null,
                    selectedDateMillis = initialTimestamp,
                    shortlistedCurrencies = effectiveShortlistedCurrencies.value,
                    fullProviderCurrencies = CurrencyService.getAllCurrencies(),
                    isNumpadVisible = previous == null && !isEditMode && prefillDto == null && initialTemplateId == null
                )
            }

            if (initialTemplateId != null && initialTemplateId != NO_TRANSACTION_ID) {
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

    private fun initializeFromTransaction(
        transaction: Transaction,
        accounts: List<Account>,
        linkedTxs: List<Transaction>
    ) {
        // Opening balances and balance adjustments are themselves TRANSFER_INFLOW/OUTFLOW rows
        // (single-leg, against Outside Wallet), so they open as EntryMode.TRANSFER too.
        val mode = if (transaction.transactionType.isTransfer) {
            EntryMode.TRANSFER
        } else {
            transaction.transactionType.toEntryMode()
        }
        val (source, destination) = if (mode == EntryMode.TRANSFER) {
            transferSides(transaction, accounts, linkedTxs)
        } else {
            plainSides(transaction, accounts)
        }

        _state.update {
            it.copy(
                source = source,
                destination = destination,
                selectedTags = transaction.tags,
                paymentMethod = transaction.paymentMethod,
                transactionStatus = transaction.status,
                entryMode = mode,
                selectedDateMillis = transaction.timestamp,
                payee = transaction.payee,
                note = transaction.note,
                warrantyExpirationMillis = transaction.warrantyExpiration ?: defaultWarrantyExpiration(),
                isWarrantyEnabled = transaction.warrantyExpiration != null,
                gpsLocation = transaction.gpsLocation,
                location = transaction.location,
                exchangeRate = impliedRate(source, destination, fallback = transaction.transferRate)
            )
        }
    }

    private fun plainSides(transaction: Transaction, accounts: List<Account>) = Pair(
        TransactionSide(
            account = walletRef(accounts, transaction.accountId, fallback = AccountRef.Unselected),
            amount = magnitude(transaction.amount),
            currency = transaction.currency,
            categoryId = transaction.categoryId
        ),
        TransactionSide(
            account = walletRef(accounts, transaction.targetAccountId, fallback = AccountRef.Unselected),
            amount = "0",
            currency = transaction.targetCurrency ?: transaction.currency
        )
    )

    /**
     * Sides for a transfer row. A null/blank account id is the persistence convention for
     * "Outside Wallet". An inflow row is flipped so the giver is always on top for display, and the
     * counterpart's amount comes from its linked row when there is one.
     */
    private fun transferSides(
        transaction: Transaction,
        accounts: List<Account>,
        linkedTxs: List<Transaction>
    ): Pair<TransactionSide, TransactionSide> {
        val counterpartAmount = linkedTxs.find { tx ->
            Category.isTransfer(tx.categoryId) &&
                tx.transactionType.isTransfer &&
                tx.id != transaction.id &&
                abs(tx.timestamp - transaction.timestamp) < LINKED_LEG_WINDOW_MILLIS
        }?.let { magnitude(it.amount) }

        // An ordinary transfer has a single category; an opening balance/adjustment keeps its own.
        val categoryId = if (Category.isSystemManaged(transaction.categoryId)) {
            transaction.categoryId
        } else {
            Category.SYSTEM_TRANSFER
        }
        val ownSide = TransactionSide(
            account = walletRef(accounts, transaction.accountId, fallback = AccountRef.Outside),
            amount = magnitude(transaction.amount),
            currency = transaction.currency
        )
        val otherAccount = walletRef(accounts, transaction.targetAccountId, fallback = AccountRef.Outside)
        val otherCurrency = transaction.targetCurrency ?: transaction.currency
        val absAmount = transaction.amount.abs()

        return if (transaction.transactionType == TransactionType.TRANSFER_INFLOW) {
            Pair(
                TransactionSide(
                    account = otherAccount,
                    amount = counterpartAmount
                        ?: TransactionEntryService.calculateSourceAmount(absAmount, transaction.transferRate),
                    currency = otherCurrency,
                    categoryId = categoryId
                ),
                ownSide
            )
        } else {
            Pair(
                ownSide.copy(categoryId = categoryId),
                TransactionSide(
                    account = otherAccount,
                    amount = counterpartAmount
                        ?: TransactionEntryService.calculateTargetAmount(absAmount, transaction.transferRate),
                    currency = otherCurrency
                )
            )
        }
    }

    private fun walletRef(accounts: List<Account>, id: String?, fallback: AccountRef): AccountRef =
        accounts.find { it.id == id }?.let { AccountRef.fromStored(it.id) } ?: fallback

    private fun magnitude(amount: BigDecimal): String = amount.abs().stripTrailingZeros().toPlainString()

    private fun impliedRate(source: TransactionSide, destination: TransactionSide, fallback: BigDecimal): BigDecimal {
        val sourceAmount = source.amount.toBigDecimalOrNull()
        val destinationAmount = destination.amount.toBigDecimalOrNull()
        return if (sourceAmount != null && sourceAmount.signum() != 0 && destinationAmount != null) {
            destinationAmount.divide(sourceAmount, RATE_SCALE, RoundingMode.HALF_UP).stripTrailingZeros()
        } else fallback
    }

    private fun applyPrefill(prefill: TransactionPrefillDTO) {
        val current = _state.value
        val accounts = current.accounts
        val mode = prefill.entryMode
        val isTransfer = mode == EntryMode.TRANSFER
        val isReceiver = isTransfer && prefill.transactionType == TransactionType.TRANSFER_INFLOW

        // In TRANSFER mode a null id means Outside Wallet (DB convention) for every prefill source;
        // otherwise null means "no preference" and the current selection stays.
        fun decode(prefillId: String?, fallback: AccountRef): AccountRef = when {
            !isTransfer -> prefillId?.let { AccountRef.fromStored(it) } ?: fallback
            prefillId != null -> AccountRef.fromTransferSide(prefillId)
            else -> AccountRef.Outside
        }

        val sourceRef = decode(prefill.accountId, current.source.account)
        val targetRef = decode(prefill.targetAccountId, current.destination.account)
        val sourceCurrency = prefill.currency.ifEmpty {
            accounts.find { it.id == (sourceRef as? AccountRef.Wallet)?.id }?.currencyCode ?: current.selectedCurrency
        }
        val targetCurrency = prefill.targetCurrency
            ?: accounts.find { it.id == (targetRef as? AccountRef.Wallet)?.id }?.currencyCode
            ?: ""

        val rate = prefill.exchangeRate.toBigDecimalOrNull() ?: BigDecimal.ONE
        val sourceAmount = prefill.amount.toBigDecimalOrNull()?.abs() ?: BigDecimal.ZERO
        val targetAmount = sourceAmount.multiply(rate).setScale(RATE_SCALE, RoundingMode.HALF_UP)

        // An inflow is flipped so the form reads source -> destination: sides, currencies and
        // amounts swap, and the stored (inflow) rate is inverted.
        val source: TransactionSide
        val destination: TransactionSide
        val effectiveRate: BigDecimal
        if (isReceiver) {
            source = TransactionSide(targetRef, plain(targetAmount), targetCurrency, Category.SYSTEM_TRANSFER)
            destination = TransactionSide(sourceRef, plain(sourceAmount), sourceCurrency)
            effectiveRate = if (rate.signum() != 0) {
                BigDecimal.ONE.divide(rate, RATE_SCALE, RoundingMode.HALF_UP).stripTrailingZeros()
            } else rate
        } else {
            source = TransactionSide(
                sourceRef, plain(sourceAmount), sourceCurrency,
                if (isTransfer) Category.SYSTEM_TRANSFER else prefill.categoryId
            )
            destination = TransactionSide(targetRef, plain(targetAmount), targetCurrency)
            effectiveRate = rate
        }
        val hasAmount = prefill.amount.isNotEmpty()

        _state.update {
            it.copy(
                source = source.copy(amount = if (hasAmount) source.amount else it.amount),
                destination = destination.copy(amount = if (hasAmount) destination.amount else it.destination.amount),
                selectedTags = prefill.tags,
                paymentMethod = prefill.paymentMethod,
                transactionStatus = prefill.status,
                entryMode = mode,
                payee = prefill.payee,
                note = prefill.note,
                warrantyExpirationMillis = prefill.warrantyExpiration ?: defaultWarrantyExpiration(),
                isWarrantyEnabled = prefill.warrantyExpiration != null,
                gpsLocation = prefill.gpsLocation,
                location = prefill.location,
                linkedTransactionIds = prefill.clonedFromTransactionId?.let { id -> listOf(id) }
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

        // Only recalculate the live rate when no custom rate was provided (1 is the default).
        // compareTo avoids the "1" vs "1.0" scale mismatch.
        if (isTransfer && rate.compareTo(BigDecimal.ONE) == 0) recalculateExchangeRate()
    }

    private fun plain(amount: BigDecimal): String = amount.stripTrailingZeros().toPlainString()

    private fun defaultWarrantyExpiration(): Long = System.currentTimeMillis() + ONE_YEAR_MILLIS

    // endregion

    // region Events

    /** Handle state modification events. */
    fun onEvent(event: TransactionEntryEvent) {
        when (event) {
            is TransactionEntryEvent.SetName -> update { it.copy(name = event.name) }
            is TransactionEntryEvent.SetAmount -> update { s ->
                s.applyUpdate(
                    TransactionEntryService.handleAmountChange(
                        amountStr = event.amount,
                        currentState = s.toUpdateResult(),
                        exchangeRates = s.rateSnapshot(),
                        isSplitMode = s.isSplitMode
                    )
                )
            }

            is TransactionEntryEvent.SetTargetAmount -> update { s ->
                s.applyUpdate(
                    TransactionEntryService.handleTargetAmountChange(
                        amountStr = event.amount,
                        currentState = s.toUpdateResult(),
                        exchangeRates = s.rateSnapshot()
                    )
                )
            }

            // Transfer, Initial Balance and Adjustment are transfer categories: picking one from the
            // Income/Expense list switches the form to a (single-leg) transfer.
            is TransactionEntryEvent.SetCategoryId ->
                if (Category.isTransfer(event.id)) selectTransferCategory(event.id)
                else update { it.copy(source = it.source.copy(categoryId = event.id)) }

            is TransactionEntryEvent.ConfirmReplaceInitialBalance -> update {
                it.copy(replaceInitialBalanceId = it.initialBalanceConflict?.id, initialBalanceConflict = null)
            }

            is TransactionEntryEvent.DeclineReplaceInitialBalance -> update {
                it.withCategory(Category.SYSTEM_TRANSFER).withoutInitialBalanceConflict()
            }

            is TransactionEntryEvent.SetTags -> update { it.copy(selectedTags = event.tags) }
            is TransactionEntryEvent.AddTag -> {
                update { it.copy(selectedTags = it.selectedTags + event.tag) }
                addTag(event.tag, event.color)
            }

            is TransactionEntryEvent.DeleteTag -> {
                update { it.copy(selectedTags = it.selectedTags - event.tag.name) }
                viewModelScope.launch { tagRepository.deleteTagAndRemoveFromTransactions(event.tag) }
            }

            is TransactionEntryEvent.RemoveTag -> update { it.copy(selectedTags = it.selectedTags - event.tag) }
            is TransactionEntryEvent.SetPaymentMethod -> update { it.copy(paymentMethod = event.method) }
            is TransactionEntryEvent.SetTransactionStatus -> update { it.copy(transactionStatus = event.status) }
            is TransactionEntryEvent.SetTransactionType -> transitionTo(event.type)
            is TransactionEntryEvent.SetDate -> update { it.copy(selectedDateMillis = event.millis) }
            is TransactionEntryEvent.SetPayee -> update { it.copy(payee = event.payee) }
            is TransactionEntryEvent.SetNote -> update { it.copy(note = event.note) }
            is TransactionEntryEvent.SetWarrantyEnabled -> update { it.copy(isWarrantyEnabled = event.enabled) }
            is TransactionEntryEvent.SetWarrantyExpiration -> update {
                it.copy(warrantyExpirationMillis = event.millis, showWarrantyDatePicker = false)
            }

            is TransactionEntryEvent.SetLocation -> update {
                it.copy(gpsLocation = event.gpsLocation, location = event.location)
            }

            is TransactionEntryEvent.AddLinkedTransaction -> addLinkedTransaction(event.id)
            // The database is updated on save; this only edits the local selection.
            is TransactionEntryEvent.RemoveLinkedTransaction -> update {
                it.copy(
                    linkedTransactionIds = it.linkedTransactionIds.filter { id -> id != event.linkedTransactionId },
                    linkedTxs = it.linkedTxs.filter { tx -> tx.id != event.linkedTransactionId }
                )
            }

            is TransactionEntryEvent.SetLinkedTransactionIds -> setLinkedTransactions(event.ids)

            is TransactionEntryEvent.SetAccountId -> selectAccount(event.id, isSource = true)
            is TransactionEntryEvent.SetTargetAccountId -> selectAccount(event.id, isSource = false)
            is TransactionEntryEvent.SetCurrency -> {
                update { s ->
                    val nextTarget = if (s.isTransfer && s.destination.account.isOutside) event.currency else s.targetCurrency
                    s.copy(
                        source = s.source.copy(currency = event.currency),
                        destination = s.destination.copy(currency = nextTarget)
                    )
                }
                recalculateUnlessRateFixed()
            }

            is TransactionEntryEvent.SetTargetCurrency -> {
                update { s ->
                    val nextSource = if (s.isTransfer && s.source.account.isOutside) event.currency else s.selectedCurrency
                    s.copy(
                        destination = s.destination.copy(currency = event.currency),
                        source = s.source.copy(currency = nextSource)
                    )
                }
                recalculateUnlessRateFixed()
            }

            is TransactionEntryEvent.SetExchangeRate -> update { it.withManualRate(event.rate) }
            is TransactionEntryEvent.SetTemplate -> update { it.copy(selectedTemplateName = event.templateName) }
            is TransactionEntryEvent.ApplyTemplate -> applyPrefill(TransactionPrefillDTO.fromTemplate(event.template))
            is TransactionEntryEvent.ApplyPrefill -> applyPrefill(event.prefill)

            is TransactionEntryEvent.ToggleSplitMode -> update {
                it.copy(isSplitMode = event.enabled, splits = if (event.enabled) it.splits else emptyList())
            }

            is TransactionEntryEvent.AddSplit -> update {
                it.copy(splits = it.splits + TransactionSplit(categoryId = it.selectedCategoryId))
            }

            is TransactionEntryEvent.RemoveSplit -> update { it.withSplitRemoved(event.index) }
            is TransactionEntryEvent.UpdateSplit -> update { s ->
                s.copy(
                    splits = s.splits.mapIndexed { index, split ->
                        if (index != event.index) split
                        else split.copy(
                            categoryId = event.categoryId ?: split.categoryId,
                            amount = event.amount ?: split.amount,
                            note = event.note ?: split.note
                        )
                    }
                )
            }

            is TransactionEntryEvent.ShowDatePicker -> update { it.copy(showDatePicker = event.show) }
            is TransactionEntryEvent.ShowCategorySheet -> update { it.copy(showCategorySheet = event.show) }
            is TransactionEntryEvent.ShowAccountSheet -> update { it.copy(showAccountSheet = event.show) }
            is TransactionEntryEvent.ShowTargetAccountSheet -> update { it.copy(showTargetAccountSheet = event.show) }
            is TransactionEntryEvent.ShowExchangeRateDialog -> update { it.copy(showExchangeRateDialog = event.show) }
            is TransactionEntryEvent.ShowTemplateSheet -> update { it.copy(showTemplateSheet = event.show) }
            is TransactionEntryEvent.ShowLinkedTransactionSheet ->
                update { it.copy(showLinkedTransactionSheet = event.show) }
            is TransactionEntryEvent.ShowTagSheet -> update { it.copy(showTagSheet = event.show) }
            is TransactionEntryEvent.ShowPaymentMethodSheet -> update { it.copy(showPaymentMethodSheet = event.show) }
            is TransactionEntryEvent.ShowStatusSheet -> update { it.copy(showStatusSheet = event.show) }
            is TransactionEntryEvent.ShowWarrantyDatePicker -> update { it.copy(showWarrantyDatePicker = event.show) }
            is TransactionEntryEvent.ShowLocationSheet -> update { it.copy(showLocationSheet = event.show) }
            is TransactionEntryEvent.ShowSplitSheet -> update {
                // Closing the sheet with no splits leaves split mode.
                it.copy(
                    showSplitSheet = event.show,
                    isSplitMode = if (!event.show && it.splits.isEmpty()) false else it.isSplitMode
                )
            }
            is TransactionEntryEvent.ShowSplitCategorySheet -> update { it.copy(showSplitCategorySheet = event.splitIndex) }
            is TransactionEntryEvent.ShowSplitNumpad -> update { it.copy(showSplitNumpad = event.splitIndex) }
            is TransactionEntryEvent.ShowNumpad -> update {
                it.copy(isNumpadVisible = event.show, activeAmountSide = event.side)
            }
            is TransactionEntryEvent.ShowCurrencySheet -> update { it.copy(showCurrencySheet = event.show) }
            is TransactionEntryEvent.ShowTargetCurrencySheet ->
                update { it.copy(showTargetCurrencySheet = event.show) }
            is TransactionEntryEvent.DismissAllSheets -> update { it.withAllSheetsDismissed() }

            is TransactionEntryEvent.SwapAccounts -> {
                update { s ->
                    if (!s.isTransfer) return@update s
                    val (newSource, newDestination, newRate) = TransactionEntryService.swapAccounts(
                        source = s.source,
                        destination = s.destination,
                        transferRate = s.exchangeRate
                    )
                    s.copy(source = newSource, destination = newDestination, exchangeRate = newRate)
                }
                recalculateUnlessRateFixed()
            }

            is TransactionEntryEvent.ToggleRateMode -> {
                update { it.copy(isRateFixed = !it.isRateFixed) }
                recalculateUnlessRateFixed()
            }

            is TransactionEntryEvent.TriggerNavigation -> update { it.copy(navTriggered = true) }
            is TransactionEntryEvent.ClearNavigationTrigger -> update { it.copy(navTriggered = false) }
        }
    }

    private fun TransactionEntryState.rateSnapshot() = ExchangeRateSnapshot(
        anchor = selectedCurrency,
        rates = exchangeRates.ifEmpty { mapOf(targetCurrency to exchangeRate) }
    )

    // Setting a rate by hand always pins it.
    private fun TransactionEntryState.withManualRate(rate: BigDecimal): TransactionEntryState {
        val sync = TransactionEntryService.syncTransferState(
            sourceAmount = source.amountValue,
            targetAmount = TransactionEntryService.calculateTargetAmountValue(source.amountValue, rate),
            sourceCurrency = source.currency,
            targetCurrency = destination.currency,
            isFixed = true,
            activeSide = AmountEditingSide.SOURCE,
            exchangeRates = ExchangeRateSnapshot(source.currency, exchangeRates)
        )
        return copy(
            isRateFixed = true,
            exchangeRate = rate,
            destination = destination.copy(amount = sync.targetAmount.stripTrailingZeros().toPlainString())
        )
    }

    // One split left is no split at all.
    private fun TransactionEntryState.withSplitRemoved(index: Int): TransactionEntryState {
        val remaining = splits.filterIndexed { i, _ -> i != index }
        return if (remaining.size <= 1) {
            copy(isSplitMode = false, showSplitSheet = false, splits = emptyList())
        } else {
            copy(splits = remaining)
        }
    }

    private fun TransactionEntryState.withAllSheetsDismissed() = copy(
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
        showSplitNumpad = null
    ).let { if (it.splits.isEmpty()) it.copy(isSplitMode = false) else it }

    private fun selectAccount(accountId: String?, isSource: Boolean) {
        update { s ->
            s.applyUpdate(
                TransactionEntryService.handleAccountSelection(
                    accountId = accountId,
                    isSource = isSource,
                    currentState = s.toUpdateResult(),
                    accounts = s.accounts,
                    baseCurrency = s.baseCurrency
                )
            ).withTransferCategoryForShape()
        }
        recalculateUnlessRateFixed()
    }

    private fun recalculateUnlessRateFixed() {
        if (!_state.value.isRateFixed) recalculateExchangeRate()
    }

    private fun addLinkedTransaction(id: Long) {
        viewModelScope.launch {
            withProfile { profileId ->
                transactionUseCases.getTransactionById(id, profileId)?.let { linked ->
                    update {
                        it.copy(
                            linkedTransactionIds = it.linkedTransactionIds + id,
                            linkedTxs = (it.linkedTxs + linked).distinctBy { tx -> tx.id }
                        )
                    }
                }
            }
        }
    }

    private fun setLinkedTransactions(ids: List<Long>) {
        viewModelScope.launch {
            withProfile { profileId ->
                val linked = ids.mapNotNull { transactionUseCases.getTransactionById(it, profileId) }
                update { it.copy(linkedTransactionIds = ids, linkedTxs = linked) }
            }
        }
    }

    // endregion

    // region Save and delete

    /** Build a TransactionRequest from current state. */
    fun buildTransactionRequest(): TransactionRequest? {
        val s = _state.value
        if (!s.canSave) return null

        return TransactionEntryService.createRequest(
            name = requestName(s),
            amount = s.amountValue,
            entryMode = s.entryMode,
            sourceAccount = s.source.account,
            sourceCurrency = s.selectedCurrency,
            targetAccount = s.destination.account,
            targetCurrency = s.targetCurrency,
            categoryId = s.selectedCategoryId,
            note = s.note,
            payee = s.payee,
            tags = s.selectedTags,
            status = s.transactionStatus,
            targetAmountOverride = if (s.isTransfer) s.destination.amountValue else null,
            exchangeRates = ExchangeRateSnapshot(anchor = "USD", rates = s.exchangeRates),
            accounts = s.accounts,
            baseCurrency = s.baseCurrency,
            timestamp = requestTimestamp(s)
        ).copy(
            gpsLocation = s.gpsLocation,
            location = s.location,
            warrantyExpiration = if (s.isWarrantyEnabled) s.warrantyExpirationMillis else null,
            paymentMethod = s.paymentMethod
        )
    }

    /** Save the transaction (create new or update existing). */
    fun saveTransaction(onSuccess: (transactionId: Long) -> Unit = {}) {
        if (_state.value.isSaving) return
        update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                val s = _state.value
                if (!s.canSave || (s.isEditMode && s.existingTransaction == null)) return@launch

                val profileId = settingsRepository.currentProfileId.first()
                val id = (if (s.isSplitMode) saveSplit(s, profileId) else saveSingle(s, profileId))
                    ?: return@launch
                attachmentStaging.commit(attachmentTargets(id, profileId), profileId)
                onSuccess(id)
            } catch (e: DuplicateInitialBalanceException) {
                // Lost a race with another writer between the pre-check and the save.
                update { it.copy(initialBalanceConflict = e.existing) }
            } finally {
                update { it.copy(isSaving = false) }
            }
        }
    }

    private suspend fun saveSplit(s: TransactionEntryState, profileId: Int): Long =
        transactionManager.handleSaveSplitTransactions(
            existingTransactionId = if (s.isEditMode) s.transactionId else null,
            splits = s.splits,
            baseRequest = buildBaseRequest(),
            profileId = profileId,
            categoryNameLookup = { categoryId -> s.categories.find { it.id == categoryId }?.name ?: DEFAULT_NAME }
        )

    /** Returns null when the save is held back for the user to resolve an opening-balance conflict. */
    private suspend fun saveSingle(s: TransactionEntryState, profileId: Int): Long? {
        val request = buildTransactionRequest() ?: return null
        val takenInitialBalance = findTakenInitialBalance(s)
        if (takenInitialBalance != null && s.replaceInitialBalanceId != takenInitialBalance.id) {
            update { it.copy(initialBalanceConflict = takenInitialBalance) }
            return null
        }
        return transactionManager.handleSaveTransaction(
            existingTransaction = if (s.isEditMode) s.existingTransaction else null,
            request = request,
            profileId = profileId,
            linkedTransactionIds = s.linkedTransactionIds,
            clonedFromId = if (s.isEditMode) null else s.prefillDto?.clonedFromTransactionId,
            sourcePlannedPaymentId = s.prefillDto?.sourcePlannedPaymentId,
            scheduledDate = s.prefillDto?.plannedScheduledDate ?: s.selectedDateMillis,
            replaceExistingInitialBalance = takenInitialBalance != null
        )
    }

    // The fields shared by every split row; the caller sets each row's amount.
    private fun buildBaseRequest(): TransactionRequest {
        val s = _state.value
        return TransactionRequest(
            name = requestName(s),
            amount = BigDecimal.ZERO,
            entryMode = when {
                s.isIncome -> EntryMode.INCOME
                s.isTransfer -> EntryMode.TRANSFER
                else -> EntryMode.EXPENSE
            },
            accountId = s.source.account.toDbTransferSide(),
            currency = s.selectedCurrency,
            categoryId = s.selectedCategoryId,
            tags = s.selectedTags,
            paymentMethod = s.paymentMethod,
            timestamp = requestTimestamp(s),
            payee = s.payee,
            note = s.note,
            gpsLocation = s.gpsLocation,
            location = s.location,
            warrantyExpiration = if (s.isWarrantyEnabled) s.warrantyExpirationMillis else null,
            status = s.transactionStatus,
            sourcePlannedPaymentId = s.prefillDto?.sourcePlannedPaymentId,
            targetAccountId = s.destination.account.toDbTransferSide(),
            targetCurrency = s.targetCurrency,
            transferRate = if (s.isTransfer) s.exchangeRate else BigDecimal.ONE
        )
    }

    private fun requestName(s: TransactionEntryState) = s.name.ifBlank { s.selectedCategory?.name ?: DEFAULT_NAME }

    // A new entry whose date was never touched is stamped at save time, not at screen-open time.
    private fun requestTimestamp(s: TransactionEntryState) =
        if (!s.isEditMode && s.selectedDateMillis == initialTimestamp) System.currentTimeMillis() else s.selectedDateMillis

    /** Delete the current transaction. */
    fun deleteTransaction(onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            _state.value.transactionId?.let { id ->
                withProfile { profileId ->
                    transactionManager.deleteTransaction(id, profileId)
                    runCatching { attachmentRepository.deleteForTransaction(id, profileId) }
                }
                onDeleted()
            }
        }
    }

    // endregion

    // region Misc

    /** Add a new tag to the repository. */
    fun addTag(name: String, color: Int? = null) {
        viewModelScope.launch { tagRepository.insertTag(Tag(name = name, colorHex = color ?: -1)) }
    }

    fun clearPrefillDto() = update { it.copy(prefillDto = null) }

    /** Reset navigation trigger on resume. */
    fun onScreenResume() = onEvent(TransactionEntryEvent.ClearNavigationTrigger)

    /** Search transactions for linking. */
    fun searchTransactions(query: String): Flow<List<Transaction>> = flow {
        val profileId = settingsRepository.currentProfileId.first()
        emitAll(transactionUseCases.searchTransactions(profileId, query))
    }

    private suspend fun attachmentTargets(id: Long, profileId: Int) = transactionUseCases.attachmentTargets(id, profileId)

    // endregion

    // region Entry mode and transfer categories

    /** Moves the form to [newMode], resetting what does not carry over (category, sides, split). */
    private fun transitionTo(newMode: EntryMode) {
        val oldMode = _state.value.entryMode
        update { state ->
            val result = TransactionEntryService.transitionType(
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
            ).withoutInitialBalanceConflict()
        }

        if (newMode == EntryMode.TRANSFER) recalculateExchangeRate()
    }

    private fun selectTransferCategory(categoryId: Long) {
        if (_state.value.entryMode != EntryMode.TRANSFER) transitionTo(EntryMode.TRANSFER)
        update { it.withCategory(categoryId) }
        if (!Category.isInitialBalance(categoryId)) return

        viewModelScope.launch {
            val taken = findTakenInitialBalance(_state.value) ?: return@launch
            if (_state.value.replaceInitialBalanceId != taken.id) {
                update { it.copy(initialBalanceConflict = taken) }
            }
        }
    }

    /** The opening balance this form's row would collide with (same account + currency), if any. */
    private suspend fun findTakenInitialBalance(s: TransactionEntryState): Transaction? {
        if (!s.isTransfer || !Category.isInitialBalance(s.selectedCategoryId)) return null
        val side = listOf(s.source, s.destination).firstOrNull { it.account is AccountRef.Wallet } ?: return null
        val accountId = (side.account as AccountRef.Wallet).id
        val profileId = settingsRepository.currentProfileId.first()
        return transactionUseCases.getInitialBalanceTransaction(accountId, side.currency, profileId)
            ?.takeIf { it.id != s.existingTransaction?.id }
    }

    private fun TransactionEntryState.withCategory(categoryId: Long) = copy(
        source = source.copy(categoryId = categoryId),
        destination = destination.copy(categoryId = categoryId)
    )

    private fun TransactionEntryState.withoutInitialBalanceConflict() =
        copy(initialBalanceConflict = null, replaceInitialBalanceId = null)

    // Only a single-leg row can be an opening balance/adjustment: once both sides are real wallets
    // it is an ordinary transfer again.
    private fun TransactionEntryState.withTransferCategoryForShape(): TransactionEntryState {
        val twoSided = source.account is AccountRef.Wallet && destination.account is AccountRef.Wallet
        if (!isTransfer || !Category.isSystemManaged(selectedCategoryId) || !twoSided) return this
        return withCategory(Category.SYSTEM_TRANSFER).withoutInitialBalanceConflict()
    }

    // endregion

    private inline fun update(transform: (TransactionEntryState) -> TransactionEntryState) {
        _state.update(transform)
    }

    private companion object {
        const val NO_TRANSACTION_ID = -1L
        const val DEFAULT_NAME = "Transaction"
        const val RATE_SCALE = 8
        const val ONE_YEAR_MILLIS = 365L * 24 * 60 * 60 * 1000
        const val LINKED_LEG_WINDOW_MILLIS = 5_000L
    }
}
