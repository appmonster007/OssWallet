package com.osscabinet.ossWallet.ui.screens.budgets.entry

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.ui.components.entry.AeonEntryScaffold
import com.osscabinet.ossWallet.ui.screens.budgets.entry.components.BudgetFormBody

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetEntryScreen(
    budgetId: Long? = null,
    viewModel: BudgetEntryViewModel,
    onBackClick: () -> Unit
) {
    LaunchedEffect(budgetId) {
        viewModel.initialize(budgetId)
    }

    val state by viewModel.state.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(state.navTriggered) {
        if (state.navTriggered) {
            onBackClick()
        }
    }

    AeonEntryScaffold(
        title = if (state.isEditMode) "Edit Budget" else "New Budget",
        onBackClick = onBackClick,
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
            onEvent = viewModel::onEvent,
            modifier = Modifier.padding(innerPadding)
        )
    }
}
