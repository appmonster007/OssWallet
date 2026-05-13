package com.osscabinet.ossWallet.ui.screens.plannedpayments.entry

import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.model.*
import com.osscabinet.ossWallet.domain.model.dto.PlannedPaymentPrefillDTO
import com.osscabinet.ossWallet.domain.repository.*
import com.osscabinet.ossWallet.ui.util.BaseViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class PlannedPaymentEntryViewModel(
    private val plannedPaymentRepository: PlannedPaymentRepository,
    private val templateRepository: TemplateRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val tagRepository: TagRepository,
    private val transactionRepository: TransactionRepository,
    settingsRepository: SettingsRepository
) : BaseViewModel(settingsRepository) {

    private val _state = MutableStateFlow(PlannedPaymentEntryState())
    val state: StateFlow<PlannedPaymentEntryState> = _state.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            settingsRepository.currentProfileId.flatMapLatest { profileId ->
                combine(
                    accountRepository.getAllAccounts(profileId),
                    categoryRepository.categories,
                    templateRepository.getAllTemplates(),
                    tagRepository.getAllTags(),
                    transactionRepository.getAllTransactions(profileId)
                ) { accounts, categories, templates, tags, transactions ->
                    val uniquePayees = transactions.map { it.payee }.filter { it.isNotBlank() }.distinct().sorted()
                    val uniqueNotes = transactions.map { it.note }.filter { it.isNotBlank() }.distinct().sorted()
                    
                    _state.update {
                        it.copy(
                            accounts = accounts,
                            categories = categories,
                            templates = templates,
                            availableTags = tags,
                            uniquePayees = uniquePayees,
                            uniqueNotes = uniqueNotes,
                            currentProfileId = profileId
                        )
                    }
                }
            }.collect()
        }
    }

    fun initialize(plannedPaymentId: Long? = null, prefill: PlannedPaymentPrefillDTO? = null) {
        if (plannedPaymentId == null) {
            if (prefill != null) {
                val template = prefill.template
                val schedule = prefill.schedule
                _state.update {
                    it.copy(
                        isEditMode = false,
                        plannedPaymentId = null,
                        linkedTemplate = null,
                        title = template.name,
                        amountText = template.amount.abs().toPlainString(),
                        currency = template.currency ?: "USD",
                        transactionType = template.transactionType,
                        selectedAccountId = template.accountId ?: "",
                        targetAccountId = template.targetAccountId,
                        targetCurrency = template.targetCurrency,
                        transferRate = template.transferRate,
                        recurrenceType = schedule.recurrenceRule.type,
                        recurrenceInterval = schedule.recurrenceRule.interval,
                        dayOfWeek = schedule.recurrenceRule.dayOfWeek,
                        dayOfMonth = schedule.recurrenceRule.dayOfMonth,
                        autoConfirm = true,
                        startDateMillis = schedule.startDate,
                        hasEndDate = false,
                        endDateMillis = schedule.startDate,
                        selectedCategoryId = template.categoryId,
                        payee = template.payee ?: "",
                        noteUserPart = template.note ?: "",
                        paymentMethod = template.paymentMethod ?: "Cash",
                        selectedTagNames = template.tags,
                        status = PlannedPaymentStatus.ACTIVE
                    )
                }
            } else {
                val now = LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
                _state.update {
                    it.copy(
                        isEditMode = false,
                        plannedPaymentId = null,
                        startDateMillis = now,
                        endDateMillis = now,
                        selectedAccountId = it.accounts.firstOrNull()?.id ?: ""
                    )
                }
            }
            return
        }

        viewModelScope.launch {
            plannedPaymentRepository.getById(plannedPaymentId)?.let { existing ->
                val template = existing.template
                val schedule = existing.schedule
                _state.update {
                    it.copy(
                        plannedPaymentId = plannedPaymentId,
                        isEditMode = true,
                        existingPayment = existing,
                        linkedTemplate = template,
                        title = template.name,
                        amountText = template.amount.toPlainString(),
                        currency = template.currency ?: "USD",
                        transactionType = template.transactionType,
                        selectedAccountId = template.accountId ?: "",
                        targetAccountId = template.targetAccountId,
                        targetCurrency = template.targetCurrency,
                        transferRate = template.transferRate,
                        recurrenceType = schedule.recurrenceRule.type,
                        recurrenceInterval = schedule.recurrenceRule.interval,
                        dayOfWeek = schedule.recurrenceRule.dayOfWeek,
                        dayOfMonth = schedule.recurrenceRule.dayOfMonth,
                        cronExpression = schedule.recurrenceRule.cronExpression,
                        autoConfirm = existing.autoConfirm,
                        startDateMillis = schedule.startDate,
                        hasEndDate = schedule.endDate != null,
                        endDateMillis = schedule.endDate ?: schedule.startDate,
                        selectedCategoryId = template.categoryId,
                        payee = template.payee ?: "",
                        noteUserPart = template.note ?: "",
                        paymentMethod = template.paymentMethod ?: "Cash",
                        selectedTagNames = template.tags,
                        status = existing.status
                    )
                }
            }
        }
    }

    fun onEvent(event: PlannedPaymentEntryEvent) {
        when (event) {
            is PlannedPaymentEntryEvent.SetTitle -> _state.update { it.copy(title = event.title) }
            is PlannedPaymentEntryEvent.SetAmountText -> _state.update { it.copy(amountText = event.amount) }
            is PlannedPaymentEntryEvent.SetCurrency -> _state.update { it.copy(currency = event.currency) }
            is PlannedPaymentEntryEvent.SetTransactionType -> _state.update { it.copy(transactionType = event.type) }
            is PlannedPaymentEntryEvent.SetAccountId -> {
                _state.update { state ->
                    val account = state.accounts.find { it.id == event.id }
                    state.copy(
                        selectedAccountId = event.id,
                        currency = if (state.currency.isBlank() || state.currency == "USD") account?.currencyCode ?: state.currency else state.currency
                    )
                }
            }
            is PlannedPaymentEntryEvent.SetTargetAccountId -> {
                _state.update { state ->
                    val account = state.accounts.find { it.id == event.id }
                    state.copy(
                        targetAccountId = event.id,
                        targetCurrency = account?.currencyCode ?: state.targetCurrency
                    )
                }
            }
            is PlannedPaymentEntryEvent.SetTargetCurrency -> _state.update { it.copy(targetCurrency = event.currency) }
            is PlannedPaymentEntryEvent.SetTransferRate -> _state.update { it.copy(transferRate = event.rate) }
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
            is PlannedPaymentEntryEvent.SetCategoryId -> _state.update { it.copy(selectedCategoryId = event.id) }
            is PlannedPaymentEntryEvent.SetPayee -> _state.update { it.copy(payee = event.payee) }
            is PlannedPaymentEntryEvent.SetNote -> _state.update { it.copy(noteUserPart = event.note) }
            is PlannedPaymentEntryEvent.SetPaymentMethod -> _state.update { it.copy(paymentMethod = event.method) }
            is PlannedPaymentEntryEvent.SetTags -> _state.update { it.copy(selectedTagNames = event.tags) }
            is PlannedPaymentEntryEvent.SetStatus -> _state.update { it.copy(status = event.status) }
            is PlannedPaymentEntryEvent.SetLinkedTemplate -> {
                _state.update { state ->
                    if (event.template != null) {
                        state.copy(
                            linkedTemplate = event.template,
                            title = event.template.name,
                            amountText = event.template.amount.toPlainString(),
                            currency = event.template.currency ?: "USD",
                            transactionType = event.template.transactionType,
                            selectedAccountId = event.template.accountId ?: "",
                            targetAccountId = event.template.targetAccountId,
                            targetCurrency = event.template.targetCurrency,
                            transferRate = event.template.transferRate,
                            selectedCategoryId = event.template.categoryId,
                            payee = event.template.payee ?: "",
                            noteUserPart = event.template.note ?: "",
                            paymentMethod = event.template.paymentMethod ?: "Cash",
                            selectedTagNames = event.template.tags
                        )
                    } else {
                        state.copy(linkedTemplate = null)
                    }
                }
            }
            is PlannedPaymentEntryEvent.SetDeleteTemplateWithPayment -> _state.update { it.copy(deleteTemplateWithPayment = event.delete) }
            
            is PlannedPaymentEntryEvent.ShowScheduleBuilder -> _state.update { it.copy(showScheduleBuilder = event.show) }
            is PlannedPaymentEntryEvent.ShowEndDatePicker -> _state.update { it.copy(showEndDatePicker = event.show) }
            is PlannedPaymentEntryEvent.ShowCategorySheet -> _state.update { it.copy(showCategorySheet = event.show) }
            is PlannedPaymentEntryEvent.ShowAccountSheet -> _state.update { it.copy(showAccountSheet = event.show) }
            is PlannedPaymentEntryEvent.ShowTargetAccountSheet -> _state.update { it.copy(showTargetAccountSheet = event.show) }
            is PlannedPaymentEntryEvent.ShowPaymentMethodSheet -> _state.update { it.copy(showPaymentMethodSheet = event.show) }
            is PlannedPaymentEntryEvent.ShowTagSheet -> _state.update { it.copy(showTagSheet = event.show) }
            is PlannedPaymentEntryEvent.ShowStatusSheet -> _state.update { it.copy(showStatusSheet = event.show) }
            is PlannedPaymentEntryEvent.ShowNumpad -> _state.update { it.copy(showNumpad = event.show) }
            is PlannedPaymentEntryEvent.ShowCurrencySheet -> _state.update { it.copy(showCurrencySheet = event.show) }
            is PlannedPaymentEntryEvent.ShowTargetCurrencySheet -> _state.update { it.copy(showTargetCurrencySheet = event.show) }
            is PlannedPaymentEntryEvent.ShowTemplateSheet -> _state.update { it.copy(showTemplateSheet = event.show) }
            is PlannedPaymentEntryEvent.ShowExchangeRateDialog -> _state.update { it.copy(showExchangeRateDialog = event.show) }
            is PlannedPaymentEntryEvent.ShowDeleteDialog -> _state.update { it.copy(showDeleteDialog = event.show) }
            is PlannedPaymentEntryEvent.TriggerNavigation -> _state.update { it.copy(navTriggered = true) }
        }
    }

    fun savePlannedPayment(onSuccess: () -> Unit) {
        val s = _state.value
        if (!s.canSave) return

        viewModelScope.launch {
            val amount = s.amountText.toBigDecimalOrNull() ?: BigDecimal.ZERO
            val rule = RecurrenceRule(
                type = s.recurrenceType,
                interval = s.recurrenceInterval.coerceAtLeast(1),
                dayOfWeek = s.dayOfWeek,
                dayOfMonth = s.dayOfMonth,
                cronExpression = s.cronExpression
            )
            val resolvedAccountId = s.selectedAccountId.ifBlank {
                s.accounts.firstOrNull()?.id ?: ""
            }
            
            val templateToSave = (s.linkedTemplate ?: Template(
                name = s.title,
                categoryId = s.selectedCategoryId,
                amount = amount,
                transactionType = s.transactionType,
                accountId = resolvedAccountId,
                targetAccountId = s.targetAccountId,
                targetCurrency = s.targetCurrency,
                transferRate = s.transferRate,
                currency = s.currency,
                payee = s.payee,
                note = s.noteUserPart,
                paymentMethod = s.paymentMethod,
                tags = s.selectedTagNames,
                profileId = s.currentProfileId
            )).copy(
                name = s.title,
                categoryId = s.selectedCategoryId,
                amount = amount,
                transactionType = s.transactionType,
                accountId = resolvedAccountId,
                targetAccountId = s.targetAccountId,
                targetCurrency = s.targetCurrency,
                transferRate = s.transferRate,
                currency = s.currency,
                payee = s.payee,
                note = s.noteUserPart,
                paymentMethod = s.paymentMethod,
                tags = s.selectedTagNames
            )

            val finalTemplateId = if (templateToSave.id == 0L) {
                templateRepository.insertTemplate(templateToSave)
            } else {
                templateRepository.updateTemplate(templateToSave)
                templateToSave.id
            }
            val finalTemplate = templateToSave.copy(id = finalTemplateId)

            val payment = (s.existingPayment ?: PlannedPayment(
                id = 0,
                templateId = finalTemplateId,
                template = finalTemplate,
                schedule = PaymentSchedule(
                    recurrenceRule = rule,
                    startDate = s.startDateMillis,
                    endDate = if (s.hasEndDate) s.endDateMillis else null,
                    nextDueDate = s.startDateMillis
                ),
                profileId = s.currentProfileId
            )).copy(
                id = s.plannedPaymentId ?: 0L,
                templateId = finalTemplateId,
                template = finalTemplate,
                schedule = PaymentSchedule(
                    recurrenceRule = rule,
                    startDate = s.startDateMillis,
                    endDate = if (s.hasEndDate) s.endDateMillis else null,
                    nextDueDate = s.existingPayment?.schedule?.nextDueDate ?: s.startDateMillis,
                    lastTriggeredDate = s.existingPayment?.schedule?.lastTriggeredDate
                ),
                autoConfirm = s.autoConfirm,
                status = s.status,
                profileId = s.currentProfileId
            )

            if (s.isEditMode) {
                plannedPaymentRepository.update(payment)
            } else {
                plannedPaymentRepository.insert(payment)
            }
            onSuccess()
        }
    }

    fun deletePlannedPayment(onSuccess: () -> Unit) {
        val id = _state.value.plannedPaymentId ?: return
        val deleteTemplate = _state.value.deleteTemplateWithPayment
        viewModelScope.launch {
            if (deleteTemplate) {
                plannedPaymentRepository.getById(id)?.let { payment ->
                    templateRepository.getTemplateById(payment.templateId)?.let { template ->
                        templateRepository.deleteTemplate(template)
                    }
                }
            }
            plannedPaymentRepository.delete(id)
            onSuccess()
        }
    }
}
