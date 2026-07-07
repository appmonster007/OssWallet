package com.osscabinet.ossWallet.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class EntryMode(val displayName: String, val value: String) {
    EXPENSE(displayName = "Expense", value = "expense"),
    INCOME(displayName = "Income", value = "income"),
    TRANSFER(displayName = "Transfer", value = "transfer");

    companion object {
        fun fromValue(value: String?): EntryMode =
            entries.find { it.value.equals(value, ignoreCase = true) } ?: EXPENSE
    }
}

fun EntryMode.toTransactionTypeOrNull(): TransactionType? = when (this) {
    EntryMode.EXPENSE -> TransactionType.EXPENSE
    EntryMode.INCOME -> TransactionType.INCOME
    EntryMode.TRANSFER -> null
}

fun TransactionType.toEntryMode(): EntryMode = when (this) {
    TransactionType.EXPENSE -> EntryMode.EXPENSE
    TransactionType.INCOME -> EntryMode.INCOME
    TransactionType.TRANSFER_INFLOW, TransactionType.TRANSFER_OUTFLOW -> EntryMode.TRANSFER
}
