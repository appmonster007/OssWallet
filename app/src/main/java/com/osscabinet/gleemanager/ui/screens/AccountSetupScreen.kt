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
    var primaryCurrency by remember { mutableStateOf("USD") }
    var allowedCurrencies by remember { mutableStateOf(setOf("USD")) }
    
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
                        prefix = primaryCurrency
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
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(48.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
            ) {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Text("Currency Management", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                    
                    // Search Bar
                    Surface(
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier.weight(1f),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                decorationBox = { innerTextField ->
                                    if (searchQuery.isEmpty()) Text("Search ISO codes...", color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                                    innerTextField()
                                }
                            )
                        }
                    }

                    // Chips
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        filteredCurrencies.forEach { code ->
                            val isSelected = allowedCurrencies.contains(code)
                            val isPrimary = primaryCurrency == code
                            val accentColor = try { Color(android.graphics.Color.parseColor(accentColorHex)) } catch (e: Exception) { Color(0xFF004D5F) }
                            
                            Surface(
                                modifier = Modifier.clip(CircleShape).combinedClickable(
                                    onClick = {
                                        if (isSelected) {
                                            if (!isPrimary) allowedCurrencies = allowedCurrencies - code
                                        } else {
                                            allowedCurrencies = allowedCurrencies + code
                                        }
                                    },
                                    onLongClick = {
                                        primaryCurrency = code
                                        allowedCurrencies = allowedCurrencies + code
                                    }
                                ),
                                shape = CircleShape,
                                color = when {
                                    isPrimary -> accentColor
                                    isSelected -> accentColor.copy(alpha = 0.3f)
                                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                },
                                contentColor = when {
                                    isPrimary -> if (accentColor.isDark()) Color.White else Color.Black
                                    isSelected -> MaterialTheme.colorScheme.onSurface
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(code, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                                    if (isPrimary) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(Icons.Rounded.Star, contentDescription = "Primary", modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
                    Text("Tip: Long-press a currency to set as primary.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                }
            }

            // Visual Identity
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Text("Visual Identity", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Identifier Icon", style = MaterialTheme.typography.titleSmall)
                        Text("LIBRARY OF ICONS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    }
                    
                    AeonTextField(
                        value = iconSearchQuery,
                        onValueChange = { iconSearchQuery = it },
                        placeholder = "Search icons (e.g. 'bank', 'wallet')...",
                        leadingIcon = Icons.Rounded.Search
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        filteredIcons.forEach { name ->
                            val icon = iconMap[name]!!
                            val isSelected = selectedIconName == name
                            val accentColor = try { Color(android.graphics.Color.parseColor(accentColorHex)) } catch (e: Exception) { Color(0xFF004D5F) }

                            Surface(
                                modifier = Modifier.size(48.dp).clickable { selectedIconName = name },
                                shape = CircleShape,
                                color = if (isSelected) accentColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                contentColor = if (isSelected) {
                                    if (accentColor.isDark()) Color.White else Color.Black
                                } else MaterialTheme.colorScheme.onSurfaceVariant
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Accent Brand Color", style = MaterialTheme.typography.titleSmall)
                    
                    // Preset Colors
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        presetColors.forEach { colorHex ->
                            val isSelected = accentColorHex.equals(colorHex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(colorHex)))
                                    .clickable { accentColorHex = colorHex }
                                    .border(
                                        width = if (isSelected) 2.dp else 0.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        Icons.Rounded.Check, 
                                        contentDescription = null, 
                                        tint = if (Color(android.graphics.Color.parseColor(colorHex)).isDark()) Color.White else Color.Black,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        val color = try { Color(android.graphics.Color.parseColor(accentColorHex)) } catch (e: Exception) { Color.DarkGray }
                        Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(color))
                        AeonTextField(
                            value = accentColorHex,
                            onValueChange = { 
                                val formatted = if (it.startsWith("#")) it else "#$it"
                                if (formatted.length <= 7) {
                                    accentColorHex = formatted
                                }
                            },
                            placeholder = "#004D5F",
                            prefix = "HEX",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

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
                enabled = accountName.isNotBlank() && isBalanceValid
            ) {
                Text("INITIALIZE LEDGER", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }
        }
    }
}

@Composable
fun AeonTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    prefix: String? = null
) {
    Surface(
        modifier = modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                Icon(leadingIcon, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
            }
            if (prefix != null) {
                Text(prefix, style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.outline, fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.width(8.dp))
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) Text(placeholder, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                    innerTextField()
                }
            )
        }
    }
}
