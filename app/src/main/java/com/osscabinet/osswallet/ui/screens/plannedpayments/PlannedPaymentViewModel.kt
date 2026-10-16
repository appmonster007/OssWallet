package com.osscabinet.ossWallet.ui.screens.plannedpayments

import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.model.OccurrenceStatus
import com.osscabinet.ossWallet.domain.model.PaymentMethod
import com.osscabinet.ossWallet.domain.model.PlannedPayment
import com.osscabinet.ossWallet.domain.model.PlannedPaymentOccurrence
import com.osscabinet.ossWallet.domain.model.PlannedPaymentStatus
import com.osscabinet.ossWallet.domain.model.UpcomingPaymentItem
import com.osscabinet.ossWallet.domain.model.dto.TransactionRequest
import com.osscabinet.ossWallet.domain.repository.CategoryRepository
import com.osscabinet.ossWallet.domain.repository.PlannedPaymentRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.repository.TemplateRepository
import com.osscabinet.ossWallet.domain.service.management.ConfirmResult
import com.osscabinet.ossWallet.domain.service.management.PlannedPaymentConfirmer
import com.osscabinet.ossWallet.domain.service.management.PlannedPaymentService
import com.osscabinet.ossWallet.domain.service.management.TransactionEntryService
import com.osscabinet.ossWallet.domain.service.management.TransactionManager
import com.osscabinet.ossWallet.ui.util.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
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

data class PlannedPaymentListUiState(
    val overdue: List<PlannedPayment> = emptyList(),
    val upcoming: List<UpcomingPaymentItem> = emptyList(),
    val scheduled: List<PlannedPayment> = emptyList(),
    val paused: List<PlannedPayment> = emptyList()
) {
    val isEmpty: Boolean get() = overdue.isEmpty() && upcoming.isEmpty() && scheduled.isEmpty() && paused.isEmpty()
}

sealed class UndoAction {
    data class SkipUndo(
        val paymentId: Long,
        val scheduledDate: Long,
        val prevLastTriggeredDate: Long?
    ) : UndoAction()

    data class ConfirmUndo(
        val paymentId: Long,
        val scheduledDate: Long,
        val transactionId: Long,
        val prevLastTriggeredDate: Long?
    ) : UndoAction()
}

@HiltViewModel
class PlannedPaymentViewModel @Inject constructor(
    private val plannedPaymentRepository: PlannedPaymentRepository,
    private val templateRepository: TemplateRepository,
    private val plannedPaymentConfirmer: PlannedPaymentConfirmer,
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
            val upcomingNonOverdue =
                upcoming.filter { !it.isOverdue }.distinctBy { it.plannedPayment.id }
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

    fun getPaymentById(id: Long): Flow<PlannedPayment?> =
        plannedPaymentRepository.getPaymentById(id)

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
            plannedPaymentConfirmer.confirm(payment, occurrence.scheduledDate, profileId)
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
            val confirmed = plannedPaymentConfirmer.confirm(payment, scheduledDate, profileId)
                as? ConfirmResult.Confirmed ?: return@launch
            val transactionId = confirmed.transactionId
            _lastUndoAction.value = UndoAction.ConfirmUndo(
                paymentId = payment.id,
                scheduledDate = scheduledDate,
                transactionId = transactionId,
                prevLastTriggeredDate = payment.schedule.lastTriggeredDate
            )
        }
    }

    /** Skip the current instance of a payment directly from the list view. */
    fun quickSkip(payment: PlannedPayment) {
        quickSkipDate(payment, payment.schedule.nextDueDate)
    }

    /** Skip a payment for a specific scheduledDate (used when the occurrence date differs from nextDueDate). */
    fun quickSkipDate(payment: PlannedPayment, scheduledDate: Long) {
        viewModelScope.launch {
            transactionManager.skipPlannedPayment(
                paymentId = payment.id,
                scheduledDate = scheduledDate
            )
            _lastUndoAction.value = UndoAction.SkipUndo(
                paymentId = payment.id,
                scheduledDate = scheduledDate,
                prevLastTriggeredDate = payment.schedule.lastTriggeredDate
            )
        }
    }

    fun undoLastAction() {
        val action = _lastUndoAction.value ?: return
        _lastUndoAction.value = null
        viewModelScope.launch {
            when (action) {
                is UndoAction.SkipUndo -> {
                    val occ = plannedPaymentRepository.getOccurrenceForDate(
                        action.paymentId,
                        action.scheduledDate
                    )
                    if (occ != null) plannedPaymentRepository.updateOccurrenceStatus(
                        occ.id,
                        OccurrenceStatus.PENDING
                    )
                    val payment =
                        plannedPaymentRepository.getById(action.paymentId) ?: return@launch
                    plannedPaymentRepository.update(
                        payment.copy(
                            schedule = payment.schedule.copy(
                                nextDueDate = action.scheduledDate,
                                lastTriggeredDate = action.prevLastTriggeredDate
                            )
                        )
                    )
                }

                is UndoAction.ConfirmUndo -> {
                    withProfile { profileId ->
                        transactionManager.deleteTransaction(action.transactionId, profileId)
                    }
                    val occ = plannedPaymentRepository.getOccurrenceForDate(
                        action.paymentId,
                        action.scheduledDate
                    )
                    if (occ != null) plannedPaymentRepository.updateOccurrenceStatus(
                        occ.id,
                        OccurrenceStatus.PENDING
                    )
                    val payment =
                        plannedPaymentRepository.getById(action.paymentId) ?: return@launch
                    plannedPaymentRepository.update(
                        payment.copy(
                            schedule = payment.schedule.copy(
                                nextDueDate = action.scheduledDate,
                                lastTriggeredDate = action.prevLastTriggeredDate
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
