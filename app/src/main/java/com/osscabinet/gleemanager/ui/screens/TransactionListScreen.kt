package com.osscabinet.gleemanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(
    viewModel: TransactionViewModel,
    onAddTransactionClick: () -> Unit,
    onBackClick: () -> Unit,
    onAccountDetailClick: (String) -> Unit
) {
    val transactions by viewModel.transactions.collectAsState()

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
                    IconButton(onClick = {}) { Icon(Icons.Default.FilterList, contentDescription = "Filter") }
                    IconButton(onClick = {}) { Icon(Icons.Default.Search, contentDescription = "Search") }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddTransactionClick,
                containerColor = Color(0xFF004D40),
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            // Period Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("OCTOBER 2024", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.outline)
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
                                onClick = { onAccountDetailClick("CHECKING") }, // Mocked account ID
                                showTonalShift = false
                            )
                        }
                    }
                }
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
                    Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
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
                        fontFeatureSettings = "tnum" // Tabular numerals
                    ),
                    color = if (transaction.isIncome) Color(0xFF004D40) else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "CHECKING", // Mocked account
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

private fun <T> itemsIndexed(items: List<T>, itemContent: @Composable (Int, T) -> Unit) {
    // This is a helper for LazyColumn
}
