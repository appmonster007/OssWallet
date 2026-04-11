package com.osscabinet.gleemanager.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.gleemanager.ui.components.AeonTextField
import com.osscabinet.gleemanager.ui.components.CurrencyManagementSection
import com.osscabinet.gleemanager.ui.components.VisualIdentitySection
import com.osscabinet.gleemanager.ui.viewmodel.AccountViewModel
import com.osscabinet.gleemanager.ui.viewmodel.TransactionViewModel
import com.osscabinet.gleemanager.ui.util.isDark

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
            TopAppBar(
                title = { Text("Edit Account", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.deleteAccount(accountId)
                        onBackClick()
                    }) {
                        Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
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
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            Column {
                Text("CONFIGURATION", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline, letterSpacing = 1.sp)
                Text("Edit Account Identity", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
            }

            // Account Name
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Account Name", style = MaterialTheme.typography.titleSmall)
                AeonTextField(
                    value = accountName,
                    onValueChange = { accountName = it },
                    placeholder = "e.g. Global Treasury"
                )
            }

            // Structure Type
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Structure Type", style = MaterialTheme.typography.titleSmall)
                Box {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .clickable { isStructureMenuExpanded = true }
                            .padding(horizontal = 20.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(structureType, style = MaterialTheme.typography.bodyLarge)
                            Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null)
                        }
                    }
                    DropdownMenu(
                        expanded = isStructureMenuExpanded,
                        onDismissRequest = { isStructureMenuExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.85f).background(MaterialTheme.colorScheme.surface)
                    ) {
                        structureTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                    structureType = type
                                    isStructureMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Currency Management
            CurrencyManagementSection(
                primaryCurrency = primaryCurrency,
                onPrimaryCurrencyChange = { primaryCurrency = it },
                allowedCurrencies = allowedCurrencies,
                onAllowedCurrenciesChange = { allowedCurrencies = it },
                accentColorHex = accentColorHex,
                isEditMode = true,
                currencyUsageMap = currencyUsageMap
            )

            // Visual Identity
            VisualIdentitySection(
                selectedIconName = selectedIconName,
                onIconSelected = { selectedIconName = it },
                accentColorHex = accentColorHex,
                onColorSelected = { accentColorHex = it }
            )

            val currentAccentColor = try { Color(android.graphics.Color.parseColor(accentColorHex)) } catch (e: Exception) { Color(0xFF004D5F) }

            Button(
                onClick = {
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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(32.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = currentAccentColor,
                    contentColor = if (currentAccentColor.isDark()) Color.White else Color.Black
                ),
                enabled = accountName.isNotBlank() && primaryCurrency.isNotBlank()
            ) {
                Text("SAVE CHANGES", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }
        }
    }
}
