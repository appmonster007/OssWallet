package com.osscabinet.ossWallet.domain.model

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
        const val SYSTEM_TRANSFER = 1003L
        const val SYSTEM_UNKNOWN = 1002L
        const val SYSTEM_INITIAL_BALANCE = 1004L
    }
}
