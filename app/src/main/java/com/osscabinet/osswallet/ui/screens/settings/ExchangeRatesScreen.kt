package com.osscabinet.ossWallet.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
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
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
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
    val pagerState = remember {
        object : PagerState() {
            override val pageCount: Int get() = 2
        }
    }
    val scope = rememberCoroutineScope()
    val tabs = listOf("Rates", "Converter")

    // navigateBack removed

    if (uiState.pendingResponse != null) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelSaveRates() },
            title = { Text("Confirm Exchange Rates") },
            text = {
                val response = uiState.pendingResponse
                val jsonPreview = remember(response) {
                    """
                    {
                      "result": "${response?.result}",
                      "base_code": "${response?.baseCode}",
                      "rates": {
                        ${response?.rates?.entries?.take(5)?.joinToString(",\n                        ") { "\"${it.key}\": ${it.value}" }}
                        ... (+${(response?.rates?.size ?: 5) - 5} more)
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
                            val pattern = if (uiState.use24HourFormat) "MMM dd, yyyy HH:mm" else "MMM dd, yyyy hh:mm a"
                            val dateFormat = remember(uiState.use24HourFormat) { SimpleDateFormat(pattern, Locale.getDefault()) }
                            val lastUpdatedStr = if (uiState.lastUpdated > 0) dateFormat.format(Date(uiState.lastUpdated)) else "Never"
                            
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
                0 -> RatesTab(uiState, viewModel)
                1 -> ConverterTab(uiState, viewModel)
            }
        }
    }
}

@Composable
fun RatesTab(uiState: CurrencyUiState, viewModel: CurrencyViewModel) {
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredRates = remember(uiState.rates, searchQuery) {
        uiState.rates.filter { 
            it.currencyCode.contains(searchQuery, ignoreCase = true) 
        }.sortedBy { it.currencyCode }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AeonTheme.dimensions.screenPadding)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search currencies...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear")
                    }
                }
            },
            shape = RoundedCornerShape(AeonTheme.dimensions.listPadding),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = AppColors.panelSurface,
                unfocusedContainerColor = AppColors.panelSurface
            )
        )

        Spacer(modifier = Modifier.height(AeonTheme.dimensions.screenPadding))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AeonTheme.dimensions.screenPadding, vertical = AeonTheme.dimensions.spacingSm),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Currency", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = AppColors.primary)
            Text("Rate (1 ${uiState.baseCurrency})", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = AppColors.primary)
        }

        HorizontalDivider(color = AppColors.divider)

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(filteredRates) { rate ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AeonTheme.dimensions.screenPadding),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(AeonTheme.dimensions.spacingSm),
                            color = AppColors.primaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.size(AeonTheme.dimensions.iconSizeXl)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(rate.currencyCode.take(2), fontWeight = FontWeight.Bold, color = AppColors.onPrimaryContainer)
                            }
                        }
                        Spacer(modifier = Modifier.width(AeonTheme.dimensions.spacingLg))
                        Text(rate.currencyCode, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }
                    Text(
                        text = String.format(Locale.US, "%.4f", rate.rate),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFeatureSettings = "tnum"
                        )
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = AeonTheme.dimensions.screenPadding), color = AppColors.divider.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
fun ConverterTab(uiState: CurrencyUiState, viewModel: CurrencyViewModel) {
    var amountText by remember { mutableStateOf("1") }
    var fromCurrency by remember { mutableStateOf("USD") }
    var toCurrency by remember { mutableStateOf("EUR") }
    
    val currencies = uiState.rates.map { it.currencyCode }.sorted()
    
    var conversionResult by remember { mutableStateOf(0.0) }
    
    LaunchedEffect(amountText, fromCurrency, toCurrency, uiState.rates) {
        val amount = amountText.toDoubleOrNull() ?: 0.0
        viewModel.convert(amount, fromCurrency, toCurrency) { result ->
            conversionResult = result
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AeonTheme.dimensions.spacingXl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingXl)
    ) {
        // Big Display
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(AeonTheme.dimensions.dialogCornerRadius),
            color = AppColors.secondaryContainer.copy(alpha = 0.5f)
        ) {
            Column(
                modifier = Modifier
                    .padding(AeonTheme.dimensions.spacing2xl)
                    .fillMaxWidth()
            ) {
                // Source - Align Left
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = amountText.ifEmpty { "0" },
                        style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold),
                        color = AppColors.onSecondaryContainer,
                        textAlign = TextAlign.Start
                    )
                    Text(
                        text = fromCurrency,
                        style = MaterialTheme.typography.headlineSmall,
                        color = AppColors.primary
                    )
                }

                Icon(
                    imageVector = Icons.Default.ArrowDownward,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(vertical = AeonTheme.dimensions.spacingMd)
                        .align(Alignment.CenterHorizontally),
                    tint = AppColors.border
                )

                // Destination - Align Right
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = String.format(Locale.US, "%.2f", conversionResult),
                        style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold),
                        color = AppColors.onSecondaryContainer,
                        textAlign = TextAlign.End
                    )
                    Text(
                        text = toCurrency,
                        style = MaterialTheme.typography.headlineSmall,
                        color = AppColors.primary
                    )
                }
            }
        }

        // Controls
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingLg)) {
            CurrencySelector(
                label = "From",
                selected = fromCurrency,
                currencies = currencies,
                onSelected = { fromCurrency = it },
                modifier = Modifier.weight(1f)
            )
            CurrencySelector(
                label = "To",
                selected = toCurrency,
                currencies = currencies,
                onSelected = { toCurrency = it },
                modifier = Modifier.weight(1f)
            )
        }

        // Keypad for "Big numbered UI"
        Column(verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingSm)) {
            val keys = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf(".", "0", "C")
            )
            
            keys.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingSm)) {
                    row.forEach { key ->
                        KeyButton(
                            text = key,
                            onClick = {
                                when (key) {
                                    "C" -> amountText = ""
                                    "." -> if (!amountText.contains(".")) amountText += "."
                                    else -> amountText += key
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencySelector(
    label: String,
    selected: String,
    currencies: List<String>,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var textValue by remember(selected) { mutableStateOf(selected) }

    val filteredCurrencies = remember(currencies, textValue, expanded) {
        if (expanded) {
            currencies.filter { it.contains(textValue, ignoreCase = true) }
        } else {
            currencies
        }
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = textValue,
            onValueChange = { 
                textValue = it
                expanded = true
            },
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable, true),
            singleLine = true
        )
        
        if (filteredCurrencies.isNotEmpty()) {
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { 
                    expanded = false
                    if (!currencies.contains(textValue)) {
                        textValue = selected
                    }
                }
            ) {
                filteredCurrencies.forEach { curr ->
                    DropdownMenuItem(
                        text = { Text(curr) },
                        onClick = {
                            onSelected(curr)
                            textValue = curr
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun KeyButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.height(AeonTheme.dimensions.iconSize3xl),
        shape = RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius),
        contentPadding = PaddingValues(0.dp)
    ) {
        if (text == "C") {
            Icon(Icons.AutoMirrored.Rounded.Backspace, contentDescription = "Clear")
        } else {
            Text(text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}
