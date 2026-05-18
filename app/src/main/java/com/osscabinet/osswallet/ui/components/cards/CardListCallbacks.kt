package com.osscabinet.ossWallet.ui.components.cards

import com.osscabinet.ossWallet.domain.model.TransactionStatus
import com.osscabinet.ossWallet.domain.model.TransactionFilter

data class CardListCallbacks(
    val onTransactionClick: (Long) -> Unit = {},
    val onManageAccountsClick: () -> Unit = {},
    val onAccountClick: (String) -> Unit = {},
    val onPlannedPaymentConfirm: ((Long) -> Unit)? = null,
    val onPlannedPaymentSkip: ((Long) -> Unit)? = null,
    val onItemExpanded: (Long) -> Unit = {},
    val onItemCollapsed: (Long) -> Unit = {},
    val onStatusChange: (Long, TransactionStatus) -> Unit = { _, _ -> },
    val onClone: (Long) -> Unit = {},
    val onDeleteTransaction: (Long) -> Unit = {},
    val onSplit: (Long) -> Unit = {},
    val onFullSwipeRight: (Long) -> Unit = {},
    val onSaveFilterClick: (TransactionFilter) -> Unit = {},
)
