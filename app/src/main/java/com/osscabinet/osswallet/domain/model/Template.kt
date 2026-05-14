package com.osscabinet.ossWallet.domain.model

import java.math.BigDecimal

data class Template(
    val id: Long = 0,
    val name: String,
    val categoryId: Long,
    val amount: BigDecimal,
    val transactionType: Int, // 0: Expense, 1: Income, 2: Transfer
    val icon: String? = null,
    val colorHex: String? = null,
    val accountId: String?,
    val targetAccountId: String? = null,
    val targetCurrency: String? = null,
    val transferRate: BigDecimal = BigDecimal.ONE,
    val note: String? = null,
    val payee: String? = null,
    val location: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val warrantyExpiration: Long? = null,
    val transactionStatus: TransactionStatus = TransactionStatus.CLEARED,
    val paymentMethod: PaymentMethod? = null,
    val currency: String,
    val tags: List<String> = emptyList(),
    val isStarred: Boolean = false,
    val displayOrder: Int = 0,
    val profileId: Int = 1
)
