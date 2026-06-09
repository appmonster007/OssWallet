package com.osscabinet.ossWallet.ui.screens.dashboard.quickadd.steps

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.ui.components.CalculatorNumpad
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles

@Composable
fun QuickAddAmountStep(
    amount: String,
    templateName: String?,
    categoryName: String?,
    account: Account?,
    selectedCurrency: String,
    onCurrencySelected: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val chipLabel = when {
        templateName != null -> "Template: $templateName"
        categoryName != null -> "Category: $categoryName"
        else -> null
    }

    // Currencies available for the selected account
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
        if (chipLabel != null) {
            AssistChip(
                onClick = {},
                label = { Text(chipLabel, style = AppTextStyles.chartAxisLabel, color = AppColors.primary) },
                shape = RoundedCornerShape(12.dp),
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = AppColors.primary.copy(alpha = 0.1f)
                ),
                border = AssistChipDefaults.assistChipBorder(
                    enabled = true,
                    borderColor = AppColors.primary.copy(alpha = 0.3f)
                )
            )
            Spacer(modifier = Modifier.height(6.dp))
        }

        // Currency selector — only shown when account has currencies to pick from
        if (currencies.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                currencies.forEach { currency ->
                    val isSelected = currency == selectedCurrency
                    FilterChip(
                        selected = isSelected,
                        onClick = { onCurrencySelected(currency) },
                        label = { Text(currency, style = AppTextStyles.formSwitchLabel) },
                        shape = RoundedCornerShape(10.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AppColors.primary.copy(alpha = 0.15f),
                            selectedLabelColor = AppColors.primary,
                            labelColor = AppColors.contentSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            selectedBorderColor = AppColors.primary.copy(alpha = 0.4f),
                            borderColor = AppColors.divider.copy(alpha = 0.3f)
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        } else if (currencies.size == 1) {
            // Single currency — show as a non-interactive label
            Text(
                text = currencies.first(),
                style = AppTextStyles.formSwitchLabel,
                color = AppColors.contentSecondary,
                modifier = Modifier.padding(horizontal = 24.dp)
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
