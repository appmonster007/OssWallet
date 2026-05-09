package com.osscabinet.ossWallet.domain.model

import java.math.BigDecimal

data class Template(
    val id: Long,
    val name: String,
    val categoryId: Long,
    val amount: BigDecimal,
    val transactionType: Int,
    val icon: String?,
    val colorHex: String?,
    val accountId: String?,
    val note: String?,
    val payee: String?,
    val location: String?,
    val paymentMethod: String?,
    val currency: String?,
    val tags: List<String>,
    val isStarred: Boolean,
    val displayOrder: Int
)
