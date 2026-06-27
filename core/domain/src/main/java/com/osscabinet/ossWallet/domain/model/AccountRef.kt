package com.osscabinet.ossWallet.domain.model

/**
 * Closed set of states the account-selection slot of a [TransactionSide] can be in.
 *
 * Replaces the previous stringly-typed `accountId: String?` which overloaded three meanings
 * (`null` = not selected, `""` = Outside Wallet, non-blank = real account id) and caused
 * subtle load-path bugs when DB rows storing `null` for the outside-wallet side were
 * decoded as "not selected" instead of "Outside".
 */
sealed interface AccountRef {
    /** No selection yet. Only valid during creation / pre-fill, never persisted to DB as a wallet. */
    data object Unselected : AccountRef

    /** External counterparty in a transfer. Renders as [Account.OUTSIDE_WALLET]. */
    data object Outside : AccountRef

    /** A real wallet selected by id. Invariant: id is non-blank. */
    @JvmInline value class Wallet(val id: String) : AccountRef {
        init { require(id.isNotBlank()) { "AccountRef.Wallet.id must be non-blank" } }
    }

    companion object {
        /**
         * Decode the in-memory storage convention used by form state and pre-fill DTOs:
         * `null` => Unselected, `""` => Outside, non-blank => Wallet.
         */
        fun fromStored(id: String?): AccountRef = when {
            id == null   -> Unselected
            id.isBlank() -> Outside
            else         -> Wallet(id)
        }

        /**
         * Decode a DB row's account column for a *transfer* side. The persistence layer
         * stores the outside-wallet side as `null`, so a missing id here means Outside,
         * not Unselected.
         */
        fun fromTransferSide(id: String?): AccountRef = when {
            id == null   -> Outside
            id.isBlank() -> Outside
            else         -> Wallet(id)
        }
    }
}

/**
 * Encode for in-memory / DTO transport. Mirrors [AccountRef.fromStored]: Outside => `""`.
 */
fun AccountRef.toStored(): String? = when (this) {
    AccountRef.Unselected -> null
    AccountRef.Outside    -> ""
    is AccountRef.Wallet  -> id
}

/**
 * Encode for the DB transfer convention. Outside is persisted as `null` (not `""`).
 */
fun AccountRef.toDbTransferSide(): String? = when (this) {
    AccountRef.Unselected -> null
    AccountRef.Outside    -> null
    is AccountRef.Wallet  -> id
}

/** Resolve to a renderable [Account]. */
fun AccountRef.resolve(accounts: List<Account>): Account? = when (this) {
    AccountRef.Unselected -> null
    AccountRef.Outside    -> Account.OUTSIDE_WALLET
    is AccountRef.Wallet  -> accounts.find { it.id == id }
}

val AccountRef.isPicked: Boolean get() = this !is AccountRef.Unselected

val AccountRef.isOutside: Boolean get() = this is AccountRef.Outside
