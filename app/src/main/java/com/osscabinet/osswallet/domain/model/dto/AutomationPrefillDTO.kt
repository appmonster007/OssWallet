package com.osscabinet.ossWallet.domain.model.dto

import com.osscabinet.ossWallet.domain.model.PaymentSchedule
import com.osscabinet.ossWallet.domain.model.Template
import java.math.BigDecimal

/**
 * Data for pre-filling the Template creation screen from a smart suggestion.
 */
data class TemplatePrefillDTO(
    val name: String,
    val categoryId: Long,
    val amount: BigDecimal,
    val transactionType: Int, // 0: Expense, 1: Income
    val accountId: String?,
    val note: String?,
    val payee: String?,
    val location: String?,
    val paymentMethod: String?,
    val currency: String?,
    val tags: List<String>
)

/**
 * Data for pre-filling the Planned Payment creation screen from a smart suggestion.
 */
data class PlannedPaymentPrefillDTO(
    val template: Template,
    val schedule: PaymentSchedule
)
