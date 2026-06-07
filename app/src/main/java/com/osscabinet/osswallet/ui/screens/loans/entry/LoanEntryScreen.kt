package com.osscabinet.ossWallet.ui.screens.loans.entry

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.ui.components.entry.AeonEntryScaffold
import com.osscabinet.ossWallet.ui.screens.loans.entry.components.LoanFormBody

/**
 * A Value Object representing a valid configuration for the Loan Entry screen.
 */
data class LoanEntryConfig(
    val loanId: Long? = null
)

/**
 * Grouped actions for the Loan Entry screen.
 */
data class LoanEntryActions(
    val onBackClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanEntryScreen(
    config: LoanEntryConfig,
    actions: LoanEntryActions,
    viewModel: LoanEntryViewModel
) {
    LaunchedEffect(config.loanId) {
        viewModel.initialize(config.loanId)
    }

    val state by viewModel.state.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(state.navTriggered) {
        if (state.navTriggered) {
            actions.onBackClick()
        }
    }

    AeonEntryScaffold(
        title = if (state.isEditMode) "Edit Loan" else "New Loan",
        onBackClick = actions.onBackClick,
        onSaveClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            viewModel.saveLoan {
                viewModel.onEvent(LoanEntryEvent.TriggerNavigation)
            }
        },
        canSave = state.canSave && !state.navTriggered,
        isEditMode = state.isEditMode,
        onDeleteClick = if (state.isEditMode) {
            { viewModel.onEvent(LoanEntryEvent.ShowDeleteDialog(true)) }
        } else null
    ) { innerPadding ->
        LoanFormBody(
            state = state,
            onSearch = viewModel::searchTransactions,
            onEvent = viewModel::onEvent,
            contentPadding = innerPadding
        )
    }
}
