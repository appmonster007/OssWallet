package com.osscabinet.ossWallet.ui.screens.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.ui.components.scrollBottomPadding
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import kotlinx.coroutines.launch
import java.util.Locale

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
                        ${
                        rates?.rates?.entries?.take(5)
                            ?.joinToString(",\n                        ") { "\"${it.key}\": ${it.value}" }
                    }
                        ... (+${(rates?.rates?.size ?: 5) - 5} more)
                      }
                    }
                    """.trimIndent()
                }
                Column {
                    Text(
                        "New rates fetched. Do you want to save them?",
                        style = AppTextStyles.transactionTitle
                    )
                    Spacer(modifier = Modifier.height(AeonTheme.dimensions.spacingSm))
                    Surface(
                        color = AppColors.insetSurface,
                        shape = RoundedCornerShape(AeonTheme.dimensions.spacingSm),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = jsonPreview,
                            modifier = Modifier.padding(AeonTheme.dimensions.spacingSm),
                            style = AppTextStyles.caption,
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
            text = {
                Text(
                    uiState.syncError
                        ?: "An unexpected error occurred while fetching exchange rates."
                )
            },
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
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                        }
                    },
                    actions = {
                        if (pagerState.currentPage == 0) {
                            val lastUpdatedStr =
                                if (uiState.lastUpdated > 0) com.osscabinet.ossWallet.ui.util.TimeUtils.formatFullDateTime(
                                    uiState.lastUpdated
                                ) else "Never"

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = lastUpdatedStr,
                                        style = AppTextStyles.formFieldHint,
                                        color = AppColors.border
                                    )
                                }
                                IconButton(onClick = { viewModel.refreshRates() }) {
                                    if (uiState.isLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(AeonTheme.dimensions.iconSizeSm),
                                            strokeWidth = 2.dp
                                        )
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
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = AppColors.primary
                    )
                },
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
                style = AppTextStyles.tableColumnHeader,
                color = AppColors.contentSecondary.copy(alpha = 0.8f)
            )
            Text(
                "VALUE (1 ${uiState.baseCurrency})",
                style = AppTextStyles.tableColumnHeader,
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
                            .padding(
                                horizontal = AeonTheme.dimensions.screenPadding,
                                vertical = AeonTheme.dimensions.spacingMd
                            ),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (isBase) AppColors.primary else AppColors.inputSurface,
                                modifier = Modifier.size(40.dp),
                                border = if (!isBase) BorderStroke(
                                    1.dp,
                                    AppColors.divider.copy(alpha = 0.4f)
                                ) else null,
                                tonalElevation = if (isBase) 2.dp else 1.dp
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = rate.currencyCode.take(2),
                                        style = AppTextStyles.cardDelta.copy(fontWeight = FontWeight.Black),
                                        color = if (isBase) AppColors.onPrimary else AppColors.contentPrimary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(AeonTheme.dimensions.spacingLg))
                            Column(verticalArrangement = Arrangement.Center) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        rate.currencyCode,
                                        style = AppTextStyles.screenSubTitle,
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
                                                style = AppTextStyles.metaLabel.copy(fontWeight = FontWeight.Black),
                                                color = AppColors.onPrimary,
                                                modifier = Modifier.padding(
                                                    horizontal = 4.dp,
                                                    vertical = 1.dp
                                                )
                                            )
                                        }
                                    }
                                }
                                Text(
                                    "1 ${uiState.baseCurrency} = ${
                                        String.format(
                                            Locale.US,
                                            "%.4f",
                                            rate.rate
                                        )
                                    }",
                                    style = AppTextStyles.formFieldHint,
                                    color = AppColors.contentSecondary,
                                    modifier = Modifier.padding(top = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = String.format(Locale.US, "%.4f", rate.rate),
                            style = AppTextStyles.cardTitle.copy(
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
                    val lastUpdatedStr =
                        com.osscabinet.ossWallet.ui.util.TimeUtils.formatFullDateTime(uiState.lastUpdated)

                    Text(
                        text = "Last updated: $lastUpdatedStr",
                        style = AppTextStyles.formFieldHint,
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
            .padding(
                horizontal = AeonTheme.dimensions.screenPadding,
                vertical = AeonTheme.dimensions.spacingSm
            ),
        verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)
    ) {
        // ── Display Card (38% of available height) ──────────────────────────
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.38f),
            color = AppColors.panelSurface,
            shape = RoundedCornerShape(AeonTheme.dimensions.cardCornerRadius),
            border = BorderStroke(1.dp, AppColors.divider.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(AeonTheme.dimensions.cardPadding),
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                // FROM row
                ConverterRow(
                    label = "FROM",
                    amount = amountText.ifEmpty { "0" },
                    currencyCode = fromCurrency,
                    currencies = currencies,
                    onCurrencyChange = { fromCurrency = it },
                    isSource = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            AppColors.inputSurface,
                            RoundedCornerShape(AeonTheme.dimensions.containerCornerRadius)
                        )
                        .border(
                            BorderStroke(1.dp, AppColors.divider.copy(alpha = 0.4f)),
                            RoundedCornerShape(AeonTheme.dimensions.containerCornerRadius)
                        )
                        .padding(
                            horizontal = AeonTheme.dimensions.spacingLg,
                            vertical = AeonTheme.dimensions.spacingMd
                        )
                )

                // Swap + rate divider row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)
                ) {
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = AppColors.divider.copy(alpha = 0.3f)
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            color = AppColors.primaryContainer.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, AppColors.primary.copy(alpha = 0.1f))
                        ) {
                            Text(
                                text = "1 $fromCurrency = ${
                                    String.format(
                                        Locale.US,
                                        "%.4f",
                                        currentRate
                                    )
                                } $toCurrency",
                                style = AppTextStyles.formFieldHint.copy(fontWeight = FontWeight.Black),
                                color = AppColors.primary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
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
                            modifier = Modifier.size(36.dp),
                            shadowElevation = 4.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.SwapVert,
                                    contentDescription = "Swap",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = AppColors.divider.copy(alpha = 0.3f)
                    )
                }

                // TO row
                ConverterRow(
                    label = "TO",
                    amount = String.format(Locale.US, "%,.2f", conversionResult),
                    currencyCode = toCurrency,
                    currencies = currencies,
                    onCurrencyChange = { toCurrency = it },
                    isSource = false,
                    onCopy = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        clipboardManager.setText(
                            androidx.compose.ui.text.AnnotatedString(
                                String.format(
                                    Locale.US,
                                    "%,.2f",
                                    conversionResult
                                )
                            )
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            AppColors.primaryContainer.copy(alpha = 0.08f),
                            RoundedCornerShape(AeonTheme.dimensions.containerCornerRadius)
                        )
                        .border(
                            BorderStroke(1.dp, AppColors.primary.copy(alpha = 0.15f)),
                            RoundedCornerShape(AeonTheme.dimensions.containerCornerRadius)
                        )
                        .padding(
                            horizontal = AeonTheme.dimensions.spacingLg,
                            vertical = AeonTheme.dimensions.spacingMd
                        )
                )
            }
        }

        // ── Numpad (62% of available height) ────────────────────────────────
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.62f),
            color = AppColors.recessedSurface.copy(alpha = 0.4f),
            shape = RoundedCornerShape(AeonTheme.dimensions.cardCornerRadius),
            border = BorderStroke(1.dp, AppColors.divider.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(AeonTheme.dimensions.spacingLg),
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
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)
                    ) {
                        row.forEach { key ->
                            KeyButton(
                                text = key,
                                onClick = {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                    when (key) {
                                        "C" -> if (amountText.isNotEmpty()) amountText =
                                            amountText.dropLast(1)

                                        "." -> if (!amountText.contains(".")) amountText += "."
                                        else -> if (amountText == "0" && key != ".") amountText =
                                            key
                                        else if (amountText.length < 10) amountText += key
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
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
private fun ConverterRow(
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

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)
    ) {
        // Left: label + currency pill stacked
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = AppTextStyles.tableColumnHeader,
                color = if (isSource) AppColors.contentSecondary else AppColors.primary
            )
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                Surface(
                    onClick = { expanded = true },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
                    color = AppColors.panelSurface,
                    shape = RoundedCornerShape(AeonTheme.dimensions.containerCornerRadius),
                    border = BorderStroke(
                        1.5.dp,
                        if (isSource) AppColors.divider.copy(alpha = 0.6f) else AppColors.primary.copy(
                            alpha = 0.4f
                        )
                    ),
                    tonalElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = currencyCode,
                            style = AppTextStyles.cardTitle.copy(fontWeight = FontWeight.Black),
                            color = AppColors.contentPrimary
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = AppColors.primary,
                            modifier = Modifier.size(20.dp)
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
                                    style = AppTextStyles.listItemTitle.copy(
                                        fontWeight = if (currency == currencyCode) FontWeight.Bold else FontWeight.Normal
                                    )
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
        }

        // Right: big amount + optional copy button
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
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
                    style = when {
                        targetAmount.length > 10 -> AppTextStyles.heroAmountDisplay
                        targetAmount.length > 7 -> AppTextStyles.heroAmountMedium
                        else -> AppTextStyles.heroAmountLarge
                    }.copy(letterSpacing = (-1.5).sp),
                    color = if (isSource) AppColors.contentPrimary else AppColors.primary,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (onCopy != null) {
                Spacer(modifier = Modifier.width(AeonTheme.dimensions.spacingSm))
                Surface(
                    onClick = onCopy,
                    color = AppColors.primaryContainer.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.size(30.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            tint = AppColors.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun KeyButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interactionSource =
        remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(AeonTheme.dimensions.containerCornerRadius),
        color = if (isPressed) AppColors.primary.copy(alpha = 0.1f) else AppColors.inputSurface,
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
                    style = AppTextStyles.keyButtonLabel,
                    color = if (text == ".") AppColors.primary else AppColors.contentPrimary
                )
            }
        }
    }
}

// --- PREVIEWS ---

