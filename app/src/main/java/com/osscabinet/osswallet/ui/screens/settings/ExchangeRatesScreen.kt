package com.osscabinet.ossWallet.ui.screens.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.ui.components.AeonSectionHeader
import com.osscabinet.ossWallet.ui.components.scrollBottomPadding
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import kotlinx.coroutines.launch
import java.util.*
import com.osscabinet.ossWallet.ui.theme.AppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExchangeRatesScreen(
    onMenuClick: () -> Unit,
    onBackClick: () -> Unit = {},
    isTopLevel: Boolean = false,
    viewModel: CurrencyViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()
    val tabs = listOf("Rates", "Converter")

    BackHandler(enabled = uiState.syncError != null) { viewModel.clearError() }
    BackHandler(enabled = uiState.pendingRates != null) { viewModel.cancelSaveRates() }

    if (uiState.pendingRates != null) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelSaveRates() },
            title = { Text("Confirm Exchange Rates") },
            text = {
                val rates = uiState.pendingRates
                val jsonPreview = remember(rates) {
                    """
                    {
                      "base_currency": "${rates?.baseCurrency}",
                      "rates": {
                        ${rates?.rates?.entries?.take(5)?.joinToString(",\n                        ") { "\"${it.key}\": ${it.value}" }}
                        ... (+${(rates?.rates?.size ?: 5) - 5} more)
                      }
                    }
                    """.trimIndent()
                }
                Column {
                    Text("New rates fetched. Do you want to save them?", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(AeonTheme.dimensions.spacingSm))
                    Surface(
                        color = AppColors.insetSurface,
                        shape = RoundedCornerShape(AeonTheme.dimensions.spacingSm),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = jsonPreview,
                            modifier = Modifier.padding(AeonTheme.dimensions.spacingSm),
                            style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                            color = AppColors.contentSecondary
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.confirmSaveRates() }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelSaveRates() }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (uiState.syncError != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("Exchange Rate Error") },
            text = { Text(uiState.syncError ?: "An unexpected error occurred while fetching exchange rates.") },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    viewModel.clearError()
                    viewModel.refreshRates()
                }) {
                    Text("Retry")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Exchange Rates") },
                    navigationIcon = {
                        if (isTopLevel) {
                            IconButton(onClick = onMenuClick) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                            }
                        } else {
                            IconButton(onClick = onBackClick) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                    },
                    actions = {
                        if (pagerState.currentPage == 0) {
                            val lastUpdatedStr = if (uiState.lastUpdated > 0) com.osscabinet.ossWallet.ui.util.TimeUtils.formatFullDateTime(uiState.lastUpdated) else "Never"
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = lastUpdatedStr,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AppColors.border
                                    )
                                }
                                IconButton(onClick = { viewModel.refreshRates() }) {
                                    if (uiState.isLoading) {
                                        CircularProgressIndicator(modifier = Modifier.size(AeonTheme.dimensions.iconSizeSm), strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                                    }
                                }
                            }
                        }
                    }
                )
                TabRow(
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = AppColors.panelSurface,
                    indicator = { tabPositions ->
                        if (pagerState.currentPage < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                                color = AppColors.primary
                            )
                        }
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                            text = { Text(title) }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) { page ->
            when (page) {
                0 -> RatesTab(uiState)
                1 -> ConverterTab(
                    uiState = uiState,
                    onConvert = { amount, from, to, callback ->
                        viewModel.convert(amount, from, to, callback)
                    }
                )
            }
        }
    }
}

@Composable
fun RatesTab(uiState: CurrencyUiState) {
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredRates = remember(uiState.rates, searchQuery) {
        uiState.rates.filter { 
            it.currencyCode.contains(searchQuery, ignoreCase = true) 
        }.sortedBy { it.currencyCode }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Box(modifier = Modifier.padding(AeonTheme.dimensions.screenPadding)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search currencies...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = AppColors.primary) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                shape = RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = AppColors.inputSurface,
                    unfocusedContainerColor = AppColors.inputSurface,
                    focusedBorderColor = AppColors.primary,
                    unfocusedBorderColor = AppColors.divider
                )
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColors.recessedSurface.copy(alpha = 0.6f))
                .padding(horizontal = AeonTheme.dimensions.screenPadding, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "CURRENCY", 
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, letterSpacing = 2.sp), 
                color = AppColors.contentSecondary.copy(alpha = 0.8f)
            )
            Text(
                "VALUE (1 ${uiState.baseCurrency})", 
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, letterSpacing = 2.sp), 
                color = AppColors.contentSecondary.copy(alpha = 0.8f)
            )
        }

        HorizontalDivider(color = AppColors.divider)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = scrollBottomPadding())
        ) {
            items(filteredRates) { rate ->
                val isBase = rate.currencyCode == uiState.baseCurrency
                Surface(
                    onClick = { /* Set as base logic */ },
                    color = if (isBase) AppColors.primaryContainer.copy(alpha = 0.1f) else Color.Transparent,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = AeonTheme.dimensions.screenPadding, vertical = AeonTheme.dimensions.spacingMd),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (isBase) AppColors.primary else Color.White,
                                modifier = Modifier.size(40.dp),
                                border = if (!isBase) BorderStroke(1.dp, AppColors.divider.copy(alpha = 0.4f)) else null,
                                tonalElevation = if (isBase) 2.dp else 1.dp
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = rate.currencyCode.take(2),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Black,
                                        color = if (isBase) AppColors.onPrimary else AppColors.contentPrimary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(AeonTheme.dimensions.spacingLg))
                            Column(verticalArrangement = Arrangement.Center) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        rate.currencyCode,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isBase) AppColors.primary else AppColors.contentPrimary
                                    )
                                    if (isBase) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            color = AppColors.primary,
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                "BASE",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Black),
                                                color = AppColors.onPrimary,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    "1 ${uiState.baseCurrency} = ${String.format(Locale.US, "%.4f", rate.rate)}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                    color = AppColors.contentSecondary,
                                    modifier = Modifier.padding(top = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = String.format(Locale.US, "%.4f", rate.rate),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontFeatureSettings = "tnum",
                                color = if (isBase) AppColors.primary else AppColors.contentPrimary,
                                letterSpacing = (-0.5).sp
                            )
                        )
                    }
                }
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = AeonTheme.dimensions.screenPadding), 
                    color = AppColors.divider.copy(alpha = 0.2f)
                )
            }
            
            item {
                if (uiState.lastUpdated > 0) {
                    val lastUpdatedStr = com.osscabinet.ossWallet.ui.util.TimeUtils.formatFullDateTime(uiState.lastUpdated)
                    
                    Text(
                        text = "Last updated: $lastUpdatedStr",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.contentSecondary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AeonTheme.dimensions.screenPadding),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun ConverterTab(
    uiState: CurrencyUiState,
    onConvert: (Double, String, String, (Double) -> Unit) -> Unit
) {
    var amountText by remember { mutableStateOf("1") }
    var fromCurrency by remember { mutableStateOf(uiState.baseCurrency.ifEmpty { "USD" }) }
    var toCurrency by remember { mutableStateOf("EUR") }
    
    val currencies = uiState.rates.map { it.currencyCode }.sorted()
    var conversionResult by remember { mutableStateOf(0.0) }
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    
    // Calculate current rate for display
    val currentRate = remember(fromCurrency, toCurrency, uiState.rates) {
        val fromRate = uiState.rates.find { it.currencyCode == fromCurrency }?.rate ?: 1.0
        val toRate = uiState.rates.find { it.currencyCode == toCurrency }?.rate ?: 1.0
        if (fromRate != 0.0) toRate / fromRate else 0.0
    }
    
    LaunchedEffect(amountText, fromCurrency, toCurrency, uiState.rates) {
        val amount = amountText.toDoubleOrNull() ?: 0.0
        onConvert(amount, fromCurrency, toCurrency) { result ->
            conversionResult = result
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AeonTheme.dimensions.screenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingLg)
    ) {
        // High-Precision Instrument Display
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColors.panelSurface, RoundedCornerShape(AeonTheme.dimensions.cardCornerRadius))
                .border(BorderStroke(1.dp, AppColors.divider.copy(alpha = 0.5f)), RoundedCornerShape(AeonTheme.dimensions.cardCornerRadius))
                .padding(AeonTheme.dimensions.cardPadding),
            verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingSm)
        ) {
            // Source Section
            ConversionUnit(
                label = "FROM",
                amount = amountText.ifEmpty { "0" },
                currencyCode = fromCurrency,
                currencies = currencies,
                onCurrencyChange = { fromCurrency = it },
                isSource = true,
                modifier = Modifier
                    .background(Color.White, RoundedCornerShape(AeonTheme.dimensions.containerCornerRadius))
                    .border(BorderStroke(1.dp, AppColors.divider.copy(alpha = 0.4f)), RoundedCornerShape(AeonTheme.dimensions.containerCornerRadius))
                    .padding(AeonTheme.dimensions.spacingLg)
            )

            // Dynamic Interaction Layer with Rate Badge
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Rate Badge (Floating above)
                Surface(
                    color = AppColors.primaryContainer.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, AppColors.primary.copy(alpha = 0.1f))
                ) {
                    Text(
                        text = "1 $fromCurrency = ${String.format(Locale.US, "%.4f", currentRate)} $toCurrency",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.primary,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Background Connector Lines
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = AeonTheme.dimensions.spacingXl),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = AppColors.divider.copy(alpha = 0.3f))
                        Spacer(modifier = Modifier.width(72.dp))
                        HorizontalDivider(modifier = Modifier.weight(1f), color = AppColors.divider.copy(alpha = 0.3f))
                    }
                    
                    // Swap Button (Elevated)
                    Surface(
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            val temp = fromCurrency
                            fromCurrency = toCurrency
                            toCurrency = temp
                        },
                        shape = RoundedCornerShape(50),
                        color = AppColors.primary,
                        contentColor = AppColors.onPrimary,
                        modifier = Modifier.size(44.dp),
                        shadowElevation = 6.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.SwapVert,
                                contentDescription = "Swap",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            // Target Section
            ConversionUnit(
                label = "TO",
                amount = String.format(Locale.US, "%,.2f", conversionResult),
                currencyCode = toCurrency,
                currencies = currencies,
                onCurrencyChange = { toCurrency = it },
                isSource = false,
                onCopy = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(String.format(Locale.US, "%,.2f", conversionResult)))
                },
                modifier = Modifier
                    .background(AppColors.primaryContainer.copy(alpha = 0.08f), RoundedCornerShape(AeonTheme.dimensions.containerCornerRadius))
                    .border(BorderStroke(1.dp, AppColors.primary.copy(alpha = 0.15f)), RoundedCornerShape(AeonTheme.dimensions.containerCornerRadius))
                    .padding(AeonTheme.dimensions.spacingLg)
            )
        }

        // Professional Keypad Container
        Surface(
            modifier = Modifier.weight(1f),
            color = AppColors.recessedSurface.copy(alpha = 0.4f),
            shape = RoundedCornerShape(AeonTheme.dimensions.cardCornerRadius),
            border = BorderStroke(1.dp, AppColors.divider.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(AeonTheme.dimensions.spacingLg),
                verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)
            ) {
                val keys = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf(".", "0", "C")
                )
                
                keys.forEach { row ->
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)
                    ) {
                        row.forEach { key ->
                            KeyButton(
                                text = key,
                                onClick = {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                    when (key) {
                                        "C" -> if (amountText.isNotEmpty()) amountText = amountText.dropLast(1)
                                        "." -> if (!amountText.contains(".")) amountText += "."
                                        else -> if (amountText == "0" && key != ".") amountText = key
                                                else if (amountText.length < 10) amountText += key
                                    }
                                },
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )
                        }
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConversionUnit(
    label: String,
    amount: String,
    currencyCode: String,
    currencies: List<String>,
    onCurrencyChange: (String) -> Unit,
    isSource: Boolean,
    modifier: Modifier = Modifier,
    onCopy: (() -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)
    ) {
        // Label and Copy
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, letterSpacing = 2.sp),
                color = if (isSource) AppColors.contentSecondary else AppColors.primary
            )
            
            if (onCopy != null) {
                Surface(
                    onClick = onCopy,
                    color = AppColors.primaryContainer.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            tint = AppColors.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Selector and Value
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Currency Selector Pill
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it },
                modifier = Modifier.wrapContentWidth()
            ) {
                Surface(
                    onClick = { expanded = true },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
                    color = Color.White,
                    shape = RoundedCornerShape(AeonTheme.dimensions.containerCornerRadius),
                    border = BorderStroke(1.5.dp, if (isSource) AppColors.divider.copy(alpha = 0.6f) else AppColors.primary.copy(alpha = 0.4f)),
                    tonalElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = currencyCode,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = AppColors.contentPrimary
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = AppColors.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(AppColors.popupSurface)
                ) {
                    currencies.forEach { currency ->
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    currency, 
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (currency == currencyCode) FontWeight.Bold else FontWeight.Normal
                                ) 
                            },
                            onClick = {
                                onCurrencyChange(currency)
                                expanded = false
                            },
                            colors = MenuDefaults.itemColors(
                                textColor = if (currency == currencyCode) AppColors.primary else AppColors.contentPrimary
                            )
                        )
                    }
                }
            }

            // Big Number Display
            androidx.compose.animation.AnimatedContent(
                targetState = amount,
                transitionSpec = {
                    (androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically { it / 2 })
                        .togetherWith(androidx.compose.animation.fadeOut() + androidx.compose.animation.slideOutVertically { -it / 2 })
                },
                label = "AmountAnimation",
                modifier = Modifier.weight(1f)
            ) { targetAmount ->
                Text(
                    text = targetAmount,
                    style = (if (targetAmount.length > 10) MaterialTheme.typography.displaySmall 
                            else if (targetAmount.length > 7) MaterialTheme.typography.displayMedium 
                            else MaterialTheme.typography.displayLarge).copy(
                        fontFeatureSettings = "tnum",
                        letterSpacing = (-1.5).sp
                    ),
                    fontWeight = FontWeight.Bold,
                    color = if (isSource) AppColors.contentPrimary else AppColors.primary,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp)
                )
            }
        }
    }
}




@Composable
fun KeyButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(AeonTheme.dimensions.containerCornerRadius),
        color = if (isPressed) AppColors.primary.copy(alpha = 0.1f) else Color.White,
        contentColor = AppColors.contentPrimary,
        border = BorderStroke(1.dp, AppColors.divider.copy(alpha = 0.2f)),
        tonalElevation = if (isPressed) 2.dp else 0.dp,
        interactionSource = interactionSource
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            if (text == "C") {
                Icon(
                    Icons.AutoMirrored.Rounded.Backspace, 
                    contentDescription = "Clear",
                    modifier = Modifier.size(26.dp),
                    tint = AppColors.error.copy(alpha = 0.7f)
                )
            } else {
                Text(
                    text = text, 
                    style = MaterialTheme.typography.headlineMedium, 
                    fontWeight = FontWeight.Bold,
                    color = if (text == ".") AppColors.primary else AppColors.contentPrimary
                )
            }
        }
    }
}



