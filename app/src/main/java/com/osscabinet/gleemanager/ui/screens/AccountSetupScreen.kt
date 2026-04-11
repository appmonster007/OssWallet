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
import com.osscabinet.gleemanager.ui.util.isDark

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun AccountSetupScreen(
    onBackClick: () -> Unit,
    onAccountCreated: () -> Unit,
    viewModel: AccountViewModel
) {
    var accountName by remember { mutableStateOf("") }
    var initialBalance by remember { mutableStateOf("") }
    var structureType by remember { mutableStateOf("Savings") }
    var isStructureMenuExpanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var iconSearchQuery by remember { mutableStateOf("") }
    
    // Currency logic
    var primaryCurrency by remember { mutableStateOf("") }
    var allowedCurrencies by remember { mutableStateOf(setOf<String>()) }
    
    val structureTypes = listOf("Savings", "Credit Card", "Checking", "Investment", "Cash", "Other")
    
    val commonCurrencies = listOf("USD", "EUR", "GBP", "JPY", "CHF", "CAD", "AUD", "INR", "SGD", "HKD", "CNY", "BRL")
    val filteredCurrencies = if (searchQuery.isBlank()) {
        commonCurrencies
    } else {
        commonCurrencies.filter { it.contains(searchQuery, ignoreCase = true) }
    }
    
    val iconMap = mapOf(
        "AccountBalance" to Icons.Rounded.AccountBalance,
        "Wallet" to Icons.Rounded.Wallet,
        "RocketLaunch" to Icons.Rounded.RocketLaunch,
        "ShowChart" to Icons.AutoMirrored.Rounded.ShowChart,
        "Payments" to Icons.Rounded.Payments,
        "Savings" to Icons.Rounded.Savings,
        "Paid" to Icons.Rounded.Paid,
        "AccountBalanceWallet" to Icons.Rounded.AccountBalanceWallet,
        "CreditCard" to Icons.Rounded.CreditCard,
        "MonetizationOn" to Icons.Rounded.MonetizationOn,
        "Analytics" to Icons.Rounded.Analytics,
        "Foundation" to Icons.Rounded.Foundation,
        "Stars" to Icons.Rounded.Stars,
        "Diamond" to Icons.Rounded.Diamond,
        "AccountTree" to Icons.Rounded.AccountTree,
        "AutoGraph" to Icons.Rounded.AutoGraph
    )
    
    var selectedIconName by remember { mutableStateOf("AccountBalance") }
    var accentColorHex by remember { mutableStateOf("#004D5F") }

    val presetColors = listOf("#004D5F", "#1A237E", "#1B5E20", "#B71C1C", "#4A148C", "#E65100", "#006064", "#212121")
    
    val filteredIcons = if (iconSearchQuery.isBlank()) {
        iconMap.keys.toList()
    } else {
        iconMap.keys.filter { it.contains(iconSearchQuery, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text("Aeon Ledger", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
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
                Text("New Account Identity", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
            }

            // Account Name & Balance
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Account Name", style = MaterialTheme.typography.titleSmall)
                    AeonTextField(
                        value = accountName,
                        onValueChange = { accountName = it },
                        placeholder = "e.g. Global Treasury"
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Initial Balance", style = MaterialTheme.typography.titleSmall)
                    AeonTextField(
                        value = initialBalance,
                        onValueChange = { initialBalance = it },
                        placeholder = "0.00",
                        prefix = primaryCurrency.ifEmpty { null }
                    )
                }
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

            // Allowed Currencies Card
            CurrencyManagementSection(
                primaryCurrency = primaryCurrency,
                onPrimaryCurrencyChange = { primaryCurrency = it },
                allowedCurrencies = allowedCurrencies,
                onAllowedCurrenciesChange = { allowedCurrencies = it },
                accentColorHex = accentColorHex
            )

            // Visual Identity
            VisualIdentitySection(
                selectedIconName = selectedIconName,
                onIconSelected = { selectedIconName = it },
                accentColorHex = accentColorHex,
                onColorSelected = { accentColorHex = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            val currentAccentColor = try { Color(android.graphics.Color.parseColor(accentColorHex)) } catch (e: Exception) { Color(0xFF004D5F) }
            val isBalanceValid = initialBalance.isEmpty() || initialBalance.toDoubleOrNull() != null

            Button(
                onClick = {
                    if (isBalanceValid) {
                        viewModel.addAccount(
                            name = accountName,
                            balance = initialBalance.toDoubleOrNull() ?: 0.0,
                            currency = primaryCurrency,
                            structureType = structureType,
                            colorHex = accentColorHex,
                            icon = selectedIconName,
                            allowedCurrencies = allowedCurrencies.toList()
                        )
                        onAccountCreated()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(32.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = currentAccentColor,
                    contentColor = if (currentAccentColor.isDark()) Color.White else Color.Black
                ),
                enabled = accountName.isNotBlank() && isBalanceValid && primaryCurrency.isNotBlank()
            ) {
                Text("INITIALIZE LEDGER", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }
        }
    }
}

@Composable
fun AeonTextFieldPlaceholder() {
    // Placeholder to remove the local AeonTextField
}
