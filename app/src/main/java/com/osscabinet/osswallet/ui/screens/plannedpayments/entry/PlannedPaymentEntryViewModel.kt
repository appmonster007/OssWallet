package com.osscabinet.ossWallet.ui.screens.plannedpayments.entry

import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.model.AccountRef
import com.osscabinet.ossWallet.domain.model.AmountEditingSide
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.EntryMode
import com.osscabinet.ossWallet.domain.model.PaymentMethod
import com.osscabinet.ossWallet.domain.model.PaymentSchedule
import com.osscabinet.ossWallet.domain.model.PlannedPayment
import com.osscabinet.ossWallet.domain.model.PlannedPaymentStatus
import com.osscabinet.ossWallet.domain.model.RecurrenceRule
import com.osscabinet.ossWallet.domain.model.Template
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionSide
import com.osscabinet.ossWallet.domain.model.dto.PlannedPaymentPrefillDTO
import com.osscabinet.ossWallet.domain.model.isOutside
import com.osscabinet.ossWallet.domain.repository.ComputationRepository
import com.osscabinet.ossWallet.domain.repository.PlannedPaymentRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.repository.TemplateRepository
import com.osscabinet.ossWallet.domain.service.currency.ExchangeRateSnapshot
import com.osscabinet.ossWallet.domain.service.management.TransactionEntryService
import com.osscabinet.ossWallet.domain.usecase.EntryFormDataUseCase
import com.osscabinet.ossWallet.domain.usecase.TransactionUseCases
import com.osscabinet.ossWallet.ui.util.BaseEntryViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneOffset

@HiltViewModel
class PlannedPaymentEntryViewModel @Inject constructor(
    private val plannedPaymentRepository: PlannedPaymentRepository,
    private val templateRepository: TemplateRepository,
    private val transactionUseCases: TransactionUseCases,
    entryFormDataUseCase: EntryFormDataUseCase,
    computationRepository: ComputationRepository,
    settingsRepository: SettingsRepository
) : BaseEntryViewModel(entryFormDataUseCase, computationRepository, settingsRepository) {

    private val _state = MutableStateFlow(PlannedPaymentEntryState())
    val state: StateFlow<PlannedPaymentEntryState> = _state.asStateFlow()

    private var lastInitParams: Pair<Long?, PlannedPaymentPrefillDTO?>? = null

    // BaseEntryViewModel abstract overrides
    override fun getSourceCurrency() = _state.value.source.currency
    override fun getTargetCurrency() = _state.value.destination.currency
    override fun getIsTransfer() = _state.value.isTransfer
    override fun getIsRateFixed() = _state.value.isRateFixed
    override fun getAmountValue() = _state.value.amountValue
    override fun applyRateUpdate(rate: BigDecimal, targetAmountStr: String) {
        _state.update {
            it.copy(
                exchangeRate = rate,
                destination = it.destination.copy(amount = targetAmountStr)
            )
        }
    }

    init {
        viewModelScope.launch {
            entryFormSnapshot.collect { snapshot ->
                _state.update { current ->
                    val defaultSource = if (!current.isEditMode &&
                        current.source.account is AccountRef.Unselected &&
                        snapshot.accounts.isNotEmpty()
                    ) {
                        current.source.copy(
                            account = AccountRef.fromStored(snapshot.accounts.first().id),
                            currency = snapshot.accounts.first().currencyCode
                        )
                    } else current.source
                    current.copy(
                        accounts = snapshot.accounts,
                        categories = snapshot.categories,
                        templates = snapshot.templates,
                        allTags = snapshot.allTags,
                        allPayees = snapshot.allPayees,
                        allNotes = snapshot.allNotes,
                        shortlistedCurrencies = snapshot.shortlistedCurrencies,
                        fullProviderCurrencies = snapshot.fullProviderCurrencies,
                        exchangeRates = snapshot.exchangeRates,
                        source = defaultSource
                    )
                }
            }
        }
    }

    fun initialize(plannedPaymentId: Long? = null, prefill: PlannedPaymentPrefillDTO? = null) {
        val params = plannedPaymentId to prefill
        if (lastInitParams == params) return
        if (lastInitParams?.second != null && prefill == null) return
        val isFirstInit = lastInitParams == null
        lastInitParams = params

        viewModelScope.launch {
            if (plannedPaymentId == null) {
                if (prefill != null) {
                    val template = prefill.template
                    val schedule = prefill.schedule
                    val isTemplateTransfer = template.entryMode == EntryMode.TRANSFER
                    _state.update { current ->
                        PlannedPaymentEntryState(
                            accounts = current.accounts,
                            categories = current.categories,
                            templates = current.templates,
                            allTags = current.allTags,
                            allPayees = current.allPayees,
                            allNotes = current.allNotes,

                            shortlistedCurrencies = current.shortlistedCurrencies,
                            fullProviderCurrencies = current.fullProviderCurrencies,
                            isEditMode = false,
                            plannedPaymentId = null,
                            linkedTemplate = null,
                            name = template.name,
                            isNumpadVisible = false,
                            source = TransactionSide(
                                account = if (isTemplateTransfer) AccountRef.fromTransferSide(
                                    template.accountId
                                )
                                else AccountRef.fromStored(template.accountId),
                                amount = template.amount.abs().stripTrailingZeros().toPlainString(),
                                currency = template.currency,
                                categoryId = template.categoryId
                            ),
                            destination = TransactionSide(
                                account = if (isTemplateTransfer) AccountRef.fromTransferSide(
                                    template.targetAccountId
                                )
                                else AccountRef.fromStored(template.targetAccountId),
                                amount = template.amount.abs()
                                    .multiply(template.transferRate)
                                    .setScale(8, java.math.RoundingMode.HALF_UP)
                                    .stripTrailingZeros()
                                    .toPlainString(),
                                currency = template.targetCurrency ?: ""
                            ),
                            entryMode = template.entryMode,
                            exchangeRate = template.transferRate,
                            recurrenceType = schedule.recurrenceRule.type,
                            recurrenceInterval = schedule.recurrenceRule.interval,
                            dayOfWeek = schedule.recurrenceRule.dayOfWeek,
                            dayOfMonth = schedule.recurrenceRule.dayOfMonth,
                            autoConfirm = true,
                            startDateMillis = schedule.startDate,
                            hasEndDate = false,
                            endDateMillis = schedule.startDate,
                            payee = template.payee ?: "",
                            note = template.note ?: "",
                            location = template.location ?: "",
                            gpsLocation = template.gpsLocation,
                            warrantyExpirationMillis = template.warrantyExpiration
                                ?: (System.currentTimeMillis() + 31536000000L),
                            isWarrantyEnabled = template.warrantyExpiration != null,
                            transactionStatus = template.transactionStatus,
                            paymentMethod = template.paymentMethod ?: PaymentMethod.CASH,
                            selectedTags = template.tags,
                            plannedPaymentStatus = PlannedPaymentStatus.ACTIVE
                        )
                    }
                } else {
                    val now = LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC).toInstant()
                        .toEpochMilli()
                    _state.update { current ->
                        val firstAccount = current.accounts.firstOrNull()
                        PlannedPaymentEntryState(
                            accounts = current.accounts,
                            categories = current.categories,
                            templates = current.templates,
                            allTags = current.allTags,
                            allPayees = current.allPayees,
                            allNotes = current.allNotes,

                            shortlistedCurrencies = current.shortlistedCurrencies,
                            fullProviderCurrencies = current.fullProviderCurrencies,
                            isEditMode = false,
                            plannedPaymentId = null,
                            startDateMillis = now,
                            endDateMillis = now,
                            isNumpadVisible = isFirstInit, // Auto-open only on fresh mount of new entry
                            source = current.source.copy(
                                account = firstAccount?.id?.let { AccountRef.fromStored(it) }
                                    ?: AccountRef.Unselected,
                                currency = firstAccount?.currencyCode ?: current.source.currency
                            )
                        )
                    }
                }
                return@launch
            }

            plannedPaymentRepository.getById(plannedPaymentId)?.let { existing ->
                val schedule = existing.schedule
                val usageCount =
                    existing.templateId?.let { plannedPaymentRepository.getCountByTemplateId(it) }
                        ?: 0

                val sourceAccId = existing.accountId
                val targetAccId = existing.targetAccountId
                val sourceCurrency = existing.currency
                val targetCurr = existing.targetCurrency ?: existing.currency
                val sourceAmtStr = existing.amount.abs().stripTrailingZeros().toPlainString()
                val targetAmtStr = existing.amount.abs().multiply(existing.transferRate)
                    .setScale(8, java.math.RoundingMode.HALF_UP).stripTrailingZeros()
                    .toPlainString()
                var existingCategoryId = existing.categoryId

                if (existing.entryMode == EntryMode.TRANSFER) {
                    existingCategoryId = if (Category.isInitialBalance(existing.categoryId))
                        Category.SYSTEM_INITIAL_BALANCE else Category.SYSTEM_TRANSFER
                }

                val sourceRef = if (existing.entryMode == EntryMode.TRANSFER)
                    AccountRef.fromTransferSide(sourceAccId) else AccountRef.fromStored(sourceAccId)
                val targetRef = if (existing.entryMode == EntryMode.TRANSFER)
                    AccountRef.fromTransferSide(targetAccId) else AccountRef.fromStored(targetAccId)

                _state.update { current ->
                    PlannedPaymentEntryState(
                        accounts = current.accounts,
                        categories = current.categories,
                        templates = current.templates,
                        allTags = current.allTags,
                        allPayees = current.allPayees,
                        allNotes = current.allNotes,
                        shortlistedCurrencies = current.shortlistedCurrencies,
                        fullProviderCurrencies = current.fullProviderCurrencies,
                        plannedPaymentId = plannedPaymentId,
                        isEditMode = true,
                        originalStartDate = existing.schedule.startDate,
                        originalRecurrenceRule = existing.schedule.recurrenceRule,
                        originalLastTriggeredDate = existing.schedule.lastTriggeredDate,
                        originalNextDueDate = existing.schedule.nextDueDate,
                        linkedTemplate = existing.templateId?.let { id -> current.templates.find { t -> t.id == id } },
                        templateUsageCount = usageCount,
                        name = existing.name,
                        source = TransactionSide(
                            account = sourceRef,
                            amount = sourceAmtStr,
                            currency = sourceCurrency,
                            categoryId = existingCategoryId
                        ),
                        destination = TransactionSide(
                            account = targetRef,
                            amount = targetAmtStr,
                            currency = targetCurr
                        ),
                        entryMode = existing.entryMode,
                        recurrenceType = schedule.recurrenceRule.type,
                        recurrenceInterval = schedule.recurrenceRule.interval,
                        dayOfWeek = schedule.recurrenceRule.dayOfWeek,
                        dayOfMonth = schedule.recurrenceRule.dayOfMonth,
                        cronExpression = schedule.recurrenceRule.cronExpression,
                        autoConfirm = existing.autoConfirm,
                        startDateMillis = schedule.startDate,
                        hasEndDate = schedule.endDate != null,
                        endDateMillis = schedule.endDate ?: schedule.startDate,
                        payee = existing.payee ?: "",
                        note = existing.note ?: "",
                        location = existing.location ?: "",
                        gpsLocation = existing.gpsLocation,
                        warrantyExpirationMillis = existing.warrantyExpiration
                            ?: (System.currentTimeMillis() + 31536000000L),
                        isWarrantyEnabled = existing.warrantyExpiration != null,
                        transactionStatus = existing.transactionStatus,
                        paymentMethod = existing.paymentMethod ?: PaymentMethod.CASH,
                        selectedTags = existing.tags,
                        plannedPaymentStatus = existing.status,
                        exchangeRate = existing.transferRate,
                        isNumpadVisible = false // Never auto-open in edit mode
                    )
                }
            }
        }
    }

    fun onEvent(event: PlannedPaymentEntryEvent) {
        when (event) {
            is PlannedPaymentEntryEvent.SetName -> {
                _state.update { state ->
                    val newNote = when {
                        state.note.isEmpty() -> "${event.name} - "
                        state.name.isNotEmpty() && state.note.startsWith("${state.name} - ") -> {
                            state.note.replaceFirst("${state.name} - ", "${event.name} - ")
                        }

                        else -> state.note
                    }
                    state.copy(name = event.name, note = newNote)
                }
            }

            is PlannedPaymentEntryEvent.SetAmount -> {
                _state.update { s ->
                    s.applyUpdate(
                        TransactionEntryService.handleAmountChange(
                            amountStr = event.amount,
                            currentState = s.toUpdateResult(),
                            exchangeRates = ExchangeRateSnapshot(s.currency, s.exchangeRates)
                        )
                    )
                }
            }

            is PlannedPaymentEntryEvent.SetTargetAmount -> {
                _state.update { s ->
                    s.applyUpdate(
                        TransactionEntryService.handleTargetAmountChange(
                            amountStr = event.amount,
                            currentState = s.toUpdateResult(),
                            exchangeRates = ExchangeRateSnapshot(s.currency, s.exchangeRates)
                        )
                    )
                }
            }

            is PlannedPaymentEntryEvent.SetTransactionType -> {
                val oldMode = _state.value.entryMode
                val newMode = event.type
                _state.update { state ->
                    val result =
                        TransactionEntryService.transitionType(
                            oldMode = oldMode,
                            newMode = newMode,
                            currentSource = state.source,
                            currentDestination = state.destination,
                            isSplitMode = false // Planned payments don't support split mode yet
                        )

                    state.copy(
                        entryMode = result.newMode,
                        source = result.source,
                        destination = result.destination,
                        isRateFixed = if (newMode != EntryMode.TRANSFER) false else state.isRateFixed
                    )
                }
                if (newMode == EntryMode.TRANSFER) {
                    recalculateExchangeRate()
                }
            }

            is PlannedPaymentEntryEvent.SetAccountId -> {
                _state.update { s ->
                    val result = TransactionEntryService.handleAccountSelection(
                        accountId = event.id,
                        isSource = true,
                        currentState = s.toUpdateResult(),
                        accounts = s.accounts,
                        baseCurrency = s.accounts.firstOrNull()?.currencyCode ?: "USD"
                    )
                    s.applyUpdate(result)
                }
                if (!_state.value.isRateFixed) recalculateExchangeRate()
            }

            is PlannedPaymentEntryEvent.SetTargetAccountId -> {
                _state.update { s ->
                    val result = TransactionEntryService.handleAccountSelection(
                        accountId = event.id,
                        isSource = false,
                        currentState = s.toUpdateResult(),
                        accounts = s.accounts,
                        baseCurrency = s.accounts.firstOrNull()?.currencyCode ?: "USD"
                    )
                    s.applyUpdate(result)
                }
                if (!_state.value.isRateFixed) recalculateExchangeRate()
            }

            is PlannedPaymentEntryEvent.SetTargetCurrency -> {
                _state.update { state ->
                    val nextSource = if (state.isTransfer && state.source.account.isOutside) {
                        event.currency
                    } else state.currency

                    state.copy(
                        destination = state.destination.copy(currency = event.currency ?: ""),
                        source = state.source.copy(currency = nextSource ?: state.currency)
                    )
                }
                if (!_state.value.isRateFixed) recalculateExchangeRate()
            }

            is PlannedPaymentEntryEvent.SetExchangeRate -> _state.update {
                val sync = TransactionEntryService.syncTransferState(
                    sourceAmount = it.source.amountValue,
                    targetAmount = TransactionEntryService.calculateTargetAmountValue(
                        it.source.amountValue,
                        event.rate
                    ),
                    sourceCurrency = it.currency,
                    targetCurrency = it.targetCurrency,
                    isFixed = true,
                    activeSide = AmountEditingSide.SOURCE,
                    exchangeRates = ExchangeRateSnapshot(it.currency, it.exchangeRates)
                )

                it.copy(
                    isRateFixed = true,
                    exchangeRate = event.rate,
                    destination = it.destination.copy(
                        amount = sync.targetAmount.stripTrailingZeros().toPlainString()
                    )
                )
            }

            is PlannedPaymentEntryEvent.SetCurrency -> {
                _state.update { state ->
                    val nextTarget = if (state.isTransfer && state.destination.account.isOutside) {
                        event.currency
                    } else state.targetCurrency

                    state.copy(
                        source = state.source.copy(currency = event.currency),
                        destination = state.destination.copy(currency = nextTarget)
                    )
                }
                if (!_state.value.isRateFixed) recalculateExchangeRate()
            }

            is PlannedPaymentEntryEvent.SetRecurrence -> _state.update {
                it.copy(
                    startDateMillis = event.startDate,
                    recurrenceType = event.type,
                    recurrenceInterval = event.interval,
                    dayOfWeek = event.dayOfWeek,
                    dayOfMonth = event.dayOfMonth,
                    cronExpression = event.cronExpression
                )
            }

            is PlannedPaymentEntryEvent.SetAutoConfirm -> _state.update { it.copy(autoConfirm = event.auto) }
            is PlannedPaymentEntryEvent.SetHasEndDate -> _state.update { it.copy(hasEndDate = event.has) }
            is PlannedPaymentEntryEvent.SetEndDate -> _state.update { it.copy(endDateMillis = event.date) }
            is PlannedPaymentEntryEvent.SetCategoryId -> {
                if (!Category.isInitialBalance(event.id)) _state.update {
                    it.copy(source = it.source.copy(categoryId = event.id))
                }
            }

            is PlannedPaymentEntryEvent.SetPayee -> _state.update { it.copy(payee = event.payee) }
            is PlannedPaymentEntryEvent.SetNote -> _state.update { it.copy(note = event.note) }
            is PlannedPaymentEntryEvent.SetLocation -> _state.update {
                it.copy(
                    gpsLocation = event.gpsLocation,
                    location = event.location ?: ""
                )
            }

            is PlannedPaymentEntryEvent.SetWarrantyEnabled -> _state.update {
                it.copy(
                    isWarrantyEnabled = event.enabled
                )
            }

            is PlannedPaymentEntryEvent.SetWarrantyExpiration -> _state.update {
                it.copy(warrantyExpirationMillis = event.millis, showWarrantyDatePicker = false)
            }

            is PlannedPaymentEntryEvent.SetTransactionStatus -> _state.update {
                it.copy(
                    transactionStatus = event.status
                )
            }

            is PlannedPaymentEntryEvent.SetPaymentMethod -> _state.update { it.copy(paymentMethod = event.method) }
            is PlannedPaymentEntryEvent.SetTags -> _state.update { it.copy(selectedTags = event.tags) }
            is PlannedPaymentEntryEvent.SetLinkedTransactionIds -> {
                viewModelScope.launch {
                    val profileId = settingsRepository.currentProfileId.first()
                    // Fetch specific transactions by ID for display
                    val transactions = event.ids.mapNotNull { id ->
                        transactionUseCases.getTransactionById(id, profileId)
                    }
                    _state.update {
                        it.copy(
                            displayLinkedTxs = transactions,
                            linkedTransactionIds = event.ids
                        )
                    }
                }
            }

            is PlannedPaymentEntryEvent.RemoveLinkedTransaction -> {
                _state.update { state ->
                    val newList = state.displayLinkedTxs.filter { it.id != event.targetId }
                    state.copy(
                        displayLinkedTxs = newList,
                        linkedTransactionIds = state.linkedTransactionIds.filter { it != event.targetId }
                    )
                }
            }

            is PlannedPaymentEntryEvent.SetPlannedPaymentStatus -> _state.update {
                it.copy(
                    plannedPaymentStatus = event.status
                )
            }

            is PlannedPaymentEntryEvent.SetLinkedTemplate -> {
                _state.update { state ->
                    if (event.template != null) {
                        val isTemplateTransfer = event.template.entryMode == EntryMode.TRANSFER
                        state.copy(
                            linkedTemplate = event.template,
                            name = event.template.name,
                            source = TransactionSide(
                                account = if (isTemplateTransfer) AccountRef.fromTransferSide(event.template.accountId)
                                else AccountRef.fromStored(event.template.accountId),
                                amount = event.template.amount.abs().stripTrailingZeros()
                                    .toPlainString(),
                                currency = event.template.currency,
                                categoryId = event.template.categoryId
                            ),
                            destination = TransactionSide(
                                account = if (isTemplateTransfer) AccountRef.fromTransferSide(event.template.targetAccountId)
                                else AccountRef.fromStored(event.template.targetAccountId),
                                amount = event.template.amount.abs()
                                    .multiply(event.template.transferRate)
                                    .setScale(8, java.math.RoundingMode.HALF_UP)
                                    .stripTrailingZeros().toPlainString(),
                                currency = event.template.targetCurrency ?: ""
                            ),
                            entryMode = event.template.entryMode,
                            exchangeRate = event.template.transferRate,
                            payee = event.template.payee ?: "",
                            note = event.template.note ?: "",
                            paymentMethod = event.template.paymentMethod ?: PaymentMethod.CASH,
                            selectedTags = event.template.tags
                        )
                    } else {
                        state.copy(linkedTemplate = null)
                    }
                }
            }

            is PlannedPaymentEntryEvent.SetDeleteTemplateWithPayment -> _state.update {
                it.copy(
                    deleteTemplateWithPayment = event.delete
                )
            }

            is PlannedPaymentEntryEvent.ShowScheduleBuilder -> _state.update {
                it.copy(
                    showScheduleBuilder = event.show
                )
            }

            is PlannedPaymentEntryEvent.ShowEndDatePicker -> _state.update {
                it.copy(
                    showEndDatePicker = event.show
                )
            }

            is PlannedPaymentEntryEvent.ShowCategorySheet -> _state.update {
                it.copy(
                    showCategorySheet = event.show
                )
            }

            is PlannedPaymentEntryEvent.ShowAccountSheet -> _state.update { it.copy(showAccountSheet = event.show) }
            is PlannedPaymentEntryEvent.ShowTargetAccountSheet -> _state.update {
                it.copy(
                    showTargetAccountSheet = event.show
                )
            }

            is PlannedPaymentEntryEvent.ShowPaymentMethodSheet -> _state.update {
                it.copy(
                    showPaymentMethodSheet = event.show
                )
            }

            is PlannedPaymentEntryEvent.ShowTagSheet -> _state.update { it.copy(showTagSheet = event.show) }
            is PlannedPaymentEntryEvent.ShowStatusSheet -> _state.update { it.copy(showStatusSheet = event.show) }
            is PlannedPaymentEntryEvent.ShowTransactionStatusSheet -> _state.update {
                it.copy(
                    showTransactionStatusSheet = event.show
                )
            }

            is PlannedPaymentEntryEvent.ShowWarrantyDatePicker -> _state.update {
                it.copy(
                    showWarrantyDatePicker = event.show
                )
            }

            is PlannedPaymentEntryEvent.ShowLocationSheet -> _state.update {
                it.copy(
                    showLocationSheet = event.show
                )
            }

            is PlannedPaymentEntryEvent.ShowNumpad -> _state.update {
                it.copy(
                    isNumpadVisible = event.show,
                    activeAmountSide = event.side
                )
            }

            is PlannedPaymentEntryEvent.ShowCurrencySheet -> _state.update {
                it.copy(
                    showCurrencySheet = event.show
                )
            }

            is PlannedPaymentEntryEvent.ShowTargetCurrencySheet -> _state.update {
                it.copy(
                    showTargetCurrencySheet = event.show
                )
            }

            is PlannedPaymentEntryEvent.ShowTemplateSheet -> _state.update {
                it.copy(
                    showTemplateSheet = event.show
                )
            }

            is PlannedPaymentEntryEvent.ShowLinkedTransactionSheet -> _state.update {
                it.copy(
                    showLinkedTransactionSheet = event.show
                )
            }

            is PlannedPaymentEntryEvent.ShowExchangeRateDialog -> _state.update {
                it.copy(
                    showExchangeRateDialog = event.show
                )
            }

            is PlannedPaymentEntryEvent.ShowDeleteDialog -> _state.update { it.copy(showDeleteDialog = event.show) }

            is PlannedPaymentEntryEvent.SwapAccounts -> {
                _state.update { state ->
                    if (!state.isTransfer) return@update state
                    val (newSrc, newDst, newRate) = TransactionEntryService.swapAccounts(
                        source = state.source,
                        destination = state.destination,
                        transferRate = state.exchangeRate
                    )
                    state.copy(
                        source = newSrc,
                        destination = newDst,
                        exchangeRate = newRate
                    )
                }
                if (!_state.value.isRateFixed) recalculateExchangeRate()
            }

            is PlannedPaymentEntryEvent.ToggleRateMode -> {
                _state.update { s ->
                    val nextFixed = !s.isRateFixed
                    s.copy(isRateFixed = nextFixed)
                }
                if (!_state.value.isRateFixed) {
                    recalculateExchangeRate()
                }
            }

            is PlannedPaymentEntryEvent.DismissAllSheets -> _state.update {
                it.copy(
                    showAccountSheet = false,
                    showTargetAccountSheet = false,
                    showCategorySheet = false,
                    showPaymentMethodSheet = false,
                    showTagSheet = false,
                    showStatusSheet = false,
                    showTransactionStatusSheet = false,
                    showWarrantyDatePicker = false,
                    showLocationSheet = false,
                    showLinkedTransactionSheet = false,
                    showCurrencySheet = false,
                    showTargetCurrencySheet = false,
                    showTemplateSheet = false,
                    showExchangeRateDialog = false,
                )
            }

            is PlannedPaymentEntryEvent.TriggerNavigation -> _state.update { it.copy(navTriggered = true) }
        }
    }

    fun savePlannedPayment(onSuccess: () -> Unit) {
        val s = _state.value
        if (!s.canSave) return

        viewModelScope.launch {
            val profileId = settingsRepository.currentProfileId.first()
            val baseCurrency = settingsRepository.currency.first()

            val templateCategoryId = if (s.isTransfer) {
                if (Category.isInitialBalance(s.selectedCategoryId)) Category.SYSTEM_INITIAL_BALANCE
                else Category.SYSTEM_TRANSFER
            } else s.selectedCategoryId

            val request = TransactionEntryService.createRequest(
                name = s.name,
                amount = s.amountValue,
                entryMode = s.entryMode,
                sourceAccount = s.source.account,
                sourceCurrency = s.currency,
                targetAccount = s.destination.account,
                targetCurrency = s.targetCurrency,
                categoryId = templateCategoryId,
                note = s.note,
                payee = s.payee,
                tags = s.selectedTags + com.osscabinet.ossWallet.domain.model.Tag.SYSTEM_PLANNED,
                status = s.transactionStatus,
                targetAmountOverride = if (s.isTransfer) s.destination.amountValue else null,
                exchangeRates = ExchangeRateSnapshot(s.currency, s.exchangeRates),
                accounts = s.accounts,
                baseCurrency = baseCurrency
            )

            // 1. Create or Update Template
            val template = Template(
                id = s.linkedTemplate?.id ?: 0L,
                name = request.name,
                categoryId = request.categoryId,
                amount = request.amount,
                entryMode = request.entryMode,
                accountId = request.accountId,
                targetAccountId = request.targetAccountId,
                targetCurrency = request.targetCurrency,
                transferRate = request.transferRate,
                currency = request.currency,
                payee = request.payee,
                note = request.note,
                location = s.location.ifBlank { null },
                gpsLocation = s.gpsLocation,
                warrantyExpiration = if (s.isWarrantyEnabled) s.warrantyExpirationMillis else null,
                transactionStatus = request.status,
                paymentMethod = s.paymentMethod,
                tags = request.tags,
                profileId = profileId
            )

            val templateId = if (template.id == 0L) {
                templateRepository.insertTemplate(template)
            } else {
                templateRepository.updateTemplate(template)
                template.id
            }

            // 2. Create or Update Planned Payment
            val rule = RecurrenceRule(
                type = s.recurrenceType,
                interval = s.recurrenceInterval.coerceAtLeast(1),
                dayOfWeek = s.dayOfWeek,
                dayOfMonth = s.dayOfMonth,
                cronExpression = s.cronExpression
            )

            val scheduleChanged = s.originalStartDate == null ||
                    s.originalStartDate != s.startDateMillis ||
                    s.originalRecurrenceRule?.type != s.recurrenceType ||
                    s.originalRecurrenceRule.interval != s.recurrenceInterval ||
                    s.originalRecurrenceRule.dayOfMonth != s.dayOfMonth ||
                    s.originalRecurrenceRule.dayOfWeek != s.dayOfWeek ||
                    s.originalRecurrenceRule.cronExpression != s.cronExpression
            val nextDueDate = if (scheduleChanged) s.startDateMillis
            else s.originalNextDueDate ?: s.startDateMillis

            val payment = PlannedPayment(
                id = s.plannedPaymentId ?: 0L,
                name = request.name,
                categoryId = request.categoryId,
                amount = request.amount,
                entryMode = request.entryMode,
                accountId = request.accountId,
                targetAccountId = request.targetAccountId,
                targetCurrency = request.targetCurrency,
                transferRate = request.transferRate,
                currency = request.currency,
                payee = request.payee,
                note = request.note,
                location = s.location.ifBlank { null },
                gpsLocation = s.gpsLocation,
                warrantyExpiration = if (s.isWarrantyEnabled) s.warrantyExpirationMillis else null,
                transactionStatus = request.status,
                paymentMethod = s.paymentMethod,
                tags = request.tags,
                schedule = PaymentSchedule(
                    recurrenceRule = rule,
                    startDate = s.startDateMillis,
                    endDate = if (s.hasEndDate) s.endDateMillis else null,
                    nextDueDate = nextDueDate,
                    lastTriggeredDate = s.originalLastTriggeredDate
                ),
                autoConfirm = s.autoConfirm,
                status = s.plannedPaymentStatus,
                profileId = profileId,
                templateId = templateId
            )

            if (s.isEditMode) {
                plannedPaymentRepository.update(payment)
            } else {
                plannedPaymentRepository.insert(payment)
            }
            onSuccess()
        }
    }

    fun deletePlannedPayment() {
        val id = _state.value.plannedPaymentId ?: return
        val templateId = _state.value.linkedTemplate?.id
        val deleteTemplate = _state.value.deleteTemplateWithPayment

        // Close dialog immediately to prevent double clicks, but wait for deletion to navigate
        _state.update { it.copy(showDeleteDialog = false) }

        viewModelScope.launch {
            try {
                // Perform deletion in a way that handles cascade correctly
                if (deleteTemplate && templateId != null) {
                    // Deleting the template will CASCADE delete all linked planned payments (including this one)
                    templateRepository.getTemplateById(templateId)?.let { template ->
                        templateRepository.deleteTemplate(template)
                    }
                } else {
                    // Only delete this specific planned payment
                    plannedPaymentRepository.delete(id)
                }

                // Now that it's deleted from DB, trigger navigation
                _state.update { it.copy(navTriggered = true) }
            } catch (e: Exception) {
                // Optional: Show error message to user
                _state.update { it.copy(navTriggered = false) }
            }
        }
    }

    fun onScreenResume() {
        _state.update { it.copy(navTriggered = false) }
    }

    /**
     * Search transactions for linking.
     */
    fun searchTransactions(query: String): Flow<List<Transaction>> = flow {
        val profileId = settingsRepository.currentProfileId.first()
        emitAll(transactionUseCases.searchTransactions(profileId, query))
    }
}
