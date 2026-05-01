package com.osscabinet.ossWallet.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.domain.model.PlannedPayment
import com.osscabinet.ossWallet.domain.model.PlannedPaymentStatus
import com.osscabinet.ossWallet.domain.model.RecurrenceRule
import com.osscabinet.ossWallet.domain.model.RecurrenceType
import com.osscabinet.ossWallet.domain.repository.PlannedPaymentRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneOffset

class PlannedPaymentViewModel(
    private val plannedPaymentRepository: PlannedPaymentRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

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
            categoryId = null,
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
