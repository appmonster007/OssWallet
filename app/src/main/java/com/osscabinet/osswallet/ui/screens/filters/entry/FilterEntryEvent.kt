package com.osscabinet.ossWallet.ui.screens.filters.entry

import com.osscabinet.ossWallet.domain.model.TransactionFilter

sealed class FilterEntryEvent {
    data class SetName(val name: String) : FilterEntryEvent()
    data class SetTransactionFilter(val filter: TransactionFilter) : FilterEntryEvent()
    data object TriggerNavigation : FilterEntryEvent()
    data object ResetFilter : FilterEntryEvent()
}
