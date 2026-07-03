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
    INITIAL_BALANCE(1003L, "Initial Balance", "AccountBalance", -7829368, 1000L),
    TRANSFER(1005L, "Transfer", "SwapHoriz", -7829368, 1000L);
}

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
        val SYSTEM_OTHERS          = SystemCategory.OTHERS.id
        val SYSTEM_MISSING         = SystemCategory.MISSING.id
        val SYSTEM_UNKNOWN         = SystemCategory.UNKNOWN.id
        val SYSTEM_INITIAL_BALANCE = SystemCategory.INITIAL_BALANCE.id // 1003
        val SYSTEM_TRANSFER        = SystemCategory.TRANSFER.id        // 1005

        fun isSystemCategory(id: Long) = SystemCategory.entries.any { it.id == id }
        fun isInitialBalance(id: Long) = id == SYSTEM_INITIAL_BALANCE
        fun isTransfer(id: Long) = id == SYSTEM_TRANSFER || isInitialBalance(id)
    }
}
