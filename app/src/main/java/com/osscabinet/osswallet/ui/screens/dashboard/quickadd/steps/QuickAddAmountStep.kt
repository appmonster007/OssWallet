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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.AccountStructureType
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Template
import java.math.BigDecimal
import com.osscabinet.ossWallet.ui.components.CalculatorNumpad
import com.osscabinet.ossWallet.ui.components.CurrencySelector
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.components.getIconByName
import com.osscabinet.ossWallet.ui.components.GlassSurface
import com.osscabinet.ossWallet.ui.components.aeonHairlineBorder
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.GlassVariant
import com.osscabinet.ossWallet.ui.theme.GlassTokens
import com.osscabinet.ossWallet.ui.theme.LocalAppFont
import com.osscabinet.ossWallet.ui.theme.copyStyle
import com.osscabinet.ossWallet.ui.util.formatCurrency

@Composable
fun QuickAddAmountStep(
    amount: BigDecimal,
    template: Template?,
    category: Category?,
    account: Account?,
    accountBalance: BigDecimal,
    exchangeRates: Map<String, BigDecimal>,
    baseCurrency: String,
    selectedCurrency: String,
    onCurrencySelected: (String) -> Unit,
    onAmountChange: (BigDecimal) -> Unit,
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
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Editorial Context Strip: [Icon] Template/Category · Account
        EditorialContextStrip(
            template = template,
            category = category,
            account = account,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        CalculatorNumpad(
            amount = amount.stripTrailingZeros().toPlainString(),
            onAmountChange = { onAmountChange(it.toBigDecimalOrNull() ?: BigDecimal.ZERO) },
            onDone = onDone,
            showDoneButton = false,
            modifier = Modifier.fillMaxWidth(),
            header = { state, _, onHistory ->
                AmountHeroPanel(
                    amount = state.display,
                    expression = state.tokens.joinToString(" ") { it.toString() },
                    currency = selectedCurrency.ifBlank { currencies.firstOrNull() ?: "USD" },
                    currencies = currencies,
                    onCurrencyChange = onCurrencySelected,
                    accountName = account?.name ?: "Unknown",
                    accountBalance = accountBalance,
                    accountCurrency = account?.currencyCode ?: "USD"
                )
            }
        )
    }
}

@Composable
private fun EditorialContextStrip(
    template: Template?,
    category: Category?,
    account: Account?,
    modifier: Modifier = Modifier
) {
    val displayCategory = template?.categoryId?.let { _ -> category } ?: category
    val fallbackAccent = AppColors.accent
    val accentColor = remember(displayCategory?.colorHex) {
        displayCategory?.colorHex
            ?.let { Color(it or (0xFF shl 24).toInt()) }
            ?: fallbackAccent
    }

    GlassSurface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        glassVariant = GlassVariant.Item,
        hazeStyle = GlassTokens.itemStyle().copyStyle(bgColor = AppColors.recessedSurface, alpha = 0.4f),
        fallbackColor = AppColors.recessedSurface.copy(alpha = 0.4f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Icon container
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.15f))
                    .aeonHairlineBorder(RoundedCornerShape(10.dp), accent = accentColor.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getIconByName(displayCategory?.iconName),
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (template != null) "TEMPLATE" else "CATEGORY",
                    style = AppTextStyles.sectionOverline.copy(fontSize = 8.sp),
                    color = AppColors.contentSecondary.copy(alpha = 0.6f)
                )
                Text(
                    text = template?.name ?: category?.name ?: "Quick Entry",
                    style = AppTextStyles.managementItemTitle,
                    color = AppColors.contentPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Separator
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(24.dp)
                    .background(AppColors.divider.copy(alpha = 0.4f))
            )

            // Account info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
                Text(
                    text = account?.name ?: "No Account",
                    style = AppTextStyles.sectionOverline.copy(fontSize = 9.sp, fontWeight = FontWeight.Medium),
                    color = AppColors.contentSecondary
                )
            }
        }
    }
}

@Composable
private fun AmountHeroPanel(
    amount: String,
    expression: String,
    currency: String,
    currencies: List<String>,
    onCurrencyChange: (String) -> Unit,
    accountName: String,
    accountBalance: BigDecimal,
    accountCurrency: String
) {
    val (whole, decimal) = remember(amount) {
        val parts = amount.split(".")
        if (parts.size == 2) parts[0] + "." to parts[1]
        else amount + "." to "00"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .background(AppColors.background, RoundedCornerShape(20.dp))
            .aeonHairlineBorder(RoundedCornerShape(20.dp)),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Hero Content
        Column(modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "AMOUNT",
                    style = AppTextStyles.sectionOverline.copy(fontSize = 8.sp),
                    color = AppColors.accent
                )
                Spacer(Modifier.width(10.dp))
                HorizontalDivider(modifier = Modifier.weight(1f), color = AppColors.divider.copy(alpha = 0.3f))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Hero Amount Display
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = whole,
                        style = AppTextStyles.heroAmountLarge.copy(
                            fontFamily = LocalAppFont.current.displayFamily,
                            fontWeight = FontWeight.Light,
                            fontSize = 54.sp
                        ),
                        color = AppColors.contentPrimary
                    )
                    Text(
                        text = decimal,
                        style = AppTextStyles.heroAmountLarge.copy(
                            fontFamily = LocalAppFont.current.displayFamily,
                            fontWeight = FontWeight.Light,
                            fontSize = 38.sp
                        ),
                        color = AppColors.contentSecondary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    // Blinking cursor
                    Box(
                        modifier = Modifier
                            .padding(start = 6.dp, bottom = 10.dp)
                            .width(2.dp)
                            .height(46.dp)
                            .background(AppColors.accent)
                    )
                }

                // Currency Pill
                if (currencies.size > 1) {
                    CurrencySelector(
                        selectedCurrency = currency,
                        onCurrencySelected = onCurrencyChange,
                        supportedCurrencies = currencies
                    )
                } else {
                    Text(
                        text = currency,
                        style = AppTextStyles.sectionOverline,
                        color = AppColors.contentSecondary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
            }

            // Expression line
            if (expression.isNotBlank() && expression != amount) {
                Text(
                    text = expression,
                    style = AppTextStyles.metaLabel.copy(fontSize = 11.sp),
                    color = AppColors.contentSecondary.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // Footer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
                .height(1.dp)
                .background(AppColors.divider.copy(alpha = 0.3f))
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Today · Now",
                style = AppTextStyles.metaLabel.copy(fontSize = 9.sp),
                color = AppColors.contentSecondary
            )
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Deduct from ",
                    style = AppTextStyles.caption.copy(fontSize = 11.sp),
                    color = AppColors.contentSecondary
                )
                Text(
                    text = accountName,
                    style = AppTextStyles.caption.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                    color = AppColors.contentPrimary
                )
            }
        }
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 18.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = "Balance: ${formatCurrency(accountBalance, accountCurrency)}",
                style = AppTextStyles.metaLabel.copy(fontSize = 10.sp),
                color = AppColors.accent.copy(alpha = 0.8f)
            )
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
                amount = BigDecimal("123.45"),
                template = null,
                category = mockCategory,
                account = mockAccount,
                accountBalance = BigDecimal("1500.00"),
                exchangeRates = emptyMap(),
                baseCurrency = "USD",
                selectedCurrency = "USD",
                onCurrencySelected = {},
                onAmountChange = {},
                onDone = {}
            )
        }
    }
}
