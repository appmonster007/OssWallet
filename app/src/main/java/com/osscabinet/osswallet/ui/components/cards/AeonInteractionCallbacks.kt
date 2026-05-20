package com.osscabinet.ossWallet.ui.components.cards

import com.osscabinet.ossWallet.domain.model.TransactionStatus
import com.osscabinet.ossWallet.domain.model.TransactionFilter

/**
 * Unified callbacks for Aeon UI components (cards, screens, sheets).
 * Groups parameters to reduce signature bloat.
 */
data class AeonInteractionCallbacks(
    val transactionActions: TransactionActions = TransactionActions(),
    val accountActions: AccountActions = AccountActions(),
    val paymentActions: PaymentActions = PaymentActions(),
    val filterActions: FilterActions = FilterActions(),
    val tagActions: TagActions = TagActions()
)

data class TransactionActions(
    val onClick: (Long) -> Unit = {},
    val onItemExpanded: (Long) -> Unit = {},
    val onItemCollapsed: (Long) -> Unit = {},
    val onStatusChange: (Long, TransactionStatus) -> Unit = { _, _ -> },
    val onClone: (Long) -> Unit = {},
    val onDelete: (Long) -> Unit = {},
    val onSplit: (Long) -> Unit = {},
    val onFullSwipeRight: (Long) -> Unit = {}
)

data class AccountActions(
    val onManageClick: () -> Unit = {},
    val onClick: (String) -> Unit = {}
)

data class PaymentActions(
    val onConfirm: (Long) -> Unit = {},
    val onSkip: (Long) -> Unit = {}
)

data class FilterActions(
    val onSaveClick: (TransactionFilter) -> Unit = {}
)

data class TagActions(
    val onAdd: (String, Int) -> Unit = { _, _ -> }
)
