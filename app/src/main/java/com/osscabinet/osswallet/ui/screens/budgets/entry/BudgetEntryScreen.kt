package com.osscabinet.ossWallet.ui.screens.budgets.entry

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.ui.components.entry.AeonEntryScaffold
import com.osscabinet.ossWallet.ui.screens.budgets.entry.components.BudgetFormBody

/**
 * A Value Object representing a valid configuration for the Budget Entry screen.
 */
data class BudgetEntryConfig(
    val budgetId: Long? = null
)

/**
 * Grouped actions for the Budget Entry screen.
 */
data class BudgetEntryActions(
    val onBackClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetEntryScreen(
    config: BudgetEntryConfig,
    actions: BudgetEntryActions,
    viewModel: BudgetEntryViewModel
) {
    LaunchedEffect(config.budgetId) {
        viewModel.initialize(config.budgetId)
    }

    val state by viewModel.state.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(state.navTriggered) {
        if (state.navTriggered) {
            actions.onBackClick()
        }
    }

    AeonEntryScaffold(
        title = if (state.isEditMode) "Edit Budget" else "New Budget",
        onBackClick = actions.onBackClick,
        onSaveClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            viewModel.saveBudget {
                viewModel.onEvent(BudgetEntryEvent.TriggerNavigation)
            }
        },
        canSave = state.canSave && !state.navTriggered,
        isEditMode = state.isEditMode,
        onDeleteClick = if (state.isEditMode) {
            { viewModel.onEvent(BudgetEntryEvent.ShowDeleteDialog(true)) }
        } else null
    ) { innerPadding ->
        BudgetFormBody(
            state = state,
            onSearch = viewModel::searchTransactions,
            onEvent = viewModel::onEvent,
            modifier = Modifier.padding(innerPadding)
        )
    }
}
