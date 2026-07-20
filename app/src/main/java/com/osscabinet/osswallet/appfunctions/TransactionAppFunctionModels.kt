package com.osscabinet.ossWallet.appfunctions

import androidx.appfunctions.AppFunctionSerializable

@AppFunctionSerializable
data class CreateTransactionParams(
    /** Entry mode: "expense", "income", or "transfer" */
    val entryMode: String,
    /** Amount as a decimal string, e.g. "42.50" */
    val amount: String,
    /** Account name. Must match an existing account. Required for expense and income. */
    val accountName: String? = null,
    /** Category name. Defaults to "Others" if omitted or not found. */
    val categoryName: String? = null,
    /** ISO 4217 currency code, e.g. "USD". Defaults to the account's currency. */
    val currency: String? = null,
    /** Display name for the transaction. Falls back to category name if omitted. */
    val name: String? = null,
    /** Note or memo */
    val note: String? = null,
    /** Payee name */
    val payee: String? = null,
    /** ISO-8601 date, e.g. "2026-07-20". Defaults to today. */
    val date: String? = null,
    /** Comma-separated tag names, e.g. "food,work" */
    val tags: String? = null,
    /** Payment method: "cash", "credit_card", "debit_card", "bank_transfer", "digital_wallet", "mobile_payment", "check", "cryptocurrency", "other" */
    val paymentMethod: String? = null,
    /** Status: "cleared", "uncleared", "reconcile" */
    val status: String? = null,
    /** Target account name for transfers. Null or blank means outside-of-wallet. */
    val targetAccountName: String? = null,
    /** Target currency for cross-currency transfers. Defaults to target account's currency. */
    val targetCurrency: String? = null,
    /** Exchange rate as a decimal string, e.g. "1.25". Defaults to 1. */
    val transferRate: String? = null
)

@AppFunctionSerializable
data class GetTransactionsParams(
    /** Filter by type: "expense", "income", "transfer", or null for all */
    val type: String? = null,
    /** Filter by account name, or null for all accounts */
    val accountName: String? = null,
    /** Filter by category name, or null for all categories */
    val categoryName: String? = null,
    /** Time period: "today", "this_week", "this_month", "this_year", or null for all time */
    val timePeriod: String? = null,
    /** Maximum number of results. Defaults to 50. */
    val limit: Int = 50
)

@AppFunctionSerializable
data class UpdateTransactionParams(
    /** ID of the transaction to update */
    val transactionId: Long,
    /** New entry mode. Null to keep existing. */
    val entryMode: String? = null,
    /** New amount as a decimal string. Null to keep existing. */
    val amount: String? = null,
    /** New account name. Null to keep existing. */
    val accountName: String? = null,
    /** New category name. Null to keep existing. */
    val categoryName: String? = null,
    /** New currency code. Null to keep existing. */
    val currency: String? = null,
    /** New display name. Null to keep existing. */
    val name: String? = null,
    /** New note. Null to keep existing. */
    val note: String? = null,
    /** New payee. Null to keep existing. */
    val payee: String? = null,
    /** New date in ISO-8601 format. Null to keep existing. */
    val date: String? = null,
    /** New comma-separated tags. Null to keep existing. */
    val tags: String? = null,
    /** New payment method. Null to keep existing. */
    val paymentMethod: String? = null,
    /** New status. Null to keep existing. */
    val status: String? = null,
    /** New target account name for transfers. Null to keep existing. */
    val targetAccountName: String? = null,
    /** New target currency for transfers. Null to keep existing. */
    val targetCurrency: String? = null,
    /** New transfer rate. Null to keep existing. */
    val transferRate: String? = null
)

@AppFunctionSerializable
data class DeleteByIdParams(
    /** ID of the record to delete */
    val id: Long
)

@AppFunctionSerializable
data class TransactionResult(
    val success: Boolean,
    val errorMessage: String? = null,
    val transactionId: Long = -1L,
    val name: String? = null,
    val amount: String? = null,
    val currency: String? = null,
    val type: String? = null,
    val accountName: String? = null,
    val categoryName: String? = null,
    val date: String? = null,
    val note: String? = null,
    val payee: String? = null
)

@AppFunctionSerializable
data class TransactionListResult(
    val success: Boolean,
    val errorMessage: String? = null,
    val transactions: List<TransactionResult> = emptyList(),
    val totalCount: Int = 0
)

@AppFunctionSerializable
data class SuccessResult(
    val success: Boolean,
    val errorMessage: String? = null
)
