package com.osscabinet.ossWallet.ui.components.entry

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CallSplit
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CurrencyExchange
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Title
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.AmountEditingSide
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import java.math.BigDecimal
import java.math.RoundingMode
import androidx.core.graphics.toColorInt

/**
 * Convenience overload for TransactionType.
 */
@Composable
fun AeonImpactHeaderCard(
    transactionType: TransactionType,
    onTypeSelected: (TransactionType) -> Unit,
    amount: String,
    currency: String,
    onCurrencyClick: (String) -> Unit,
    onAmountClick: (side: AmountEditingSide) -> Unit,
    amountLabel: String? = null,
    targetAmount: String? = null,
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
    onTargetCurrencyClick: (String) -> Unit = {},
    transferRate: BigDecimal = BigDecimal.ONE,
    onExchangeRateClick: () -> Unit = {},
    isSplitMode: Boolean = false,
    onSplitClick: (() -> Unit)? = null,
    amountColor: Color? = null,
    supportedCurrencies: List<String> = emptyList(),
    targetSupportedCurrencies: List<String> = emptyList(),
    accountId: String? = null,
    isAmountPositive: Boolean = false,
    amountPrefix: String = when {
        transactionType == TransactionType.EXPENSE -> "-"
        transactionType == TransactionType.INCOME -> "+"
        transactionType == TransactionType.TRANSFER && isAmountPositive -> "+"
        transactionType == TransactionType.TRANSFER && !isAmountPositive -> "-"
        else -> ""
    }
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
        amountLabel = amountLabel,
        targetAmount = targetAmount,
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
        amountColor = amountColor ?: Color(transactionType.colorHex.toColorInt()),
        supportedCurrencies = supportedCurrencies,
        targetSupportedCurrencies = targetSupportedCurrencies,
        accountId = accountId,
        isAmountPositive = isAmountPositive,
        amountPrefix = amountPrefix
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
    onCurrencyClick: (String) -> Unit,
    onAmountClick: (side: AmountEditingSide) -> Unit,
    amountLabel: String? = null,
    targetAmount: String? = null,
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
    onTargetCurrencyClick: (String) -> Unit = {},
    transferRate: BigDecimal = BigDecimal.ONE,
    onExchangeRateClick: () -> Unit = {},
    isSplitMode: Boolean = false,
    onSplitClick: (() -> Unit)? = null,
    amountColor: Color? = null,
    supportedCurrencies: List<String> = emptyList(),
    targetSupportedCurrencies: List<String> = emptyList(),
    accountId: String? = null,
    isAmountPositive: Boolean = false,
    amountPrefix: String = ""
) {
    val activeAmountColor = amountColor ?: AppColors.primary

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AeonTheme.dimensions.cardCornerRadius),
        color = AppColors.cardSurface,
        border = BorderStroke(
            (AeonTheme.dimensions.spacingXs / 8),
            AppColors.divider.copy(alpha = 0.15f)
        ),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(AeonTheme.dimensions.cardPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (showTypeSelector) {
                EntryTypeSelector(
                    selectedType = selectedType,
                    onTypeSelected = onTypeSelected,
                    types = types,
                    selectedColor = activeAmountColor,
                    itemWidth = (AeonTheme.dimensions.spacingXl * 3f)
                )

                Spacer(modifier = Modifier.height(AeonTheme.dimensions.spacingXl))
            }

            if (showAmount) {
                Box(contentAlignment = Alignment.CenterEnd, modifier = Modifier.fillMaxWidth()) {
                    EntryAmountDisplay(
                        amount = amount,
                        currency = currency,
                        onCurrencyClick = onCurrencyClick,
                        onAmountClick = { onAmountClick(AmountEditingSide.SOURCE) },
                        amountColor = activeAmountColor,
                        supportedCurrencies = supportedCurrencies,
                        showCurrency = showCurrency,
                        label = amountLabel,
                        prefix = if (isTransfer) "-" else amountPrefix
                    )

                    Row(
                        modifier = Modifier.padding(end = AeonTheme.dimensions.spacingSm),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingXs)
                    ) {
                        if (onSplitClick != null && !isTransfer) {
                            IconButton(
                                onClick = onSplitClick,
                                modifier = Modifier.size(AeonTheme.dimensions.iconSizeLg)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(AeonTheme.dimensions.iconSizeLg)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSplitMode) AppColors.primary.copy(alpha = 0.12f)
                                            else AppColors.transparent
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Rounded.CallSplit,
                                        contentDescription = "Split Transaction",
                                        modifier = Modifier.size(AeonTheme.dimensions.iconSizeMd),
                                        tint = if (isSplitMode) AppColors.primary
                                        else AppColors.contentSecondary.copy(alpha = 0.4f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (isTransfer && targetAccountId != null) {
                Spacer(modifier = Modifier.height(AeonTheme.dimensions.spacingMd))

                Surface(
                    onClick = onExchangeRateClick,
                    shape = RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius),
                    color = AppColors.primary.copy(alpha = 0.08f),
                    border = BorderStroke(
                        (AeonTheme.dimensions.spacingXs / 8),
                        AppColors.primary.copy(alpha = 0.15f)
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingSm),
                        modifier = Modifier.padding(
                            horizontal = AeonTheme.dimensions.spacingMd,
                            vertical = AeonTheme.dimensions.spacingSm
                        )
                    ) {
                        Icon(
                            Icons.Rounded.CurrencyExchange,
                            contentDescription = "Edit Exchange Rate",
                            modifier = Modifier.size(AeonTheme.dimensions.iconSizeSm),
                            tint = AppColors.primary
                        )
                        Text(
                            text = "1:${
                                transferRate.setScale(3, RoundingMode.HALF_UP).toPlainString()
                            }",
                            style = AppTextStyles.chipLabel,
                            color = AppColors.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(AeonTheme.dimensions.spacingMd))

                val displayTargetAmount = targetAmount ?: (amount.toBigDecimalOrNull() ?: BigDecimal.ZERO).multiply(transferRate)
                    .setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()

                EntryAmountDisplay(
                    amount = displayTargetAmount,
                    currency = targetCurrency ?: currency,
                    onCurrencyClick = onTargetCurrencyClick,
                    onAmountClick = { onAmountClick(AmountEditingSide.TARGET) },
                    amountColor = AppColors.contentPrimary.copy(alpha = 0.8f),
                    supportedCurrencies = targetSupportedCurrencies.ifEmpty { supportedCurrencies },
                    prefix = if (isTransfer) "+" else amountPrefix
                )
            }

            if (showName) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = AeonTheme.dimensions.spacingMd),
                    thickness = (AeonTheme.dimensions.spacingXs / 8),
                    color = AppColors.divider.copy(alpha = 0.15f)
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

@Preview(showBackground = true)
@Composable
fun AeonImpactHeaderCardPreview() {
    OssWalletTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            AeonImpactHeaderCard(
                transactionType = TransactionType.EXPENSE,
                onTypeSelected = {},
                amount = "1,250.00",
                currency = "USD",
                onCurrencyClick = {},
                onAmountClick = { _ -> },
                name = "Monthly Rent",
                onNameChange = {},
                nameLabel = "BUDGET NAME"
            )
        }
    }
}
