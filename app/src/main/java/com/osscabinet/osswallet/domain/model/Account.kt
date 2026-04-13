package com.osscabinet.ossWallet.domain.model

import java.math.BigDecimal

data class Account(
    val id: String,
    val name: String,
    val balance: BigDecimal,
    val initialBalance: BigDecimal = BigDecimal.ZERO,
    val currencyCode: String,
    val structureType: String,
    val colorHex: String,
    val icon: String,
    val allowedCurrencies: List<String> = emptyList(),
    val isArchived: Boolean = false,
    val displayOrder: Int = 0
)
