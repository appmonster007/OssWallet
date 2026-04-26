package com.osscabinet.ossWallet.domain.model.dto

import com.osscabinet.ossWallet.domain.model.TransactionStatus
import java.math.BigDecimal

/**
 * Data Transfer Object for transaction creation and updates.
 * Groups recurring parameters to optimize function signatures and improve maintainability.
 */
data class TransactionRequest(
    val title: String,
    val amount: BigDecimal,
    val category: String,
    val transactionType: String,
    val accountId: String,
    val currency: String,
    val categoryId: Long? = null,
    val tags: List<String> = emptyList(),
    val paymentMethod: String = "Cash",
    val timestamp: Long = System.currentTimeMillis(),
    val payee: String = "",
    val note: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val warrantyExpiration: Long? = null,
    val status: TransactionStatus = TransactionStatus.CLEARED,
    
    // Transfer-specific fields
    val isTransfer: Boolean = false,
    val targetAccountId: String? = null,
    val targetCurrency: String? = null,
    val transferRate: BigDecimal = BigDecimal.ONE
)
