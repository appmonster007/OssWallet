package com.osscabinet.ossWallet.ui.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.TransactionWithBalance
import com.osscabinet.ossWallet.domain.model.CashFlowType
import com.osscabinet.ossWallet.data.local.entity.CategoryEntity
import com.osscabinet.ossWallet.ui.components.AeonFlowSectionHeader
import com.osscabinet.ossWallet.ui.components.categoryIcons
import com.osscabinet.ossWallet.ui.components.transactions.AeonTransactionItem
import com.osscabinet.ossWallet.ui.util.formatCurrencyRight
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RecentActivitiesCard(
    recentTransactions: List<TransactionWithBalance>,
    accountNamesMap: Map<String, String>,
    categoryMap: Map<Long, CategoryEntity>,
    decimalPrecision: Int,
    onRecordsClick: () -> Unit,
    onTransactionClick: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.spacedBy(0.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            AeonFlowSectionHeader(
                title = "RECENT ACTIVITIES",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                trailing = {
                    TextButton(onClick = onRecordsClick) {
                        Text(
                            "View all",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            )
        }

        items(recentTransactions) { item ->
            val transaction = item.transaction
            val categoryEntity = transaction.categoryId?.let { categoryMap[it] }
            val balanceBeforeValue = item.balanceAfter.subtract(transaction.amount)

            AeonTransactionItem(
                category = transaction.category,
                account = accountNamesMap[transaction.accountId] ?: "Unknown",
                amount = formatCurrencyRight(transaction.amount.abs(), transaction.currency, decimalPrecision),
                balanceBefore = formatCurrencyRight(balanceBeforeValue, transaction.currency, decimalPrecision),
                balanceAfter = formatCurrencyRight(item.balanceAfter, transaction.currency, decimalPrecision),
                type = if (transaction.isIncome) CashFlowType.INCOME else CashFlowType.EXPENSE,
                timestamp = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(transaction.timestamp)),
                note = transaction.note,
                tags = transaction.tags,
                icon = categoryEntity?.let { categoryIcons[it.iconName] } ?: categoryIcons[categoryEntity?.name] ?: Icons.Rounded.Category,
                iconBg = categoryEntity?.let { Color(it.colorHex) },
                precision = decimalPrecision,
                onClick = { onTransactionClick(transaction.id) }
            )
        }
        
        item {
            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        }
    }
}
