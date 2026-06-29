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
import com.osscabinet.ossWallet.domain.model.EntryMode
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.ui.components.GlassSurface
import com.osscabinet.ossWallet.ui.components.entry.EntryField
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.GlassTokens
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import java.math.BigDecimal
import java.math.RoundingMode
import androidx.core.graphics.toColorInt

/**
 * Data class representing amount-related state to reduce parameter bloating.
 */
data class AeonAmountData(
    val value: String,
    val currency: String,
    val onValueClick: () -> Unit = {},
    val onCurrencyClick: (String) -> Unit = {},
    val label: String? = null,
    val color: Color? = null,
    val supportedCurrencies: List<String> = emptyList(),
    val isError: Boolean = false
)

/**
 * Convenience overload for EntryMode (form-state level).
 */
@Composable
fun AeonImpactHeaderCard(
    transactionType: EntryMode,
    onTypeSelected: (EntryMode) -> Unit,
    source: AeonAmountData,
    target: AeonAmountData? = null,
    categoryId: Long? = null,
    nameState: AeonTextFieldState? = null,
    nameIcon: ImageVector = Icons.Rounded.Title,
    showAmount: Boolean = true,
    showTypeSelector: Boolean = true,
    targetAccountId: String? = null,
    transferRate: BigDecimal = BigDecimal.ONE,
    onExchangeRateClick: () -> Unit = {},
    isSplitMode: Boolean = false,
    onSplitClick: (() -> Unit)? = null,
    onSwapClick: (() -> Unit)? = null,
    accountId: String? = null,
    errors: Set<EntryField> = emptySet()
) {
    val isTransfer = transactionType == EntryMode.TRANSFER
    val amountPrefix = when {
        transactionType == EntryMode.EXPENSE -> "-"
        transactionType == EntryMode.INCOME -> "+"
        isTransfer -> "-" // Source is always Giver in transfer
        else -> ""
    }
    val expenseColorHex = TransactionType.EXPENSE.colorHex
    val incomeColorHex = TransactionType.INCOME.colorHex
    val transferColorHex = TransactionType.TRANSFER_OUTFLOW.colorHex
    fun EntryMode.colorHex(): String = when (this) {
        EntryMode.EXPENSE -> expenseColorHex
        EntryMode.INCOME -> incomeColorHex
        EntryMode.TRANSFER -> transferColorHex
    }

    // Type selector highlight colour: reflects the direction of money relative to wallet accounts.
    // Outside source → income green; Outside target → expense red; Wallet↔Wallet → neutral transfer blue.
    val typeSelectorColor = when {
        !isTransfer -> Color(transactionType.colorHex().toColorInt())
        accountId?.isBlank() == true -> Color(incomeColorHex.toColorInt())
        targetAccountId?.isBlank() == true -> Color(expenseColorHex.toColorInt())
        else -> Color(transferColorHex.toColorInt())
    }

    val types = EntryMode.entries.map {
        Triple(
            when (it) {
                EntryMode.EXPENSE -> Icons.Rounded.Remove
                EntryMode.INCOME -> Icons.Rounded.Add
                EntryMode.TRANSFER -> Icons.Rounded.SwapHoriz
            },
            it.displayName,
            it
        )
    }

    // Default colors based on entry mode if not explicitly provided
    val defaultSourceColor = if (isTransfer) Color(expenseColorHex.toColorInt()) else Color(transactionType.colorHex().toColorInt())
    val defaultTargetColor = if (isTransfer) Color(incomeColorHex.toColorInt()) else null

    AeonImpactHeaderCard(
        selectedType = transactionType,
        onTypeSelected = onTypeSelected,
        types = types,
        source = source.copy(
            color = source.color ?: defaultSourceColor,
            // Labels are only used for transfers
            label = if (isTransfer) source.label else null
        ),
        target = target?.copy(
            color = target.color ?: defaultTargetColor,
            // Labels are only used for transfers
            label = if (isTransfer) target.label else null
        ),
        nameState = nameState,
        nameIcon = nameIcon,
        showAmount = showAmount,
        showTypeSelector = showTypeSelector,
        isTransfer = isTransfer,
        targetAccountId = targetAccountId,
        transferRate = transferRate,
        onExchangeRateClick = onExchangeRateClick,
        isSplitMode = isSplitMode,
        onSplitClick = onSplitClick,
        onSwapClick = onSwapClick,
        accountId = accountId,
        typeSelectorColor = typeSelectorColor,
        amountPrefix = amountPrefix,
        errors = errors
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
    source: AeonAmountData,
    target: AeonAmountData? = null,
    nameState: AeonTextFieldState? = null,
    nameIcon: ImageVector = Icons.Rounded.Title,
    showAmount: Boolean = true,
    showTypeSelector: Boolean = true,
    isTransfer: Boolean = false,
    targetAccountId: String? = null,
    transferRate: BigDecimal = BigDecimal.ONE,
    onExchangeRateClick: () -> Unit = {},
    isSplitMode: Boolean = false,
    onSplitClick: (() -> Unit)? = null,
    onSwapClick: (() -> Unit)? = null,
    accountId: String? = null,
    typeSelectorColor: Color? = null,
    amountPrefix: String = "",
    errors: Set<EntryField> = emptySet()
) {
    val activeAmountColor = source.color ?: AppColors.primary

    GlassSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AeonTheme.dimensions.cardCornerRadius),
        hazeStyle = GlassTokens.cardStyle(),
        glassVariant = com.osscabinet.ossWallet.ui.theme.GlassVariant.Card,
        fallbackColor = AppColors.cardSurface,
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
                    selectedColor = typeSelectorColor ?: activeAmountColor,
                    itemWidth = (AeonTheme.dimensions.spacingXl * 3f)
                )

                Spacer(modifier = Modifier.height(AeonTheme.dimensions.spacingXl))
            }

            // "" = outside wallet explicitly chosen; null = not yet selected — semantically distinct
            val isOutsideSource    = isTransfer && accountId?.isBlank() == true
            val isOutsideTarget    = isTransfer && targetAccountId?.isBlank() == true
            val bothWalletAccounts = isTransfer && !accountId.isNullOrBlank() && !targetAccountId.isNullOrBlank()
            // Outside→Wallet: source is outside wallet AND target is a real wallet account (non-null, non-blank)
            val isOutsideToWallet  = isOutsideSource && !targetAccountId.isNullOrBlank()

            val primaryAmount = if (isOutsideToWallet && target != null) target else source
            // Any outside-source transfer is "money coming in" — use income styling even before target is picked
            val primaryPrefix  = if (isOutsideSource) "+" else amountPrefix
            val primaryColor   = if (isOutsideSource) (target?.color ?: AppColors.contentPrimary) else activeAmountColor

            if (showAmount) {
                Box(contentAlignment = Alignment.CenterEnd, modifier = Modifier.fillMaxWidth()) {
                    EntryAmountDisplay(
                        amount = primaryAmount.value,
                        currency = primaryAmount.currency,
                        onCurrencyClick = primaryAmount.onCurrencyClick,
                        onAmountClick = primaryAmount.onValueClick,
                        amountColor = primaryColor,
                        supportedCurrencies = primaryAmount.supportedCurrencies,
                        showCurrency = true,
                        label = primaryAmount.label,
                        prefix = primaryPrefix,
                        isError = primaryAmount.isError || errors.contains(EntryField.AMOUNT)
                    )
                }
            }

            val showRateChip = isTransfer && bothWalletAccounts && target != null
                    && (source.currency != target.currency
                    || transferRate.compareTo(BigDecimal.ONE) != 0)

            if (showRateChip) {
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
            }

            if (isTransfer && bothWalletAccounts && target != null) {
                Spacer(modifier = Modifier.height(AeonTheme.dimensions.spacingMd))

                val displayTargetAmount = target.value.ifBlank {
                    (source.value.toBigDecimalOrNull() ?: BigDecimal.ZERO).multiply(transferRate)
                        .setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
                }

                EntryAmountDisplay(
                    amount = displayTargetAmount,
                    currency = target.currency,
                    onCurrencyClick = target.onCurrencyClick,
                    onAmountClick = target.onValueClick,
                    amountColor = target.color ?: AppColors.contentPrimary.copy(alpha = 0.8f),
                    supportedCurrencies = target.supportedCurrencies.ifEmpty { source.supportedCurrencies },
                    label = target.label,
                    prefix = "+", // Target is always Receiver in transfer
                    isError = target.isError || (isTransfer && errors.contains(EntryField.AMOUNT))
                )
            }

            if (nameState != null) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = AeonTheme.dimensions.spacingMd),
                    thickness = (AeonTheme.dimensions.spacingXs / 8),
                    color = AppColors.divider.copy(alpha = 0.15f)
                )

                EntryEditorialInput(
                    value = nameState.value,
                    onValueChange = nameState.onValueChange,
                    label = nameState.label ?: "",
                    icon = nameIcon,
                    isError = nameState.isError,
                    isFlush = true
                )
            }
        }

        // Fixed action button (Split/Swap) at the top right of the header card
        val overlayAction = when {
            onSplitClick != null && !isTransfer -> onSplitClick
            onSwapClick != null && isTransfer -> onSwapClick
            else -> null
        }

        if (overlayAction != null) {
            IconButton(
                onClick = overlayAction,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(AeonTheme.dimensions.cardPadding)
                    .size(AeonTheme.dimensions.iconSizeLg)
            ) {
                Box(
                    modifier = Modifier
                        .size(AeonTheme.dimensions.iconSizeLg)
                        .clip(CircleShape)
                        .background(
                            if (isSplitMode && !isTransfer) AppColors.primary.copy(alpha = 0.12f)
                            else AppColors.transparent
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isTransfer) Icons.Rounded.SwapHoriz
                        else Icons.AutoMirrored.Rounded.CallSplit,
                        contentDescription = if (isTransfer) "Swap accounts" else "Split Transaction",
                        modifier = Modifier.size(AeonTheme.dimensions.iconSizeMd),
                        tint = when {
                            isTransfer -> AppColors.contentSecondary.copy(alpha = 0.4f)
                            isSplitMode -> AppColors.primary
                            else -> AppColors.contentSecondary.copy(alpha = 0.4f)
                        }
                    )
                }
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
                transactionType = EntryMode.EXPENSE,
                onTypeSelected = {},
                source = AeonAmountData(
                    value = "1,250.00",
                    currency = "USD"
                ),
                nameState = AeonTextFieldState(
                    value = "Monthly Rent",
                    onValueChange = {},
                    label = "BUDGET NAME"
                )
            )
        }
    }
}
