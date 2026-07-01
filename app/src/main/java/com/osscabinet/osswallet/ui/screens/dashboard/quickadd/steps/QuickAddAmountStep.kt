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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.osscabinet.ossWallet.domain.model.AccountStructureType
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.EntryMode
import com.osscabinet.ossWallet.domain.model.Template
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.domain.service.currency.ExchangeRateSnapshot
import com.osscabinet.ossWallet.ui.components.CalculatorNumpad
import com.osscabinet.ossWallet.ui.components.CurrencySelector
import com.osscabinet.ossWallet.ui.components.GlassSurface
import com.osscabinet.ossWallet.ui.components.aeonHairlineBorder
import com.osscabinet.ossWallet.ui.components.getAccountIconByName
import com.osscabinet.ossWallet.ui.components.getIconByName
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.GlassTokens
import com.osscabinet.ossWallet.ui.theme.GlassVariant
import com.osscabinet.ossWallet.ui.theme.LocalAppFont
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.theme.copyStyle
import com.osscabinet.ossWallet.ui.util.formatCurrency
import java.math.BigDecimal

@Composable
fun QuickAddAmountStep(
    amount: BigDecimal,
    template: Template?,
    category: Category?,
    account: Account?,
    toAccount: Account? = null,
    accountBalance: BigDecimal,
    toAccountBalance: BigDecimal = BigDecimal.ZERO,
    entryMode: EntryMode,
    exchangeRates: ExchangeRateSnapshot,
    baseCurrency: String,
    selectedCurrency: String,
    selectedToCurrency: String = "",
    onCurrencySelected: (String, Boolean) -> Unit,
    onAmountChange: (BigDecimal) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currencies = remember(account) {
        account?.let {
            (listOf(it.currencyCode) + it.allowedCurrencies).distinct()
                .filter { c -> c.isNotBlank() }
        } ?: emptyList()
    }
    
    val toCurrencies = remember(toAccount) {
        toAccount?.let {
            (listOf(it.currencyCode) + it.allowedCurrencies).distinct()
                .filter { c -> c.isNotBlank() }
        } ?: emptyList()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Editorial Context Strip: [Icon] Template/Category · Account + Balance
        EditorialContextStrip(
            template = template,
            category = category,
            account = account,
            toAccount = toAccount,
            accountBalance = accountBalance,
            toAccountBalance = toAccountBalance,
            entryMode = entryMode,
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
                    toCurrency = selectedToCurrency.ifBlank { toCurrencies.firstOrNull() ?: "" },
                    currencies = currencies,
                    toCurrencies = toCurrencies,
                    onCurrencyChange = { onCurrencySelected(it, true) },
                    onToCurrencyChange = { onCurrencySelected(it, false) },
                    accountBalance = accountBalance,
                    toAccountBalance = toAccountBalance,
                    accountCurrency = account?.currencyCode ?: "USD",
                    toAccountCurrency = toAccount?.currencyCode ?: "USD",
                    entryMode = entryMode,
                    inputAmount = amount,
                    exchangeRates = exchangeRates,
                    baseCurrency = baseCurrency
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
    toAccount: Account? = null,
    accountBalance: BigDecimal,
    toAccountBalance: BigDecimal = BigDecimal.ZERO,
    entryMode: EntryMode,
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
    val toAccountColor = remember(toAccount?.colorHex) {
        toAccount?.colorHex?.let { runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull() }
            ?: fallbackAccent
    }
    
    val accountCurrency = account?.currencyCode ?: "USD"
    val toAccountCurrency = toAccount?.currencyCode ?: "USD"

    GlassSurface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        glassVariant = GlassVariant.Item,
        hazeStyle = GlassTokens.itemStyle()
            .copyStyle(bgColor = AppColors.recessedSurface, alpha = 0.4f),
        fallbackColor = AppColors.recessedSurface.copy(alpha = 0.4f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (entryMode == EntryMode.TRANSFER) {
                // From Account
                AccountSummary(
                    name = account?.name ?: "From",
                    balance = accountBalance,
                    currency = accountCurrency,
                    color = accountColor,
                    icon = getAccountIconByName(account?.icon),
                    isEnd = false,
                    modifier = Modifier.weight(1f)
                )

                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = null,
                    tint = AppColors.contentSecondary.copy(alpha = 0.4f),
                    modifier = Modifier.size(14.dp)
                )

                // To Account
                AccountSummary(
                    name = toAccount?.name ?: "To",
                    balance = toAccountBalance,
                    currency = toAccountCurrency,
                    color = toAccountColor,
                    icon = getAccountIconByName(toAccount?.icon),
                    isEnd = true,
                    modifier = Modifier.weight(1f)
                )
            } else {
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
                AccountSummary(
                    name = account?.name ?: "No Account",
                    balance = accountBalance,
                    currency = accountCurrency,
                    color = accountColor,
                    icon = getAccountIconByName(account?.icon),
                    isEnd = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun AccountSummary(
    name: String,
    balance: BigDecimal,
    currency: String,
    color: Color,
    icon: ImageVector,
    isEnd: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp, if (isEnd) Alignment.End else Alignment.Start)
    ) {
        if (!isEnd) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f))
                    .aeonHairlineBorder(CircleShape, accent = color.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Column(
            horizontalAlignment = if (isEnd) Alignment.End else Alignment.Start,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = name.uppercase(),
                style = AppTextStyles.sectionOverline.copy(fontSize = 7.sp),
                color = AppColors.contentSecondary.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = formatCurrency(balance, currency),
                style = AppTextStyles.caption.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = LocalAppFont.current.monoFamily
                ),
                color = AppColors.contentPrimary,
                maxLines = 1
            )
        }

        if (isEnd) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f))
                    .aeonHairlineBorder(CircleShape, accent = color.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(14.dp)
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
    toCurrency: String = "",
    currencies: List<String>,
    toCurrencies: List<String> = emptyList(),
    onCurrencyChange: (String) -> Unit,
    onToCurrencyChange: (String) -> Unit = {},
    accountBalance: BigDecimal,
    toAccountBalance: BigDecimal = BigDecimal.ZERO,
    accountCurrency: String,
    toAccountCurrency: String = "USD",
    entryMode: EntryMode,
    inputAmount: BigDecimal,
    exchangeRates: ExchangeRateSnapshot,
    baseCurrency: String
) {
    val (whole, decimal) = remember(amount) {
        val parts = amount.split(".")
        if (parts.size == 2) {
            parts[0] + "." to parts[1]
        } else {
            amount + "." to "00"
        }
    }

    val closingBalance = remember(
        accountBalance,
        inputAmount,
        entryMode,
        currency,
        accountCurrency,
        exchangeRates,
        baseCurrency
    ) {
        val convertedInput = if (currency == accountCurrency) {
            inputAmount
        } else {
            CurrencyService.convertAmount(
                amount = inputAmount,
                fromCurrency = currency,
                toCurrency = accountCurrency,
                exchangeRates = exchangeRates
            )
        }

        if (entryMode == EntryMode.INCOME) accountBalance + convertedInput
        else accountBalance - convertedInput
    }
    
    val toClosingBalance = remember(
        toAccountBalance,
        inputAmount,
        entryMode,
        currency,
        toAccountCurrency,
        exchangeRates,
        baseCurrency
    ) {
        if (entryMode != EntryMode.TRANSFER) BigDecimal.ZERO
        else {
            val convertedInput = if (currency == toAccountCurrency) {
                inputAmount
            } else {
                CurrencyService.convertAmount(
                    amount = inputAmount,
                    fromCurrency = currency,
                    toCurrency = toAccountCurrency,
                    exchangeRates = exchangeRates
                )
            }
            toAccountBalance + convertedInput
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.background, RoundedCornerShape(20.dp))
            .aeonHairlineBorder(RoundedCornerShape(20.dp))
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
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(AppColors.divider.copy(alpha = 0.3f))
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Currency Selectors
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    CurrencyPill(
                        currency = currency,
                        currencies = currencies,
                        onCurrencyChange = onCurrencyChange
                    )
                    
                    if (entryMode == EntryMode.TRANSFER) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            tint = AppColors.contentSecondary.copy(alpha = 0.4f),
                            modifier = Modifier.size(10.dp)
                        )
                        CurrencyPill(
                            currency = toCurrency,
                            currencies = toCurrencies,
                            onCurrencyChange = onToCurrencyChange
                        )
                    }
                }

                // Hero Amount Display (Right)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = whole,
                        style = AppTextStyles.heroAmountLarge.copy(
                            fontFamily = LocalAppFont.current.displayFamily,
                            fontWeight = FontWeight.Light,
                            fontSize = 24.sp,
                            lineHeight = 24.sp
                        ),
                        color = AppColors.contentPrimary
                    )
                    Text(
                        text = decimal,
                        style = AppTextStyles.heroAmountLarge.copy(
                            fontFamily = LocalAppFont.current.displayFamily,
                            fontWeight = FontWeight.Light,
                            fontSize = 20.sp,
                            lineHeight = 20.sp
                        ),
                        color = AppColors.contentSecondary.copy(alpha = 0.6f),
                    )
                }
            }

            // Expression line
            if (expression.isNotBlank() && expression != amount && expression != "0") {
                Text(
                    text = expression,
                    style = AppTextStyles.metaLabel.copy(
                        fontSize = 10.sp,
                        fontFamily = LocalAppFont.current.monoFamily
                    ),
                    color = AppColors.contentSecondary.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 1.dp, bottom = 8.dp)
                )
            } else {
                Spacer(Modifier.height(8.dp))
            }
        }

        // Footer
        val dividerColor = AppColors.divider.copy(alpha = 0.45f)
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
        ) {
            drawLine(
                color = dividerColor,
                start = Offset(0f, 0f),
                end = Offset(size.width, 0f),
                strokeWidth = size.height,
                pathEffect = PathEffect.dashPathEffect(
                    intervals = floatArrayOf(15f, 8f),
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
                style = AppTextStyles.metaLabel.copy(fontSize = 9.sp),
                color = AppColors.contentSecondary
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (entryMode == EntryMode.TRANSFER) {
                    ClosingBalanceLabel("From: ${formatCurrency(closingBalance, accountCurrency)}")
                    ClosingBalanceLabel("To: ${formatCurrency(toClosingBalance, toAccountCurrency)}")
                } else {
                    ClosingBalanceLabel("Closing: ${formatCurrency(closingBalance, accountCurrency)}")
                }
            }
        }
    }
}

@Composable
private fun CurrencyPill(
    currency: String,
    currencies: List<String>,
    onCurrencyChange: (String) -> Unit
) {
    Box {
        if (currencies.size > 1) {
            CurrencySelector(
                selectedCurrency = currency,
                onCurrencySelected = onCurrencyChange,
                supportedCurrencies = currencies
            )
        } else {
            Text(
                text = currency,
                style = AppTextStyles.sectionOverline.copy(
                    fontSize = 8.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                ),
                color = AppColors.contentSecondary,
                modifier = Modifier
                    .background(
                        AppColors.recessedSurface.copy(alpha = 0.3f),
                        RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            )
        }
    }
}

@Composable
private fun ClosingBalanceLabel(text: String) {
    Text(
        text = text,
        style = AppTextStyles.metaLabel.copy(
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = LocalAppFont.current.monoFamily
        ),
        color = AppColors.accent.copy(alpha = 0.9f)
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewQuickAddAmountStep() {
    val mockAccount = Account(
        "1",
        "Cash",
        "USD",
        AccountStructureType.CASH,
        "#4CAF50",
        "Payments",
        allowedCurrencies = listOf("USD", "EUR")
    )
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
                exchangeRates = ExchangeRateSnapshot.EMPTY,
                baseCurrency = "USD",
                selectedCurrency = "USD",
                onCurrencySelected = { _, _ -> },
                onAmountChange = {},
                onDone = {}
            )
        }
    }
}
