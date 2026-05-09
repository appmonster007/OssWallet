package com.osscabinet.ossWallet.ui.accountdetail.components

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
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Filter
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.ui.components.AccountIconMap
import com.osscabinet.ossWallet.ui.components.cards.AeonCardFilterButton
import com.osscabinet.ossWallet.ui.components.cards.AeonCardSettingsSheet
import com.osscabinet.ossWallet.ui.components.cards.AeonExpressiveCard
import com.osscabinet.ossWallet.ui.util.formatAmount
import com.osscabinet.ossWallet.ui.util.formatCurrencyRight
import java.math.BigDecimal

@Composable
fun AccountHeroCard(
    account: Account,
    balance: BigDecimal,
    income: BigDecimal,
    expense: BigDecimal,
    baseCurrency: String,
    exchangeRates: Map<String, BigDecimal>,
    modifier: Modifier = Modifier,
    headerModifier: Modifier = Modifier,
    currencyBalances: Map<String, BigDecimal> = emptyMap(),
    precision: Int = 2,
    showCurrencyBadges: Boolean = true,
    filter: TransactionFilter = TransactionFilter(),
    onShowCurrencyBadgesChange: (Boolean) -> Unit = {},
    onFilterChange: (TransactionFilter) -> Unit = {},
    onClick: (() -> Unit)? = null,
    onDelete: () -> Unit = {},
    filterRules: CardFilterRules = CardFilterRules.DEFAULT,
    baseFilter: TransactionFilter = TransactionFilter(),
    accounts: List<Account> = emptyList(),
    allCategories: List<Category> = emptyList(),
    savedFilters: List<Filter> = emptyList(),
    @Suppress("UNUSED_PARAMETER") currentTheme: String = "System"
) {
    val accountColor = try {
        Color(android.graphics.Color.parseColor(account.colorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    val icon = AccountIconMap[account.icon] ?: Icons.Rounded.AccountBalance

    val balanceInBase = CurrencyService.convertAmount(
        amount = balance,
        fromCurrency = account.currencyCode,
        toCurrency = baseCurrency,
        exchangeRates = exchangeRates,
        precision = precision
    )

    var showSettingsSheet by remember { mutableStateOf(false) }

    AeonExpressiveCard(
        title = account.name,
        subtitle = account.structureType,
        icon = icon,
        iconColor = accountColor,
        accentColor = accountColor,
        modifier = modifier.clickable { onClick?.invoke() },
        headerModifier = headerModifier,
        filter = filter,
        headerTrailing = { activeCount ->
            AeonCardFilterButton(
                onClick = { showSettingsSheet = true },
                activeFilterCount = activeCount,
                icon = Icons.Rounded.Settings,
                activeIcon = Icons.Rounded.Settings
            )
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Main Balance Section
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "TOTAL BALANCE",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )

                val balanceParts =
                    formatCurrencyRight(balance, account.currencyCode, precision = precision, shorten = true).split(
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
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                fontSize = 18.sp
                            )
                        ) {
                            append(symbolText)
                        }
                    },
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 32.sp,
                        letterSpacing = (-1).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (account.currencyCode != baseCurrency) {
                    Text(
                        text = "≈ ${
                            formatCurrencyRight(
                                balanceInBase,
                                baseCurrency,
                                precision = precision,
                                shorten = true
                            )
                        }",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
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
                    icon = Icons.Rounded.TrendingUp,
                    color = Color(0xFF00695C),
                    modifier = Modifier.weight(1f)
                )
                FlowSummaryItem(
                    label = "Expense",
                    amount = expense,
                    currency = account.currencyCode,
                    icon = Icons.Rounded.TrendingDown,
                    color = Color(0xFFC62828),
                    modifier = Modifier.weight(1f)
                )
            }

            // Multi-Currency Section
            if (showCurrencyBadges && currencyBalances.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "DISTRIBUTION",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    )

                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        currencyBalances.toList().sortedBy { it.first }.forEach { (curr, bal) ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                                )
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
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Black,
                                            color = accountColor
                                        )
                                    )
                                    Text(
                                        formatAmount(bal, precision = precision),
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = (-0.2).sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSettingsSheet) {
        AeonCardSettingsSheet(
            title = "Account Details",
            filter = filter,
            onFilterChange = { onFilterChange(it) },
            onDismiss = { showSettingsSheet = false },
            onDelete = onDelete,
            accounts = accounts,
            allCategories = allCategories,
            savedFilters = savedFilters,
            filterRules = filterRules,
            baseFilter = baseFilter,
            settingsContent = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Multi-Currency",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            "Show breakdown of holdings",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                    Switch(
                        checked = showCurrencyBadges,
                        onCheckedChange = { onShowCurrencyBadgesChange(it) }
                    )
                }
            }
        )
    }
}

@Composable
private fun FlowSummaryItem(
    label: String,
    amount: BigDecimal,
    currency: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = color.copy(alpha = 0.05f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.1f)),
        modifier = modifier
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
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = color.copy(alpha = 0.8f)
                )
            }
            Text(
                text = formatAmount(amount),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun AccountHeroCardPreview() {
    val account =
        Account("1", "Wealth Fund", "USD", "Investment", "#004D40", "AccountBalance", emptyList())
    com.osscabinet.ossWallet.ui.theme.OssWalletTheme {
        Box(modifier = Modifier.padding(24.dp)) {
            AccountHeroCard(
                account = account,
                balance = BigDecimal("245800.50"),
                income = BigDecimal("15000.00"),
                expense = BigDecimal("4200.00"),
                baseCurrency = "USD",
                exchangeRates = emptyMap(),
                currencyBalances = mapOf(
                    "USD" to BigDecimal("180000.00"),
                    "EUR" to BigDecimal("45000.00"),
                    "GBP" to BigDecimal("12000.00")
                )
            )
        }
    }
}
