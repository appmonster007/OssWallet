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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.service.aggregation.AggregationService
import com.osscabinet.ossWallet.ui.components.*
import com.osscabinet.ossWallet.ui.components.entry.*
import com.osscabinet.ossWallet.ui.viewmodel.AccountViewModel
import com.osscabinet.ossWallet.ui.viewmodel.TransactionViewModel

@Composable
fun AccountFormBody(
    account: Account?,
    viewModel: AccountViewModel,
    transactionViewModel: TransactionViewModel,
    isEditMode: Boolean,
    accountId: String?,
    onValidationChanged: (Boolean) -> Unit,
    registerSaveAction: (() -> Unit) -> Unit,
    onBackClick: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val defaultCurrency by viewModel.defaultCurrency.collectAsStateWithLifecycle()
    val allTransactions by transactionViewModel.transactions.collectAsStateWithLifecycle()
    val exchangeRates by transactionViewModel.exchangeRates.collectAsStateWithLifecycle()

    val accountBalance = remember(allTransactions, accountId, exchangeRates) {
        if (isEditMode && account != null && accountId != null) {
            AggregationService.calculateAccountBalance(
                accountId = accountId,
                allTransactions = allTransactions,
                accountCurrency = account.currencyCode,
                exchangeRates = exchangeRates
            )
        } else null
    }

    val currencyUsageMap = remember(allTransactions, accountId) {
        if (isEditMode) {
            allTransactions.filter { it.accountId == accountId }
                .groupBy { it.currency }
                .mapValues { it.value.isNotEmpty() }
        } else emptyMap()
    }

    var accountName by remember(account) { mutableStateOf(account?.name ?: "") }
    var structureType by remember(account) { mutableStateOf(account?.structureType ?: "Savings") }
    var showStructureSheet by remember { mutableStateOf(false) }
    
    var primaryCurrency by remember(account, defaultCurrency) {
        mutableStateOf(account?.currencyCode ?: defaultCurrency)
    }
    var allowedCurrencies by remember(account, defaultCurrency) {
        mutableStateOf(
            account?.allowedCurrencies?.toSet()
                ?: if (defaultCurrency.isNotBlank()) setOf(defaultCurrency) else emptySet()
        )
    }
    var initialBalances by remember { mutableStateOf(mapOf<String, String>()) }
    var selectedIconName by remember(account) { mutableStateOf(account?.icon ?: "AccountBalance") }
    var accentColorHex by remember(account) { mutableStateOf(account?.colorHex ?: "#004D5F") }

    val isBalanceValid = initialBalances.values.all { it.isEmpty() || it.toBigDecimalOrNull() != null }
    val canSave = accountName.isNotBlank() && primaryCurrency.isNotBlank() &&
            allowedCurrencies.isNotEmpty() && (isEditMode || isBalanceValid)

    LaunchedEffect(canSave) {
        onValidationChanged(canSave)
    }

    registerSaveAction {
        if (isEditMode && account != null && accountId != null) {
            viewModel.updateAccount(
                account.copy(
                    name = accountName,
                    structureType = structureType,
                    currencyCode = primaryCurrency,
                    allowedCurrencies = allowedCurrencies.toList(),
                    colorHex = accentColorHex,
                    icon = selectedIconName
                )
            )
            onBackClick()
        } else {
            viewModel.addAccountWithInitialBalances(
                name = accountName,
                initialBalances = initialBalances,
                currency = primaryCurrency,
                structureType = structureType,
                colorHex = accentColorHex,
                icon = selectedIconName,
                allowedCurrencies = allowedCurrencies.toList()
            )
            onDone()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AeonHeroHeader(
            title = accountName.ifBlank { "Untitled" },
            subtitle = if (isEditMode && accountBalance != null)
                "$primaryCurrency $accountBalance"
            else if (primaryCurrency.isNotBlank())
                "$primaryCurrency ${initialBalances[primaryCurrency]?.ifBlank { "0.00" } ?: "0.00"}"
            else "0.00",
            accentColorHex = accentColorHex,
            iconName = selectedIconName
        )

        EntrySection(title = "IDENTITY", icon = Icons.Rounded.Fingerprint) {
            EntryEditorialInput(
                value = accountName,
                onValueChange = { accountName = it },
                label = "ACCOUNT NAME",
                icon = Icons.AutoMirrored.Rounded.Label
            )

            EntryActionCard(
                icon = Icons.Rounded.Layers,
                label = "STRUCTURE TYPE",
                value = structureType,
                onClick = { showStructureSheet = true },
                iconContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                iconTint = MaterialTheme.colorScheme.secondary
            )
        }

        // Currency management
        CurrencyManagementSection(
            primaryCurrency = primaryCurrency,
            onPrimaryCurrencyChange = { primaryCurrency = it },
            allowedCurrencies = allowedCurrencies,
            onAllowedCurrenciesChange = { allowedCurrencies = it },
            accentColorHex = accentColorHex,
            isEditMode = isEditMode,
            currencyUsageMap = currencyUsageMap
        )

        // Initial balances (create mode only)
        if (!isEditMode && allowedCurrencies.isNotEmpty()) {
            EntrySection(title = "INITIAL BALANCES", icon = Icons.Rounded.AccountBalanceWallet) {
                allowedCurrencies.forEach { currencyCode ->
                    EntryEditorialInput(
                        value = initialBalances[currencyCode] ?: "",
                        onValueChange = { newBalance ->
                            initialBalances = initialBalances.toMutableMap()
                                .apply { put(currencyCode, newBalance) }
                        },
                        label = "BALANCE ($currencyCode)",
                        icon = Icons.Rounded.AccountBalanceWallet
                    )
                }
            }
        }

        // Visual identity
        VisualIdentitySection(
            selectedIconName = selectedIconName,
            onIconSelected = { selectedIconName = it },
            accentColorHex = accentColorHex,
            onColorSelected = { accentColorHex = it }
        )

        Spacer(modifier = Modifier.height(32.dp))
    }

    if (showStructureSheet) {
        StructureTypeSelection(
            selectedType = structureType,
            onTypeSelected = { 
                structureType = it
                showStructureSheet = false 
            },
            onDismiss = { showStructureSheet = false }
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
            Column(modifier = Modifier.padding(padding).padding(horizontal = 24.dp)) {
                AeonSearchField(
                    query = query,
                    onQueryChange = { query = it },
                    placeholder = "Search types..."
                )
                Spacer(modifier = Modifier.height(24.dp))
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
