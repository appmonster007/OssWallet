package com.osscabinet.ossWallet.domain.model.dto

import com.osscabinet.ossWallet.domain.model.EntryMode
import com.osscabinet.ossWallet.domain.model.PaymentMethod
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import java.math.BigDecimal

/**
 * Data Transfer Object for transaction creation and updates.
 *
 * `entryMode` is the form-level intent (Expense / Income / Transfer); the row-level
 * [com.osscabinet.ossWallet.domain.model.TransactionType] is stamped by the save handler
 * — for transfers, each leg gets `TRANSFER_INFLOW` or `TRANSFER_OUTFLOW` based on its side.
 */
data class TransactionRequest(
    val name: String,
    val amount: BigDecimal,
    val entryMode: EntryMode,
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
    val targetAccountId: String? = null,
    val targetCurrency: String? = null,
    val transferRate: BigDecimal = BigDecimal.ONE
) {
    val isTransfer: Boolean get() = entryMode == EntryMode.TRANSFER
}
