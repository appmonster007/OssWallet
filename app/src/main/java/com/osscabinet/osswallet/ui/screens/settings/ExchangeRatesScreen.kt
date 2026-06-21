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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.ui.components.AeonHazeProvider
import com.osscabinet.ossWallet.ui.components.aeonHazeSource
import com.osscabinet.ossWallet.ui.components.AeonTopAppBar
import com.osscabinet.ossWallet.ui.components.AeonTopBarActionPill
import com.osscabinet.ossWallet.ui.components.AeonTopBarNavTitlePill
import com.osscabinet.ossWallet.ui.components.AeonTopBarSection
import com.osscabinet.ossWallet.ui.components.AppBarIconButton
import com.osscabinet.ossWallet.ui.components.GlassSurface
import com.osscabinet.ossWallet.ui.components.scrollBottomPadding
import com.osscabinet.ossWallet.ui.theme.AeonSize
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.HazeLayer
import kotlinx.coroutines.launch
import java.util.Locale

import com.osscabinet.ossWallet.ui.navigation.SharedNavViewModels

/**
 * A Value Object representing a valid configuration for the Exchange Rates screen.
 */
data class ExchangeRatesConfig(
    val isTopLevel: Boolean = false
)

/**
 * Grouped actions for the Exchange Rates screen.
 */
data class ExchangeRatesActions(
    val onMenuClick: () -> Unit,
    val onBackClick: () -> Unit = {}
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExchangeRatesScreen(
    config: ExchangeRatesConfig,
    actions: ExchangeRatesActions,
    vms: SharedNavViewModels
) {
    val viewModel = vms.currencyViewModel
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

    AeonHazeProvider(backgroundColor = AppColors.screenBackground) { hazeLayers ->
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets(0)
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .aeonHazeSource(hazeLayers.states[HazeLayer.Overlay])
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .padding(top = AeonSize.D64.value)
                    ) {
                        // Custom Tab Row Pill - Glassmorphism correctly implemented here
                        GlassSurface(
                            shape = CircleShape,
                            glassVariant = com.osscabinet.ossWallet.ui.theme.GlassVariant.Pill,
                            modifier = Modifier
                                .padding(horizontal = AeonTheme.dimensions.screenPadding)
                                .padding(bottom = AeonTheme.dimensions.spacingMd),
                            fallbackColor = AppColors.panelSurface
                        ) {
                            TabRow(
                                selectedTabIndex = pagerState.currentPage,
                                containerColor = Color.Transparent,
                                divider = {},
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
                                        text = { 
                                            Text(
                                                title,
                                                style = AppTextStyles.caption.copy(
                                                    fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Normal
                                                )
                                            ) 
                                        }
                                    )
                                }
                            }
                        }

                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
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
            }

            AeonTopAppBar(
                left = AeonTopBarSection(
                    content = {
                        AeonTopBarNavTitlePill(
                            title = "Exchange Rates",
                            isTopLevel = config.isTopLevel,
                            onMenuClick = if (config.isTopLevel) actions.onMenuClick else null,
                            onBackClick = if (!config.isTopLevel) actions.onBackClick else null
                        )
                    }
                ),
                right = AeonTopBarSection(
                    visible = pagerState.currentPage == 0,
                    content = {
                        AeonTopBarActionPill {
                            if (uiState.isLoading) {
                                Box(
                                    modifier = Modifier.size(AeonSize.D40.value),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(AeonTheme.dimensions.iconSizeSm),
                                        strokeWidth = 2.dp,
                                        color = AppColors.primary
                                    )
                                }
                            } else {
                                AppBarIconButton(
                                    icon = Icons.Default.Refresh,
                                    onClick = { viewModel.refreshRates() }
                                )
                            }
                        }
                    }
                )
            )
        }
    }
}

@Composable
fun RatesTab(uiState: CurrencyUiState) {
    var searchQuery by remember { mutableStateOf("") }
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isSmallScreen = configuration.screenHeightDp < 700

    val filteredRates = remember(uiState.rates, searchQuery) {
        uiState.rates.filter {
            it.currencyCode.contains(searchQuery, ignoreCase = true)
        }.sortedBy { it.currencyCode }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Box(modifier = Modifier.padding(
            horizontal = AeonTheme.dimensions.screenPadding,
            vertical = if (isSmallScreen) AeonTheme.dimensions.spacingSm else AeonTheme.dimensions.spacingMd
        )) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search currencies...", style = AppTextStyles.formFieldHint) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = AppColors.primary,
                        modifier = Modifier.size(if (isSmallScreen) 18.dp else 24.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(if (isSmallScreen) 18.dp else 24.dp))
                        }
                    }
                },
                shape = RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius),
                singleLine = true,
                textStyle = AppTextStyles.formFieldValue.copy(fontSize = if (isSmallScreen) 14.sp else 16.sp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = AppColors.inputSurface,
                    unfocusedContainerColor = AppColors.inputSurface,
                    focusedBorderColor = AppColors.primary,
                    unfocusedBorderColor = AppColors.divider.copy(alpha = 0.5f)
                )
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColors.recessedSurface)
                .padding(
                    horizontal = AeonTheme.dimensions.screenPadding, 
                    vertical = if (isSmallScreen) 8.dp else 12.dp
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "CURRENCY",
                style = if (isSmallScreen) AppTextStyles.tableColumnHeader.copy(fontSize = 9.sp) else AppTextStyles.tableColumnHeader,
                color = AppColors.contentSecondary.copy(alpha = 0.6f)
            )
            Text(
                "VALUE (1 ${uiState.baseCurrency})",
                style = if (isSmallScreen) AppTextStyles.tableColumnHeader.copy(fontSize = 9.sp) else AppTextStyles.tableColumnHeader,
                color = AppColors.contentSecondary.copy(alpha = 0.6f)
            )
        }

        HorizontalDivider(color = AppColors.divider.copy(alpha = 0.3f))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = scrollBottomPadding())
        ) {
            items(filteredRates) { rate ->
                val isBase = rate.currencyCode == uiState.baseCurrency
                Surface(
                    onClick = { /* Set as base logic */ },
                    color = if (isBase) AppColors.primaryContainer.copy(alpha = 0.08f) else Color.Transparent,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = AeonTheme.dimensions.screenPadding,
                                vertical = if (isSmallScreen) AeonTheme.dimensions.spacingSm else AeonTheme.dimensions.spacingMd
                            ),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(if (isSmallScreen) AeonTheme.dimensions.spacingMd else AeonTheme.dimensions.spacingLg)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius),
                                color = if (isBase) AppColors.primary else AppColors.inputSurface,
                                modifier = Modifier.size(if (isSmallScreen) 36.dp else 42.dp),
                                border = if (!isBase) BorderStroke(
                                    1.dp,
                                    AppColors.divider.copy(alpha = 0.3f)
                                ) else null,
                                tonalElevation = if (isBase) 2.dp else 0.dp
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = rate.currencyCode.take(2),
                                        style = if (isSmallScreen) AppTextStyles.cardDelta.copy(fontWeight = FontWeight.Black, fontSize = 11.sp)
                                                else AppTextStyles.cardDelta.copy(fontWeight = FontWeight.Black),
                                        color = if (isBase) AppColors.onPrimary else AppColors.contentPrimary
                                    )
                                }
                            }
                            Column(verticalArrangement = Arrangement.Center) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        rate.currencyCode,
                                        style = if (isSmallScreen) AppTextStyles.managementItemTitle else AppTextStyles.screenSubTitle,
                                        color = if (isBase) AppColors.primary else AppColors.contentPrimary
                                    )
                                    if (isBase) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            color = AppColors.primary,
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                "BASE",
                                                style = AppTextStyles.metaLabel.copy(fontWeight = FontWeight.Black, fontSize = 8.sp),
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
                                    style = AppTextStyles.formFieldHint.copy(fontSize = if (isSmallScreen) 10.sp else 11.sp),
                                    color = AppColors.contentSecondary.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(top = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = String.format(Locale.US, "%.4f", rate.rate),
                            style = (if (isSmallScreen) AppTextStyles.cardSectionTitle else AppTextStyles.cardTitle).copy(
                                color = if (isBase) AppColors.primary else AppColors.contentPrimary,
                                letterSpacing = (-0.5).sp,
                                fontSize = if (isSmallScreen) 16.sp else 18.sp
                            )
                        )
                    }
                }
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = AeonTheme.dimensions.screenPadding),
                    color = AppColors.divider.copy(alpha = 0.15f)
                )
            }

            item {
                if (uiState.lastUpdated > 0) {
                    val lastUpdatedStr =
                        com.osscabinet.ossWallet.ui.util.TimeUtils.formatFullDateTime(uiState.lastUpdated)

                    Text(
                        text = "Last updated: $lastUpdatedStr",
                        style = AppTextStyles.metaLabel,
                        color = AppColors.contentSecondary.copy(alpha = 0.5f),
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
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isSmallScreen = configuration.screenHeightDp < 700

    val displayWeight = if (isSmallScreen) 0.42f else 0.38f
    val numpadWeight = 1f - displayWeight

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
        verticalArrangement = Arrangement.spacedBy(if (isSmallScreen) AeonTheme.dimensions.spacingXs else AeonTheme.dimensions.spacingSm)
    ) {
        // ── Display Section ───────────────────────────────────────────────
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(displayWeight),
            color = AppColors.cardSurface,
            shape = RoundedCornerShape(AeonTheme.dimensions.cardCornerRadius),
            border = BorderStroke(1.dp, AppColors.divider.copy(alpha = 0.15f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (isSmallScreen) AeonTheme.dimensions.spacingSm else AeonTheme.dimensions.cardPadding),
                verticalArrangement = Arrangement.Center
            ) {
                ConverterRow(
                    label = "FROM",
                    amount = amountText.ifEmpty { "0" },
                    currencyCode = fromCurrency,
                    currencies = currencies,
                    onCurrencyChange = { fromCurrency = it },
                    isSource = true,
                    isSmall = isSmallScreen
                )

                // Compact Rate Badge + Swap Button
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = if (isSmallScreen) 2.dp else 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    HorizontalDivider(color = AppColors.divider.copy(alpha = 0.1f))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingSm)
                    ) {
                        Surface(
                            color = AppColors.recessedSurface.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, AppColors.divider.copy(alpha = 0.1f))
                        ) {
                            Text(
                                text = "1 $fromCurrency = ${String.format(Locale.US, "%.4f", currentRate)} $toCurrency",
                                style = AppTextStyles.metaLabel.copy(fontWeight = FontWeight.Bold, fontSize = if (isSmallScreen) 8.sp else 9.sp),
                                color = AppColors.contentSecondary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }

                        Surface(
                            onClick = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                val temp = fromCurrency
                                fromCurrency = toCurrency
                                toCurrency = temp
                            },
                            shape = CircleShape,
                            color = AppColors.primary,
                            contentColor = AppColors.onPrimary,
                            modifier = Modifier.size(if (isSmallScreen) 28.dp else 34.dp),
                            shadowElevation = 2.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.SwapVert,
                                    contentDescription = "Swap",
                                    modifier = Modifier.size(if (isSmallScreen) 16.dp else 20.dp)
                                )
                            }
                        }
                    }
                }

                ConverterRow(
                    label = "TO",
                    amount = String.format(Locale.US, "%,.2f", conversionResult),
                    currencyCode = toCurrency,
                    currencies = currencies,
                    onCurrencyChange = { toCurrency = it },
                    isSource = false,
                    isSmall = isSmallScreen,
                    onCopy = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        clipboardManager.setText(
                            androidx.compose.ui.text.AnnotatedString(String.format(Locale.US, "%,.2f", conversionResult))
                        )
                    }
                )
            }
        }

        // ── Numpad ────────────────────────────────────────────────────────
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(numpadWeight),
            color = AppColors.recessedSurface,
            shape = RoundedCornerShape(AeonTheme.dimensions.cardCornerRadius),
            border = BorderStroke(1.dp, AppColors.divider.copy(alpha = 0.1f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (isSmallScreen) AeonTheme.dimensions.spacingSm else AeonTheme.dimensions.spacingMd),
                verticalArrangement = Arrangement.spacedBy(if (isSmallScreen) AeonTheme.dimensions.spacingXs else AeonTheme.dimensions.spacingSm)
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
                        horizontalArrangement = Arrangement.spacedBy(if (isSmallScreen) AeonTheme.dimensions.spacingXs else AeonTheme.dimensions.spacingSm)
                    ) {
                        row.forEach { key ->
                            KeyButton(
                                text = key,
                                onClick = {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                    when (key) {
                                        "C" -> if (amountText.isNotEmpty()) amountText = amountText.dropLast(1)
                                        "." -> if (!amountText.contains(".")) amountText += "."
                                        else -> if (amountText == "0") amountText = key
                                                else if (amountText.length < 10) amountText += key
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                isSmall = isSmallScreen
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
    isSmall: Boolean = false,
    onCopy: (() -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isSource) AppColors.inputSurface else AppColors.primaryContainer.copy(alpha = 0.1f),
                RoundedCornerShape(AeonTheme.dimensions.containerCornerRadius)
            )
            .border(
                1.dp,
                if (isSource) AppColors.divider.copy(alpha = 0.2f) else AppColors.primary.copy(alpha = 0.15f),
                RoundedCornerShape(AeonTheme.dimensions.containerCornerRadius)
            )
            .padding(
                horizontal = if (isSmall) AeonTheme.dimensions.spacingSm else AeonTheme.dimensions.spacingMd,
                vertical = if (isSmall) 6.dp else 10.dp
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingSm)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            Text(
                text = label,
                style = if (isSmall) AppTextStyles.metaLabel.copy(fontWeight = FontWeight.Black, fontSize = 8.sp) else AppTextStyles.sectionOverline.copy(fontSize = 10.sp),
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
                    shape = RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius),
                    border = BorderStroke(1.dp, AppColors.divider.copy(alpha = 0.3f)),
                    tonalElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = currencyCode,
                            style = if (isSmall) AppTextStyles.managementItemTitle.copy(fontSize = 12.sp) else AppTextStyles.cardTitle.copy(fontSize = 16.sp),
                            color = AppColors.contentPrimary
                        )
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = AppColors.primary,
                            modifier = Modifier.size(if (isSmall) 14.dp else 18.dp)
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
                            text = { Text(currency, style = AppTextStyles.listItemTitle) },
                            onClick = {
                                onCurrencyChange(currency)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterEnd
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.animation.AnimatedContent(
                    targetState = amount,
                    transitionSpec = {
                        (androidx.compose.animation.fadeIn() + androidx.compose.animation.scaleIn()).togetherWith(
                            androidx.compose.animation.fadeOut() + androidx.compose.animation.scaleOut()
                        )
                    },
                    label = "AmountAnim"
                ) { targetAmount ->
                    Text(
                        text = targetAmount,
                        style = when {
                            isSmall -> when {
                                targetAmount.length > 10 -> AppTextStyles.heroAmount.copy(fontSize = 16.sp)
                                targetAmount.length > 7 -> AppTextStyles.heroAmount.copy(fontSize = 20.sp)
                                else -> AppTextStyles.heroAmount.copy(fontSize = 24.sp)
                            }
                            targetAmount.length > 10 -> AppTextStyles.heroAmountDisplay.copy(fontSize = 28.sp)
                            targetAmount.length > 7 -> AppTextStyles.heroAmountMedium.copy(fontSize = 32.sp)
                            else -> AppTextStyles.heroAmountLarge.copy(fontSize = 36.sp)
                        },
                        color = if (isSource) AppColors.contentPrimary else AppColors.primary,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                if (onCopy != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onCopy,
                        modifier = Modifier.size(if (isSmall) 20.dp else 28.dp)
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            tint = AppColors.primary.copy(alpha = 0.5f),
                            modifier = Modifier.size(if (isSmall) 12.dp else 16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun KeyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSmall: Boolean = false
) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(if (isSmall) AeonTheme.dimensions.spacingMd else AeonTheme.dimensions.componentCornerRadius),
        color = if (isPressed) AppColors.primary.copy(alpha = 0.08f) else AppColors.inputSurface,
        contentColor = if (isPressed) AppColors.primary else AppColors.contentPrimary,
        border = BorderStroke(
            1.dp,
            if (isPressed) AppColors.primary.copy(alpha = 0.2f) else AppColors.divider.copy(alpha = 0.05f)
        ),
        interactionSource = interactionSource
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            if (text == "C") {
                Icon(
                    Icons.AutoMirrored.Rounded.Backspace,
                    contentDescription = "Clear",
                    modifier = Modifier.size(if (isSmall) 18.dp else 22.dp),
                    tint = AppColors.error.copy(alpha = 0.6f)
                )
            } else {
                Text(
                    text = text,
                    style = (if (text == ".") AppTextStyles.calculatorOperator else AppTextStyles.calculatorKey).copy(
                        fontSize = if (isSmall) 18.sp else 22.sp
                    ),
                    color = if (text == ".") AppColors.primary else AppColors.contentPrimary
                )
            }
        }
    }
}


