package com.osscabinet.ossWallet.ui.screens.dashboard.quickadd.steps

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.AccountStructureType
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Template
import com.osscabinet.ossWallet.ui.components.CalculatorNumpad
import com.osscabinet.ossWallet.ui.components.CurrencySelector
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.theme.scaledDp
import com.osscabinet.ossWallet.ui.components.getIconByName
import com.osscabinet.ossWallet.ui.components.GlassSurface
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.GlassVariant
import com.osscabinet.ossWallet.ui.theme.GlassTokens
import com.osscabinet.ossWallet.ui.theme.copyStyle

@Composable
fun QuickAddAmountStep(
    amount: String,
    template: Template?,
    category: Category?,
    account: Account?,
    selectedCurrency: String,
    onCurrencySelected: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currencies = remember(account) {
        account?.let {
            (listOf(it.currencyCode) + it.allowedCurrencies).distinct().filter { c -> c.isNotBlank() }
        } ?: emptyList()
    }

    val uiScale = AeonTheme.uiScale
    val columnSpacing = scaledDp(10f, uiScale, min = 7f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(columnSpacing)
    ) {
        // Context badge: template or category
        val badgeCategory = template?.categoryId?.let { _ -> category } ?: category
        val badgeLabel = when {
            template != null -> template.name
            category != null -> category.name
            else -> null
        }
        val badgePrefix = if (template != null) "Template" else "Category"

        if (badgeLabel != null) {
            ContextBadge(
                prefix = badgePrefix,
                label = badgeLabel,
                category = badgeCategory,
                scale = uiScale,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }

        // Currency dropdown — only when account has multiple currencies
        if (currencies.size > 1) {
            CurrencySelector(
                selectedCurrency = selectedCurrency.ifBlank { currencies.first() },
                onCurrencySelected = onCurrencySelected,
                supportedCurrencies = currencies
            )
        } else if (currencies.size == 1) {
            Text(
                text = currencies.first(),
                style = AppTextStyles.formSwitchLabel,
                color = AppColors.contentSecondary
            )
        }

        CalculatorNumpad(
            amount = amount,
            onAmountChange = onAmountChange,
            onDone = onDone,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ContextBadge(
    prefix: String,
    label: String,
    category: Category?,
    modifier: Modifier = Modifier,
    scale: Float = 1.0f
) {
    val bgColor = remember(category?.colorHex) {
        category?.colorHex
            ?.let { Color(it or (0xFF shl 24).toInt()) }
            ?: Color(0xFF6750A4.toInt()) // fallback purple
    }

    val iconContainerSize: Dp = scaledDp(28f, scale, min = 20f)
    val iconSize: Dp = scaledDp(16f, scale, min = 12f)
    val cornerRadius: Dp = scaledDp(12f, scale, min = 8f)
    val iconCornerRadius: Dp = scaledDp(8f, scale, min = 6f)
    val horizontalPad: Dp = scaledDp(10f, scale, min = 8f)
    val verticalPad: Dp = scaledDp(6f, scale, min = 4f)

    GlassSurface(
        modifier = modifier,
        shape = RoundedCornerShape(cornerRadius),
        glassVariant = GlassVariant.Item,
        hazeStyle = GlassTokens.itemStyle().copyStyle(bgColor = bgColor, alpha = 0.12f),
        fallbackColor = bgColor.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = horizontalPad, vertical = verticalPad),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(iconContainerSize)
                    .clip(RoundedCornerShape(iconCornerRadius))
                    .background(bgColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getIconByName(category?.iconName),
                    contentDescription = null,
                    tint = bgColor,
                    modifier = Modifier.size(iconSize)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = prefix,
                    style = AppTextStyles.chartAxisLabel,
                    color = bgColor.copy(alpha = 0.7f)
                )
                Text(
                    text = label,
                    style = AppTextStyles.formSwitchLabel,
                    color = AppColors.contentPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewQuickAddAmountStep() {
    val mockAccount = Account("1", "Cash", "USD", AccountStructureType.CASH, "#4CAF50", "Payments", allowedCurrencies = listOf("USD", "EUR"))
    val mockCategory = Category(1, "Food", "Restaurant", -16737025, false, null)
    
    OssWalletTheme {
        Box(modifier = Modifier.background(AppColors.background)) {
            QuickAddAmountStep(
                amount = "123.45",
                template = null,
                category = mockCategory,
                account = mockAccount,
                selectedCurrency = "USD",
                onCurrencySelected = {},
                onAmountChange = {},
                onDone = {}
            )
        }
    }
}
