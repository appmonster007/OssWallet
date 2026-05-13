package com.osscabinet.ossWallet.ui.screens.filters.entry

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.ui.components.entry.AeonEntryScaffold
import com.osscabinet.ossWallet.ui.components.transactions.CompactTextField
import com.osscabinet.ossWallet.ui.components.transactions.FilterFields

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

    LaunchedEffect(state.navTriggered) {
        if (state.navTriggered) {
            onBackClick()
        }
    }

    AeonEntryScaffold(
        title = if (state.isEditMode) "Edit Filter" else "New Filter",
        onBackClick = onBackClick,
        onSaveClick = {
            viewModel.saveFilter {
                viewModel.onEvent(FilterEntryEvent.TriggerNavigation)
            }
        },
        canSave = state.canSave,
        isEditMode = state.isEditMode,
        onDeleteClick = if (state.isEditMode) {
            {
                viewModel.deleteFilter {
                    onBackClick()
                }
            }
        } else null
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Filter Name field
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "FILTER NAME",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                CompactTextField(
                    value = state.name,
                    onValueChange = { viewModel.onEvent(FilterEntryEvent.SetName(it)) },
                    placeholder = "Enter filter name...",
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Shared Filter Fields
            FilterFields(
                filter = state.transactionFilter,
                onFilterChange = { viewModel.onEvent(FilterEntryEvent.SetTransactionFilter(it)) },
                accounts = state.accounts,
                allCategories = state.categories
            )
        }
    }
}
