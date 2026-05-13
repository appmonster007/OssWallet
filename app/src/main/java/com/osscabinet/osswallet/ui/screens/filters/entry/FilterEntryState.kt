package com.osscabinet.ossWallet.ui.screens.filters.entry

import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Filter
import com.osscabinet.ossWallet.domain.model.TransactionFilter

data class FilterEntryState(
    val name: String = "",
    val transactionFilter: TransactionFilter = TransactionFilter(),
    
    // UI State
    val isEditMode: Boolean = false,
    val filterId: Long? = null,
    val navTriggered: Boolean = false,
    
    // External data
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val existingFilter: Filter? = null
) {
    val canSave: Boolean get() = name.isNotBlank() && !navTriggered
}
