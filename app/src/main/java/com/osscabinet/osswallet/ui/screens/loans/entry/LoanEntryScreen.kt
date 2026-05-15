package com.osscabinet.ossWallet.ui.screens.loans.entry

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.ui.components.entry.AeonEntryScaffold
import com.osscabinet.ossWallet.ui.screens.loans.entry.components.LoanFormBody

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanEntryScreen(
    loanId: Long? = null,
    viewModel: LoanEntryViewModel,
    onBackClick: () -> Unit
) {
    LaunchedEffect(loanId) {
        viewModel.initialize(loanId)
    }

    val state by viewModel.state.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(state.navTriggered) {
        if (state.navTriggered) {
            onBackClick()
        }
    }

    AeonEntryScaffold(
        title = if (state.isEditMode) "Edit Loan" else "New Loan",
        onBackClick = onBackClick,
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
            onEvent = viewModel::onEvent,
            modifier = Modifier.padding(innerPadding)
        )
    }
}
