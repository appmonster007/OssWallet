package com.osscabinet.ossWallet.ui.screens.plannedpayments

import com.osscabinet.ossWallet.domain.model.EntryMode
import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.model.OccurrenceStatus
import com.osscabinet.ossWallet.domain.model.PaymentMethod
import com.osscabinet.ossWallet.domain.model.PlannedPayment
import com.osscabinet.ossWallet.domain.model.PlannedPaymentOccurrence
import com.osscabinet.ossWallet.domain.model.PlannedPaymentStatus
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.UpcomingPaymentItem
import com.osscabinet.ossWallet.domain.model.dto.TransactionRequest
import com.osscabinet.ossWallet.domain.repository.CategoryRepository
import com.osscabinet.ossWallet.domain.repository.PlannedPaymentRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.repository.TemplateRepository
import com.osscabinet.ossWallet.domain.service.management.PlannedPaymentService
import com.osscabinet.ossWallet.domain.service.management.TransactionManager
import com.osscabinet.ossWallet.ui.util.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import jakarta.inject.Inject

data class PlannedPaymentListUiState(
    val overdue: List<PlannedPayment> = emptyList(),
    val upcoming: List<UpcomingPaymentItem> = emptyList(),
    val scheduled: List<PlannedPayment> = emptyList(),
    val paused: List<PlannedPayment> = emptyList()
) {
    val isEmpty: Boolean get() = overdue.isEmpty() && upcoming.isEmpty() && scheduled.isEmpty() && paused.isEmpty()
}

sealed class UndoAction {
    data class SkipUndo(val payment: PlannedPayment, val scheduledDate: Long) : UndoAction()
    data class ConfirmUndo(val payment: PlannedPayment, val scheduledDate: Long, val transactionId: Long) : UndoAction()
}

@HiltViewModel
class PlannedPaymentViewModel @Inject constructor(
    private val plannedPaymentRepository: PlannedPaymentRepository,
    private val templateRepository: TemplateRepository,
    private val categoryRepository: CategoryRepository,
    private val transactionManager: TransactionManager,
    settingsRepository: SettingsRepository,
) : BaseViewModel(settingsRepository) {

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentProfileId: StateFlow<Int> = settingsRepository.currentProfileId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    private val _lastUndoAction = MutableStateFlow<UndoAction?>(null)
    val lastUndoAction: StateFlow<UndoAction?> = _lastUndoAction.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val plannedPayments: StateFlow<List<PlannedPayment>> = settingsRepository.currentProfileId
        .flatMapLatest { profileId ->
            plannedPaymentRepository.getAllPlannedPayments(profileId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val listUiState: StateFlow<PlannedPaymentListUiState> = plannedPayments
        .map { payments ->
            val now = System.currentTimeMillis()
            val windowEnd = now + 30L * 24 * 60 * 60 * 1000
            val overdue = PlannedPaymentService.getOverdue(payments, now)
            val upcoming = PlannedPaymentService.generateUpcoming(
                payments = payments.filter { it.status == PlannedPaymentStatus.ACTIVE },
                fromDate = now,
                daysAhead = 30
            )
            val upcomingNonOverdue = upcoming.filter { !it.isOverdue }.distinctBy { it.plannedPayment.id }
            val upcomingIds = upcomingNonOverdue.map { it.plannedPayment.id }.toHashSet()
            val overdueIds = overdue.map { it.id }.toHashSet()
            val scheduled = payments.filter { p ->
                p.status == PlannedPaymentStatus.ACTIVE &&
                    p.schedule.nextDueDate > windowEnd &&
                    p.id !in upcomingIds &&
                    p.id !in overdueIds
            }
            val paused = payments.filter { it.status == PlannedPaymentStatus.PAUSED }
            PlannedPaymentListUiState(
                overdue = overdue,
                upcoming = upcomingNonOverdue,
                scheduled = scheduled,
                paused = paused
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlannedPaymentListUiState())

    fun delete(paymentId: Long, deleteTemplate: Boolean = false) {
        viewModelScope.launch {
            val payment = plannedPaymentRepository.getById(paymentId)
            if (payment != null) {
                plannedPaymentRepository.delete(paymentId)
                if (deleteTemplate) {
                    payment.templateId?.let { tid ->
                        templateRepository.getTemplateById(tid)?.let { template ->
                            templateRepository.deleteTemplate(template)
                        }
                    }
                }
            }
        }
    }

    fun pause(payment: PlannedPayment) {
        viewModelScope.launch {
            plannedPaymentRepository.update(payment.copy(status = PlannedPaymentStatus.PAUSED))
        }
    }

    fun resume(payment: PlannedPayment) {
        viewModelScope.launch {
            plannedPaymentRepository.update(payment.copy(status = PlannedPaymentStatus.ACTIVE))
        }
    }

    fun confirmOccurrence(
        occurrence: PlannedPaymentOccurrence,
        payment: PlannedPayment,
        profileId: Int
    ) {
        viewModelScope.launch {
            val request = buildTransactionRequest(payment) ?: return@launch
            transactionManager.handleSaveTransaction(
                existingTransaction = null,
                request = request,
                profileId = profileId,
                sourcePlannedPaymentId = payment.id,
                scheduledDate = occurrence.scheduledDate
            )
        }
    }

    fun skipOccurrence(occurrence: PlannedPaymentOccurrence, payment: PlannedPayment) {
        viewModelScope.launch {
            transactionManager.skipPlannedPayment(
                paymentId = payment.id,
                scheduledDate = occurrence.scheduledDate
            )
        }
    }

    /** Confirm a payment directly from the list view (no pre-loaded occurrence needed). */
    fun quickConfirm(payment: PlannedPayment, profileId: Int) {
        quickConfirmDate(payment, payment.schedule.nextDueDate, profileId)
    }

    /** Confirm a payment for a specific scheduledDate (used when the occurrence date differs from nextDueDate). */
    fun quickConfirmDate(payment: PlannedPayment, scheduledDate: Long, profileId: Int) {
        viewModelScope.launch {
            val request = buildTransactionRequest(payment) ?: return@launch
            val transactionId = transactionManager.handleSaveTransaction(
                existingTransaction = null,
                request = request,
                profileId = profileId,
                sourcePlannedPaymentId = payment.id,
                scheduledDate = scheduledDate
            )
            _lastUndoAction.value = UndoAction.ConfirmUndo(payment, scheduledDate, transactionId)
        }
    }

    private suspend fun buildTransactionRequest(payment: PlannedPayment): TransactionRequest? {
        val template = payment.templateId?.let { templateRepository.getTemplateById(it) }

        val amount          = template?.amount          ?: payment.amount
        val entryMode       = template?.entryMode       ?: payment.entryMode
        val accountId       = (template?.accountId     ?: payment.accountId).takeIf { !it.isNullOrBlank() } ?: return null
        val currency        = template?.currency        ?: payment.currency
        val categoryId      = template?.categoryId      ?: payment.categoryId
        val tags            = template?.tags            ?: payment.tags
        val paymentMethod   = template?.paymentMethod   ?: payment.paymentMethod ?: PaymentMethod.CASH
        val payee           = template?.payee           ?: payment.payee ?: ""
        val note            = template?.note            ?: payment.note ?: ""
        val gpsLocation     = template?.gpsLocation     ?: payment.gpsLocation
        val warrantyExp     = template?.warrantyExpiration ?: payment.warrantyExpiration
        val status          = template?.transactionStatus ?: payment.transactionStatus
        val targetAccountId = template?.targetAccountId ?: payment.targetAccountId
        val targetCurrency  = template?.targetCurrency  ?: payment.targetCurrency
        val transferRate    = template?.transferRate     ?: payment.transferRate

        val categoryName = categoryRepository.getAllCategoriesSync()
            .find { it.id == categoryId }?.name ?: "Planned Payment"

        return TransactionRequest(
            name = categoryName,
            amount = amount,
            entryMode = entryMode,
            accountId = accountId,
            currency = currency,
            categoryId = categoryId,
            tags = tags,
            paymentMethod = paymentMethod,
            timestamp = System.currentTimeMillis(),
            payee = payee,
            note = note,
            gpsLocation = gpsLocation,
            warrantyExpiration = warrantyExp,
            status = status,
            targetAccountId = targetAccountId,
            targetCurrency = targetCurrency,
            transferRate = transferRate
        )
    }

    /** Skip the current instance of a payment directly from the list view. */
    fun quickSkip(payment: PlannedPayment) {
        quickSkipDate(payment, payment.schedule.nextDueDate)
    }

    /** Skip a payment for a specific scheduledDate (used when the occurrence date differs from nextDueDate). */
    fun quickSkipDate(payment: PlannedPayment, scheduledDate: Long) {
        viewModelScope.launch {
            transactionManager.skipPlannedPayment(paymentId = payment.id, scheduledDate = scheduledDate)
            _lastUndoAction.value = UndoAction.SkipUndo(payment, scheduledDate)
        }
    }

    fun undoLastAction() {
        val action = _lastUndoAction.value ?: return
        _lastUndoAction.value = null
        viewModelScope.launch {
            when (action) {
                is UndoAction.SkipUndo -> {
                    val occ = plannedPaymentRepository.getOccurrenceForDate(action.payment.id, action.scheduledDate)
                    if (occ != null) {
                        plannedPaymentRepository.updateOccurrenceStatus(occ.id, OccurrenceStatus.PENDING)
                    }
                    plannedPaymentRepository.update(
                        action.payment.copy(
                            schedule = action.payment.schedule.copy(
                                nextDueDate = action.scheduledDate,
                                lastTriggeredDate = action.payment.schedule.lastTriggeredDate
                            )
                        )
                    )
                }
                is UndoAction.ConfirmUndo -> {
                    withProfile { profileId ->
                        transactionManager.deleteTransaction(action.transactionId, profileId)
                    }
                    val occ = plannedPaymentRepository.getOccurrenceForDate(action.payment.id, action.scheduledDate)
                    if (occ != null) {
                        plannedPaymentRepository.updateOccurrenceStatus(occ.id, OccurrenceStatus.PENDING)
                    }
                    plannedPaymentRepository.update(
                        action.payment.copy(
                            schedule = action.payment.schedule.copy(
                                nextDueDate = action.scheduledDate,
                                lastTriggeredDate = action.payment.schedule.lastTriggeredDate
                            )
                        )
                    )
                }
            }
        }
    }

    fun clearUndoAction() {
        _lastUndoAction.value = null
    }

    fun getOccurrencesForPayment(paymentId: Long): Flow<List<PlannedPaymentOccurrence>> =
        plannedPaymentRepository.getOccurrences(paymentId)
}
