package com.osscabinet.ossWallet.domain.model

enum class AccountStructureType(val displayName: String) {
    BANK("Bank"),
    SAVINGS("Savings"),
    CASH("Cash"),
    DIGITAL_WALLET("Digital Wallet"),
    CREDIT("Credit"),
    INVESTMENT("Investment"),
    CRYPTO("Crypto"),
    OTHER("Other");

    companion object {
        fun fromString(value: String): AccountStructureType {
            return entries.find {
                it.name.equals(value, ignoreCase = true) ||
                        it.displayName.equals(value, ignoreCase = true)
            } ?: BANK
        }
    }
}
