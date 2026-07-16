package com.osscabinet.ossWallet.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Account(
    val id: String,
    val name: String,
    val currencyCode: String,
    val structureType: AccountStructureType,
    val colorHex: String,
    val icon: String,
    val allowedCurrencies: List<String> = emptyList(),
    val isArchived: Boolean = false,
    val displayOrder: Int = 0
) {
    companion object {
        // Sentinel representing the external counterparty in a transfer (id = "" by convention).
        val OUTSIDE_WALLET = Account(
            id = "",
            name = "Outside of Wallet",
            currencyCode = "",
            structureType = AccountStructureType.OTHER,
            colorHex = "#607D8B",
            icon = "Public"
        )
    }
}
