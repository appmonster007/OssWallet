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
import com.osscabinet.gleemanager.ui.viewmodel.AccountViewModel
import com.osscabinet.gleemanager.ui.util.isDark

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun EditAccountScreen(
    accountId: String,
    viewModel: AccountViewModel,
    onBackClick: () -> Unit
) {
    val accounts by viewModel.accounts.collectAsState()
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
    var searchQuery by remember { mutableStateOf("") }
    var iconSearchQuery by remember { mutableStateOf("") }
    
    var primaryCurrency by remember { mutableStateOf(account.currencyCode) }
    var allowedCurrencies by remember { mutableStateOf(account.allowedCurrencies.toSet()) }
    
    val structureTypes = listOf("Savings", "Credit Card", "Checking", "Investment", "Cash", "Other")
    val commonCurrencies = listOf("USD", "EUR", "GBP", "JPY", "CHF", "CAD", "AUD", "INR", "SGD", "HKD", "CNY", "BRL")
    
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
    
    var selectedIconName by remember { mutableStateOf(account.icon) }
    var accentColorHex by remember { mutableStateOf(account.colorHex) }
    val presetColors = listOf("#004D5F", "#1A237E", "#1B5E20", "#B71C1C", "#4A148C", "#E65100", "#006064", "#212121")

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
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            AeonTextField(
                value = accountName,
                onValueChange = { accountName = it },
                placeholder = "Account Name"
            )

            // Structure Type
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
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(structureType)
                        Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null)
                    }
                }
                DropdownMenu(expanded = isStructureMenuExpanded, onDismissRequest = { isStructureMenuExpanded = false }) {
                    structureTypes.forEach { type ->
                        DropdownMenuItem(text = { Text(type) }, onClick = { structureType = type; isStructureMenuExpanded = false })
                    }
                }
            }

            // Visuals
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Icon", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    iconMap.forEach { (name, icon) ->
                        val isSelected = selectedIconName == name
                        Surface(
                            modifier = Modifier.size(48.dp).clickable { selectedIconName = name },
                            shape = CircleShape,
                            color = if (isSelected) Color(android.graphics.Color.parseColor(accentColorHex)) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ) {
                            Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null) }
                        }
                    }
                }
            }

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
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(android.graphics.Color.parseColor(accentColorHex)))
            ) {
                Text("SAVE CHANGES", fontWeight = FontWeight.Bold)
            }
        }
    }
}
