package com.osscabinet.ossWallet.ui.screens.transactions.entry.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.ui.components.CurrencySelector
import com.osscabinet.ossWallet.ui.components.entry.EntryTypeSelector
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onEvent(TransactionEntryEvent.ShowNumpad(true)) }
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Primary amount row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CurrencySelector(
                        selectedCurrency = state.selectedCurrency.ifEmpty { "USD" },
                        onCurrencySelected = { onEvent(TransactionEntryEvent.SetCurrency(it)) },
                        supportedCurrencies = state.selectedAccount?.allowedCurrencies ?: listOf("USD")
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    val amountText = if (state.amount.isEmpty()) "0.00" else state.amount
                    var multiplier by remember(amountText) { mutableStateOf(1f) }
                    
                    Text(
                        text = amountText,
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-1).sp,
                            fontSize = (MaterialTheme.typography.displayMedium.fontSize.value * multiplier).sp
                        ),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Clip,
                        onTextLayout = { textLayoutResult ->
                            if (textLayoutResult.didOverflowWidth && multiplier > 0.3f) {
                                multiplier *= 0.8f
                            }
                        },
                        color = when {
                            state.isExpense -> Color(0xFFB3261E)
                            state.isIncome -> Color(0xFF004D40)
                            else -> MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (!state.isTransfer) {
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (!state.isSplitMode) {
                                    onEvent(TransactionEntryEvent.ToggleSplitMode(true))
                                }
                                onEvent(TransactionEntryEvent.ShowSplitSheet(true))
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    if (state.isSplitMode) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                    else Color.Transparent
                                )
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        val amountValue = state.amount.toBigDecimalOrNull() ?: BigDecimal.ZERO
                        val convertedAmount = amountValue.multiply(state.exchangeRate)
                        val convertedText = convertedAmount.setScale(state.decimalPrecision, RoundingMode.HALF_UP).toPlainString()

                        CurrencySelector(
                            selectedCurrency = state.targetCurrency.ifEmpty { "USD" },
                            onCurrencySelected = { onEvent(TransactionEntryEvent.SetTargetCurrency(it)) },
                            supportedCurrencies = state.targetAccount?.allowedCurrencies ?: listOf("USD")
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        var convertedMultiplier by remember(convertedText) { mutableStateOf(1f) }
                        
                        Text(
                            text = convertedText,
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.5).sp,
                                fontSize = (MaterialTheme.typography.headlineLarge.fontSize.value * convertedMultiplier).sp
                            ),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Clip,
                            onTextLayout = { textLayoutResult ->
                                if (textLayoutResult.didOverflowWidth && convertedMultiplier > 0.3f) {
                                    convertedMultiplier *= 0.8f
                                }
                            },
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                }
            }
        }
    }
}

