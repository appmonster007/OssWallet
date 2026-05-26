package com.osscabinet.ossWallet.domain.model.dto

import com.osscabinet.ossWallet.domain.model.PaymentMethod
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import com.osscabinet.ossWallet.domain.model.TransactionType
import java.math.BigDecimal

/**
 * Data Transfer Object for transaction creation and updates.
 * Groups recurring parameters to optimize function signatures and improve maintainability.
 */
data class TransactionRequest(
    val name: String,
    val amount: BigDecimal,
    val transactionType: TransactionType,
    val accountId: String?,
    val currency: String,
    val categoryId: Long,
    val tags: Set<String> = emptySet(),
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val timestamp: Long = System.currentTimeMillis(),
    val payee: String = "",
    val note: String = "",
    val gpsLocation: String? = null,
    val location: String? = null,
    val warrantyExpiration: Long? = null,
    val status: TransactionStatus = TransactionStatus.CLEARED,
    val sourcePlannedPaymentId: Long? = null,

    // Transfer-specific fields
    val isTransfer: Boolean = false,
    val targetAccountId: String? = null,
    val targetCurrency: String? = null,
    val transferRate: BigDecimal = BigDecimal.ONE
)
