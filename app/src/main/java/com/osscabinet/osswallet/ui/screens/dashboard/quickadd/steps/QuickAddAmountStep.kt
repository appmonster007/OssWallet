package com.osscabinet.ossWallet.ui.screens.dashboard.quickadd.steps

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
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Template
import com.osscabinet.ossWallet.ui.components.CalculatorNumpad
import com.osscabinet.ossWallet.ui.components.CurrencySelector
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

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
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
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Currency dropdown — only when account has multiple currencies
        if (currencies.size > 1) {
            CurrencySelector(
                selectedCurrency = selectedCurrency.ifBlank { currencies.first() },
                onCurrencySelected = onCurrencySelected,
                supportedCurrencies = currencies
            )
            Spacer(modifier = Modifier.height(6.dp))
        } else if (currencies.size == 1) {
            Text(
                text = currencies.first(),
                style = AppTextStyles.formSwitchLabel,
                color = AppColors.contentSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))
        }

        CalculatorNumpad(
            amount = amount,
            onAmountChange = onAmountChange,
            onDone = onDone,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun ContextBadge(
    prefix: String,
    label: String,
    category: Category?,
    modifier: Modifier = Modifier
) {
    val bgColor = remember(category?.colorHex) {
        category?.colorHex
            ?.let { Color(it or (0xFF shl 24).toInt()) }
            ?: Color(0xFF6750A4.toInt()) // fallback purple
    }

    GlassSurface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        glassVariant = GlassVariant.Item,
        hazeStyle = GlassTokens.itemStyle().copyStyle(bgColor = bgColor, alpha = 0.12f),
        fallbackColor = bgColor.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Category icon in a small circle
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(bgColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getIconByName(category?.iconName),
                    contentDescription = null,
                    tint = bgColor,
                    modifier = Modifier.size(16.dp)
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
