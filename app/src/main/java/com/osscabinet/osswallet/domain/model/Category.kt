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
    INITIAL_BALANCE_INFLOW(1003L, "Initial Balance", "CallReceived", -7829368, 1000L),
    INITIAL_BALANCE_OUTFLOW(1004L, "Initial Balance", "CallMade", -7829368, 1000L),
    TRANSFER_INFLOW(1005L, "Transfer Inflow", "CallReceived", -7829368, 1000L),
    TRANSFER_OUTFLOW(1006L, "Transfer Outflow", "CallMade", -7829368, 1000L);
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
        val SYSTEM_OTHERS                  = SystemCategory.OTHERS.id
        val SYSTEM_MISSING                 = SystemCategory.MISSING.id
        val SYSTEM_UNKNOWN                 = SystemCategory.UNKNOWN.id
        val SYSTEM_INITIAL_BALANCE_INFLOW  = SystemCategory.INITIAL_BALANCE_INFLOW.id   // 1003
        val SYSTEM_INITIAL_BALANCE_OUTFLOW = SystemCategory.INITIAL_BALANCE_OUTFLOW.id  // 1004
        val SYSTEM_TRANSFER_INFLOW         = SystemCategory.TRANSFER_INFLOW.id          // 1005
        val SYSTEM_TRANSFER_OUTFLOW        = SystemCategory.TRANSFER_OUTFLOW.id         // 1006
        val INITIAL_BALANCE_CATEGORY_IDS   = listOf(SYSTEM_INITIAL_BALANCE_INFLOW, SYSTEM_INITIAL_BALANCE_OUTFLOW)

        fun isSystemCategory(id: Long) = SystemCategory.entries.any { it.id == id }
        fun isInitialBalance(id: Long) = id == SYSTEM_INITIAL_BALANCE_INFLOW || id == SYSTEM_INITIAL_BALANCE_OUTFLOW
        fun isTransfer(id: Long) = id == SYSTEM_TRANSFER_INFLOW || id == SYSTEM_TRANSFER_OUTFLOW
    }
}
