package com.osscabinet.ossWallet.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class SystemCategory(
    val id: Long,
    val displayName: String,
    val iconName: String,
    val colorHex: Int,
    val parentId: Long?,
    val nature: CategoryNature = CategoryNature.NEED
) {
    OTHERS(1000L, "Others", "Menu", -7829368, null),
    MISSING(1001L, "Missing", "MoreHoriz", -7829368, 1000L),
    UNKNOWN(1002L, "Unknown", "QuestionMark", -7829368, 1000L),
    INITIAL_BALANCE(1003L, "Initial Balance", "AccountBalance", -7829368, 1000L),
    RECONCILIATION_ADJUSTMENT(1004L, "Reconciliation Adjustment", "Sync", -7829368, 1000L),
    TRANSFER(1005L, "Transfer", "SwapHoriz", -7829368, 1000L);
}

@Serializable
data class Category(
    val id: Long,
    val name: String,
    val iconName: String,
    val colorHex: Int,
    val isDefault: Boolean,
    val parentId: Long?,
    val nature: CategoryNature = CategoryNature.WANT,
    val profileId: Int = 1
) {
    companion object {
        val SYSTEM_OTHERS = SystemCategory.OTHERS.id
        val SYSTEM_MISSING = SystemCategory.MISSING.id
        val SYSTEM_UNKNOWN = SystemCategory.UNKNOWN.id
        val SYSTEM_INITIAL_BALANCE = SystemCategory.INITIAL_BALANCE.id // 1003
        val SYSTEM_RECONCILIATION_ADJUSTMENT = SystemCategory.RECONCILIATION_ADJUSTMENT.id // 1004
        val SYSTEM_TRANSFER = SystemCategory.TRANSFER.id        // 1005

        fun isSystemCategory(id: Long) = SystemCategory.entries.any { it.id == id }
        fun isInitialBalance(id: Long) = id == SYSTEM_INITIAL_BALANCE
        fun isTransfer(id: Long) =
            id == SYSTEM_TRANSFER || isInitialBalance(id) || id == SYSTEM_RECONCILIATION_ADJUSTMENT

        /** System-managed categories: never offered in the picker for ordinary transactions. */
        fun isSystemManaged(id: Long) = isInitialBalance(id) || id == SYSTEM_RECONCILIATION_ADJUSTMENT

        /**
         * Entry modes a row in this category may be saved as. Opening balances and reconciliation
         * adjustments are transfer legs against Outside Wallet - they affect the account balance
         * but must never count as real income/expense in reports and budgets. The only way to turn
         * one into real income/expense is to reassign it to an ordinary category first, which lifts
         * this restriction since allowedEntryModes is keyed on the (possibly just-changed) category.
         */
        fun allowedEntryModes(id: Long): Set<EntryMode> = when {
            isSystemManaged(id) -> setOf(EntryMode.TRANSFER)
            else -> EntryMode.entries.toSet()
        }

        // Ids below this are permanently reserved for system + seeded default categories
        // (currently 1000-1157). User-created categories always start at this floor, so
        // the app can add or remove default categories in a future release without ever
        // colliding with an id a user is already using.
        const val USER_CATEGORY_ID_FLOOR = 2000L
    }
}
