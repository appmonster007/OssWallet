package com.osscabinet.ossWallet.ui.screens

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
import com.osscabinet.ossWallet.ui.viewmodel.CurrencyViewModel
import com.osscabinet.ossWallet.ui.components.AeonSectionHeader
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExchangeRatesScreen(
    viewModel: CurrencyViewModel,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pagerState = remember {
        object : PagerState() {
            override val pageCount: Int get() = 2
        }
    }
    val scope = rememberCoroutineScope()
    val tabs = listOf("Rates", "Converter")

    var navTriggered by remember { mutableStateOf(false) }
    val navigateBack = {
        if (!navTriggered) {
            navTriggered = true
            onBackClick()
        }
    }

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
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = jsonPreview,
                            modifier = Modifier.padding(8.dp),
                            style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        IconButton(onClick = navigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        if (pagerState.currentPage == 0) {
                            val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }
                            val lastUpdatedStr = if (uiState.lastUpdated > 0) dateFormat.format(Date(uiState.lastUpdated)) else "Never"
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = lastUpdatedStr,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
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
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = jsonPreview,
                            modifier = Modifier.padding(8.dp),
                            style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                        Text(
                                            text = "Sync Failed",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.error,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                IconButton(onClick = { viewModel.refreshRates() }) {
                                    if (uiState.isLoading) {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
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
                    containerColor = MaterialTheme.colorScheme.surface,
                    indicator = { tabPositions ->
                        if (pagerState.currentPage < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                                color = MaterialTheme.colorScheme.primary
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
fun RatesTab(uiState: com.osscabinet.ossWallet.ui.viewmodel.CurrencyUiState, viewModel: CurrencyViewModel) {
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredRates = remember(uiState.rates, searchQuery) {
        uiState.rates.filter { 
            it.currencyCode.contains(searchQuery, ignoreCase = true) 
        }.sortedBy { it.currencyCode }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
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
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Currency", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text("Rate (1 ${uiState.baseCurrency})", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(filteredRates) { rate ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(rate.currencyCode.take(2), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
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
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
fun ConverterTab(uiState: com.osscabinet.ossWallet.ui.viewmodel.CurrencyUiState, viewModel: CurrencyViewModel) {
    var amountText by remember { mutableStateOf("1") }
    var fromCurrency by remember { mutableStateOf("USD") }
    var toCurrency by remember { mutableStateOf("EUR") }
    
    val currencies = uiState.rates.map { it.currencyCode }.sorted()
    
    val result = remember(amountText, fromCurrency, toCurrency, uiState.rates) {
        val amount = amountText.toDoubleOrNull() ?: 0.0
        viewModel.convert(amount, fromCurrency, toCurrency)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Big Display
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        ) {
            Column(
                modifier = Modifier
                    .padding(32.dp)
                    .fillMaxWidth()
            ) {
                // Source - Align Left
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = amountText.ifEmpty { "0" },
                        style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        textAlign = TextAlign.Start
                    )
                    Text(
                        text = fromCurrency,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Icon(
                    imageVector = Icons.Default.ArrowDownward,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .align(Alignment.CenterHorizontally),
                    tint = MaterialTheme.colorScheme.outline
                )

                // Destination - Align Right
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = String.format(Locale.US, "%.2f", result),
                        style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        textAlign = TextAlign.End
                    )
                    Text(
                        text = toCurrency,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Controls
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
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
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val keys = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf(".", "0", "C")
            )
            
            keys.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        if (text == "C") {
            Icon(Icons.AutoMirrored.Rounded.Backspace, contentDescription = "Clear")
        } else {
            Text(text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}
