package com.osscabinet.ossWallet.ui.components.cards

import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionStatus

/**
 * Unified callbacks for Aeon UI components (cards, screens, sheets).
 * Groups parameters to reduce signature bloat.
 */
data class AeonInteractionCallbacks(
    val transactionActions: TransactionActions = TransactionActions(),
    val accountActions: AccountActions = AccountActions(),
    val budgetActions: BudgetActions = BudgetActions(),
    val loanActions: LoanActions = LoanActions(),
    val paymentActions: PaymentActions = PaymentActions(),
    val filterActions: FilterActions = FilterActions(),
    val tagActions: TagActions = TagActions()
)

data class TransactionActions(
    val onClick: (Long) -> Unit = {},
    val onItemExpanded: (String) -> Unit = {},
    val onItemCollapsed: (String) -> Unit = {},
    val onStatusChange: (Long, TransactionStatus) -> Unit = { _, _ -> },
    val onClone: (Long) -> Unit = {},
    val onDelete: (Long) -> Unit = {},
    val onSplit: (Long) -> Unit = {},
    val onFullSwipeRight: (Long) -> Unit = {}
)

data class AccountActions(
    val onManageClick: () -> Unit = {},
    val onClick: (String) -> Unit = {},
    val onSeeAll: () -> Unit = {}
)

data class BudgetActions(
    val onClick: (Long) -> Unit = {},
    val onSeeAll: () -> Unit = {}
)

data class LoanActions(
    val onClick: (Long) -> Unit = {},
    val onSeeAll: () -> Unit = {}
)

data class PaymentActions(
    val onClick: (Long) -> Unit = {},
    val onConfirm: (Long) -> Unit = {},
    val onSkip: (Long) -> Unit = {},
    val onSeeAll: () -> Unit = {}
)

data class FilterActions(
    val onSaveClick: (TransactionFilter) -> Unit = {}
)

data class TagActions(
    val onAdd: (String, Int) -> Unit = { _, _ -> }
)
