package com.osscabinet.ossWallet.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.ui.components.*
import com.osscabinet.ossWallet.ui.util.formatCurrencyRight
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.ui.viewmodel.TransactionViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun TransactionListScreen(
    viewModel: TransactionViewModel,
    accountViewModel: com.osscabinet.ossWallet.ui.viewmodel.AccountViewModel,
    onAddTransactionClick: () -> Unit,
    onEditTransactionClick: (Long) -> Unit,
    onBackClick: () -> Unit,
    onAccountDetailClick: (String) -> Unit
) {
    val transactionsWithBalance by viewModel.transactionsWithBalance.collectAsState()
    val accounts by accountViewModel.accounts.collectAsState()
    val accountMap = accounts.associate { it.id to it.name }
    val categories by viewModel.categories.collectAsState()
    val categoryMap = remember(categories) { categories.associateBy { it.name.lowercase() } }

    var showFilterSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Records", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Showing all transactions", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
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
                    IconButton(onClick = { showFilterSheet = true }) { 
                        Icon(
                            Icons.Default.FilterList, 
                            contentDescription = "Filter",
                            tint = if (viewModel.selectedCategory.collectAsState().value != null || 
                                      viewModel.selectedAccountId.collectAsState().value != null ||
                                      viewModel.dateRange.collectAsState().value.first != null) 
                                      MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        ) 
                    }
                    IconButton(onClick = { /* More options */ }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddTransactionClick,
                containerColor = Color(0xFF2E7D32),
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        },
        bottomBar = {
            // Period selector seen in screenshot
            Surface(
                tonalElevation = 3.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp, top = 8.dp)
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { /* Previous period */ }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Previous")
                    }
                    
                    val dateRange by viewModel.dateRange.collectAsState()
                    val periodText = if (dateRange.first != null && dateRange.second != null) {
                        val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                        "${dateFormat.format(Date(dateRange.first!!))} - ${dateFormat.format(Date(dateRange.second!!))}"
                    } else {
                        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date()).uppercase()
                    }
                    
                    Text(
                        periodText,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    IconButton(onClick = { /* Next period */ }) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Next")
                    }
                }
            }
        }
    ) { innerPadding ->
        if (showFilterSheet) {
            FilterBottomSheet(
                viewModel = viewModel,
                accountViewModel = accountViewModel,
                onDismiss = { showFilterSheet = false }
            )
        }

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
                    contentPadding = PaddingValues(bottom = 16.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    viewModel: TransactionViewModel,
    accountViewModel: com.osscabinet.ossWallet.ui.viewmodel.AccountViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val accounts by accountViewModel.accounts.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedAccountId by viewModel.selectedAccountId.collectAsState()
    val dateRange by viewModel.dateRange.collectAsState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text("Filter Records", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

            // Date Range
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("DATE RANGE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilterChip(
                        selected = dateRange.first == null,
                        onClick = { viewModel.setDateRangeFilter(null, null) },
                        label = { Text("All Time") }
                    )
                    FilterChip(
                        selected = dateRange.first != null,
                        onClick = { 
                            val now = System.currentTimeMillis()
                            viewModel.setDateRangeFilter(now - 30 * 24 * 60 * 60 * 1000L, now)
                        },
                        label = { Text("Last 30 Days") }
                    )
                }
            }

            // Category Grid
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("CATEGORY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                val categories = listOf("Food", "Shopping", "Transport", "Salary", "Entertainment", "Health")
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { viewModel.setCategoryFilter(if (selectedCategory == category) null else category) },
                            label = { Text(category) },
                            leadingIcon = {
                                val icon = when(category) {
                                    "Food" -> Icons.Default.Restaurant
                                    "Shopping" -> Icons.Default.ShoppingBag
                                    "Transport" -> Icons.Default.DirectionsCar
                                    "Salary" -> Icons.Default.Payments
                                    else -> Icons.Default.Category
                                }
                                Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        )
                    }
                }
            }

            // Account Selection
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("ACCOUNT", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(accounts) { account ->
                        FilterChip(
                            selected = selectedAccountId == account.id,
                            onClick = { viewModel.setAccountFilter(if (selectedAccountId == account.id) null else account.id) },
                            label = { Text(account.name) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("Show Results")
            }
        }
    }
}

