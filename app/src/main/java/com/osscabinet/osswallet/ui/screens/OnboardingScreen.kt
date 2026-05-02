package com.osscabinet.ossWallet.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.ui.components.CalculatorNumpad
import com.osscabinet.ossWallet.ui.components.CurrencyManagementSection
import com.osscabinet.ossWallet.ui.util.*
import com.osscabinet.ossWallet.ui.viewmodel.AccountViewModel
import com.osscabinet.ossWallet.ui.viewmodel.SettingsViewModel
import com.osscabinet.ossWallet.ui.viewmodel.SyncState
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    viewModel: SettingsViewModel,
    accountViewModel: AccountViewModel,
    onFinished: () -> Unit
) {
    var step by remember { mutableStateOf(0) }
    val currency by viewModel.currency.collectAsState(initial = "USD")
    val theme by viewModel.theme.collectAsState(initial = "System")
    val precision by viewModel.decimalPrecision.collectAsStateWithLifecycle(initialValue = 2)
    val syncState by viewModel.onboardingSyncState.collectAsStateWithLifecycle()

    var accountName by remember { mutableStateOf("Main Wallet") }
    var initialBalances by remember { mutableStateOf(mapOf<String, String>()) }
    var allowedCurrencies by remember { mutableStateOf(emptySet<String>()) }
    var isCurrencyMenuExpanded by remember { mutableStateOf(false) }

    val steps = listOf("Currency", "Appearance", "Precision", "Account")

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Step ${step + 1} of ${steps.size}",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                navigationIcon = {
                    if (step > 0) {
                        IconButton(onClick = { step-- }) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.background,
                tonalElevation = 0.dp
            ) {
                Column {
                    // Progress indicator
                    LinearProgressIndicator(
                        progress = { (step + 1).toFloat() / steps.size },
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )

                    Row(
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                            if (step < steps.size - 1) {
                                step++
                            } else {
                                if (allowedCurrencies.isEmpty()) {
                                    isCurrencyMenuExpanded = true
                                    return@Button
                                }
                                accountViewModel.addAccountWithInitialBalances(
                                        name = accountName,
                                        initialBalances = initialBalances,
                                        currency = currency,
                                        structureType = "Cash",
                                        colorHex = "#004D40", // Teal for healthy state
                                        icon = "Payments",
                                        allowedCurrencies = allowedCurrencies.toList()
                                    )
                                    viewModel.setOnboardingCompleted(true)
                                    onFinished()
                                }
                            },
                            modifier = Modifier.height(56.dp),
                            shape = RoundedCornerShape(28.dp),
                            contentPadding = PaddingValues(horizontal = 32.dp)
                        ) {
                            Text(
                                if (step < steps.size - 1) "NEXT" else "GET STARTED",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (step) {
                    0 -> CurrencyStep(
                        selectedCurrency = currency,
                        syncState = syncState,
                        onCurrencySelected = { viewModel.setCurrency(it) }
                    )
                    1 -> ThemeStep(theme, onThemeSelected = { viewModel.setTheme(it) })
                    2 -> PrecisionStep(precision, onPrecisionSelected = { viewModel.setDecimalPrecision(it) })
                    3 -> AccountStep(
                        accountName = accountName,
                        onAccountNameChange = { accountName = it },
                        initialBalances = initialBalances,
                        onInitialBalancesChange = { initialBalances = it },
                        allowedCurrencies = allowedCurrencies,
                        onAllowedCurrenciesChange = { allowedCurrencies = it },
                        onCurrencyMenuOpen = { isCurrencyMenuExpanded = true },
                        decimalPrecision = precision
                    )
                }
            }

            if (isCurrencyMenuExpanded) {
                ModalBottomSheet(
                    onDismissRequest = { isCurrencyMenuExpanded = false },
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                    dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)) },
                    modifier = Modifier.fillMaxHeight(0.9f)
                ) {
                    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
                        CurrencyManagementSection(
                            primaryCurrency = currency,
                            onPrimaryCurrencyChange = { viewModel.setCurrency(it) },
                            allowedCurrencies = allowedCurrencies,
                            onAllowedCurrenciesChange = { allowedCurrencies = it },
                            accentColorHex = "#004D40"
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CurrencyStep(
    selectedCurrency: String,
    syncState: SyncState,
    onCurrencySelected: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        StepHeader(
            icon = Icons.Rounded.CurrencyExchange,
            title = "Base Currency",
            description = "Choose your reporting currency. This will be used to show your total balance."
        )

        var searchQuery by remember { mutableStateOf("") }
        val currencies = remember { getAllCurrencies() }
        val filteredCurrencies = remember(searchQuery) {
            currencies.filter {
                it.contains(searchQuery, ignoreCase = true) ||
                        getCurrencyName(it).contains(searchQuery, ignoreCase = true)
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            placeholder = { Text("Search currencies...") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                unfocusedIndicatorColor = Color.Transparent,
                focusedIndicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
        )

        Surface(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredCurrencies) { code ->
                    val isSelected = selectedCurrency == code
                    Surface(
                        onClick = { onCurrencySelected(code) },
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent,
                        border = if (isSelected) BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)) else null
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier.size(40.dp),
                                shape = CircleShape,
                                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceContainerHighest
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        getCurrencySymbol(code),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    getCurrencyName(code),
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold)
                                )
                                Text(code, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                            }
                            if (isSelected) {
                                when (syncState) {
                                    is SyncState.Loading -> CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                    else -> Icon(Icons.Rounded.CheckCircle, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ThemeStep(selectedTheme: String, onThemeSelected: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        StepHeader(
            icon = Icons.Rounded.Palette,
            title = "Appearance",
            description = "Select a theme that works best for your environment."
        )
        
        val themes = listOf(
            Triple("System", "Matches device settings", Icons.Rounded.SettingsSuggest),
            Triple("Light", "Clean and bright", Icons.Rounded.LightMode),
            Triple("Dark", "Easier on the eyes", Icons.Rounded.DarkMode)
        )
        
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            themes.forEach { (name, desc, icon) ->
                val isSelected = selectedTheme == name
                Surface(
                    onClick = { onThemeSelected(name) },
                    shape = RoundedCornerShape(32.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(
                        width = if (isSelected) 1.5.dp else 0.5.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = CircleShape,
                            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceContainerHighest
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(icon, contentDescription = null, tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Spacer(modifier = Modifier.width(20.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                        }
                        if (isSelected) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PrecisionStep(selectedPrecision: Int, onPrecisionSelected: (Int) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        StepHeader(
            icon = Icons.Rounded.Exposure,
            title = "Precision",
            description = "Choose how many decimal places to display for amounts."
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            listOf(2, 3).forEach { p ->
                val isSelected = selectedPrecision == p
                Surface(
                    onClick = { onPrecisionSelected(p) },
                    modifier = Modifier.weight(1f).height(120.dp),
                    shape = RoundedCornerShape(32.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(
                        width = if (isSelected) 1.5.dp else 0.5.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)
                    )
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            p.toString(),
                            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "DECIMALS",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.3f),
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier.padding(24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Rounded.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Example: ${formatAmount(BigDecimal("1234.5678"), selectedPrecision)}",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun AccountStep(
    accountName: String,
    onAccountNameChange: (String) -> Unit,
    initialBalances: Map<String, String>,
    onInitialBalancesChange: (Map<String, String>) -> Unit,
    allowedCurrencies: Set<String>,
    onAllowedCurrenciesChange: (Set<String>) -> Unit,
    onCurrencyMenuOpen: () -> Unit,
    decimalPrecision: Int = 2
) {
    var activeCurrencyField by remember { mutableStateOf<String?>(null) }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        StepHeader(
            icon = Icons.Rounded.AccountBalanceWallet,
            title = "First Account",
            description = "Start with your primary wallet or bank account. You can add more later."
        )

        SectionCard(title = "IDENTIFIER", icon = Icons.Rounded.Badge) {
            OutlinedTextField(
                value = accountName,
                onValueChange = onAccountNameChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Account Name (e.g. Savings)") },
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.3f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.3f),
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary
                )
            )
        }

        SectionCard(title = "CURRENCIES", icon = Icons.Rounded.CurrencyExchange) {
            Surface(
                onClick = onCurrencyMenuOpen,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.3f),
                border = BorderStroke(1.dp, if (allowedCurrencies.isEmpty()) MaterialTheme.colorScheme.error.copy(alpha = 0.3f) else Color.Transparent)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            if (allowedCurrencies.isEmpty()) "Select currencies"
                            else "${allowedCurrencies.size} Selected",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        if (allowedCurrencies.isNotEmpty()) {
                            Text(
                                allowedCurrencies.joinToString(", "),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                    Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        if (allowedCurrencies.isNotEmpty()) {
            SectionCard(title = "STARTING BALANCES", icon = Icons.Rounded.AccountBalance) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    allowedCurrencies.forEach { code ->
                        BalanceInputCard(
                            currencyCode = code,
                            currentBalance = initialBalances[code] ?: "",
                            isActive = activeCurrencyField == code,
                            onClick = { activeCurrencyField = code }
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }

    if (activeCurrencyField != null) {
        BalanceEditSheet(
            currencyCode = activeCurrencyField!!,
            currentBalance = initialBalances[activeCurrencyField] ?: "",
            onBalanceChange = { newBalance ->
                onInitialBalancesChange(initialBalances.toMutableMap().apply { put(activeCurrencyField!!, newBalance) })
            },
            onDismiss = { activeCurrencyField = null },
            decimalPrecision = decimalPrecision
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BalanceEditSheet(
    currencyCode: String,
    currentBalance: String,
    onBalanceChange: (String) -> Unit,
    onDismiss: () -> Unit,
    decimalPrecision: Int
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)) }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(getCurrencySymbol(currencyCode), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Column {
                        Text(getCurrencyName(currencyCode), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Text(currencyCode, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    }
                }
                IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, contentDescription = "Close") }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("STARTING BALANCE", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    Text(
                        if (currentBalance.isEmpty()) "0.00" else currentBalance,
                        style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Black, letterSpacing = (-1).sp),
                        color = if (currentBalance.startsWith("-")) Color(0xFFB3261E) else Color(0xFF004D40)
                    )
                }
            }

            CalculatorNumpad(
                amount = currentBalance,
                onAmountChange = { newAmount, _ -> onBalanceChange(newAmount) },
                transactionType = "income",
                decimalPrecision = decimalPrecision,
                onDone = onDismiss,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
            Text(
                title.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            )
        }
        content()
    }
}

@Composable
private fun BalanceInputCard(
    currencyCode: String,
    currentBalance: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(
            width = if (isActive) 1.5.dp else 0.5.dp,
            color = if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)
        )
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(getCurrencySymbol(currencyCode), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }
                Column {
                    Text(currencyCode, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    Text(
                        if (currentBalance.isEmpty()) "Tap to set" else "Balance set",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
            Text(
                if (currentBalance.isEmpty()) "0.00" else currentBalance,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                color = if (currentBalance.startsWith("-")) Color(0xFFB3261E) else if (currentBalance.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f) else Color(0xFF004D40)
            )
        }
    }
}

@Composable
fun StepHeader(icon: ImageVector, title: String, description: String) {
    Column(
        horizontalAlignment = Alignment.Start,
        modifier = Modifier.padding(bottom = 32.dp).fillMaxWidth()
    ) {
        Surface(
            modifier = Modifier.size(64.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            lineHeight = 24.sp
        )
    }
}
