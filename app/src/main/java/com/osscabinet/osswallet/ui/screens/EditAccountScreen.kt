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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.ui.components.*
import com.osscabinet.ossWallet.ui.viewmodel.AccountViewModel
import com.osscabinet.ossWallet.ui.viewmodel.TransactionViewModel
import com.osscabinet.ossWallet.ui.util.isDark

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun EditAccountScreen(
    accountId: String,
    viewModel: AccountViewModel,
    transactionViewModel: TransactionViewModel,
    onBackClick: () -> Unit
) {
    val accounts by viewModel.accounts.collectAsState()
    val allTransactions by transactionViewModel.transactions.collectAsState()
    val account = accounts.find { it.id == accountId }

    if (account == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
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

    Scaffold(
        topBar = {
            val canSave = accountName.isNotBlank() && primaryCurrency.isNotBlank()

            CenterAlignedTopAppBar(
                title = { Text("Edit Account", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    AeonFormHeader(
                        title = "",
                        onSaveClick = {
                            viewModel.updateAccount(account.copy(
                                name = accountName,
                                structureType = structureType,
                                currencyCode = primaryCurrency,
                                allowedCurrencies = allowedCurrencies.toList(),
                                colorHex = accentColorHex,
                                icon = selectedIconName
                            ))
                            onBackClick()
                        },
                        saveButtonEnabled = canSave,
                        accentColorHex = accentColorHex,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                }
            )
        }
    ) {
innerPadding ->
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
                subtitle = "$primaryCurrency ${account.balance}",
                accentColorHex = accentColorHex,
                iconName = selectedIconName
            )

            // Identity Section
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("IDENTITY", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.outline, letterSpacing = 1.sp)
                
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
                        Column(modifier = Modifier.padding(24.dp).padding(bottom = 32.dp)) {
                            Text("STRUCTURE TYPE", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Spacer(modifier = Modifier.height(24.dp))
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(structureTypes) { type ->
                                    Surface(
                                        modifier = Modifier.fillMaxWidth().clickable { structureType = type; isStructureMenuExpanded = false },
                                        shape = RoundedCornerShape(24.dp),
                                        color = if (structureType == type) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                    ) {
                                        Text(type, modifier = Modifier.padding(20.dp), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
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
                    viewModel.deleteAccount(accountId)
                    onBackClick()
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
                Text("DELETE ACCOUNT", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
