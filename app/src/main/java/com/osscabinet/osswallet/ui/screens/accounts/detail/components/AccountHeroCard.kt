package com.osscabinet.ossWallet.ui.screens.accounts.detail.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.core.graphics.toColorInt
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.card.CardFilterRules
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.AccountStructureType
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.settings.AccountHeroCardConfig
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.ui.components.AppIconMap
import com.osscabinet.ossWallet.ui.components.cards.AeonExpressiveCard
import com.osscabinet.ossWallet.ui.components.cards.CardCallbacks
import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter.formatAmount
import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter.formatCurrencyRight
import com.osscabinet.ossWallet.ui.theme.BackgroundAmoled
import com.osscabinet.ossWallet.ui.theme.BackgroundDark
import com.osscabinet.ossWallet.ui.theme.LocalAppColorScheme
import com.osscabinet.ossWallet.ui.util.applyAeonTint
import java.math.BigDecimal
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles

@Composable
fun AccountHeroCard(
    account: Account,
    balance: BigDecimal,
    income: BigDecimal,
    expense: BigDecimal,
    baseCurrency: String,
    exchangeRates: Map<String, BigDecimal>,
    config: AccountHeroCardConfig,
    modifier: Modifier = Modifier,
    headerModifier: Modifier = Modifier,
    currencyBalances: Map<String, BigDecimal> = emptyMap(),
    filterRules: CardFilterRules? = null,
    baseFilter: TransactionFilter = TransactionFilter(),
    callbacks: CardCallbacks.AccountHero = CardCallbacks.AccountHero()
) {
    val accountColor = try {
        Color(account.colorHex.toColorInt())
    } catch (_: Exception) {
        AppColors.primary
    }

    val icon = AppIconMap[account.icon] ?: Icons.Rounded.AccountBalance

    val scheme = LocalAppColorScheme.current
    val theme = when (scheme.background) {
        BackgroundAmoled -> "Amoled"
        BackgroundDark -> "Dark"
        else -> "Light"
    }
    val (containerColor, contentColor) = accountColor.applyAeonTint(theme)

    val balanceInBase = CurrencyService.convertAmount(
        amount = balance,
        fromCurrency = account.currencyCode,
        toCurrency = baseCurrency,
        exchangeRates = exchangeRates
    )

    AeonExpressiveCard(
        title = account.name,
        subtitle = account.structureType.displayName,
        icon = icon,
        iconColor = contentColor,
        accentColor = contentColor,
        containerColor = containerColor,
        contentColor = contentColor,
        backgroundIcon = icon,
        modifier = modifier.clickable { callbacks.onClick?.invoke() },
        headerModifier = headerModifier,
        filterEnabled = false,
        filter = config.filter,
        baseFilter = baseFilter,
        filterRules = filterRules ?: config.filterRules,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Main Balance Section
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "TOTAL BALANCE",
                    style = AppTextStyles.drawerItemLabel.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    ),
                    color = contentColor.copy(alpha = 0.6f)
                )

                val balanceParts =
                    formatCurrencyRight(balance, account.currencyCode, shorten = true).split(
                        " "
                    )
                val amountText = balanceParts.dropLast(1).joinToString(" ")
                val symbolText = balanceParts.lastOrNull() ?: ""

                Text(
                    text = buildAnnotatedString {
                        append(amountText)
                        append(" ")
                        withStyle(
                            SpanStyle(
                                fontWeight = FontWeight.Light,
                                color = contentColor.copy(alpha = 0.5f),
                                fontSize = 18.sp
                            )
                        ) {
                            append(symbolText)
                        }
                    },
                    style = AppTextStyles.heroAmount.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 32.sp,
                        letterSpacing = (-1).sp
                    ),
                    color = contentColor
                )

                if (account.currencyCode != baseCurrency) {
                    Text(
                        text = "≈ ${
                            formatCurrencyRight(
                                balanceInBase,
                                baseCurrency,
                                shorten = true
                            )
                        }",
                        style = AppTextStyles.transactionTitle.copy(
                            fontWeight = FontWeight.Medium,
                            color = contentColor.copy(alpha = 0.7f)
                        )
                    )
                }
            }

            // Income/Expense Flow Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FlowSummaryItem(
                    label = "Income",
                    amount = income,
                    currency = account.currencyCode,
                    icon = Icons.AutoMirrored.Rounded.TrendingUp,
                    color = contentColor,
                    modifier = Modifier.weight(1f),
                    onClick = { callbacks.onClickIncome?.invoke(config.filter.copy(transactionTypes = setOf(TransactionType.INCOME))) }
                )
                FlowSummaryItem(
                    label = "Expense",
                    amount = expense,
                    currency = account.currencyCode,
                    icon = Icons.AutoMirrored.Rounded.TrendingDown,
                    color = contentColor,
                    modifier = Modifier.weight(1f),
                    onClick = { callbacks.onClickExpense?.invoke(config.filter.copy(transactionTypes = setOf(TransactionType.EXPENSE))) }
                )
            }

            // Multi-Currency Section
            if (config.showCurrencyBadges && currencyBalances.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "DISTRIBUTION",
                        style = AppTextStyles.drawerItemLabel.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            color = contentColor.copy(alpha = 0.8f)
                        )
                    )

                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        currencyBalances.toList().sortedBy { it.first }.forEach { (curr, bal) ->
                            Surface(
                                color = contentColor.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(
                                    1.dp,
                                    contentColor.copy(alpha = 0.1f)
                                ),
                                modifier = Modifier.clickable { callbacks.onClickCurrency?.invoke(curr) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(
                                        horizontal = 10.dp,
                                        vertical = 8.dp
                                    ),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        curr,
                                        style = AppTextStyles.transactionSwipeAction.copy(
                                            fontWeight = FontWeight.Black,
                                            color = contentColor
                                        )
                                    )
                                    Text(
                                        formatAmount(bal),
                                        style = AppTextStyles.transactionTitle.copy(
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = (-0.2).sp
                                        ),
                                        color = contentColor
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FlowSummaryItem(
    label: String,
    amount: BigDecimal,
    currency: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Surface(
        color = color.copy(alpha = 0.05f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.1f)),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = label.uppercase(),
                    style = AppTextStyles.transactionSwipeAction.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = color.copy(alpha = 0.8f)
                )
            }
            Text(
                text = formatAmount(amount),
                style = AppTextStyles.screenSubTitle.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = color
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun AccountHeroCardPreview() {
    val account =
        Account("1", "Wealth Fund", "USD", AccountStructureType.INVESTMENT, "#004D40", "AccountBalance", emptyList())
    com.osscabinet.ossWallet.ui.theme.OssWalletTheme {
        Box(modifier = Modifier.padding(24.dp)) {
            AccountHeroCard(
                account = account,
                balance = BigDecimal("245800.50"),
                income = BigDecimal("15000.00"),
                expense = BigDecimal("4200.00"),
                baseCurrency = "USD",
                exchangeRates = emptyMap(),
                config = AccountHeroCardConfig(cardId = "preview"),
                currencyBalances = mapOf(
                    "USD" to BigDecimal("1800000.00"),
                    "EUR" to BigDecimal("45000.00"),
                    "GBP" to BigDecimal("12000.00")
                )
            )
        }
    }
}
