package com.osscabinet.ossWallet.domain.model.dto

import com.osscabinet.ossWallet.domain.model.RecurrenceRule
import java.math.BigDecimal

/**
 * Data for pre-filling the Template creation screen from a smart suggestion.
 */
data class TemplatePrefillDTO(
    val name: String,
    val category: String,
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
    val title: String,
    val amount: BigDecimal,
    val currency: String,
    val transactionType: String, // "income" or "expense"
    val accountId: String,
    val categoryId: Long?,
    val paymentMethod: String,
    val tags: List<String>,
    val note: String,
    val payee: String,
    val recurrenceRule: RecurrenceRule,
    val startDate: Long
)
