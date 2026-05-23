package com.osscabinet.ossWallet.domain.model

enum class AccountStructureType(val displayName: String) {
    SAVINGS("Savings"),
    CREDIT_CARD("Credit Card"),
    CHECKING("Checking"),
    INVESTMENT("Investment"),
    CASH("Cash"),
    OTHER("Other");

    companion object {
        fun fromString(value: String): AccountStructureType {
            return values().find { it.displayName.equals(value, ignoreCase = true) } ?: SAVINGS
        }
    }
}
