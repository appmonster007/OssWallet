package com.osscabinet.ossWallet.data.web.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateAccountRequest(
    val name: String,
    val currencyCode: String,
    val structureType: String = "SAVINGS",
    val colorHex: String = "#4CAF50",
    val icon: String = "account_balance",
    val allowedCurrencies: List<String> = emptyList(),
    val initialBalance: Double? = null
)

@Serializable
data class UpdateAccountRequest(
    val name: String? = null,
    val currencyCode: String? = null,
    val structureType: String? = null,
    val colorHex: String? = null,
    val icon: String? = null,
    val isArchived: Boolean? = null,
    val allowedCurrencies: List<String>? = null
)
