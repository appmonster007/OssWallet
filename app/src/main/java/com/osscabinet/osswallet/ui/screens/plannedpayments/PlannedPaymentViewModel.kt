package com.osscabinet.ossWallet.ui.screens.plannedpayments

import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.model.*
import com.osscabinet.ossWallet.domain.model.dto.TransactionRequest
import com.osscabinet.ossWallet.domain.repository.CategoryRepository
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

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

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
            val categoryName = categories.find { it.id == payment.categoryId }?.name ?: "Planned Payment"
            
            val request = TransactionRequest(
                name = categoryName,
                amount = payment.amount,
                transactionType = payment.transactionType,
                accountId = payment.accountId ?: "",
                currency = payment.currency,
                categoryId = payment.categoryId,
                tags = payment.tags,
                paymentMethod = payment.paymentMethod ?: PaymentMethod.CASH,
                timestamp = occurrence.scheduledDate,
                payee = payment.payee ?: "",
                note = payment.note ?: "",
                gpsLocation = payment.gpsLocation,
                warrantyExpiration = payment.warrantyExpiration,
                status = payment.transactionStatus,
                isTransfer = payment.transactionType == TransactionType.TRANSFER,
                targetAccountId = payment.targetAccountId,
                targetCurrency = payment.targetCurrency,
                transferRate = payment.transferRate
            )
            val transactionId = transactionManager.handleSaveTransaction(null, request, profileId)
            transactionManager.confirmPlannedPaymentTransaction(
                paymentId = payment.id,
                transactionId = transactionId,
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
    fun quickConfirm(
        payment: PlannedPayment,
        profileId: Int
    ) {
        val occurrence = PlannedPaymentOccurrence(
            id = 0, plannedPaymentId = payment.id, scheduledDate = payment.schedule.nextDueDate
        )
        confirmOccurrence(occurrence, payment, profileId)
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
}
