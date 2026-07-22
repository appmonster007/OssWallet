package com.osscabinet.ossWallet.domain.model

import kotlinx.serialization.Serializable
@Serializable
enum class AccountStructureType(val displayName: String, val colorHex: String, val iconKey: String) {
    BANK("Bank", "#2196F3", "AccountBalance"),
    SAVINGS("Savings", "#4CAF50", "Savings"),
    CASH("Cash", "#8BC34A", "Payments"),
    DIGITAL_WALLET("Digital Wallet", "#00BCD4", "AccountBalanceWallet"),
    CREDIT("Credit", "#F44336", "CreditCard"),
    INVESTMENT("Investment", "#FF9800", "TrendingUp"),
    CRYPTO("Crypto", "#FFC107", "CurrencyBitcoin"),
    OTHER("Other", "#9E9E9E", "Layers");

    companion object {
        fun fromString(value: String): AccountStructureType {
            return entries.find {
                it.name.equals(value, ignoreCase = true) ||
                        it.displayName.equals(value, ignoreCase = true)
            } ?: BANK
        }
    }
}
