package com.osscabinet.ossWallet.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.model.OccurrenceStatus
import com.osscabinet.ossWallet.domain.model.PlannedPayment
import com.osscabinet.ossWallet.domain.model.PlannedPaymentOccurrence
import com.osscabinet.ossWallet.domain.model.PlannedPaymentStatus
import com.osscabinet.ossWallet.domain.model.RecurrenceRule
import com.osscabinet.ossWallet.domain.model.RecurrenceType
import com.osscabinet.ossWallet.domain.model.dto.TransactionRequest
import com.osscabinet.ossWallet.domain.repository.PlannedPaymentRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.service.management.PlannedPaymentService
import com.osscabinet.ossWallet.domain.service.management.TransactionManager
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

class PlannedPaymentViewModel(
    private val plannedPaymentRepository: PlannedPaymentRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentProfileId: StateFlow<Int> = settingsRepository.currentProfileId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    @OptIn(ExperimentalCoroutinesApi::class)
    val plannedPayments: StateFlow<List<PlannedPayment>> = settingsRepository.currentProfileId
        .flatMapLatest { profileId ->
            plannedPaymentRepository.getAllPlannedPayments(profileId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun save(payment: PlannedPayment) {
        viewModelScope.launch {
            if (payment.id == 0L) {
                plannedPaymentRepository.insert(payment)
            } else {
                plannedPaymentRepository.update(payment)
            }
        }
    }

    fun delete(paymentId: Long) {
        viewModelScope.launch {
            plannedPaymentRepository.delete(paymentId)
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
            val request = TransactionRequest(
                title = payment.title,
                amount = payment.amount,
                transactionType = payment.transactionType,
                accountId = payment.accountId,
                currency = payment.currency,
                categoryId = payment.categoryId,
                tags = payment.tags,
                paymentMethod = payment.paymentMethod,
                timestamp = occurrence.scheduledDate,
                payee = payment.payee,
                note = payment.note
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
                        nextDueDate = nextDue,
                        lastTriggeredDate = occurrence.scheduledDate
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
                        nextDueDate = nextDue,
                        lastTriggeredDate = occurrence.scheduledDate
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
            id = 0, plannedPaymentId = payment.id, scheduledDate = payment.nextDueDate
        )
        confirmOccurrence(occurrence, payment, transactionManager, profileId)
    }

    /** Skip the current instance of a payment directly from the list view. */
    fun quickSkip(payment: PlannedPayment) {
        val occurrence = PlannedPaymentOccurrence(
            id = 0, plannedPaymentId = payment.id, scheduledDate = payment.nextDueDate
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

    fun buildDefault(accountId: String, profileId: Int): PlannedPayment {
        val now = LocalDate.now(ZoneOffset.UTC)
        val startEpoch = now.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        return PlannedPayment(
            id = 0L,
            title = "",
            amount = BigDecimal.ZERO,
            currency = "USD",
            transactionType = "expense",
            accountId = accountId,
            targetAccountId = null,
            categoryId = 1002L,
            paymentMethod = "Cash",
            tags = emptyList(),
            note = "",
            payee = "",
            recurrenceRule = RecurrenceRule(type = RecurrenceType.MONTHLY),
            startDate = startEpoch,
            endDate = null,
            nextDueDate = startEpoch,
            lastTriggeredDate = null,
            autoConfirm = false,
            status = PlannedPaymentStatus.ACTIVE,
            profileId = profileId
        )
    }
}
