package com.osscabinet.ossWallet.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.rounded.Add
import com.osscabinet.ossWallet.ui.components.*
import com.osscabinet.ossWallet.ui.components.transactions.AeonPeriodSelector
import com.osscabinet.ossWallet.ui.components.transactions.AeonTransactionList
import com.osscabinet.ossWallet.ui.util.formatCurrencyRight
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionWithBalance
import com.osscabinet.ossWallet.ui.viewmodel.TransactionViewModel
import com.osscabinet.ossWallet.ui.viewmodel.AnalyticsUiState
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun TransactionListScreen(
    viewModel: TransactionViewModel,
    accountViewModel: com.osscabinet.ossWallet.ui.viewmodel.AccountViewModel,
    analyticsViewModel: com.osscabinet.ossWallet.ui.viewmodel.AnalyticsViewModel,
    onAddTransactionClick: () -> Unit,
    onEditTransactionClick: (Long) -> Unit,
    onBackClick: () -> Unit,
    onAccountDetailClick: (String) -> Unit
) {
    val transactionsWithBalance by viewModel.transactionsWithBalance.collectAsState()
    val accounts by accountViewModel.accounts.collectAsState()
    val accountMap = accounts.associate { it.id to it.name }
    val accountCurrencyMap = accounts.associate { it.id to it.currencyCode }
    val categories by viewModel.categories.collectAsState()
    val categoryMap = remember(categories) { categories.associateBy { it.id } }
    val decimalPrecision by viewModel.decimalPrecision.collectAsState()
    val analyticsState by analyticsViewModel.uiState.collectAsState()
    val filter by viewModel.filter.collectAsState()
    val recordsSettings by viewModel.recordsSettings.collectAsState()

    TransactionListContent(
        transactionsWithBalance = transactionsWithBalance,
        accounts = accounts,
        accountMap = accountMap,
        accountCurrencyMap = accountCurrencyMap,
        categories = categories,
        categoryMap = categoryMap,
        decimalPrecision = decimalPrecision,
        analyticsState = analyticsState,
        filter = filter,
        recordsSettings = recordsSettings,
        onAddTransactionClick = onAddTransactionClick,
        onEditTransactionClick = onEditTransactionClick,
        onBackClick = onBackClick,
        onAccountDetailClick = onAccountDetailClick,
        onUpdateFilter = { viewModel.updateFilter(it) },
        onSetDateRange = { start, end -> viewModel.setDateRangeFilter(start, end) },
        onUpdateRecordsSettings = { settings -> viewModel.updateRecordsSettings { settings } }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun TransactionListContent(
    transactionsWithBalance: List<TransactionWithBalance>,
    accounts: List<com.osscabinet.ossWallet.domain.model.Account>,
    accountMap: Map<String, String>,
    accountCurrencyMap: Map<String, String>,
    categories: List<com.osscabinet.ossWallet.data.local.entity.CategoryEntity>,
    categoryMap: Map<Long, com.osscabinet.ossWallet.data.local.entity.CategoryEntity>,
    decimalPrecision: Int,
    analyticsState: AnalyticsUiState,
    filter: TransactionFilter,
    recordsSettings: com.osscabinet.ossWallet.data.local.TransactionSectionSettings,
    onAddTransactionClick: () -> Unit,
    onEditTransactionClick: (Long) -> Unit,
    onBackClick: () -> Unit,
    onAccountDetailClick: (String) -> Unit,
    onUpdateFilter: (TransactionFilter) -> Unit,
    onSetDateRange: (Long?, Long?) -> Unit,
    onUpdateRecordsSettings: (com.osscabinet.ossWallet.data.local.TransactionSectionSettings) -> Unit
) {
    var showMoreMenu by remember { mutableStateOf(false) }
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded,
            skipHiddenState = true
        )
    )
    val scope = rememberCoroutineScope()
    val backgroundColor = Color(0xFF1E1E1E)

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 90.dp,
        sheetContainerColor = backgroundColor,
        sheetContentColor = Color.White,
        sheetShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        sheetDragHandle = null,
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Records", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(if (filter == TransactionFilter()) "Showing all transactions" else "Filtered results", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Search */ }) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                    Box {
                        IconButton(onClick = { showMoreMenu = true }) {
                            Icon(Icons.Default.MoreHoriz, contentDescription = "More")
                        }
                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Use Note as Header") },
                                leadingIcon = { Checkbox(checked = recordsSettings.useNoteAsHeader, onCheckedChange = null) },
                                onClick = { 
                                    onUpdateRecordsSettings(recordsSettings.copy(useNoteAsHeader = !recordsSettings.useNoteAsHeader))
                                    showMoreMenu = false 
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Show Opening Balance") },
                                leadingIcon = { Checkbox(checked = recordsSettings.showOpeningBalance, onCheckedChange = null) },
                                onClick = { 
                                    onUpdateRecordsSettings(recordsSettings.copy(showOpeningBalance = !recordsSettings.showOpeningBalance))
                                    showMoreMenu = false 
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Show Closing Balance") },
                                leadingIcon = { Checkbox(checked = recordsSettings.showClosingBalance, onCheckedChange = null) },
                                onClick = { 
                                    onUpdateRecordsSettings(recordsSettings.copy(showClosingBalance = !recordsSettings.showClosingBalance))
                                    showMoreMenu = false 
                                }
                            )
                        }
                    }
                }
            )
        },
        sheetContent = {
            Column(
                modifier = Modifier.fillMaxHeight(0.9f)
            ) {
                AeonPeriodSelector(
                    filter = filter,
                    onSetDateRange = onSetDateRange,
                    onClick = { 
                        scope.launch {
                            if (scaffoldState.bottomSheetState.currentValue == SheetValue.PartiallyExpanded) {
                                scaffoldState.bottomSheetState.expand()
                            } else {
                                scaffoldState.bottomSheetState.partialExpand()
                            }
                        }
                    },
                    isExpanded = scaffoldState.bottomSheetState.targetValue == SheetValue.Expanded,
                    showArcTab = true,
                    backgroundColor = backgroundColor
                )

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                FilterSheetContent(
                    accounts = accounts,
                    allCategories = categories,
                    currentFilter = filter,
                    onApplyFilter = onUpdateFilter,
                    onClose = {
                        scope.launch { scaffoldState.bottomSheetState.partialExpand() }
                    }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (transactionsWithBalance.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No records found", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.outline)
                }
            } else {
                AeonTransactionList(
                    transactions = transactionsWithBalance,
                    accountMap = accountMap,
                    categoryMap = categoryMap,
                    onTransactionClick = { onEditTransactionClick(it.id) },
                    contentPadding = PaddingValues(bottom = 16.dp),
                    precision = decimalPrecision,
                    useNoteAsHeader = recordsSettings.useNoteAsHeader,
                    showOpeningBalance = recordsSettings.showOpeningBalance,
                    showClosingBalance = recordsSettings.showClosingBalance,
                    baseCurrency = analyticsState.selectedCurrency,
                    exchangeRates = analyticsState.exchangeRates,
                    accountCurrencyMap = accountCurrencyMap,
                    filterAccountId = if (filter.accountIds.size == 1) filter.accountIds.first() else null
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterSheetContent(
    accounts: List<com.osscabinet.ossWallet.domain.model.Account>,
    allCategories: List<com.osscabinet.ossWallet.data.local.entity.CategoryEntity>,
    currentFilter: TransactionFilter,
    onApplyFilter: (TransactionFilter) -> Unit,
    onClose: () -> Unit
) {
    var localFilter by remember { mutableStateOf(currentFilter) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Filter Records", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            TextButton(onClick = { localFilter = TransactionFilter() }) {
                Text("Clear All", color = MaterialTheme.colorScheme.primary)
            }
        }

        // Text Search: Note & Payee
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("SEARCH", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            
            OutlinedTextField(
                value = localFilter.queryNote ?: "",
                onValueChange = { localFilter = localFilter.copy(queryNote = it.ifBlank { null }) },
                label = { Text("Note") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
                ),
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 8.dp)) {
                        Text("Regex", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Checkbox(
                            checked = localFilter.isNoteRegex,
                            onCheckedChange = { localFilter = localFilter.copy(isNoteRegex = it) }
                        )
                    }
                }
            )

            OutlinedTextField(
                value = localFilter.queryPayee ?: "",
                onValueChange = { localFilter = localFilter.copy(queryPayee = it.ifBlank { null }) },
                label = { Text("Payee") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
                ),
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 8.dp)) {
                        Text("Regex", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Checkbox(
                            checked = localFilter.isPayeeRegex,
                            onCheckedChange = { localFilter = localFilter.copy(isPayeeRegex = it) }
                        )
                    }
                }
            )
        }

        // Type (Income/Expense)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("TRANSACTION TYPE", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = localFilter.isIncome == null,
                    onClick = { localFilter = localFilter.copy(isIncome = null) },
                    label = { Text("All") },
                    shape = RoundedCornerShape(12.dp)
                )
                FilterChip(
                    selected = localFilter.isIncome == true,
                    onClick = { localFilter = localFilter.copy(isIncome = true) },
                    label = { Text("Income") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.TrendingUp, null, Modifier.size(16.dp)) },
                    shape = RoundedCornerShape(12.dp)
                )
                FilterChip(
                    selected = localFilter.isIncome == false,
                    onClick = { localFilter = localFilter.copy(isIncome = false) },
                    label = { Text("Expense") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.TrendingDown, null, Modifier.size(16.dp)) },
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        // Date Range
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("DATE RANGE", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                var showStartDatePicker by remember { mutableStateOf(false) }
                var showEndDatePicker by remember { mutableStateOf(false) }

                OutlinedButton(
                    onClick = { showStartDatePicker = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray.copy(alpha = 0.5f))
                ) {
                    Text(localFilter.startDate?.let { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(it)) } ?: "Start Date", color = Color.White)
                }
                Text("to", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                OutlinedButton(
                    onClick = { showEndDatePicker = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray.copy(alpha = 0.5f))
                ) {
                    Text(localFilter.endDate?.let { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(it)) } ?: "End Date", color = Color.White)
                }

                if (showStartDatePicker) {
                    AeonDateTimePicker(
                        initialDateTime = localFilter.startDate ?: System.currentTimeMillis(),
                        onDateTimeSelected = { localFilter = localFilter.copy(startDate = it) },
                        onDismiss = { showStartDatePicker = false }
                    )
                }
                if (showEndDatePicker) {
                    AeonDateTimePicker(
                        initialDateTime = localFilter.endDate ?: System.currentTimeMillis(),
                        onDateTimeSelected = { localFilter = localFilter.copy(endDate = it) },
                        onDismiss = { showEndDatePicker = false }
                    )
                }
            }
        }

        // Amount Range
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("AMOUNT RANGE", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = localFilter.minAmount?.toString() ?: "",
                    onValueChange = { localFilter = localFilter.copy(minAmount = it.toBigDecimalOrNull()) },
                    label = { Text("Min") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = localFilter.maxAmount?.toString() ?: "",
                    onValueChange = { localFilter = localFilter.copy(maxAmount = it.toBigDecimalOrNull()) },
                    label = { Text("Max") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
                )
            }
        }

        // Categories (Multi-select)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("CATEGORIES", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                allCategories.forEach { category ->
                    FilterChip(
                        selected = category.id in localFilter.categoryIds,
                        onClick = {
                            val current = localFilter.categoryIds.toMutableList()
                            if (category.id in current) current.remove(category.id) else current.add(category.id)
                            localFilter = localFilter.copy(categoryIds = current)
                        },
                        label = { Text(category.name) },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }

        // Accounts (Multi-select)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("ACCOUNTS", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                accounts.forEach { account ->
                    FilterChip(
                        selected = account.id in localFilter.accountIds,
                        onClick = {
                            val current = localFilter.accountIds.toMutableList()
                            if (account.id in current) current.remove(account.id) else current.add(account.id)
                            localFilter = localFilter.copy(accountIds = current)
                        },
                        label = { Text(account.name) },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }

        // Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onClose,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray.copy(alpha = 0.5f))
            ) {
                Text("Cancel", color = Color.White)
            }
            Button(
                onClick = {
                    onApplyFilter(localFilter)
                    onClose()
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Apply Filter")
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}
