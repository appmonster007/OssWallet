package com.osscabinet.ossWallet.ui.screens.plannedpayments.entry

import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.AccountRef
import com.osscabinet.ossWallet.domain.model.AmountEditingSide
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.EntryMode
import com.osscabinet.ossWallet.domain.model.PaymentMethod
import com.osscabinet.ossWallet.domain.model.PaymentSchedule
import com.osscabinet.ossWallet.domain.model.PlannedPayment
import com.osscabinet.ossWallet.domain.model.PlannedPaymentStatus
import com.osscabinet.ossWallet.domain.model.RecurrenceRule
import com.osscabinet.ossWallet.domain.model.Tag
import com.osscabinet.ossWallet.domain.model.Template
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionSide
import com.osscabinet.ossWallet.domain.model.dto.PlannedPaymentPrefillDTO
import com.osscabinet.ossWallet.domain.model.dto.TransactionRequest
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
import java.math.RoundingMode
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

    // region BaseEntryViewModel hooks

    override fun getSourceCurrency() = _state.value.source.currency
    override fun getTargetCurrency() = _state.value.destination.currency
    override fun getIsTransfer() = _state.value.isTransfer
    override fun getIsRateFixed() = _state.value.isRateFixed
    override fun getAmountValue() = _state.value.amountValue
    override fun applyRateUpdate(rate: BigDecimal, targetAmountStr: String) {
        _state.update { it.copy(exchangeRate = rate, destination = it.destination.copy(amount = targetAmountStr)) }
    }

    // endregion

    init {
        viewModelScope.launch {
            entryFormSnapshot.collect { snapshot ->
                _state.update { current ->
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
                        source = if (current.isEditMode) current.source
                        else current.source.withDefaultAccount(snapshot.accounts.firstOrNull())
                    )
                }
            }
        }
    }

    // region Initialisation

    fun initialize(plannedPaymentId: Long? = null, prefill: PlannedPaymentPrefillDTO? = null) {
        val params = plannedPaymentId to prefill
        if (lastInitParams == params) return
        if (lastInitParams?.second != null && prefill == null) return
        val isFirstInit = lastInitParams == null
        lastInitParams = params

        when {
            plannedPaymentId != null -> loadPlannedPayment(plannedPaymentId)
            prefill != null -> _state.update { it.cleared().withPrefill(prefill) }
            else -> _state.update { it.cleared().withNewEntryDefaults(isFirstInit) }
        }
    }

    // A form reset keeps the catalog data (accounts, categories, ...) the snapshot collector filled in.
    private fun PlannedPaymentEntryState.cleared() = PlannedPaymentEntryState(
        accounts = accounts,
        categories = categories,
        templates = templates,
        allTags = allTags,
        allPayees = allPayees,
        allNotes = allNotes,
        shortlistedCurrencies = shortlistedCurrencies,
        fullProviderCurrencies = fullProviderCurrencies
    )

    private fun PlannedPaymentEntryState.withNewEntryDefaults(isFirstInit: Boolean): PlannedPaymentEntryState {
        val today = LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val firstAccount = accounts.firstOrNull()
        return copy(
            isEditMode = false,
            plannedPaymentId = null,
            startDateMillis = today,
            endDateMillis = today,
            isNumpadVisible = isFirstInit, // Auto-open only on fresh mount of new entry
            source = source.copy(
                account = firstAccount?.id?.let { AccountRef.fromStored(it) } ?: AccountRef.Unselected,
                currency = firstAccount?.currencyCode ?: source.currency
            )
        )
    }

    private fun PlannedPaymentEntryState.withPrefill(prefill: PlannedPaymentPrefillDTO): PlannedPaymentEntryState {
        val template = prefill.template
        val schedule = prefill.schedule
        val (source, destination) = sidesOf(
            entryMode = template.entryMode,
            accountId = template.accountId,
            targetAccountId = template.targetAccountId,
            currency = template.currency,
            targetCurrency = template.targetCurrency ?: "",
            amount = template.amount,
            rate = template.transferRate,
            categoryId = template.categoryId
        )
        return copy(
            isEditMode = false,
            plannedPaymentId = null,
            linkedTemplate = null,
            name = template.name,
            isNumpadVisible = false,
            source = source,
            destination = destination,
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
            warrantyExpirationMillis = template.warrantyExpiration ?: defaultWarrantyExpiration(),
            isWarrantyEnabled = template.warrantyExpiration != null,
            transactionStatus = template.transactionStatus,
            paymentMethod = template.paymentMethod ?: PaymentMethod.CASH,
            selectedTags = template.tags,
            plannedPaymentStatus = PlannedPaymentStatus.ACTIVE
        )
    }

    private fun loadPlannedPayment(plannedPaymentId: Long) {
        viewModelScope.launch {
            val existing = plannedPaymentRepository.getById(plannedPaymentId) ?: return@launch
            val usageCount = existing.templateId?.let { plannedPaymentRepository.getCountByTemplateId(it) } ?: 0
            _state.update { it.cleared().withPlannedPayment(existing, usageCount) }
        }
    }

    private fun PlannedPaymentEntryState.withPlannedPayment(
        existing: PlannedPayment,
        usageCount: Int
    ): PlannedPaymentEntryState {
        val schedule = existing.schedule
        val rule = schedule.recurrenceRule
        val isTransfer = existing.entryMode == EntryMode.TRANSFER
        val (source, destination) = sidesOf(
            entryMode = existing.entryMode,
            accountId = existing.accountId,
            targetAccountId = existing.targetAccountId,
            currency = existing.currency,
            targetCurrency = existing.targetCurrency ?: existing.currency,
            amount = existing.amount,
            rate = existing.transferRate,
            // A repeatable row can never be a one-per-currency opening balance/adjustment.
            categoryId = if (isTransfer) Category.SYSTEM_TRANSFER else existing.categoryId
        )
        return copy(
            plannedPaymentId = existing.id,
            isEditMode = true,
            originalStartDate = schedule.startDate,
            originalRecurrenceRule = rule,
            originalLastTriggeredDate = schedule.lastTriggeredDate,
            originalNextDueDate = schedule.nextDueDate,
            linkedTemplate = existing.templateId?.let { id -> templates.find { it.id == id } },
            templateUsageCount = usageCount,
            name = existing.name,
            source = source,
            destination = destination,
            entryMode = existing.entryMode,
            recurrenceType = rule.type,
            recurrenceInterval = rule.interval,
            dayOfWeek = rule.dayOfWeek,
            dayOfMonth = rule.dayOfMonth,
            cronExpression = rule.cronExpression,
            autoConfirm = existing.autoConfirm,
            startDateMillis = schedule.startDate,
            hasEndDate = schedule.endDate != null,
            endDateMillis = schedule.endDate ?: schedule.startDate,
            payee = existing.payee ?: "",
            note = existing.note ?: "",
            location = existing.location ?: "",
            gpsLocation = existing.gpsLocation,
            warrantyExpirationMillis = existing.warrantyExpiration ?: defaultWarrantyExpiration(),
            isWarrantyEnabled = existing.warrantyExpiration != null,
            transactionStatus = existing.transactionStatus,
            paymentMethod = existing.paymentMethod ?: PaymentMethod.CASH,
            selectedTags = existing.tags,
            plannedPaymentStatus = existing.status,
            exchangeRate = existing.transferRate,
            isNumpadVisible = false // Never auto-open in edit mode
        )
    }

    // A transfer stores "Outside Wallet" as a null id; every other mode always has a real wallet.
    private fun sidesOf(
        entryMode: EntryMode,
        accountId: String?,
        targetAccountId: String?,
        currency: String,
        targetCurrency: String,
        amount: BigDecimal,
        rate: BigDecimal,
        categoryId: Long
    ): Pair<TransactionSide, TransactionSide> {
        fun accountRef(id: String?) =
            if (entryMode == EntryMode.TRANSFER) AccountRef.fromTransferSide(id) else AccountRef.fromStored(id)

        val sourceAmount = amount.abs()
        return Pair(
            TransactionSide(
                account = accountRef(accountId),
                amount = sourceAmount.stripTrailingZeros().toPlainString(),
                currency = currency,
                categoryId = categoryId
            ),
            TransactionSide(
                account = accountRef(targetAccountId),
                amount = sourceAmount.multiply(rate).setScale(RATE_SCALE, RoundingMode.HALF_UP)
                    .stripTrailingZeros().toPlainString(),
                currency = targetCurrency
            )
        )
    }

    // Selects [account] only while the source is still unselected.
    private fun TransactionSide.withDefaultAccount(account: Account?) =
        if (account != null && this.account is AccountRef.Unselected) {
            copy(account = AccountRef.fromStored(account.id), currency = account.currencyCode)
        } else this

    private fun defaultWarrantyExpiration(): Long = System.currentTimeMillis() + ONE_YEAR_MILLIS

    // endregion

    // region Events

    fun onEvent(event: PlannedPaymentEntryEvent) {
        when (event) {
            is PlannedPaymentEntryEvent.SetName -> _state.update { it.withName(event.name) }
            is PlannedPaymentEntryEvent.SetAmount -> _state.update { s ->
                s.applyUpdate(
                    TransactionEntryService.handleAmountChange(
                        amountStr = event.amount,
                        currentState = s.toUpdateResult(),
                        exchangeRates = ExchangeRateSnapshot(s.currency, s.exchangeRates)
                    )
                )
            }

            is PlannedPaymentEntryEvent.SetTargetAmount -> _state.update { s ->
                s.applyUpdate(
                    TransactionEntryService.handleTargetAmountChange(
                        amountStr = event.amount,
                        currentState = s.toUpdateResult(),
                        exchangeRates = ExchangeRateSnapshot(s.currency, s.exchangeRates)
                    )
                )
            }

            is PlannedPaymentEntryEvent.SetTransactionType -> transitionTo(event.type)
            is PlannedPaymentEntryEvent.SetAccountId -> selectAccount(event.id, isSource = true)
            is PlannedPaymentEntryEvent.SetTargetAccountId -> selectAccount(event.id, isSource = false)
            is PlannedPaymentEntryEvent.SetCurrency -> {
                _state.update { s ->
                    val nextTarget = if (s.isTransfer && s.destination.account.isOutside) event.currency else s.targetCurrency
                    s.copy(
                        source = s.source.copy(currency = event.currency),
                        destination = s.destination.copy(currency = nextTarget)
                    )
                }
                recalculateUnlessRateFixed()
            }

            is PlannedPaymentEntryEvent.SetTargetCurrency -> {
                _state.update { s ->
                    val nextSource = if (s.isTransfer && s.source.account.isOutside) event.currency else s.currency
                    s.copy(
                        destination = s.destination.copy(currency = event.currency ?: ""),
                        source = s.source.copy(currency = nextSource ?: s.currency)
                    )
                }
                recalculateUnlessRateFixed()
            }

            is PlannedPaymentEntryEvent.SetExchangeRate -> _state.update { it.withManualRate(event.rate) }
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

            // The transfer categories are chosen by the entry mode, never from the picker.
            is PlannedPaymentEntryEvent.SetCategoryId -> if (!Category.isTransfer(event.id)) {
                _state.update { it.copy(source = it.source.copy(categoryId = event.id)) }
            }

            is PlannedPaymentEntryEvent.SetPayee -> _state.update { it.copy(payee = event.payee) }
            is PlannedPaymentEntryEvent.SetNote -> _state.update { it.copy(note = event.note) }
            is PlannedPaymentEntryEvent.SetLocation -> _state.update {
                it.copy(gpsLocation = event.gpsLocation, location = event.location ?: "")
            }

            is PlannedPaymentEntryEvent.SetWarrantyEnabled -> _state.update { it.copy(isWarrantyEnabled = event.enabled) }
            is PlannedPaymentEntryEvent.SetWarrantyExpiration -> _state.update {
                it.copy(warrantyExpirationMillis = event.millis, showWarrantyDatePicker = false)
            }

            is PlannedPaymentEntryEvent.SetTransactionStatus -> _state.update { it.copy(transactionStatus = event.status) }
            is PlannedPaymentEntryEvent.SetPaymentMethod -> _state.update { it.copy(paymentMethod = event.method) }
            is PlannedPaymentEntryEvent.SetTags -> _state.update { it.copy(selectedTags = event.tags) }
            is PlannedPaymentEntryEvent.SetLinkedTransactionIds -> setLinkedTransactions(event.ids)
            is PlannedPaymentEntryEvent.RemoveLinkedTransaction -> _state.update {
                it.copy(
                    displayLinkedTxs = it.displayLinkedTxs.filter { tx -> tx.id != event.targetId },
                    linkedTransactionIds = it.linkedTransactionIds.filter { id -> id != event.targetId }
                )
            }

            is PlannedPaymentEntryEvent.SetPlannedPaymentStatus -> _state.update { it.copy(plannedPaymentStatus = event.status) }
            is PlannedPaymentEntryEvent.SetLinkedTemplate -> _state.update { it.withLinkedTemplate(event.template) }
            is PlannedPaymentEntryEvent.SetDeleteTemplateWithPayment ->
                _state.update { it.copy(deleteTemplateWithPayment = event.delete) }

            is PlannedPaymentEntryEvent.ShowScheduleBuilder -> _state.update { it.copy(showScheduleBuilder = event.show) }
            is PlannedPaymentEntryEvent.ShowEndDatePicker -> _state.update { it.copy(showEndDatePicker = event.show) }
            is PlannedPaymentEntryEvent.ShowCategorySheet -> _state.update { it.copy(showCategorySheet = event.show) }
            is PlannedPaymentEntryEvent.ShowAccountSheet -> _state.update { it.copy(showAccountSheet = event.show) }
            is PlannedPaymentEntryEvent.ShowTargetAccountSheet ->
                _state.update { it.copy(showTargetAccountSheet = event.show) }
            is PlannedPaymentEntryEvent.ShowPaymentMethodSheet ->
                _state.update { it.copy(showPaymentMethodSheet = event.show) }
            is PlannedPaymentEntryEvent.ShowTagSheet -> _state.update { it.copy(showTagSheet = event.show) }
            is PlannedPaymentEntryEvent.ShowStatusSheet -> _state.update { it.copy(showStatusSheet = event.show) }
            is PlannedPaymentEntryEvent.ShowTransactionStatusSheet ->
                _state.update { it.copy(showTransactionStatusSheet = event.show) }
            is PlannedPaymentEntryEvent.ShowWarrantyDatePicker ->
                _state.update { it.copy(showWarrantyDatePicker = event.show) }
            is PlannedPaymentEntryEvent.ShowLocationSheet -> _state.update { it.copy(showLocationSheet = event.show) }
            is PlannedPaymentEntryEvent.ShowNumpad -> _state.update {
                it.copy(isNumpadVisible = event.show, activeAmountSide = event.side)
            }
            is PlannedPaymentEntryEvent.ShowCurrencySheet -> _state.update { it.copy(showCurrencySheet = event.show) }
            is PlannedPaymentEntryEvent.ShowTargetCurrencySheet ->
                _state.update { it.copy(showTargetCurrencySheet = event.show) }
            is PlannedPaymentEntryEvent.ShowTemplateSheet -> _state.update { it.copy(showTemplateSheet = event.show) }
            is PlannedPaymentEntryEvent.ShowLinkedTransactionSheet ->
                _state.update { it.copy(showLinkedTransactionSheet = event.show) }
            is PlannedPaymentEntryEvent.ShowExchangeRateDialog ->
                _state.update { it.copy(showExchangeRateDialog = event.show) }
            is PlannedPaymentEntryEvent.ShowDeleteDialog -> _state.update { it.copy(showDeleteDialog = event.show) }
            is PlannedPaymentEntryEvent.DismissAllSheets -> _state.update { it.withAllSheetsDismissed() }

            is PlannedPaymentEntryEvent.SwapAccounts -> {
                _state.update { s ->
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

            is PlannedPaymentEntryEvent.ToggleRateMode -> {
                _state.update { it.copy(isRateFixed = !it.isRateFixed) }
                recalculateUnlessRateFixed()
            }

            is PlannedPaymentEntryEvent.TriggerNavigation -> _state.update { it.copy(navTriggered = true) }
        }
    }

    // Keeps the auto-generated "<name> - " note prefix in step with the name until the user edits it.
    private fun PlannedPaymentEntryState.withName(newName: String): PlannedPaymentEntryState {
        val autoPrefix = "$name - "
        val newNote = when {
            note.isEmpty() -> "$newName - "
            name.isNotEmpty() && note.startsWith(autoPrefix) -> note.replaceFirst(autoPrefix, "$newName - ")
            else -> note
        }
        return copy(name = newName, note = newNote)
    }

    // Setting a rate by hand always pins it.
    private fun PlannedPaymentEntryState.withManualRate(rate: BigDecimal): PlannedPaymentEntryState {
        val sync = TransactionEntryService.syncTransferState(
            sourceAmount = source.amountValue,
            targetAmount = TransactionEntryService.calculateTargetAmountValue(source.amountValue, rate),
            sourceCurrency = currency,
            targetCurrency = targetCurrency,
            isFixed = true,
            activeSide = AmountEditingSide.SOURCE,
            exchangeRates = ExchangeRateSnapshot(currency, exchangeRates)
        )
        return copy(
            isRateFixed = true,
            exchangeRate = rate,
            destination = destination.copy(amount = sync.targetAmount.stripTrailingZeros().toPlainString())
        )
    }

    private fun PlannedPaymentEntryState.withLinkedTemplate(template: Template?): PlannedPaymentEntryState {
        if (template == null) return copy(linkedTemplate = null)
        val (source, destination) = sidesOf(
            entryMode = template.entryMode,
            accountId = template.accountId,
            targetAccountId = template.targetAccountId,
            currency = template.currency,
            targetCurrency = template.targetCurrency ?: "",
            amount = template.amount,
            rate = template.transferRate,
            categoryId = template.categoryId
        )
        return copy(
            linkedTemplate = template,
            name = template.name,
            source = source,
            destination = destination,
            entryMode = template.entryMode,
            exchangeRate = template.transferRate,
            payee = template.payee ?: "",
            note = template.note ?: "",
            paymentMethod = template.paymentMethod ?: PaymentMethod.CASH,
            selectedTags = template.tags
        )
    }

    private fun PlannedPaymentEntryState.withAllSheetsDismissed() = copy(
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
        showExchangeRateDialog = false
    )

    private fun transitionTo(newMode: EntryMode) {
        val oldMode = _state.value.entryMode
        _state.update { state ->
            val result = TransactionEntryService.transitionType(
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
        if (newMode == EntryMode.TRANSFER) recalculateExchangeRate()
    }

    private fun selectAccount(accountId: String?, isSource: Boolean) {
        _state.update { s ->
            s.applyUpdate(
                TransactionEntryService.handleAccountSelection(
                    accountId = accountId,
                    isSource = isSource,
                    currentState = s.toUpdateResult(),
                    accounts = s.accounts,
                    baseCurrency = s.accounts.firstOrNull()?.currencyCode ?: DEFAULT_CURRENCY
                )
            )
        }
        recalculateUnlessRateFixed()
    }

    private fun recalculateUnlessRateFixed() {
        if (!_state.value.isRateFixed) recalculateExchangeRate()
    }

    private fun setLinkedTransactions(ids: List<Long>) {
        viewModelScope.launch {
            val profileId = settingsRepository.currentProfileId.first()
            val transactions = ids.mapNotNull { transactionUseCases.getTransactionById(it, profileId) }
            _state.update { it.copy(displayLinkedTxs = transactions, linkedTransactionIds = ids) }
        }
    }

    // endregion

    // region Save, delete, search

    fun savePlannedPayment(onSuccess: () -> Unit) {
        val s = _state.value
        if (!s.canSave) return

        viewModelScope.launch {
            val profileId = settingsRepository.currentProfileId.first()
            val request = TransactionEntryService.createRequest(
                name = s.name,
                amount = s.amountValue,
                entryMode = s.entryMode,
                sourceAccount = s.source.account,
                sourceCurrency = s.currency,
                targetAccount = s.destination.account,
                targetCurrency = s.targetCurrency,
                categoryId = if (s.isTransfer) Category.SYSTEM_TRANSFER else s.selectedCategoryId,
                note = s.note,
                payee = s.payee,
                tags = s.selectedTags + Tag.SYSTEM_PLANNED,
                status = s.transactionStatus,
                targetAmountOverride = if (s.isTransfer) s.destination.amountValue else null,
                exchangeRates = ExchangeRateSnapshot(s.currency, s.exchangeRates),
                accounts = s.accounts,
                baseCurrency = settingsRepository.currency.first()
            )

            val templateId = saveLinkedTemplate(s, request, profileId)
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
                schedule = scheduleFor(s),
                autoConfirm = s.autoConfirm,
                status = s.plannedPaymentStatus,
                profileId = profileId,
                templateId = templateId
            )

            if (s.isEditMode) plannedPaymentRepository.update(payment) else plannedPaymentRepository.insert(payment)
            onSuccess()
        }
    }

    // Every planned payment is backed by a template; this creates or updates it and returns its id.
    private suspend fun saveLinkedTemplate(
        s: PlannedPaymentEntryState,
        request: TransactionRequest,
        profileId: Int
    ): Long {
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
        if (template.id == 0L) return templateRepository.insertTemplate(template)
        templateRepository.updateTemplate(template)
        return template.id
    }

    // A changed recurrence restarts the schedule at its start date; otherwise the next due date is kept.
    private fun scheduleFor(s: PlannedPaymentEntryState): PaymentSchedule {
        val original = s.originalRecurrenceRule
        val scheduleChanged = s.originalStartDate == null ||
            s.originalStartDate != s.startDateMillis ||
            original?.type != s.recurrenceType ||
            original.interval != s.recurrenceInterval ||
            original.dayOfMonth != s.dayOfMonth ||
            original.dayOfWeek != s.dayOfWeek ||
            original.cronExpression != s.cronExpression
        return PaymentSchedule(
            recurrenceRule = RecurrenceRule(
                type = s.recurrenceType,
                interval = s.recurrenceInterval.coerceAtLeast(1),
                dayOfWeek = s.dayOfWeek,
                dayOfMonth = s.dayOfMonth,
                cronExpression = s.cronExpression
            ),
            startDate = s.startDateMillis,
            endDate = if (s.hasEndDate) s.endDateMillis else null,
            nextDueDate = if (scheduleChanged) s.startDateMillis else s.originalNextDueDate ?: s.startDateMillis,
            lastTriggeredDate = s.originalLastTriggeredDate
        )
    }

    fun deletePlannedPayment() {
        val s = _state.value
        val id = s.plannedPaymentId ?: return
        val templateId = s.linkedTemplate?.id

        // Close the dialog immediately to prevent double clicks; navigate only once deletion is done.
        _state.update { it.copy(showDeleteDialog = false) }

        viewModelScope.launch {
            try {
                if (s.deleteTemplateWithPayment && templateId != null) {
                    // Deleting the template cascades to every planned payment linked to it, this one included.
                    templateRepository.getTemplateById(templateId)?.let { templateRepository.deleteTemplate(it) }
                } else {
                    plannedPaymentRepository.delete(id)
                }
                _state.update { it.copy(navTriggered = true) }
            } catch (e: Exception) {
                _state.update { it.copy(navTriggered = false) }
            }
        }
    }

    fun onScreenResume() = _state.update { it.copy(navTriggered = false) }

    /** Search transactions for linking. */
    fun searchTransactions(query: String): Flow<List<Transaction>> = flow {
        val profileId = settingsRepository.currentProfileId.first()
        emitAll(transactionUseCases.searchTransactions(profileId, query))
    }

    // endregion

    private companion object {
        const val DEFAULT_CURRENCY = "USD"
        const val RATE_SCALE = 8
        const val ONE_YEAR_MILLIS = 365L * 24 * 60 * 60 * 1000
    }
}
