package com.osscabinet.ossWallet.ui.screens.transactions.entry.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.ui.screens.transactions.entry.TransactionEntryEvent
import com.osscabinet.ossWallet.ui.screens.transactions.entry.TransactionEntryState

/**
 * Header card containing transaction type selector and amount input.
 */
@Composable
fun TransactionHeaderCard(
    state: TransactionEntryState,
    onEvent: (TransactionEntryEvent) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp),
        shape = RoundedCornerShape(48.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Transaction Type Row
            TransactionTypeSelector(
                selectedType = state.transactionType,
                onTypeSelected = { onEvent(TransactionEntryEvent.SetTransactionType(it)) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Amount & Currency Area
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onEvent(TransactionEntryEvent.ShowNumpad(true)) },
                contentAlignment = Alignment.Center
            ) {
                // Amount display would go here - using AdaptiveAmountText or similar
                // For now, showing a placeholder that demonstrates the structure
                Text(
                    text = if (state.amount.isEmpty()) "0.00" else state.amount,
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        state.isExpense -> MaterialTheme.colorScheme.error
                        state.isIncome -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.tertiary
                    }
                )
            }
        }
    }
}

@Composable
private fun TransactionTypeSelector(
    selectedType: Int,
    onTypeSelected: (Int) -> Unit
) {
    val types = listOf(
        Icons.Rounded.Remove to "Expense",
        Icons.Rounded.Add to "Income",
        Icons.Rounded.SwapHoriz to "Transfer"
    )

    Box(
        modifier = Modifier
            .height(40.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .padding(6.dp)
    ) {
        Row {
            types.forEachIndexed { index, (icon, label) ->
                val isSelected = selectedType == index
                val backgroundColor = when {
                    isSelected && index == 0 -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                    isSelected && index == 1 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    isSelected && index == 2 -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                    else -> Color.Transparent
                }
                val contentColor = when {
                    isSelected && index == 0 -> MaterialTheme.colorScheme.error
                    isSelected && index == 1 -> MaterialTheme.colorScheme.primary
                    isSelected && index == 2 -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(24.dp))
                        .background(backgroundColor)
                        .clickable { onTypeSelected(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(icon, contentDescription = null, tint = contentColor)
                        Text(label, color = contentColor, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}
