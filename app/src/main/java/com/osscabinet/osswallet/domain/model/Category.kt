package com.osscabinet.ossWallet.domain.model

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
    INITIAL_BALANCE(1003L, "Initial Balance", "AccountBalanceWallet", -7829368, 1000L),
    TRANSFER_INFLOW(1004L, "Transfer Inflow", "CallReceived", -7829368, 1000L),
    TRANSFER_OUTFLOW(1005L, "Transfer Outflow", "CallMade", -7829368, 1000L);
}

data class Category(
    val id: Long,
    val name: String,
    val iconName: String,
    val colorHex: Int,
    val isDefault: Boolean,
    val parentId: Long?,
    val nature: CategoryNature = CategoryNature.WANT
) {
    companion object {
        val SYSTEM_OTHERS           = SystemCategory.OTHERS.id
        val SYSTEM_MISSING          = SystemCategory.MISSING.id
        val SYSTEM_UNKNOWN          = SystemCategory.UNKNOWN.id
        val SYSTEM_TRANSFER_INFLOW  = SystemCategory.TRANSFER_INFLOW.id
        val SYSTEM_TRANSFER_OUTFLOW = SystemCategory.TRANSFER_OUTFLOW.id
        val SYSTEM_INITIAL_BALANCE  = SystemCategory.INITIAL_BALANCE.id

        fun isSystemCategory(id: Long) = SystemCategory.entries.any { it.id == id }
        fun isTransfer(id: Long) = id == SYSTEM_TRANSFER_INFLOW || id == SYSTEM_TRANSFER_OUTFLOW
    }
}
