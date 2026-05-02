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
            TransactionTypeSelector(
                selectedType = state.transactionType,
                onTypeSelected = { onEvent(TransactionEntryEvent.SetTransactionType(it)) }
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

    val expenseColor = Color(0xFFB3261E)
    val incomeColor = Color(0xFF004D40)
    val transferColor = MaterialTheme.colorScheme.primary

    val indicatorColor by animateColorAsState(
        targetValue = when (selectedType) {
            0 -> expenseColor
            1 -> incomeColor
            else -> transferColor
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "indicatorColor"
    )

    val indicatorOffset by animateDpAsState(
        targetValue = when (selectedType) {
            0 -> 0.dp
            1 -> 80.dp
            else -> 160.dp
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "indicatorOffset"
    )

    Box(
        modifier = Modifier
            .height(44.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f), RoundedCornerShape(32.dp))
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(80.dp)
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
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.1f else 1.0f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "iconScale"
                )

                Box(
                    modifier = Modifier
                        .width(80.dp)
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
