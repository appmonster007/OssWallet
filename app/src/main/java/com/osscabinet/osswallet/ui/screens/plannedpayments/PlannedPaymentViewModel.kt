package com.osscabinet.ossWallet.ui.screens.plannedpayments

import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.model.OccurrenceStatus
import com.osscabinet.ossWallet.domain.model.PaymentSchedule
import com.osscabinet.ossWallet.domain.model.PlannedPayment
import com.osscabinet.ossWallet.domain.model.PlannedPaymentOccurrence
import com.osscabinet.ossWallet.domain.model.PlannedPaymentStatus
import com.osscabinet.ossWallet.domain.model.RecurrenceRule
import com.osscabinet.ossWallet.domain.model.RecurrenceType
import com.osscabinet.ossWallet.domain.model.Template
import com.osscabinet.ossWallet.domain.model.dto.TransactionRequest
import com.osscabinet.ossWallet.domain.repository.PlannedPaymentRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.repository.TemplateRepository
import com.osscabinet.ossWallet.domain.service.management.PlannedPaymentService
import com.osscabinet.ossWallet.domain.service.management.TransactionManager
import com.osscabinet.ossWallet.ui.util.BaseViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

class PlannedPaymentViewModel(
    private val plannedPaymentRepository: PlannedPaymentRepository,
    private val templateRepository: TemplateRepository,
    settingsRepository: SettingsRepository
) : BaseViewModel(settingsRepository) {

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentProfileId: StateFlow<Int> = settingsRepository.currentProfileId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    @OptIn(ExperimentalCoroutinesApi::class)
    val plannedPayments: StateFlow<List<PlannedPayment>> = settingsRepository.currentProfileId
        .flatMapLatest { profileId ->
            plannedPaymentRepository.getAllPlannedPayments(profileId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTemplates: StateFlow<List<Template>> = templateRepository.getAllTemplates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    fun delete(paymentId: Long, deleteTemplate: Boolean = false) {
        viewModelScope.launch {
            val payment = plannedPaymentRepository.getById(paymentId)
            if (payment != null) {
                plannedPaymentRepository.delete(paymentId)
                if (deleteTemplate) {
                    templateRepository.deleteTemplate(payment.template)
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
        transactionManager: TransactionManager,
        profileId: Int
    ) {
        viewModelScope.launch {
            val template = payment.template
            val request = TransactionRequest(
                title = template.name,
                amount = template.amount,
                transactionType = when(template.transactionType) {
                    1 -> "income"
                    2 -> "transfer"
                    else -> "expense"
                },
                accountId = template.accountId ?: "",
                currency = template.currency ?: "USD",
                categoryId = template.categoryId,
                tags = template.tags,
                paymentMethod = template.paymentMethod ?: "Cash",
                timestamp = occurrence.scheduledDate,
                payee = template.payee ?: "",
                note = template.note ?: "",
                isTransfer = template.transactionType == 2,
                targetAccountId = template.targetAccountId,
                targetCurrency = template.targetCurrency,
                transferRate = template.transferRate
            )
            val transactionId = transactionManager.handleSaveTransaction(null, request, profileId)
            plannedPaymentRepository.upsertOccurrence(
                occurrence.copy(
                    status = OccurrenceStatus.CONFIRMED,
                    generatedTransactionId = transactionId
                )
            )
            val nextDue =
                PlannedPaymentService.calculateNextDueDate(payment, occurrence.scheduledDate)
            if (nextDue == null) {
                plannedPaymentRepository.update(payment.copy(status = PlannedPaymentStatus.COMPLETED))
            } else {
                plannedPaymentRepository.update(
                    payment.copy(
                        schedule = payment.schedule.copy(
                            nextDueDate = nextDue,
                            lastTriggeredDate = occurrence.scheduledDate
                        )
                    )
                )
            }
        }
    }

    fun skipOccurrence(occurrence: PlannedPaymentOccurrence, payment: PlannedPayment) {
        viewModelScope.launch {
            plannedPaymentRepository.upsertOccurrence(occurrence.copy(status = OccurrenceStatus.SKIPPED))
            val nextDue =
                PlannedPaymentService.calculateNextDueDate(payment, occurrence.scheduledDate)
            if (nextDue == null) {
                plannedPaymentRepository.update(payment.copy(status = PlannedPaymentStatus.COMPLETED))
            } else {
                plannedPaymentRepository.update(
                    payment.copy(
                        schedule = payment.schedule.copy(
                            nextDueDate = nextDue,
                            lastTriggeredDate = occurrence.scheduledDate
                        )
                    )
                )
            }
        }
    }

    /** Confirm a payment directly from the list view (no pre-loaded occurrence needed). */
    fun quickConfirm(
        payment: PlannedPayment,
        transactionManager: TransactionManager,
        profileId: Int
    ) {
        val occurrence = PlannedPaymentOccurrence(
            id = 0, plannedPaymentId = payment.id, scheduledDate = payment.schedule.nextDueDate
        )
        confirmOccurrence(occurrence, payment, transactionManager, profileId)
    }

    /** Skip the current instance of a payment directly from the list view. */
    fun quickSkip(payment: PlannedPayment) {
        val occurrence = PlannedPaymentOccurrence(
            id = 0, plannedPaymentId = payment.id, scheduledDate = payment.schedule.nextDueDate
        )
        skipOccurrence(occurrence, payment)
    }

    fun getOccurrencesForPayment(paymentId: Long): Flow<List<PlannedPaymentOccurrence>> =
        plannedPaymentRepository.getOccurrences(paymentId)

    fun buildDefaultOccurrence(
        payment: PlannedPayment,
        scheduledDate: Long
    ): PlannedPaymentOccurrence =
        PlannedPaymentOccurrence(
            id = 0,
            plannedPaymentId = payment.id,
            scheduledDate = scheduledDate
        )

}
