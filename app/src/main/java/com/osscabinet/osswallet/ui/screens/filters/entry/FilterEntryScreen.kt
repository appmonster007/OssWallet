package com.osscabinet.ossWallet.ui.screens.filters.entry

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.ui.components.entry.AeonEntryScaffold
import com.osscabinet.ossWallet.ui.screens.filters.entry.components.FilterFormBody

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterEntryScreen(
    filterId: Long? = null,
    viewModel: FilterEntryViewModel,
    onBackClick: () -> Unit
) {
    LaunchedEffect(filterId) {
        viewModel.initialize(filterId)
    }

    val state by viewModel.state.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(state.navTriggered) {
        if (state.navTriggered) {
            onBackClick()
        }
    }

    AeonEntryScaffold(
        title = if (state.isEditMode) "Edit Filter" else "New Filter",
        onBackClick = onBackClick,
        onSaveClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            viewModel.saveFilter {
                viewModel.onEvent(FilterEntryEvent.TriggerNavigation)
            }
        },
        onResetClick = {
            viewModel.onEvent(FilterEntryEvent.ResetFilter)
        },
        canSave = state.canSave && !state.navTriggered,
        isEditMode = state.isEditMode,
        onDeleteClick = if (state.isEditMode) {
            {
                viewModel.deleteFilter()
            }
        } else null
    ) { innerPadding ->
        FilterFormBody(
            state = state,
            onEvent = viewModel::onEvent,
            modifier = Modifier.padding(innerPadding)
        )
    }
}
