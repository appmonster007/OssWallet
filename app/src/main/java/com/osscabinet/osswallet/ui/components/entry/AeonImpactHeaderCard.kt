package com.osscabinet.ossWallet.ui.components.entry

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.TransactionType
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Convenience overload for TransactionType.
 */
@Composable
fun AeonImpactHeaderCard(
    transactionType: TransactionType,
    onTypeSelected: (TransactionType) -> Unit,
    amount: String,
    currency: String,
    onCurrencyClick: () -> Unit,
    onAmountClick: () -> Unit,
    name: String = "",
    onNameChange: (String) -> Unit = {},
    nameLabel: String = "",
    nameIcon: ImageVector = Icons.Rounded.Title,
    showName: Boolean = true,
    showAmount: Boolean = true,
    showCurrency: Boolean = true,
    showTypeSelector: Boolean = true,
    isTransfer: Boolean = false,
    targetAccountId: String? = null,
    targetCurrency: String? = null,
    onTargetCurrencyClick: () -> Unit = {},
    transferRate: BigDecimal = BigDecimal.ONE,
    onExchangeRateClick: () -> Unit = {},
    isSplitMode: Boolean = false,
    onSplitClick: (() -> Unit)? = null,
    amountColor: Color? = null,
    supportedCurrencies: List<String> = emptyList(),
    targetSupportedCurrencies: List<String> = emptyList()
) {
    val types = TransactionType.entries.map {
        Triple(
            when (it) {
                TransactionType.EXPENSE -> Icons.Rounded.Remove
                TransactionType.INCOME -> Icons.Rounded.Add
                TransactionType.TRANSFER -> Icons.Rounded.SwapHoriz
            },
            it.displayName,
            it
        )
    }

    AeonImpactHeaderCard(
        selectedType = transactionType,
        onTypeSelected = onTypeSelected,
        types = types,
        amount = amount,
        currency = currency,
        onCurrencyClick = onCurrencyClick,
        onAmountClick = onAmountClick,
        name = name,
        onNameChange = onNameChange,
        nameLabel = nameLabel,
        nameIcon = nameIcon,
        showName = showName,
        showAmount = showAmount,
        showCurrency = showCurrency,
        showTypeSelector = showTypeSelector,
        isTransfer = isTransfer,
        targetAccountId = targetAccountId,
        targetCurrency = targetCurrency,
        onTargetCurrencyClick = onTargetCurrencyClick,
        transferRate = transferRate,
        onExchangeRateClick = onExchangeRateClick,
        isSplitMode = isSplitMode,
        onSplitClick = onSplitClick,
        amountColor = amountColor ?: Color(android.graphics.Color.parseColor(transactionType.colorHex)),
        supportedCurrencies = supportedCurrencies,
        targetSupportedCurrencies = targetSupportedCurrencies
    )
}

/**
 * Generic version of AeonImpactHeaderCard for different entity types (Loans, etc).
 */
@Composable
fun <T> AeonImpactHeaderCard(
    selectedType: T,
    onTypeSelected: (T) -> Unit,
    types: List<Triple<ImageVector, String, T>>,
    amount: String,
    currency: String,
    onCurrencyClick: () -> Unit,
    onAmountClick: () -> Unit,
    name: String = "",
    onNameChange: (String) -> Unit = {},
    nameLabel: String = "",
    nameIcon: ImageVector = Icons.Rounded.Title,
    showName: Boolean = true,
    showAmount: Boolean = true,
    showCurrency: Boolean = true,
    showTypeSelector: Boolean = true,
    isTransfer: Boolean = false,
    targetAccountId: String? = null,
    targetCurrency: String? = null,
    onTargetCurrencyClick: () -> Unit = {},
    transferRate: BigDecimal = BigDecimal.ONE,
    onExchangeRateClick: () -> Unit = {},
    isSplitMode: Boolean = false,
    onSplitClick: (() -> Unit)? = null,
    amountColor: Color? = null,
    supportedCurrencies: List<String> = emptyList(),
    targetSupportedCurrencies: List<String> = emptyList()
) {
    val activeAmountColor = amountColor ?: MaterialTheme.colorScheme.primary

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (showTypeSelector) {
                EntryTypeSelector(
                    selectedType = selectedType,
                    onTypeSelected = onTypeSelected,
                    types = types,
                    selectedColor = activeAmountColor,
                    itemWidth = 100.dp
                )

                Spacer(modifier = Modifier.height(20.dp))
            }

            if (showAmount) {
                Box(contentAlignment = Alignment.CenterEnd, modifier = Modifier.fillMaxWidth()) {
                    EntryAmountDisplay(
                        amount = amount,
                        currency = currency,
                        onCurrencyClick = { onCurrencyClick() },
                        onAmountClick = onAmountClick,
                        amountColor = activeAmountColor,
                        supportedCurrencies = supportedCurrencies,
                        showCurrency = showCurrency
                    )

                    if (onSplitClick != null && !isTransfer) {
                        IconButton(
                            onClick = onSplitClick,
                            modifier = Modifier.padding(end = 8.dp).size(32.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSplitMode) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                        else Color.Transparent
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.CallSplit,
                                    contentDescription = "Split Transaction",
                                    modifier = Modifier.size(20.dp),
                                    tint = if (isSplitMode) MaterialTheme.colorScheme.primary 
                                           else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                            }
                        }
                    }
                }
            }

            if (isTransfer && targetAccountId != null) {
                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    onClick = onExchangeRateClick,
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
                            text = "1:${transferRate.setScale(3, RoundingMode.HALF_UP).toPlainString()}",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))

                EntryAmountDisplay(
                    amount = (amount.toBigDecimalOrNull() ?: BigDecimal.ZERO).multiply(transferRate).setScale(2, RoundingMode.HALF_UP).toPlainString(),
                    currency = targetCurrency ?: currency,
                    onCurrencyClick = { onTargetCurrencyClick() },
                    onAmountClick = onAmountClick,
                    amountColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    supportedCurrencies = targetSupportedCurrencies.ifEmpty { supportedCurrencies }
                )
            }

            if (showName) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)
                )

                EntryEditorialInput(
                    value = name,
                    onValueChange = onNameChange,
                    label = nameLabel,
                    icon = nameIcon
                )
            }
        }
    }
}
