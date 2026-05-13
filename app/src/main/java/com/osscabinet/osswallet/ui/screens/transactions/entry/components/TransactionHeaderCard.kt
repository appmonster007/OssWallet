package com.osscabinet.ossWallet.ui.screens.transactions.entry.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CallSplit
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.ui.components.entry.*
import com.osscabinet.ossWallet.ui.screens.transactions.entry.TransactionEntryEvent
import com.osscabinet.ossWallet.ui.screens.transactions.entry.TransactionEntryState
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Header card containing transaction type selector and amount input.
 * Following the "Architect Slate" design specification with robust auto-resizing amount.
 */
@Composable
fun TransactionHeaderCard(
    state: TransactionEntryState,
    onEvent: (TransactionEntryEvent) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Transaction Type Row
            EntryTypeSelector(
                selectedType = state.transactionType,
                onTypeSelected = { onEvent(TransactionEntryEvent.SetTransactionType(it)) },
                types = listOf(
                    Triple(Icons.Rounded.Remove, "Expense", 0),
                    Triple(Icons.Rounded.Add, "Income", 1),
                    Triple(Icons.Rounded.SwapHoriz, "Transfer", 2)
                ),
                selectedColor = when (state.transactionType) {
                    0 -> Color(0xFFB3261E)
                    1 -> Color(0xFF004D40)
                    else -> MaterialTheme.colorScheme.primary
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Amount & Currency Area
            Box(contentAlignment = Alignment.CenterEnd) {
                EntryAmountDisplay(
                    amount = state.amount,
                    currency = state.selectedCurrency,
                    onCurrencyClick = { onEvent(TransactionEntryEvent.SetCurrency(it)) },
                    onAmountClick = { onEvent(TransactionEntryEvent.ShowNumpad(true)) },
                    amountColor = when {
                        state.isExpense -> Color(0xFFB3261E)
                        state.isIncome -> Color(0xFF004D40)
                        else -> MaterialTheme.colorScheme.primary
                    },
                    supportedCurrencies = state.selectedAccount?.allowedCurrencies ?: listOf("USD")
                )

                if (!state.isTransfer) {
                    IconButton(
                        onClick = {
                            if (!state.isSplitMode) {
                                onEvent(TransactionEntryEvent.ToggleSplitMode(true))
                            }
                            onEvent(TransactionEntryEvent.ShowSplitSheet(true))
                        },
                        modifier = Modifier.padding(end = 8.dp).size(32.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    if (state.isSplitMode) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    else Color.Transparent
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.AutoMirrored.Rounded.CallSplit,
                                contentDescription = "Split Transaction",
                                modifier = Modifier.size(20.dp),
                                tint = if (state.isSplitMode) MaterialTheme.colorScheme.primary 
                                       else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            }

            // Transfer-specific
            if (state.isTransfer && state.targetAccount != null) {
                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        onClick = { onEvent(TransactionEntryEvent.ShowExchangeRateDialog(true)) },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                Icons.Rounded.CurrencyExchange,
                                contentDescription = "Edit Exchange Rate",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "1:${state.exchangeRate.setScale(3, RoundingMode.HALF_UP).toPlainString()}",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))

                    EntryAmountDisplay(
                        amount = (state.amount.toBigDecimalOrNull() ?: BigDecimal.ZERO).multiply(state.exchangeRate).setScale(state.decimalPrecision, RoundingMode.HALF_UP).toPlainString(),
                        currency = state.targetCurrency,
                        onCurrencyClick = { onEvent(TransactionEntryEvent.SetTargetCurrency(it)) },
                        onAmountClick = { onEvent(TransactionEntryEvent.ShowNumpad(true)) },
                        amountColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        supportedCurrencies = state.targetAccount?.allowedCurrencies ?: listOf("USD")
                    )
                }
        }
    }
}
