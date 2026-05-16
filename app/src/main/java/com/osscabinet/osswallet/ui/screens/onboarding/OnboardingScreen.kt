package com.osscabinet.ossWallet.ui.screens.onboarding

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
import com.osscabinet.ossWallet.ui.screens.accounts.entry.components.CurrencyManagementSection
import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter.formatAmount
import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter.getCurrencyName
import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter.getCurrencySymbol
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService.getAllCurrencies
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.util.*
import com.osscabinet.ossWallet.ui.screens.accounts.entry.AccountEntryViewModel
import com.osscabinet.ossWallet.ui.screens.accounts.entry.AccountEntryEvent
import com.osscabinet.ossWallet.ui.screens.settings.SettingsViewModel
import com.osscabinet.ossWallet.ui.screens.settings.SyncState
import java.math.BigDecimal
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.domain.model.TransactionType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    viewModel: SettingsViewModel,
    accountEntryViewModel: AccountEntryViewModel,
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
        containerColor = AppColors.screenBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Step ${step + 1} of ${steps.size}",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = AppColors.primary
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
                    containerColor = AppColors.transparent
                )
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = AppColors.screenBackground,
                tonalElevation = 0.dp
            ) {
                Column {
                    // Progress indicator
                    LinearProgressIndicator(
                        progress = { (step + 1).toFloat() / steps.size },
                        modifier = Modifier.fillMaxWidth().height(AeonTheme.dimensions.spacingXs),
                        color = AppColors.primary,
                        trackColor = AppColors.componentOnCard
                    )

                    Row(
                        modifier = Modifier
                            .padding(AeonTheme.dimensions.spacingXl)
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
                                allowedCurrencies.forEach { curr ->
                                    accountEntryViewModel.onEvent(AccountEntryEvent.SetInitialBalance(curr, initialBalances[curr] ?: ""))
                                }
                                accountEntryViewModel.onEvent(AccountEntryEvent.SetName(accountName))
                                accountEntryViewModel.onEvent(AccountEntryEvent.SetPrimaryCurrency(currency))
                                accountEntryViewModel.onEvent(AccountEntryEvent.SetAllowedCurrencies(allowedCurrencies))
                                accountEntryViewModel.onEvent(AccountEntryEvent.SetStructureType("Cash"))
                                accountEntryViewModel.onEvent(AccountEntryEvent.SetColor("#004D40"))
                                accountEntryViewModel.onEvent(AccountEntryEvent.SetIcon("Payments"))
                                
                                accountEntryViewModel.saveAccount {
                                    viewModel.setOnboardingCompleted(true)
                                    onFinished()
                                }
                            }
                        },
                            modifier = Modifier.height(56.dp),
                            shape = RoundedCornerShape(AeonTheme.dimensions.buttonCornerRadius),
                            contentPadding = PaddingValues(horizontal = AeonTheme.dimensions.dialogCornerRadius)
                        ) {
                            Text(
                                if (step < steps.size - 1) "NEXT" else "GET STARTED",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold)
                            )
                            Spacer(modifier = Modifier.width(AeonTheme.dimensions.spacingMd))
                            Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(AeonTheme.dimensions.iconSizeSm))
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
                    .padding(horizontal = AeonTheme.dimensions.spacingXl),
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
                    containerColor = AppColors.cardSurface,
                    shape = RoundedCornerShape(topStart = AeonTheme.dimensions.dialogCornerRadius, topEnd = AeonTheme.dimensions.dialogCornerRadius),
                    dragHandle = { BottomSheetDefaults.DragHandle(color = AppColors.divider.copy(alpha = 0.4f)) },
                    modifier = Modifier.fillMaxHeight(0.9f)
                ) {
                    Column(modifier = Modifier.fillMaxSize().padding(horizontal = AeonTheme.dimensions.spacingXl).verticalScroll(rememberScrollState())) {
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
            modifier = Modifier.fillMaxWidth().padding(bottom = AeonTheme.dimensions.screenPadding),
            placeholder = { Text("Search currencies...") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(AeonTheme.dimensions.iconSizeMd)) },
            shape = RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius),
            singleLine = true,
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = AppColors.cardSurface,
                focusedContainerColor = AppColors.cardSurface,
                unfocusedIndicatorColor = AppColors.transparent,
                focusedIndicatorColor = AppColors.primary.copy(alpha = 0.5f)
            )
        )

        Surface(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            shape = RoundedCornerShape(AeonTheme.dimensions.dialogCornerRadius),
            color = AppColors.cardSurface,
            border = BorderStroke(0.5.dp, AppColors.divider.copy(alpha = 0.15f))
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(AeonTheme.dimensions.screenPadding),
                verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingSm)
            ) {
                items(filteredCurrencies) { code ->
                    val isSelected = selectedCurrency == code
                    Surface(
                        onClick = { onCurrencySelected(code) },
                        shape = RoundedCornerShape(AeonTheme.dimensions.containerCornerRadius),
                        color = if (isSelected) AppColors.primary.copy(alpha = 0.1f) else AppColors.transparent,
                        border = if (isSelected) BorderStroke(0.5.dp, AppColors.primary.copy(alpha = 0.3f)) else null
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(AeonTheme.dimensions.screenPadding),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier.size(AeonTheme.dimensions.iconSizeXl),
                                shape = CircleShape,
                                color = if (isSelected) AppColors.primary.copy(alpha = 0.2f) else AppColors.componentOnCard
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        getCurrencySymbol(code),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (isSelected) AppColors.primary else AppColors.contentSecondary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(AeonTheme.dimensions.screenPadding))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    getCurrencyName(code),
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold)
                                )
                                Text(code, style = MaterialTheme.typography.labelSmall, color = AppColors.contentSecondary.copy(alpha = 0.6f))
                            }
                            if (isSelected) {
                                when (syncState) {
                                    is SyncState.Loading -> CircularProgressIndicator(modifier = Modifier.size(AeonTheme.dimensions.iconSizeMd), strokeWidth = 2.dp)
                                    else -> Icon(Icons.Rounded.CheckCircle, contentDescription = "Selected", tint = AppColors.primary, modifier = Modifier.size(AeonTheme.dimensions.iconSizeMd))
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(AeonTheme.dimensions.dialogCornerRadius))
    }
}

@Composable
fun ThemeStep(selectedTheme: String, onThemeSelected: (String) -> Unit) {
    val scrollState = rememberScrollState()
    Column(modifier = Modifier.fillMaxWidth().verticalScroll(scrollState)) {
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
        
        Column(verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingLg)) {
            themes.forEach { (name, desc, icon) ->
                val isSelected = selectedTheme == name
                Surface(
                    onClick = { onThemeSelected(name) },
                    shape = RoundedCornerShape(AeonTheme.dimensions.dialogCornerRadius),
                    color = if (isSelected) AppColors.primary.copy(alpha = 0.08f) else AppColors.cardSurface,
                    border = BorderStroke(
                        width = if (isSelected) 1.5.dp else 0.5.dp,
                        color = if (isSelected) AppColors.primary.copy(alpha = 0.5f) else AppColors.divider.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(AeonTheme.dimensions.spacingXl),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(AeonTheme.dimensions.iconSize2xl),
                            shape = CircleShape,
                            color = if (isSelected) AppColors.primary.copy(alpha = 0.15f) else AppColors.componentOnCard
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(icon, contentDescription = null, tint = if (isSelected) AppColors.primary else AppColors.contentSecondary)
                            }
                        }
                        Spacer(modifier = Modifier.width(AeonTheme.dimensions.containerCornerRadius))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            Text(desc, style = MaterialTheme.typography.bodySmall, color = AppColors.contentSecondary.copy(alpha = 0.6f))
                        }
                        if (isSelected) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = AppColors.primary)
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(AeonTheme.dimensions.dialogCornerRadius))
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
            horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingLg)
        ) {
            listOf(2, 3).forEach { p ->
                val isSelected = selectedPrecision == p
                Surface(
                    onClick = { onPrecisionSelected(p) },
                    modifier = Modifier.weight(1f).height(120.dp),
                    shape = RoundedCornerShape(AeonTheme.dimensions.dialogCornerRadius),
                    color = if (isSelected) AppColors.primary.copy(alpha = 0.08f) else AppColors.cardSurface,
                    border = BorderStroke(
                        width = if (isSelected) 1.5.dp else 0.5.dp,
                        color = if (isSelected) AppColors.primary.copy(alpha = 0.5f) else AppColors.divider.copy(alpha = 0.15f)
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
                            color = if (isSelected) AppColors.primary else AppColors.contentPrimary
                        )
                        Text(
                            "DECIMALS",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                            color = if (isSelected) AppColors.primary.copy(alpha = 0.6f) else AppColors.contentSecondary.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(AeonTheme.dimensions.cardCornerRadius),
            color = AppColors.componentOnCard.copy(alpha = 0.3f),
            border = BorderStroke(0.5.dp, AppColors.divider.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier.padding(AeonTheme.dimensions.spacingXl),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Rounded.Info, contentDescription = null, tint = AppColors.primary, modifier = Modifier.size(AeonTheme.dimensions.iconSizeMd))
                Spacer(modifier = Modifier.width(AeonTheme.dimensions.spacingMd))
                Text(
                    text = "Example: ${formatAmount(BigDecimal("1234.5678"), selectedPrecision)}",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp),
                    color = AppColors.contentPrimary
                )
            }
        }
        Spacer(modifier = Modifier.height(AeonTheme.dimensions.dialogCornerRadius))
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
        verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingXl)
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
                shape = RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius),
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = AppColors.componentOnCard.copy(alpha = 0.3f),
                    focusedContainerColor = AppColors.componentOnCard.copy(alpha = 0.3f),
                    unfocusedIndicatorColor = AppColors.transparent,
                    focusedIndicatorColor = AppColors.primary
                )
            )
        }

        SectionCard(title = "CURRENCIES", icon = Icons.Rounded.CurrencyExchange) {
            Surface(
                onClick = onCurrencyMenuOpen,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius),
                color = AppColors.componentOnCard.copy(alpha = 0.3f),
                border = BorderStroke(1.dp, if (allowedCurrencies.isEmpty()) AppColors.error.copy(alpha = 0.3f) else AppColors.transparent)
            ) {
                Row(
                    modifier = Modifier.padding(AeonTheme.dimensions.screenPadding),
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
                                color = AppColors.contentSecondary.copy(alpha = 0.6f)
                            )
                        }
                    }
                    Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = AppColors.primary)
                }
            }
        }

        if (allowedCurrencies.isNotEmpty()) {
            SectionCard(title = "STARTING BALANCES", icon = Icons.Rounded.AccountBalance) {
                Column(verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)) {
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
        
        Spacer(modifier = Modifier.height(AeonTheme.dimensions.dialogCornerRadius))
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
        containerColor = AppColors.cardSurface,
        shape = RoundedCornerShape(topStart = AeonTheme.dimensions.dialogCornerRadius, topEnd = AeonTheme.dimensions.dialogCornerRadius),
        dragHandle = { BottomSheetDefaults.DragHandle(color = AppColors.divider.copy(alpha = 0.4f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AeonTheme.dimensions.spacingXl)
                .padding(bottom = AeonTheme.dimensions.spacing3xl)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.containerCornerRadius)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)) {
                    Surface(
                        modifier = Modifier.size(AeonTheme.dimensions.iconSize2xl),
                        shape = CircleShape,
                        color = AppColors.primary.copy(alpha = 0.15f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(getCurrencySymbol(currencyCode), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = AppColors.primary)
                        }
                    }
                    Column {
                        Text(getCurrencyName(currencyCode), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Text(currencyCode, style = MaterialTheme.typography.labelSmall, color = AppColors.contentSecondary.copy(alpha = 0.6f))
                    }
                }
                IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, contentDescription = "Close") }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(AeonTheme.dimensions.cardCornerRadius),
                color = AppColors.recessedSurface,
                border = BorderStroke(0.5.dp, AppColors.divider.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(AeonTheme.dimensions.spacingXl),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("STARTING BALANCE", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp), color = AppColors.contentSecondary.copy(alpha = 0.5f))
                    Text(
                        if (currentBalance.isEmpty()) "0.00" else currentBalance,
                        style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Black, letterSpacing = (-1).sp),
                        color = if (currentBalance.startsWith("-")) AppColors.error else AppColors.primary
                    )
                }
            }

            CalculatorNumpad(
                amount = currentBalance,
                onAmountChange = { newAmount, _ -> onBalanceChange(newAmount) },
                transactionType = TransactionType.INCOME,
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
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingSm)) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = AppColors.primary.copy(alpha = 0.7f))
            Text(
                title.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp),
                color = AppColors.primary.copy(alpha = 0.7f)
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
        shape = RoundedCornerShape(AeonTheme.dimensions.cardCornerRadius),
        color = if (isActive) AppColors.primary.copy(alpha = 0.08f) else AppColors.cardSurface,
        border = BorderStroke(
            width = if (isActive) 1.5.dp else 0.5.dp,
            color = if (isActive) AppColors.primary.copy(alpha = 0.5f) else AppColors.divider.copy(alpha = 0.15f)
        )
    ) {
        Row(
            modifier = Modifier.padding(AeonTheme.dimensions.containerCornerRadius),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingLg)) {
                Surface(
                    modifier = Modifier.size(AeonTheme.dimensions.iconSizeXl),
                    shape = CircleShape,
                    color = AppColors.componentOnCard
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
                        color = AppColors.contentSecondary.copy(alpha = 0.6f)
                    )
                }
            }
            Text(
                if (currentBalance.isEmpty()) "0.00" else currentBalance,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                color = if (currentBalance.startsWith("-")) AppColors.error else if (currentBalance.isEmpty()) AppColors.contentSecondary.copy(alpha = 0.3f) else AppColors.primary
            )
        }
    }
}

@Composable
fun StepHeader(icon: ImageVector, title: String, description: String) {
    Column(
        horizontalAlignment = Alignment.Start,
        modifier = Modifier.padding(bottom = AeonTheme.dimensions.dialogCornerRadius).fillMaxWidth()
    ) {
        Surface(
            modifier = Modifier.size(AeonTheme.dimensions.iconSize3xl),
            shape = RoundedCornerShape(AeonTheme.dimensions.containerCornerRadius),
            color = AppColors.primary.copy(alpha = 0.1f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(AeonTheme.dimensions.iconSizeLg), tint = AppColors.primary)
            }
        }
        Spacer(modifier = Modifier.height(AeonTheme.dimensions.spacingXl))
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp)
        )
        Spacer(modifier = Modifier.height(AeonTheme.dimensions.spacingSm))
        Text(
            description,
            style = MaterialTheme.typography.bodyLarge,
            color = AppColors.contentSecondary.copy(alpha = 0.7f),
            lineHeight = 24.sp
        )
    }
}
