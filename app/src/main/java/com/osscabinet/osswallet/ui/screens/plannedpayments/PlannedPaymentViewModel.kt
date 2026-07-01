package com.osscabinet.ossWallet.ui.screens.plannedpayments

import com.osscabinet.ossWallet.domain.model.EntryMode
import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.model.OccurrenceStatus
import com.osscabinet.ossWallet.domain.model.PaymentMethod
import com.osscabinet.ossWallet.domain.model.PlannedPayment
import com.osscabinet.ossWallet.domain.model.PlannedPaymentOccurrence
import com.osscabinet.ossWallet.domain.model.PlannedPaymentStatus
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.dto.TransactionRequest
import com.osscabinet.ossWallet.domain.repository.CategoryRepository
import com.osscabinet.ossWallet.domain.repository.PlannedPaymentRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.repository.TemplateRepository
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import jakarta.inject.Inject

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
            val categories = categoryRepository.getAllCategoriesSync()
            val categoryName =
                categories.find { it.id == payment.categoryId }?.name ?: "Planned Payment"

            val request = TransactionRequest(
                name = categoryName,
                amount = payment.amount,
                entryMode = payment.entryMode,
                accountId = payment.accountId.takeIf { !it.isNullOrBlank() } ?: return@launch,
                currency = payment.currency,
                categoryId = payment.categoryId,
                tags = payment.tags,
                paymentMethod = payment.paymentMethod ?: PaymentMethod.CASH,
                timestamp = System.currentTimeMillis(),
                payee = payment.payee ?: "",
                note = payment.note ?: "",
                gpsLocation = payment.gpsLocation,
                warrantyExpiration = payment.warrantyExpiration,
                status = payment.transactionStatus,
                targetAccountId = payment.targetAccountId,
                targetCurrency = payment.targetCurrency,
                transferRate = payment.transferRate
            )
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
            val categories = categoryRepository.getAllCategoriesSync()
            val categoryName = categories.find { it.id == payment.categoryId }?.name ?: "Planned Payment"
            val request = TransactionRequest(
                name = categoryName,
                amount = payment.amount,
                entryMode = payment.entryMode,
                accountId = payment.accountId.takeIf { !it.isNullOrBlank() } ?: return@launch,
                currency = payment.currency,
                categoryId = payment.categoryId,
                tags = payment.tags,
                paymentMethod = payment.paymentMethod ?: PaymentMethod.CASH,
                timestamp = System.currentTimeMillis(),
                payee = payment.payee ?: "",
                note = payment.note ?: "",
                gpsLocation = payment.gpsLocation,
                warrantyExpiration = payment.warrantyExpiration,
                status = payment.transactionStatus,
                targetAccountId = payment.targetAccountId,
                targetCurrency = payment.targetCurrency,
                transferRate = payment.transferRate
            )
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
