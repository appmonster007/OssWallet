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
import java.math.BigDecimal
import com.osscabinet.ossWallet.ui.components.*
import com.osscabinet.ossWallet.ui.viewmodel.AccountViewModel
import com.osscabinet.ossWallet.ui.util.isDark

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

    // Currency logic
    var primaryCurrency by remember { mutableStateOf("") }
    var allowedCurrencies by remember { mutableStateOf(emptySet<String>()) }

    val structureTypes = listOf("Savings", "Credit Card", "Checking", "Investment", "Cash", "Other")

    var selectedIconName by remember { mutableStateOf("AccountBalance") }
    var accentColorHex by remember { mutableStateOf("#004D5F") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text("New Account", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    val currentAccentColor = try { Color(android.graphics.Color.parseColor(accentColorHex)) } catch (e: Exception) { MaterialTheme.colorScheme.primary }
                    val isBalanceValid = initialBalance.isEmpty() || initialBalance.toBigDecimalOrNull() != null
                    val canCreate = accountName.isNotBlank() && isBalanceValid && primaryCurrency.isNotBlank()
                    
                    Surface(
                        modifier = Modifier.padding(end = 16.dp),
                        shape = RoundedCornerShape(24.dp),
                        color = if (canCreate) currentAccentColor else MaterialTheme.colorScheme.surfaceVariant,
                        onClick = {
                            if (canCreate) {
                                viewModel.addAccount(
                                    name = accountName,
                                    balance = initialBalance.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                                    currency = primaryCurrency,
                                    structureType = structureType,
                                    colorHex = accentColorHex,
                                    icon = selectedIconName,
                                    allowedCurrencies = allowedCurrencies.toList()
                                )
                                onAccountCreated()
                            }
                        },
                        enabled = canCreate
                    ) {
                        Text(
                            "CREATE",
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (canCreate) (if (currentAccentColor.isDark()) Color.White else Color.Black) else MaterialTheme.colorScheme.outline
                            )
                        )
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
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header Section
            AccountHeroHeader(
                name = accountName,
                balance = initialBalance,
                currencyCode = primaryCurrency,
                colorHex = accentColorHex,
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

                EditorialInput(
                    value = initialBalance,
                    onValueChange = { initialBalance = it },
                    label = "INITIAL BALANCE",
                    icon = Icons.Rounded.AccountBalanceWallet
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
                accentColorHex = accentColorHex
            )

            // Visual Identity Section
            VisualIdentitySection(
                selectedIconName = selectedIconName,
                onIconSelected = { selectedIconName = it },
                accentColorHex = accentColorHex,
                onColorSelected = { accentColorHex = it }
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
