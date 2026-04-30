package com.osscabinet.ossWallet.ui.accountdetail.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.CashFlowType
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.card.CardFilterRules
import com.osscabinet.ossWallet.domain.service.CurrencyService
import com.osscabinet.ossWallet.ui.components.AccountIconMap
import com.osscabinet.ossWallet.ui.components.AeonFlowBadge
import com.osscabinet.ossWallet.ui.util.formatAmount
import com.osscabinet.ossWallet.ui.util.formatCurrencyRight
import com.osscabinet.ossWallet.ui.util.toContainerColor
import com.osscabinet.ossWallet.ui.util.toOnContainerColor
import java.math.BigDecimal
import androidx.compose.foundation.isSystemInDarkTheme

@Composable
fun AccountHeroCard(
    account: Account,
    balance: BigDecimal,
    income: BigDecimal,
    expense: BigDecimal,
    baseCurrency: String,
    exchangeRates: Map<String, BigDecimal>,
    modifier: Modifier = Modifier,
    currencyBalances: Map<String, BigDecimal> = emptyMap(),
    precision: Int = 2,
    showCurrencyBadges: Boolean = true,
    filter: TransactionFilter = TransactionFilter(),
    onShowCurrencyBadgesChange: (Boolean) -> Unit = {},
    onFilterChange: (TransactionFilter) -> Unit = {},
    onClick: (() -> Unit)? = null,
    onDelete: () -> Unit = {},
    filterRules: CardFilterRules = CardFilterRules.DEFAULT
) {
    val isDark = isSystemInDarkTheme()
    val isAmoled = false

    val accountColor = try {
        Color(android.graphics.Color.parseColor(account.colorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }
    val containerColor = accountColor.toContainerColor(isDark, isAmoled)
    val contentColor = accountColor.toOnContainerColor(isDark, isAmoled)
    val icon = AccountIconMap[account.icon] ?: Icons.Rounded.AccountBalance

    var cardSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current

    // Calculate balance in base currency
    val balanceInBase = CurrencyService.convertAmount(
        amount = balance,
        fromCurrency = account.currencyCode,
        toCurrency = baseCurrency,
        exchangeRates = exchangeRates,
        precision = precision
    )

    // Calculate net flow
    val netFlow = income.subtract(expense)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick?.invoke() }
            .onSizeChanged { cardSize = it },
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(28.dp),
        border = if (isAmoled) BorderStroke(1.dp, contentColor.copy(alpha = 0.3f)) else null
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Large background icon
            val iconHeight = with(density) { (cardSize.height * 0.7f).toDp() }
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 24.dp, end = 8.dp)
                    .size(iconHeight)
                    .alpha(0.12f),
                tint = contentColor
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Account Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Account Icon Circle
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = contentColor.copy(alpha = 0.15f),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = contentColor
                                )
                            }
                        }

                        Column {
                            Text(
                                text = account.name,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = contentColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = account.structureType.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    letterSpacing = 0.5.sp
                                ),
                                color = contentColor.copy(alpha = 0.6f)
                            )
                        }
                    }

                    // Structure Type Badge
                    Surface(
                        color = contentColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = account.currencyCode,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            ),
                            color = contentColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Main Balance Section
                Text(
                    text = "Current Balance",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp
                    ),
                    color = contentColor.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatCurrencyRight(balance, account.currencyCode, precision = precision),
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 32.sp
                    ),
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (account.currencyCode != baseCurrency) {
                    Text(
                        text = "≈ ${formatCurrencyRight(balanceInBase, baseCurrency, precision = precision)}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = contentColor.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Income/Expense Flow Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Flow Badges
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AeonFlowBadge(
                            amount = income,
                            type = CashFlowType.INCOME,
                            currency = account.currencyCode,
                            precision = precision
                        )
                        AeonFlowBadge(
                            amount = expense,
                            type = CashFlowType.EXPENSE,
                            currency = account.currencyCode,
                            precision = precision
                        )
                    }
                }

                // Currency Balances (if any and enabled)
                if (showCurrencyBadges && currencyBalances.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Multi-Currency Holdings",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            letterSpacing = 0.5.sp
                        ),
                        color = contentColor.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val sortedCurrencyBalances = remember(currencyBalances) {
                            currencyBalances.toList().sortedBy { it.first }
                        }

                        sortedCurrencyBalances.forEach { (curr, bal) ->
                            Surface(
                                color = contentColor.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        curr,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        ),
                                        color = contentColor.copy(alpha = 0.6f)
                                    )
                                    Text(
                                        formatAmount(bal, precision = precision),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = contentColor
                                    )
                                }
                            }
                        }
                    }
                }

                // Allowed Currencies (if multi-currency account)
                if (account.allowedCurrencies.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Accepts:",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp
                            ),
                            color = contentColor.copy(alpha = 0.5f)
                        )
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            account.allowedCurrencies.forEach { currency ->
                                Surface(
                                    color = contentColor.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        currency,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        color = contentColor.copy(alpha = 0.7f)
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
