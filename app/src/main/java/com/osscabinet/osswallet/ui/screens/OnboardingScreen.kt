package com.osscabinet.ossWallet.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import com.osscabinet.ossWallet.ui.components.CalculatorNumpadV2
import com.osscabinet.ossWallet.ui.util.determineTransactionType
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.ui.viewmodel.SettingsViewModel
import com.osscabinet.ossWallet.ui.viewmodel.AccountViewModel
import com.osscabinet.ossWallet.ui.viewmodel.SyncState
import com.osscabinet.ossWallet.ui.util.*
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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

    var accountName by remember { mutableStateOf("Cash") }
    var initialBalances by remember { mutableStateOf(mapOf<String, String>()) }
    var allowedCurrencies by remember { mutableStateOf(emptySet<String>()) }
    var isCurrencyMenuExpanded by remember { mutableStateOf(false) }

    val steps = listOf("Currency", "Appearance", "Precision", "Account")

    Scaffold(
        topBar = {
            if (step > 0) {
                TopAppBar(
                    title = { },
                    navigationIcon = {
                        IconButton(onClick = { step-- }) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Progress indicator
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        steps.indices.forEach { index ->
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        if (index <= step) MaterialTheme.colorScheme.primary 
                                        else MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(4.dp)
                                    )
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (step < steps.size - 1) {
                                // Validate Account step before proceeding
                                if (step == 3) {
                                    if (allowedCurrencies.isEmpty()) {
                                        // Show currency menu if no currencies selected
                                        isCurrencyMenuExpanded = true
                                        return@Button
                                    }
                                }
                                step++
                            } else {
                                // Finalize Onboarding
                                accountViewModel.addAccountWithInitialBalances(
                                    name = accountName,
                                    initialBalances = initialBalances,
                                    currency = currency,
                                    structureType = "Cash",
                                    colorHex = "#4CAF50",
                                    icon = "Payments",
                                    allowedCurrencies = allowedCurrencies.toList()
                                )
                                viewModel.setOnboardingCompleted(true)
                                onFinished()
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Text(if (step < steps.size - 1) "NEXT" else "GET STARTED")
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null)
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (step) {
                0 -> CurrencyStep(
                    selectedCurrency = currency,
                    syncState = syncState,
                    onCurrencySelected = { viewModel.setCurrency(it) }
                )
                1 -> {
                    Spacer(modifier = Modifier.weight(1f))
                    ThemeStep(theme, onThemeSelected = { viewModel.setTheme(it) })
                    Spacer(modifier = Modifier.weight(1f))
                }
                2 -> {
                    Spacer(modifier = Modifier.weight(1f))
                    PrecisionStep(precision, onPrecisionSelected = { viewModel.setDecimalPrecision(it) })
                    Spacer(modifier = Modifier.weight(1f))
                }
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

            if (isCurrencyMenuExpanded) {
                ModalBottomSheet(
                    onDismissRequest = { isCurrencyMenuExpanded = false },
                    containerColor = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                    modifier = Modifier.fillMaxHeight(0.9f)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        com.osscabinet.ossWallet.ui.components.CurrencyManagementSection(
                            primaryCurrency = currency,
                            onPrimaryCurrencyChange = { viewModel.setCurrency(it) },
                            allowedCurrencies = allowedCurrencies,
                            onAllowedCurrenciesChange = { allowedCurrencies = it },
                            accentColorHex = "#4CAF50"
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CurrencyStep(
    selectedCurrency: String,
    syncState: SyncState,
    onCurrencySelected: (String) -> Unit
) {
    StepHeader(
        icon = Icons.Rounded.Payments,
        title = "Base Currency",
        description = "Select your primary currency for reporting and balances."
    )

    var searchQuery by remember { mutableStateOf("") }
    val currencies = remember { getAllCurrencies() }
    val filteredCurrencies = remember(searchQuery) {
        currencies.filter {
            it.contains(searchQuery, ignoreCase = true) ||
                    getCurrencyName(it).contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Show currently selected currency above search bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    getCurrencySymbol(selectedCurrency),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        getCurrencyName(selectedCurrency),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        selectedCurrency,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search currency...") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Clear")
                    }
                }
            },
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(8.dp)
            ) {
                items(filteredCurrencies) { code ->
                    val name = getCurrencyName(code)
                    val symbol = getCurrencySymbol(code)
                    val isSelected = selectedCurrency == code

                    Surface(
                        onClick = { onCurrencySelected(code) },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                symbol,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(40.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                Text(code, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            }
                            if (isSelected) {
                                when (syncState) {
                                    is SyncState.Loading -> CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                    is SyncState.Error -> Icon(Icons.Rounded.Error, contentDescription = "Sync Error", tint = MaterialTheme.colorScheme.error)
                                    else -> Icon(Icons.Rounded.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
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
    StepHeader(
        icon = Icons.Rounded.Palette,
        title = "Appearance",
        description = "Choose a look that suits you best."
    )
    
    val themes = listOf("System", "Light", "Dark")
    
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        themes.forEach { theme ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onThemeSelected(theme) },
                shape = RoundedCornerShape(16.dp),
                color = if (selectedTheme == theme) MaterialTheme.colorScheme.primaryContainer 
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                border = if (selectedTheme == theme) null 
                         else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        when(theme) {
                            "Light" -> Icons.Rounded.LightMode
                            "Dark" -> Icons.Rounded.DarkMode
                            else -> Icons.Rounded.SettingsSuggest
                        },
                        contentDescription = null,
                        tint = if (selectedTheme == theme) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(theme, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun PrecisionStep(selectedPrecision: Int, onPrecisionSelected: (Int) -> Unit) {
    StepHeader(
        icon = Icons.Rounded.Pin,
        title = "Decimal Precision",
        description = "How many decimal places should we show for amounts?"
    )

    val precisions = listOf(2, 3)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            precisions.forEachIndexed { index, p ->
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .clickable { onPrecisionSelected(p) },
                    shape = if (index == 0) RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                            else RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
                    color = if (selectedPrecision == p) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    border = if (selectedPrecision == p) null
                             else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            p.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedPrecision == p) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Rounded.Visibility,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Example: ${formatAmount(BigDecimal("1234.567"), selectedPrecision)}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        StepHeader(
            icon = Icons.Rounded.AccountBalanceWallet,
            title = "First Account",
            description = "Let's set up your main account. You can add more later."
        )

        // Account Name Section
        SectionCard(
            title = "Account Details",
            icon = Icons.Rounded.Badge
        ) {
            OutlinedTextField(
                value = accountName,
                onValueChange = onAccountNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Account Name") },
                placeholder = { Text("e.g. Cash, Bank, Wallet") },
                leadingIcon = { Icon(Icons.Rounded.Badge, contentDescription = null) },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
        }

        // Currency Selection Section
        SectionCard(
            title = "Supported Currencies",
            icon = Icons.Rounded.Payments
        ) {
            Surface(
                onClick = onCurrencyMenuOpen,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (allowedCurrencies.isEmpty()) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            if (allowedCurrencies.isEmpty()) "No currencies selected"
                            else "${allowedCurrencies.size} currency/currencies selected",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        if (allowedCurrencies.isNotEmpty()) {
                            Text(
                                allowedCurrencies.take(3).joinToString(", ") + 
                                    if (allowedCurrencies.size > 3) "..." else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(
                        Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Select currencies",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Initial Balances Section
        if (allowedCurrencies.isNotEmpty()) {
            SectionCard(
                title = "Initial Balances",
                icon = Icons.Rounded.AccountBalance,
                description = "Set initial balances for each currency. Tap a card to edit."
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    allowedCurrencies.forEach { currencyCode ->
                        BalanceInputCard(
                            currencyCode = currencyCode,
                            currentBalance = initialBalances[currencyCode] ?: "",
                            isActive = activeCurrencyField == currencyCode,
                            onClick = {
                                activeCurrencyField = currencyCode
                            }
                        )
                    }
                }
            }
        } else {
            // Empty state for currencies
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        "Please select at least 1 currency to continue",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }

    // Balance Edit Bottom Sheet
    if (activeCurrencyField != null) {
        BalanceEditSheet(
            currencyCode = activeCurrencyField!!,
            currentBalance = initialBalances[activeCurrencyField] ?: "",
            onBalanceChange = { newBalance ->
                onInitialBalancesChange(initialBalances.toMutableMap().apply { 
                    put(activeCurrencyField!!, newBalance) 
                })
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
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        tonalElevation = 8.dp,
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp),
                    shape = RoundedCornerShape(2.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                ) {}
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(48.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                getCurrencySymbol(currencyCode),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Column {
                        Text(
                            getCurrencyName(currencyCode),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            currencyCode,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close")
                }
            }

            // Current balance display
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Initial Balance",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        if (currentBalance.isEmpty()) "0.00" else currentBalance,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (currentBalance.startsWith("-")) MaterialTheme.colorScheme.error
                               else MaterialTheme.colorScheme.primary
                    )
                    Text(
                        if (currentBalance.isEmpty()) "No balance set"
                        else if (currentBalance.startsWith("-")) "Debt / Expense"
                        else "Asset / Income",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Calculator Numpad
            CalculatorNumpadV2(
                amount = currentBalance,
                onAmountChange = onBalanceChange,
                onTransactionTypeChange = { /* Not needed for initial balance */ },
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
    description: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            icon,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Column {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    description?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            content()
        }
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
        shape = RoundedCornerShape(12.dp),
        color = if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
               else MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isActive) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    getCurrencySymbol(currencyCode),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Column {
                    Text(
                        currencyCode,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        if (currentBalance.isEmpty()) "No balance set" 
                        else if (currentBalance.startsWith("-")) "Debt / Expense" 
                        else "Asset / Income",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    if (currentBalance.isEmpty()) "0.00" else currentBalance,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (currentBalance.startsWith("-")) MaterialTheme.colorScheme.error
                           else MaterialTheme.colorScheme.primary
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Rounded.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Edit",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun StepHeader(icon: ImageVector, title: String, description: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(bottom = 48.dp)
    ) {
        Surface(
            modifier = Modifier.size(80.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.padding(20.dp).fillMaxSize(),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
