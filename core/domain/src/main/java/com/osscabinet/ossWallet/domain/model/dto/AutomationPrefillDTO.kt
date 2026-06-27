package com.osscabinet.ossWallet.domain.model.dto

import com.osscabinet.ossWallet.domain.model.EntryMode
import com.osscabinet.ossWallet.domain.model.*
import java.math.BigDecimal

/**
 * Data for pre-filling the Template creation screen from a smart suggestion.
 */
data class TemplatePrefillDTO(
    val name: String,
    val categoryId: Long,
    val amount: BigDecimal,
    val entryMode: EntryMode,
    val accountId: String?,
    val note: String?,
    val payee: String?,
    val location: String?,
    val gpsLocation: String? = null,
    val warrantyExpiration: Long? = null,
    val status: TransactionStatus = TransactionStatus.CLEARED,
    val paymentMethod: PaymentMethod? = null,
    val currency: String?,
    val tags: Set<String>
)

/**
 * Data for pre-filling the Planned Payment creation screen from a smart suggestion.
 */
data class PlannedPaymentPrefillDTO(
    val template: Template,
    val schedule: PaymentSchedule
)
