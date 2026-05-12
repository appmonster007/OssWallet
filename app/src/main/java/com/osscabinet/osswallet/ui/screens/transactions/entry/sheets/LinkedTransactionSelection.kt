package com.osscabinet.ossWallet.ui.screens.transactions.entry.sheets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.CashFlowType
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.ui.components.AeonFullScreenDialog
import com.osscabinet.ossWallet.ui.components.AeonSelectionScreen
import com.osscabinet.ossWallet.ui.components.AppIconMap
import com.osscabinet.ossWallet.ui.components.AeonSearchField
import com.osscabinet.ossWallet.ui.components.transactions.AeonTransactionItem
import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter.formatCurrencyRight
import com.osscabinet.ossWallet.domain.util.fuzzyMatch
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.*

/**
 * Full-screen selection for Linking Transactions.
 * Replaced LinkedTransactionSheet for a more immersive "select from all" experience.
 */
@Composable
fun LinkedTransactionSelection(
    transactions: List<Transaction>,
    currentTransactionId: Long?,
    linkedIds: Set<Long>,
    accounts: List<Account>,
    categories: List<Category>,
    is24Hour: Boolean = false,
    onLinkedTransactionsChanged: (Set<Long>) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedIds by remember { mutableStateOf(linkedIds) }
    var query by remember { mutableStateOf("") }
    val pattern = if (is24Hour) "MMM dd, HH:mm" else "MMM dd, hh:mm a"
    val sdf = remember(is24Hour) { SimpleDateFormat(pattern, Locale.getDefault()) }

    val filteredTransactions = remember(query, transactions) {
        val base = transactions.filter { it.id != currentTransactionId }
        if (query.isBlank()) {
            base.sortedByDescending { it.timestamp }
        } else {
            base.filter { 
                val categoryName = categories.find { cat -> cat.id == it.categoryId }?.name ?: ""
                it.note.contains(query, ignoreCase = true) || 
                it.payee.contains(query, ignoreCase = true) ||
                categoryName.contains(query, ignoreCase = true) ||
                it.note.fuzzyMatch(query) ||
                it.payee.fuzzyMatch(query) ||
                categoryName.fuzzyMatch(query)
            }.sortedByDescending { it.timestamp }
        }
    }

    AeonFullScreenDialog(onDismissRequest = onDismiss) {
        AeonSelectionScreen(
            title = "Link Records",
            onBack = onDismiss,
            actions = {
                if (selectedIds.isNotEmpty()) {
                    TextButton(onClick = { selectedIds = emptySet() }) {
                        Text("CLEAR ALL", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "Select related activities from your history to link them to this record.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                    )
                    
                    AeonSearchField(
                        query = query,
                        onQueryChange = { query = it },
                        placeholder = "Search by note or payee...",
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            filteredTransactions,
                            key = { it.id }
                        ) { tx ->
                            val isSelected = tx.id in selectedIds
                            val account = accounts.find { it.id == tx.accountId }
                            val category = categories.find { it.id == tx.categoryId }
                            val icon = AppIconMap[category?.iconName] ?: Icons.Rounded.Category
                            val iconBg = category?.colorHex?.let { Color(it) }

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                else MaterialTheme.colorScheme.surface,
                                border = if (isSelected) BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                ) else null,
                                shadowElevation = if (isSelected) 1.dp else 0.dp
                            ) {
                                Box {
                                    AeonTransactionItem(
                                        category = category?.name ?: "Unknown",
                                        account = account?.name ?: "Unknown",
                                        amount = formatCurrencyRight(tx.amount, tx.currency, precision = 2),
                                        balanceBefore = formatCurrencyRight(BigDecimal.ZERO, tx.currency, precision = 2),
                                        balanceAfter = formatCurrencyRight(tx.amount, tx.currency, precision = 2),
                                        type = if (tx.amount.signum() >= 0) CashFlowType.INCOME else CashFlowType.EXPENSE,
                                        timestamp = sdf.format(Date(tx.timestamp)),
                                        note = tx.note,
                                        tags = tx.tags,
                                        icon = icon,
                                        iconBg = iconBg,
                                        showOpeningBalance = false,
                                        showClosingBalance = false,
                                        showDividers = false,
                                        status = tx.status,
                                        containerColor = Color.Transparent,
                                        modifier = Modifier.padding(end = if (isSelected) 48.dp else 0.dp),
                                        onClick = {
                                            selectedIds = if (isSelected) {
                                                selectedIds - tx.id
                                            } else {
                                                selectedIds + tx.id
                                            }
                                        }
                                    )

                                    if (isSelected) {
                                        Surface(
                                            modifier = Modifier
                                                .align(Alignment.CenterEnd)
                                                .padding(end = 16.dp)
                                                .size(24.dp),
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primary,
                                            shadowElevation = 2.dp
                                        ) {
                                            Icon(
                                                Icons.Rounded.Check,
                                                contentDescription = "Selected",
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.padding(4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        
                        // Bottom spacer for the button
                        item { Spacer(modifier = Modifier.height(100.dp)) }
                    }
                }

                // Floating Action Area at bottom
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        Button(
                            onClick = {
                                onLinkedTransactionsChanged(selectedIds)
                                onDismiss()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(
                                "Link ${selectedIds.size} Record${if (selectedIds.size != 1) "s" else ""}",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }
    }
}
