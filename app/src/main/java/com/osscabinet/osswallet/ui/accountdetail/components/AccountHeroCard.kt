package com.osscabinet.ossWallet.ui.accountdetail.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.card.CardFilterRules
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.CashFlowType
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Filter
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.service.CurrencyService
import com.osscabinet.ossWallet.ui.components.AccountIconMap
import com.osscabinet.ossWallet.ui.components.AeonFlowBadge
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
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Main Balance Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "TOTAL BALANCE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp,
                            fontSize = 10.sp
                        ),
                        color = accountColor.copy(alpha = 0.8f)
                    )
                }

                Text(
                    text = formatCurrencyRight(balance, account.currencyCode, precision = precision),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 36.sp,
                        letterSpacing = (-1.5).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (account.currencyCode != baseCurrency) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f))
                    ) {
                        Text(
                            text = "≈ ${formatCurrencyRight(balanceInBase, baseCurrency, precision = precision)}",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Income/Expense Flow Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AeonFlowBadge(
                    amount = income,
                    type = CashFlowType.INCOME,
                    currency = account.currencyCode,
                    precision = precision,
                    modifier = Modifier.weight(1f),
                    incomeColor = Color(0xFF004D40) // Teal
                )
                AeonFlowBadge(
                    amount = expense,
                    type = CashFlowType.EXPENSE,
                    currency = account.currencyCode,
                    precision = precision,
                    modifier = Modifier.weight(1f),
                    expenseColor = Color(0xFFB3261E) // Red
                )
            }

            // Multi-Currency Section
            if (showCurrencyBadges && currencyBalances.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "DISTRIBUTION",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    )
                    
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        currencyBalances.toList().sortedBy { it.first }.forEach { (curr, bal) ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = accountColor.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            curr,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Black,
                                                fontSize = 9.sp,
                                                color = accountColor
                                            ),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(
                                        formatAmount(bal, precision = precision),
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
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
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Multi-Currency", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
                        Text("Show breakdown of holdings", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
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

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun AccountHeroCardPreview() {
    val account = Account("1", "Wealth Fund", "USD", "Investment", "#004D40", "AccountBalance", emptyList())
    com.osscabinet.ossWallet.ui.theme.OssWalletTheme {
        Box(modifier = Modifier.padding(24.dp)) {
            AccountHeroCard(
                account = account,
                balance = BigDecimal("245800.50"),
                income = BigDecimal("15000.00"),
                expense = BigDecimal("4200.00"),
                baseCurrency = "USD",
                exchangeRates = emptyMap(),
                currencyBalances = mapOf("USD" to BigDecimal("180000.00"), "EUR" to BigDecimal("45000.00"), "GBP" to BigDecimal("12000.00"))
            )
        }
    }
}
