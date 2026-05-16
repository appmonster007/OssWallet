package com.osscabinet.ossWallet.ui.screens.accounts.entry.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.service.aggregation.AggregationService
import com.osscabinet.ossWallet.ui.components.*
import com.osscabinet.ossWallet.ui.components.entry.*
import com.osscabinet.ossWallet.ui.screens.accounts.entry.AccountEntryState
import com.osscabinet.ossWallet.ui.screens.accounts.entry.AccountEntryEvent
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors

@Composable
fun AccountFormBody(
    state: AccountEntryState,
    onEvent: (AccountEntryEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val accountBalance = remember(state.allTransactions, state.accountId, state.exchangeRates) {
        if (state.isEditMode && state.existingAccount != null && state.accountId != null) {
            AggregationService.calculateAccountBalance(
                accountId = state.accountId,
                allTransactions = state.allTransactions,
                accountCurrency = state.existingAccount.currencyCode,
                exchangeRates = state.exchangeRates
            )
        } else null
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = AeonTheme.dimensions.screenPadding,
                vertical = AeonTheme.dimensions.spacingLg
            ),
        verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingLg)
    ) {
        AeonHeroHeader(
            title = state.accountName.ifBlank { "Untitled" },
            subtitle = if (state.isEditMode && accountBalance != null)
                "${state.primaryCurrency} $accountBalance"
            else if (state.primaryCurrency.isNotBlank())
                "${state.primaryCurrency} ${state.initialBalances[state.primaryCurrency]?.ifBlank { "0.00" } ?: "0.00"}"
            else "0.00",
            accentColorHex = state.accentColorHex,
            iconName = state.selectedIconName
        )

        EntrySection(title = "IDENTITY", icon = Icons.Rounded.Fingerprint) {
            EntryEditorialInput(
                value = state.accountName,
                onValueChange = { onEvent(AccountEntryEvent.SetName(it)) },
                label = "ACCOUNT NAME",
                icon = Icons.AutoMirrored.Rounded.Label
            )

            EntryActionCard(
                icon = Icons.Rounded.Layers,
                label = "STRUCTURE TYPE",
                value = state.structureType,
                onClick = { onEvent(AccountEntryEvent.ShowStructureSheet(true)) },
                iconContainerColor = AppColors.formIconContainerSecondary.copy(alpha = 0.1f),
                iconTint = AppColors.secondary
            )
        }

        // Currency management
        CurrencyManagementSection(
            primaryCurrency = state.primaryCurrency,
            onPrimaryCurrencyChange = { onEvent(AccountEntryEvent.SetPrimaryCurrency(it)) },
            allowedCurrencies = state.allowedCurrencies,
            onAllowedCurrenciesChange = { onEvent(AccountEntryEvent.SetAllowedCurrencies(it)) },
            accentColorHex = state.accentColorHex,
            isEditMode = state.isEditMode,
            currencyUsageMap = state.currencyUsageMap
        )

        // Initial balances (create mode only)
        if (!state.isEditMode && state.allowedCurrencies.isNotEmpty()) {
            EntrySection(title = "INITIAL BALANCES", icon = Icons.Rounded.AccountBalanceWallet) {
                state.allowedCurrencies.forEach { currencyCode ->
                    EntryEditorialInput(
                        value = state.initialBalances[currencyCode] ?: "",
                        onValueChange = { newBalance ->
                            onEvent(AccountEntryEvent.SetInitialBalance(currencyCode, newBalance))
                        },
                        label = "BALANCE ($currencyCode)",
                        icon = Icons.Rounded.AccountBalanceWallet
                    )
                }
            }
        }

        // Visual identity
        VisualIdentitySection(
            selectedIconName = state.selectedIconName,
            onIconSelected = { onEvent(AccountEntryEvent.SetIcon(it)) },
            accentColorHex = state.accentColorHex,
            onColorSelected = { onEvent(AccountEntryEvent.SetColor(it)) }
        )

        Spacer(modifier = Modifier.height(AeonTheme.dimensions.spacing2xl))
    }

    if (state.showStructureSheet) {
        StructureTypeSelection(
            selectedType = state.structureType,
            onTypeSelected = { 
                onEvent(AccountEntryEvent.SetStructureType(it))
                onEvent(AccountEntryEvent.ShowStructureSheet(false))
            },
            onDismiss = { onEvent(AccountEntryEvent.ShowStructureSheet(false)) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StructureTypeSelection(
    selectedType: String,
    onTypeSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val structureTypes = listOf("Savings", "Credit Card", "Checking", "Investment", "Cash", "Other")
    var query by remember { mutableStateOf("") }
    val filtered = remember(query) {
        structureTypes.filter { it.contains(query, ignoreCase = true) }
    }

    AeonFullScreenDialog(onDismissRequest = onDismiss) {
        AeonSelectionScreen(
            title = "Structure Type",
            onBack = onDismiss
        ) { padding ->
            Column(modifier = Modifier.padding(padding).padding(horizontal = AeonTheme.dimensions.screenPadding)) {
                AeonSearchField(
                    query = query,
                    onQueryChange = { query = it },
                    placeholder = "Search types..."
                )
                Spacer(modifier = Modifier.height(AeonTheme.dimensions.spacingXl))
                Box(modifier = Modifier.weight(1f, fill = false)) {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        AeonHybridSelector(
                            items = filtered,
                            mode = AeonSelectMode.SINGLE,
                            selectedIds = setOf(selectedType),
                            onSelectionChange = { ids ->
                                ids.firstOrNull()?.let { onTypeSelected(it) }
                            },
                            idMapper = { it },
                            labelMapper = { it },
                            iconMapper = { Icons.Rounded.Layers }
                        )
                    }
                }
            }
        }
    }
}
