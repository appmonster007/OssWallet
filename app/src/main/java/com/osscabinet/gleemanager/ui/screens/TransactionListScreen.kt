package com.osscabinet.gleemanager.ui.screens

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.gleemanager.ui.util.getCurrencySymbol
import com.osscabinet.gleemanager.domain.model.Transaction
import com.osscabinet.gleemanager.ui.viewmodel.TransactionViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransactionListScreen(
    viewModel: TransactionViewModel,
    accountViewModel: com.osscabinet.gleemanager.ui.viewmodel.AccountViewModel,
    onAddTransactionClick: () -> Unit,
    onBackClick: () -> Unit,
    onAccountDetailClick: (String) -> Unit
) {
    val transactions by viewModel.transactions.collectAsState()
    var showFilterSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Records", style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
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
                    IconButton(onClick = {}) { Icon(Icons.Default.Search, contentDescription = "Search") }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddTransactionClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
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
            // Period / Filter Status
            val dateRange by viewModel.dateRange.collectAsState()
            val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            val periodText = if (dateRange.first != null && dateRange.second != null) {
                "${dateFormat.format(Date(dateRange.first!!))} - ${dateFormat.format(Date(dateRange.second!!))}"
            } else {
                SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date()).uppercase()
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(periodText, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                if (dateRange.first != null) {
                    TextButton(onClick = { viewModel.clearFilters() }) {
                        Text("Clear", style = MaterialTheme.typography.labelSmall)
                    }
                } else {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.outline)
                }
            }

            if (transactions.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No records found", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.outline)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    val grouped = transactions.sortedByDescending { it.timestamp }.groupBy { 
                        SimpleDateFormat("EEEE, dd MMMM", Locale.getDefault()).format(Date(it.timestamp)) 
                    }

                    grouped.forEach { (date, items) ->
                        item {
                            Text(
                                text = date.uppercase(),
                                modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                                letterSpacing = 1.sp
                            )
                        }
                        items(items) { transaction ->
                            TransactionItemEditorial(
                                transaction = transaction,
                                onClick = { onAccountDetailClick(transaction.accountId) },
                                showTonalShift = false
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
fun FilterBottomSheet(
    viewModel: TransactionViewModel,
    accountViewModel: com.osscabinet.gleemanager.ui.viewmodel.AccountViewModel,
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

@Composable
fun TransactionItemEditorial(
    transaction: Transaction,
    onClick: () -> Unit,
    showTonalShift: Boolean
) {
    val bg = if (showTonalShift) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) else Color.Transparent
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = bg
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Box(contentAlignment = Alignment.Center) {
                    val icon = when(transaction.category.lowercase()) {
                        "food", "dining" -> Icons.Default.Restaurant
                        "shopping" -> Icons.Default.ShoppingBag
                        "transport" -> Icons.Default.DirectionsCar
                        "salary", "income" -> Icons.Default.Payments
                        else -> Icons.Default.Receipt
                    }
                    Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = transaction.category,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (transaction.isIncome) "+" else "-"} ${getCurrencySymbol(transaction.currency)}${String.format(Locale.getDefault(), "%,.2f", transaction.amount)}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontFeatureSettings = "tnum"
                    ),
                    color = if (transaction.isIncome) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "CHECKING",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
