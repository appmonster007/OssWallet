package com.osscabinet.ossWallet.ui.components.entry

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.CashFlowType
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter.formatCurrencyRight
import com.osscabinet.ossWallet.ui.components.AppIconMap
import com.osscabinet.ossWallet.ui.components.transactions.AeonTransactionItem
import java.math.BigDecimal
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles

@Composable
fun EntryLinkedTransactionsSection(
    linkedTransactions: List<Transaction>,
    accounts: List<Account>,
    categories: List<Category>,
    is24Hour: Boolean,
    onShowSheet: () -> Unit,
    onRemoveLink: (Long) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = AppColors.primary.copy(alpha = 0.05f),
            border = BorderStroke(0.5.dp, AppColors.primary.copy(alpha = 0.15f)),
            modifier = Modifier.fillMaxWidth(),
            onClick = onShowSheet
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = AppColors.primary.copy(alpha = 0.1f),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.Link,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = AppColors.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Link Transaction",
                    style = AppTextStyles.transactionAmount,
                    color = AppColors.primary
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    Icons.Rounded.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = AppColors.primary
                )
            }
        }

        linkedTransactions.forEach { tx ->
            val account = accounts.find { it.id == tx.accountId }
            val category = categories.find { it.id == tx.categoryId }
            
            EntryLinkedTransactionItem(
                tx = tx,
                accountName = account?.name ?: "Unknown",
                category = category,
                is24Hour = is24Hour,
                onRemove = { onRemoveLink(tx.id) }
            )
        }
    }
}

@Composable
private fun EntryLinkedTransactionItem(
    tx: Transaction,
    accountName: String,
    category: Category?,
    is24Hour: Boolean,
    onRemove: () -> Unit
) {
    val categoryName = category?.name ?: "Unknown"
    val icon: ImageVector = category?.iconName?.let { AppIconMap[it] }
        ?: AppIconMap[categoryName]
        ?: Icons.Rounded.Category
    val iconBg = category?.colorHex?.let { Color(it) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = AppColors.insetSurface.copy(alpha = 0.2f),
        border = BorderStroke(1.dp, AppColors.divider.copy(alpha = 0.3f))
    ) {
        Box {
            AeonTransactionItem(
                category = categoryName,
                account = accountName,
                amount = formatCurrencyRight(tx.amount, tx.currency, precision = 2),
                balanceBefore = BigDecimal.ZERO.toPlainString(),
                balanceAfter = tx.amount.toPlainString(),
                type = when (tx.transactionType) {
                    TransactionType.TRANSFER -> CashFlowType.TRANSFER
                    TransactionType.INCOME -> CashFlowType.INCOME
                    else -> CashFlowType.EXPENSE
                },
                timestamp = formatDate(tx.timestamp, is24Hour),
                note = tx.note,
                tags = tx.tags,
                icon = icon,
                iconBg = iconBg,
                showOpeningBalance = false,
                showClosingBalance = false,
                showDividers = false,
                status = tx.status,
                containerColor = AppColors.transparent,
                onClick = { /* Read-only */ },
                modifier = Modifier.padding(end = 48.dp)
            )

            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp)
                    .size(32.dp)
                    .background(AppColors.errorContainer.copy(alpha = 0.1f), CircleShape)
            ) {
                Icon(
                    Icons.Rounded.LinkOff,
                    contentDescription = "Remove link",
                    tint = AppColors.error,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

private fun formatDate(millis: Long, is24Hour: Boolean): String {
    val pattern = if (is24Hour) "MMM dd, HH:mm" else "MMM dd, hh:mm a"
    val formatter = java.text.SimpleDateFormat(pattern, java.util.Locale.getDefault())
    return formatter.format(java.util.Date(millis))
}
