package com.osscabinet.ossWallet.domain.model

data class Account(
    val id: String,
    val name: String,
    val balance: Double,
    val initialBalance: Double = 0.0,
    val currencyCode: String,
    val structureType: String,
    val colorHex: String,
    val icon: String,
    val allowedCurrencies: List<String> = emptyList(),
    val isArchived: Boolean = false,
    val displayOrder: Int = 0
)
