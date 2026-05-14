package com.osscabinet.ossWallet.ui.screens.filters.entry

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.ui.components.AeonSelectMode
import com.osscabinet.ossWallet.ui.components.entry.*
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
        onResetClick = {
            viewModel.onEvent(FilterEntryEvent.ResetFilter)
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
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            // IMPACT: Header card (Filter Name)
            AeonImpactHeaderCard(
                transactionType = state.transactionFilter.transactionType ?: com.osscabinet.ossWallet.domain.model.TransactionType.EXPENSE,
                onTypeSelected = { type ->
                    viewModel.onEvent(FilterEntryEvent.SetTransactionFilter(state.transactionFilter.copy(transactionType = type)))
                },
                amount = "",
                currency = "",
                onCurrencyClick = { },
                onAmountClick = { },
                name = state.name,
                onNameChange = { viewModel.onEvent(FilterEntryEvent.SetName(it)) },
                nameLabel = "FILTER NAME",
                showTypeSelector = true,
                showAmount = false,
                showCurrency = false
            )

            // CONTEXT: Action cards grid
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "CONTEXT",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )
                
                AeonContextGrid {
                    // Accounts
                    EntryActionCard(
                        icon = Icons.Default.AccountBalance,
                        label = "ACCOUNTS",
                        value = if (state.transactionFilter.accountIds.isEmpty()) "All" else "${state.transactionFilter.accountIds.size} Selected",
                        onClick = { /* This will be handled by FilterFields for now or a separate sheet */ },
                        modifier = Modifier.weight(1f)
                    )

                    // Categories
                    EntryActionCard(
                        icon = Icons.Default.Category,
                        label = "CATEGORIES",
                        value = if (state.transactionFilter.categoryIds.isEmpty()) "All" else "${state.transactionFilter.categoryIds.size} Selected",
                        onClick = { /* This will be handled by FilterFields for now or a separate sheet */ },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // DETAIL: Full Filter Capabilities
            FilterFields(
                filter = state.transactionFilter,
                onFilterChange = { viewModel.onEvent(FilterEntryEvent.SetTransactionFilter(it)) },
                accounts = state.accounts,
                allCategories = state.categories,
                allTransactions = state.allTransactions
            )
        }
    }
}
