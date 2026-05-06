package com.osscabinet.ossWallet.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.domain.service.aggregation.AggregationService
import com.osscabinet.ossWallet.ui.components.AeonFormHeader
import com.osscabinet.ossWallet.ui.components.AeonHeroHeader
import com.osscabinet.ossWallet.ui.components.CurrencyManagementSection
import com.osscabinet.ossWallet.ui.components.EditorialInput
import com.osscabinet.ossWallet.ui.components.RecordFieldCard
import com.osscabinet.ossWallet.ui.components.VisualIdentitySection
import com.osscabinet.ossWallet.ui.viewmodel.AccountViewModel
import com.osscabinet.ossWallet.ui.viewmodel.TransactionViewModel

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class,
    ExperimentalFoundationApi::class
)
@Composable
fun EditAccountScreen(
    accountId: String,
    viewModel: AccountViewModel,
    transactionViewModel: TransactionViewModel,
    onBackClick: () -> Unit
) {
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val allTransactions by transactionViewModel.transactions.collectAsStateWithLifecycle()
    val exchangeRates by transactionViewModel.exchangeRates.collectAsStateWithLifecycle()
    val account = accounts.find { it.id == accountId }

    if (account == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val accountBalance = remember(allTransactions, accountId, exchangeRates) {
        AggregationService.calculateAccountBalance(
            accountId = accountId,
            allTransactions = allTransactions,
            accountCurrency = account.currencyCode,
            exchangeRates = exchangeRates
        )
    }

    var accountName by remember { mutableStateOf(account.name) }
    var structureType by remember { mutableStateOf(account.structureType) }
    var isStructureMenuExpanded by remember { mutableStateOf(false) }

    var primaryCurrency by remember { mutableStateOf(account.currencyCode) }
    var allowedCurrencies by remember { mutableStateOf(account.allowedCurrencies.toSet()) }

    val structureTypes = listOf("Savings", "Credit Card", "Checking", "Investment", "Cash", "Other")

    // Map to check if a currency is used in any transaction for this account
    val currencyUsageMap = remember(allTransactions, accountId) {
        allTransactions.filter { it.accountId == accountId }
            .groupBy { it.currency }
            .mapValues { it.value.isNotEmpty() }
    }

    var selectedIconName by remember { mutableStateOf(account.icon) }
    var accentColorHex by remember { mutableStateOf(account.colorHex) }

    var navTriggered by remember { mutableStateOf(false) }

    val navigate: (() -> Unit) -> Unit = { action ->
        if (!navTriggered) {
            navTriggered = true
            action()
        }
    }

    Scaffold(
        topBar = {
            val canSave = accountName.isNotBlank() && primaryCurrency.isNotBlank()

            CenterAlignedTopAppBar(
                title = { Text("Edit Account", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navigate(onBackClick) }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    AeonFormHeader(
                        title = "",
                        onSaveClick = {
                            navigate {
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
                            }
                        },
                        saveButtonEnabled = canSave,
                        accentColorHex = accentColorHex,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                }
            )
        }
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
            // Header Section
            AeonHeroHeader(
                title = accountName,
                subtitle = "$primaryCurrency $accountBalance",
                accentColorHex = accentColorHex,
                iconName = selectedIconName
            )

            // Identity Section
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "IDENTITY",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.outline,
                    letterSpacing = 1.sp
                )

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
                    ModalBottomSheet(
                        onDismissRequest = { isStructureMenuExpanded = false },
                        containerColor = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                    ) {
                        Column(modifier = Modifier
                            .padding(24.dp)
                            .padding(bottom = 32.dp)) {
                            Text(
                                "STRUCTURE TYPE",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(structureTypes) { type ->
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                structureType = type; isStructureMenuExpanded =
                                                false
                                            },
                                        shape = RoundedCornerShape(24.dp),
                                        color = if (structureType == type) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(
                                            alpha = 0.2f
                                        )
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

            // Currency Management Card
            CurrencyManagementSection(
                primaryCurrency = primaryCurrency,
                onPrimaryCurrencyChange = { primaryCurrency = it },
                allowedCurrencies = allowedCurrencies,
                onAllowedCurrenciesChange = { allowedCurrencies = it },
                accentColorHex = accentColorHex,
                isEditMode = true,
                currencyUsageMap = currencyUsageMap
            )

            // Visual Identity Section
            VisualIdentitySection(
                selectedIconName = selectedIconName,
                onIconSelected = { selectedIconName = it },
                accentColorHex = accentColorHex,
                onColorSelected = { accentColorHex = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    navigate {
                        viewModel.deleteAccount(accountId)
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

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
