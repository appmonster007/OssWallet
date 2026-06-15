package com.osscabinet.ossWallet.domain.model

import com.osscabinet.ossWallet.domain.model.EntryMode
import kotlinx.serialization.Serializable

/**
 * Enum representing the type of a transaction leg.
 *
 * Each row in the `transactions` table holds exactly one of these values.
 * Transfers are split into `TRANSFER_INFLOW` / `TRANSFER_OUTFLOW` so the
 * "direction" of a transfer leg is encoded in the type itself, not in the
 * amount sign. Use [EntryMode] for event-level form/template state where the
 * leg distinction is not yet known.
 */
@Serializable
enum class TransactionType(
    val displayName: String,
    val value: String,
    val iconName: String,
    val colorHex: String
) {
    EXPENSE(
        displayName = "Expense",
        value = "expense",
        iconName = "Remove",
        colorHex = "#B3261E"
    ),
    INCOME(
        displayName = "Income",
        value = "income",
        iconName = "Add",
        colorHex = "#004D40"
    ),
    TRANSFER_INFLOW(
        displayName = "Transfer In",
        value = "transfer_inflow",
        iconName = "ArrowDownward",
        colorHex = "#2196F3"
    ),
    TRANSFER_OUTFLOW(
        displayName = "Transfer Out",
        value = "transfer_outflow",
        iconName = "ArrowUpward",
        colorHex = "#2196F3"
    );

    companion object {
        val TRANSFER_TYPES: Set<TransactionType> = setOf(TRANSFER_INFLOW, TRANSFER_OUTFLOW)

        /**
         * Resolve from the persisted string value. Defaults to EXPENSE when unrecognised.
         * Legacy "transfer" is intentionally NOT accepted here — the schema migration
         * 9 → 10 backfills existing rows by sign; any caller still seeing the literal
         * "transfer" string after migration is a bug worth surfacing.
         */
        fun fromValue(value: String?): TransactionType =
            entries.find { it.value.equals(value, ignoreCase = true) } ?: EXPENSE

        /**
         * Lossy mapping retained only for the integer-encoded legacy CSV path:
         * 0 → EXPENSE, 1 → INCOME, 2 → TRANSFER_OUTFLOW (sign unknown; default to outflow).
         */
        fun fromInt(type: Int): TransactionType = when (type) {
            1 -> INCOME
            2 -> TRANSFER_OUTFLOW
            else -> EXPENSE
        }

        const val ALL = "all"
    }

    /**
     * Lossy: both transfer subtypes map to 2.
     */
    fun toInt(): Int = when (this) {
        EXPENSE -> 0
        INCOME -> 1
        TRANSFER_INFLOW, TRANSFER_OUTFLOW -> 2
    }
}

val TransactionType.isTransfer: Boolean
    get() = this in TransactionType.TRANSFER_TYPES

enum class AmountEditingSide { SOURCE, TARGET }
