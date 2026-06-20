package com.osscabinet.ossWallet.ui.screens.dashboard.quickadd.steps

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.EntryMode
import com.osscabinet.ossWallet.domain.model.AccountStructureType
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Template
import java.math.BigDecimal
import com.osscabinet.ossWallet.ui.components.CalculatorNumpad
import com.osscabinet.ossWallet.ui.components.CurrencySelector
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.components.getAccountIconByName
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
    entryMode: EntryMode,
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
        // Editorial Context Strip: [Icon] Template/Category · Account + Balance
        EditorialContextStrip(
            template = template,
            category = category,
            account = account,
            accountBalance = accountBalance,
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
                    expression = state.displayExpression,
                    currency = selectedCurrency.ifBlank { currencies.firstOrNull() ?: "USD" },
                    currencies = currencies,
                    onCurrencyChange = onCurrencySelected,
                    accountBalance = accountBalance,
                    accountCurrency = account?.currencyCode ?: "USD",
                    entryMode = entryMode,
                    inputAmount = amount
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
    accountBalance: BigDecimal,
    modifier: Modifier = Modifier
) {
    val displayCategory = template?.categoryId?.let { _ -> category } ?: category
    val fallbackAccent = AppColors.accent
    val categoryColor = remember(displayCategory?.colorHex) {
        displayCategory?.colorHex
            ?.let { Color(it or (0xFF shl 24).toInt()) }
            ?: fallbackAccent
    }
    val accountColor = remember(account?.colorHex) {
        account?.colorHex?.let { runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull() }
            ?: fallbackAccent
    }
    val accountCurrency = account?.currencyCode ?: "USD"

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
            // Category Icon container
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(categoryColor.copy(alpha = 0.15f))
                    .aeonHairlineBorder(CircleShape, accent = categoryColor.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getIconByName(displayCategory?.iconName),
                    contentDescription = null,
                    tint = categoryColor,
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

            // Vertical Divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(24.dp)
                    .background(AppColors.divider.copy(alpha = 0.4f))
            )

            // Account info
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = account?.name ?: "No Account",
                    style = AppTextStyles.sectionOverline.copy(fontSize = 8.sp, fontWeight = FontWeight.Medium),
                    color = AppColors.contentSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatCurrency(accountBalance, accountCurrency),
                    style = AppTextStyles.caption.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                    color = AppColors.contentPrimary,
                    maxLines = 1
                )
            }

            // Account Icon container
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(accountColor.copy(alpha = 0.15f))
                    .aeonHairlineBorder(CircleShape, accent = accountColor.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getAccountIconByName(account?.icon),
                    contentDescription = null,
                    tint = accountColor,
                    modifier = Modifier.size(16.dp)
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
    accountBalance: BigDecimal,
    accountCurrency: String,
    entryMode: EntryMode,
    inputAmount: BigDecimal
) {
    val closingBalance = remember(accountBalance, inputAmount, entryMode) {
        if (entryMode == EntryMode.INCOME) accountBalance + inputAmount
        else accountBalance - inputAmount
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
//            .padding(horizontal = 20.dp, vertical = 8.dp)
            .background(AppColors.background, RoundedCornerShape(20.dp))
            .aeonHairlineBorder(RoundedCornerShape(20.dp)),
//        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Hero Content
        Column(modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp)) {
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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
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

                // Hero Amount Display
                Row(
                    verticalAlignment = Alignment.CenterVertically,
//                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Text(
                        text = amount,
                        style = AppTextStyles.heroAmountLarge.copy(
                            fontFamily = LocalAppFont.current.displayFamily,
                            fontWeight = FontWeight.Light,
                            fontSize = 24.sp
                        ),
                        color = AppColors.contentPrimary
                    )
                }
            }

            // Expression line
            if (expression.isNotBlank() && expression != amount && expression != "0") {
                Text(
                    text = expression,
                    style = AppTextStyles.metaLabel.copy(fontSize = 11.sp),
                    color = AppColors.contentSecondary.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // Footer

        val dividerColor = AppColors.divider.copy(alpha = 0.45f)
//        Box(
//            modifier = Modifier
//                .fillMaxWidth()
////                .padding(top = 4.dp)
//                .height(1.dp)
//                .background(AppColors.divider.copy(alpha = 0.45f))
//
//        )

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .height(1.dp)
        ) {
            drawLine(
                color = dividerColor,
                start = Offset(0f, 0f),
                end = Offset(size.width, 0f),
                strokeWidth = size.height, // 1.dp thickness
                pathEffect = PathEffect.dashPathEffect(
                    intervals = floatArrayOf(20f, 10f), // 20px dash, 10px space
                    phase = 0f
                )
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Today · Now",
                style = AppTextStyles.metaLabel.copy(fontSize = 10.sp),
                color = AppColors.contentSecondary
            )

            Text(
                text = "Closing: ${formatCurrency(closingBalance, accountCurrency)}",
                style = AppTextStyles.metaLabel.copy(fontSize = 10.sp, fontWeight = FontWeight.Medium),
                color = AppColors.accent.copy(alpha = 0.9f)
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
                entryMode = EntryMode.EXPENSE,
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
