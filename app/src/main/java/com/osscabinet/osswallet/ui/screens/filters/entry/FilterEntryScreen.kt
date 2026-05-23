package com.osscabinet.ossWallet.ui.screens.filters.entry

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.ui.components.entry.AeonEntryScaffold
import com.osscabinet.ossWallet.ui.screens.filters.entry.components.FilterFormBody
import kotlinx.serialization.json.Json

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterEntryScreen(
    filterId: Long? = null,
    initialFilterJson: String? = null,
    viewModel: FilterEntryViewModel,
    onBackClick: () -> Unit
) {
    LaunchedEffect(filterId, initialFilterJson) {
        val initialFilter = initialFilterJson?.let {
            try {
                Json.decodeFromString<TransactionFilter>(java.net.URLDecoder.decode(it, "UTF-8"))
            } catch (e: Exception) {
                null
            }
        }
        viewModel.initialize(filterId, initialFilter)
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
            onSearch = viewModel::searchTransactions,
            onEvent = viewModel::onEvent,
            modifier = Modifier.padding(innerPadding)
        )
    }
}
