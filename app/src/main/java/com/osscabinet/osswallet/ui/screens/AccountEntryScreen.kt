package com.osscabinet.ossWallet.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.service.aggregation.AggregationService
import com.osscabinet.ossWallet.ui.components.*
import com.osscabinet.ossWallet.ui.components.AeonFlowSectionHeader
import com.osscabinet.ossWallet.ui.components.AeonSearchField
import com.osscabinet.ossWallet.ui.viewmodel.AccountViewModel
import com.osscabinet.ossWallet.ui.viewmodel.TransactionViewModel
import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter.getCurrencySymbol

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun AccountEntryScreen(
    onBackClick: () -> Unit,
    onDone: () -> Unit,
    viewModel: AccountViewModel,
    transactionViewModel: TransactionViewModel,
    accountId: String? = null
) {
    val isEditMode = accountId != null
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val defaultCurrency by viewModel.defaultCurrency.collectAsStateWithLifecycle()

    val account = if (isEditMode) accounts.find { it.id == accountId } else null

    // Show a loader until the account is available in edit mode
    if (isEditMode && account == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val allTransactions by transactionViewModel.transactions.collectAsStateWithLifecycle()
    val exchangeRates by transactionViewModel.exchangeRates.collectAsStateWithLifecycle()

    val accountBalance = remember(allTransactions, accountId, exchangeRates) {
        if (isEditMode && account != null) {
            AggregationService.calculateAccountBalance(
                accountId = accountId!!,
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
    var isStructureMenuExpanded by remember { mutableStateOf(false) }
    var structureQuery by remember { mutableStateOf("") }
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

    val structureTypes = listOf("Savings", "Credit Card", "Checking", "Investment", "Cash", "Other")

    var navTriggered by remember { mutableStateOf(false) }
    val navigate: (() -> Unit) -> Unit = { action ->
        if (!navTriggered) {
            navTriggered = true
            action()
        }
    }

    val isBalanceValid = initialBalances.values.all { it.isEmpty() || it.toBigDecimalOrNull() != null }
    val canSave = accountName.isNotBlank() && primaryCurrency.isNotBlank() &&
            allowedCurrencies.isNotEmpty() && (isEditMode || isBalanceValid)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        if (isEditMode) "Edit Account" else "New Account",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navigate(onBackClick) }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isEditMode) {
                        IconButton(
                            onClick = {
                                navigate {
                                    viewModel.deleteAccount(accountId!!)
                                    onBackClick()
                                }
                            }
                        ) {
                            Icon(
                                Icons.Rounded.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            navigate {
                                if (isEditMode && account != null) {
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
                        },
                        enabled = canSave
                    ) {
                        Icon(
                            Icons.Rounded.Done,
                            contentDescription = "Save",
                            tint = if (canSave) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.systemBars
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
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

            // Identity
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AeonFlowSectionHeader(title = "IDENTITY")

                EditorialInput(
                    value = accountName,
                    onValueChange = { accountName = it },
                    label = "ACCOUNT NAME",
                    icon = Icons.AutoMirrored.Rounded.Label
                )

                RecordFieldCard(
                    icon = Icons.Rounded.Layers,
                    label = "STRUCTURE TYPE",
                    value = structureType,
                    onClick = { isStructureMenuExpanded = true },
                    trailingIcon = Icons.Rounded.KeyboardArrowDown,
                    iconContainerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                )

                if (isStructureMenuExpanded) {
                    val filteredStructures = remember(structureQuery) {
                        if (structureQuery.isBlank()) structureTypes
                        else structureTypes.filter { it.contains(structureQuery, ignoreCase = true) }
                    }

                    ModalBottomSheet(
                        onDismissRequest = { isStructureMenuExpanded = false },
                        containerColor = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(24.dp)
                                .padding(bottom = 32.dp)
                        ) {
                            AeonFlowSectionHeader(title = "STRUCTURE TYPE")
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            AeonSearchField(
                                query = structureQuery,
                                onQueryChange = { structureQuery = it },
                                placeholder = "Search types..."
                            )

                            Spacer(modifier = Modifier.height(24.dp))
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(filteredStructures) { type ->
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                structureType = type
                                                isStructureMenuExpanded = false
                                            },
                                        shape = RoundedCornerShape(24.dp),
                                        color = if (structureType == type)
                                            MaterialTheme.colorScheme.primaryContainer
                                        else
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            type,
                                            modifier = Modifier.padding(20.dp),
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Currency
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
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    AeonFlowSectionHeader(title = "INITIAL BALANCES")
                    allowedCurrencies.forEach { currencyCode ->
                        EditorialInput(
                            value = initialBalances[currencyCode] ?: "",
                            onValueChange = { newBalance ->
                                initialBalances = initialBalances.toMutableMap()
                                    .apply { put(currencyCode, newBalance) }
                            },
                            label = "INITIAL BALANCE ($currencyCode)",
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

            Spacer(modifier = Modifier.height(16.dp))

            // Delete button (edit mode only)
            if (isEditMode) {
                Button(
                    onClick = {
                        navigate {
                            viewModel.deleteAccount(accountId!!)
                            onBackClick()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    shape = RoundedCornerShape(32.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Icon(Icons.Rounded.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "DELETE ACCOUNT",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
