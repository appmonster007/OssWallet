package com.osscabinet.ossWallet.ui.screens.transactions.entry.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.ui.components.CurrencySelector
import com.osscabinet.ossWallet.ui.screens.transactions.entry.TransactionEntryEvent
import com.osscabinet.ossWallet.ui.screens.transactions.entry.TransactionEntryState
import java.math.BigDecimal
import java.math.RoundingMode

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
            .wrapContentHeight(),
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onEvent(TransactionEntryEvent.ShowNumpad(true)) }
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Primary amount row with currency selector on left
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

                // Transfer-specific: Show converted amount and exchange rate
                if (state.isTransfer && state.targetAccount != null) {
                    Spacer(modifier = Modifier.height(8.dp))

                    // Arrow with exchange rate to the right (clickable to edit)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.clickable {
                            onEvent(TransactionEntryEvent.ShowExchangeRateDialog(true))
                        }
                    ) {
                        Icon(
                            Icons.Rounded.CurrencyExchange,
                            contentDescription = "Edit Exchange Rate",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "1:${state.exchangeRate.setScale(3, RoundingMode.HALF_UP).toPlainString()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))

                    // Converted amount row with target currency selector on left
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
                        Text(
                            text = convertedText,
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = androidx.compose.ui.graphics.lerp(
                                MaterialTheme.colorScheme.secondary,
                                Color(0xFF2E7D32),
                                0.5f
                            )
                        )
                    }
                }
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

    // Expense: red tint, Income: green tint, Transfer: tertiary
    val expenseColor = androidx.compose.ui.graphics.lerp(MaterialTheme.colorScheme.primary, Color(0xFFB71C1C), 0.6f)
    val incomeColor = androidx.compose.ui.graphics.lerp(MaterialTheme.colorScheme.secondary, Color(0xFF2E7D32), 0.6f)
    val transferColor = MaterialTheme.colorScheme.tertiary

    // Animated indicator color
    val indicatorColor by animateColorAsState(
        targetValue = when (selectedType) {
            0 -> expenseColor
            1 -> incomeColor
            else -> transferColor
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "indicatorColor"
    )

    // Animated indicator offset (sliding effect)
    val indicatorOffset by animateDpAsState(
        targetValue = when (selectedType) {
            0 -> 0.dp
            1 -> 72.dp
            else -> 144.dp
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "indicatorOffset"
    )

    Box(
        modifier = Modifier
            .height(40.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .padding(6.dp)
    ) {
        // Animated sliding indicator
        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(72.dp)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(indicatorColor)
        )

        Row(
            modifier = Modifier.fillMaxHeight(),
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            types.forEachIndexed { index, (icon, label) ->
                val isSelected = selectedType == index

                // Icon scale animation when selected
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.2f else 1.0f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "iconScale"
                )

                Box(
                    modifier = Modifier
                        .width(72.dp)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onTypeSelected(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier
                            .size(26.dp)
                            .graphicsLayer(scaleX = scale, scaleY = scale)
                    )
                }
            }
        }
    }
}
