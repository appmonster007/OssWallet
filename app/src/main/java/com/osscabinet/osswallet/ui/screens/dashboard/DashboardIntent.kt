package com.osscabinet.ossWallet.ui.screens.dashboard

import com.osscabinet.ossWallet.domain.model.TransactionStatus
import com.osscabinet.ossWallet.ui.navigation.DashboardViewContext

sealed class DashboardIntent {
    data class InitializeView(val viewContext: DashboardViewContext) : DashboardIntent()
    data object ResetToInitialView : DashboardIntent()
    data object ClearViewFocus : DashboardIntent()
    data class QuickConfirmPlannedPayment(val plannedPaymentId: Long, val scheduledDate: Long) :
        DashboardIntent()

    data class QuickSkipPlannedPayment(val plannedPaymentId: Long, val scheduledDate: Long) :
        DashboardIntent()

    data object UndoLastPlannedPaymentAction : DashboardIntent()
    data object ClearUndoAction : DashboardIntent()

    // Dashboard UI State Consolidation (Story 2.2)
    data class OnCardExpanded(val cardId: String) : DashboardIntent()
    data class OnCardCollapsed(val cardId: String) : DashboardIntent()
    data object ClearSnackbar : DashboardIntent()
    data object ClearScrollTo : DashboardIntent()
    data object CloseLinkedPopup : DashboardIntent()
    data class HandleJumpRequest(val transactionId: Long, val availableIds: Set<Long>? = null) :
        DashboardIntent()

    data class PerformJump(val transactionId: Long) : DashboardIntent()
    data class ToggleStatusClearedUncleared(val transactionId: Long) : DashboardIntent()

    // Story 2.2 Patch: Consolidated UI Intents
    data class QuickDeleteTransaction(val transactionId: Long) : DashboardIntent()
    data class QuickCloneTransaction(val transactionId: Long) : DashboardIntent()
    data class AddTag(val name: String, val color: Int) : DashboardIntent()
    data class ChangeTransactionStatus(val transactionId: Long, val status: TransactionStatus) :
        DashboardIntent()
}
