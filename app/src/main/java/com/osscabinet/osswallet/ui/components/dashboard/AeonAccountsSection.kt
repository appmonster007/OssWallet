package com.osscabinet.ossWallet.ui.components.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.ui.components.*
import com.osscabinet.ossWallet.ui.components.accounts.*
import com.osscabinet.ossWallet.ui.viewmodel.AccountStats
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

import androidx.compose.ui.tooling.preview.Preview
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme

@Serializable
data class AccountsSectionSettings(
    val layout: String = "grid", // "scroll" or "grid"
    val gridColumns: Int = 2,
    val showAddAccountCard: Boolean = true,
    val timelineRange: TimelineRange = TimelineRange.AllTime
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AeonAccountsSection(
    onManageClick: () -> Unit,
    onAccountClick: (String) -> Unit,
    onGridColumnsChange: (Int) -> Unit,
    accounts: List<Account>,
    accountStats: Map<String, AccountStats>,
    baseCurrency: String,
    exchangeRates: Map<String, BigDecimal>,
    currentTheme: String,
    modifier: Modifier = Modifier,
    headerModifier: Modifier = Modifier,
    gridColumns: Int = 2,
    layout: String = "grid",
    precision: Int = 2,
    timelineRange: TimelineRange = TimelineRange.AllTime,
    onLayoutChange: (String) -> Unit = {},
    onTimelineRangeChange: (TimelineRange) -> Unit = {},
    onHeaderClick: (() -> Unit)? = null,
    settingsTitle: String = "Account Settings",
    showAddAccountCard: Boolean = true,
    onShowAddAccountCardChange: (Boolean) -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }

    val timelineLabel = when (timelineRange) {
        is TimelineRange.AllTime -> "All Time"
        is TimelineRange.ThisPeriod -> timelineRange.period.label
        is TimelineRange.LastPeriod -> timelineRange.period.label
        is TimelineRange.CustomRange -> {
            val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy").withZone(ZoneId.systemDefault())
            val start = formatter.format(Instant.ofEpochMilli(timelineRange.start))
            val end = formatter.format(Instant.ofEpochMilli(timelineRange.end))
            "$start - $end"
        }
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            AeonFlowSectionHeader(
                title = "ACCOUNTS",
                modifier = if (onHeaderClick != null) headerModifier.clickable(onClick = onHeaderClick) else headerModifier,
                trailing = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = timelineLabel.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                fontSize = 10.sp
                            ),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        )
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.MoreHoriz,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            )

            if (showMenu) {
                AlertDialog(
                    onDismissRequest = { showMenu = false },
                    confirmButton = {
                        TextButton(onClick = { showMenu = false }) {
                            Text("Done")
                        }
                    },
                    title = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                settingsTitle,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            IconButton(
                                onClick = {
                                    onManageClick()
                                    showMenu = false
                                },
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                    contentColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(
                                    Icons.Default.Settings,
                                    contentDescription = "Manage Accounts"
                                )
                            }
                        }
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Timeline Range Selection
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        "TIMELINE",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        ),
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    TimelineFilterBar(
                                        currentRange = timelineRange,
                                        onRangeSelected = onTimelineRangeChange,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            AeonLayoutToggle(
                                layout = layout,
                                onLayoutChange = onLayoutChange
                            )

                            if (layout == "grid") {
                                AeonGridColumnsSettings(
                                    gridColumns = gridColumns,
                                    onGridColumnsChange = onGridColumnsChange
                                )
                            }

                            AeonSettingsToggle(
                                label = "SHOW ADD ACCOUNT CARD",
                                checked = showAddAccountCard,
                                onCheckedChange = onShowAddAccountCardChange
                            )
                        }
                    },
                    shape = RoundedCornerShape(32.dp),
                    containerColor = MaterialTheme.colorScheme.surface
                )
            }

            if (accounts.isEmpty()) {
                AeonAddAccountCard(
                    baseCurrency = baseCurrency,
                    currentTheme = currentTheme,
                    onClick = onManageClick,
                    gridColumns = if (layout == "grid") gridColumns else 2,
                    precision = precision,
                    modifier = Modifier.fillMaxWidth()
                )
            } else if (layout == "grid") {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    maxItemsInEachRow = gridColumns
                ) {
                    accounts.forEach { account ->
                        AccountItemWrapper(
                            account = account,
                            accountStats = accountStats,
                            baseCurrency = baseCurrency,
                            exchangeRates = exchangeRates,
                            currentTheme = currentTheme,
                            gridColumns = gridColumns,
                            precision = precision,
                            onAccountClick = onAccountClick,
                            modifier = if (gridColumns == 1) Modifier.fillMaxWidth() else Modifier.weight(1f)
                        )
                    }

                    if (showAddAccountCard) {
                        AeonAddAccountCard(
                            baseCurrency = baseCurrency,
                            currentTheme = currentTheme,
                            onClick = onManageClick,
                            gridColumns = gridColumns,
                            precision = precision,
                            modifier = if (gridColumns == 1) Modifier.fillMaxWidth() else Modifier.weight(1f)
                        )
                    } else if (gridColumns > 1 && accounts.size % gridColumns != 0) {
                        repeat(gridColumns - (accounts.size % gridColumns)) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 4.dp)
                ) {
                    items(accounts) { account ->
                        AccountItemWrapper(
                            account = account,
                            accountStats = accountStats,
                            baseCurrency = baseCurrency,
                            exchangeRates = exchangeRates,
                            currentTheme = currentTheme,
                            gridColumns = 2,
                            precision = precision,
                            onAccountClick = onAccountClick,
                            modifier = Modifier.width(160.dp)
                        )
                    }
                    if (showAddAccountCard) {
                        item {
                            AeonAddAccountCard(
                                baseCurrency = baseCurrency,
                                currentTheme = currentTheme,
                                onClick = onManageClick,
                                gridColumns = 2,
                                precision = precision,
                                modifier = Modifier.width(160.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRowScope.AccountItemWrapper(
    account: Account,
    accountStats: Map<String, AccountStats>,
    baseCurrency: String,
    exchangeRates: Map<String, BigDecimal>,
    currentTheme: String,
    gridColumns: Int,
    precision: Int,
    onAccountClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    AccountItemContent(
        account = account,
        accountStats = accountStats,
        baseCurrency = baseCurrency,
        exchangeRates = exchangeRates,
        currentTheme = currentTheme,
        gridColumns = gridColumns,
        precision = precision,
        onAccountClick = onAccountClick,
        modifier = modifier
    )
}

@Composable
private fun AccountItemWrapper(
    account: Account,
    accountStats: Map<String, AccountStats>,
    baseCurrency: String,
    exchangeRates: Map<String, BigDecimal>,
    currentTheme: String,
    gridColumns: Int,
    precision: Int,
    onAccountClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    AccountItemContent(
        account = account,
        accountStats = accountStats,
        baseCurrency = baseCurrency,
        exchangeRates = exchangeRates,
        currentTheme = currentTheme,
        gridColumns = gridColumns,
        precision = precision,
        onAccountClick = onAccountClick,
        modifier = modifier
    )
}

@Composable
private fun AccountItemContent(
    account: Account,
    accountStats: Map<String, AccountStats>,
    baseCurrency: String,
    exchangeRates: Map<String, BigDecimal>,
    currentTheme: String,
    gridColumns: Int,
    precision: Int,
    onAccountClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val stats = accountStats[account.id]
    val totalInBase = stats?.currencyBalances?.let { balances ->
        com.osscabinet.ossWallet.ui.util.calculateTotalInCurrency(
            balances,
            baseCurrency,
            exchangeRates,
            precision
        )
    } ?: BigDecimal.ZERO

    val isDarkTheme = when (currentTheme) {
        "Light" -> false
        "Dark", "Amoled" -> true
        else -> isSystemInDarkTheme()
    }
    val isAmoled = currentTheme == "Amoled"

    when (gridColumns) {
        1 -> AeonAccountCardLarge(
            account = account,
            balanceInBase = totalInBase,
            baseCurrency = baseCurrency,
            currencyBalances = stats?.currencyBalances ?: emptyMap(),
            incomeInAccountCurrency = stats?.incomeInAccountCurrency ?: BigDecimal.ZERO,
            expenseInAccountCurrency = stats?.expenseInAccountCurrency ?: BigDecimal.ZERO,
            isDarkTheme = isDarkTheme,
            isAmoled = isAmoled,
            onClick = { onAccountClick(account.id) },
            modifier = modifier,
            precision = precision
        )
        2 -> AeonAccountCardMedium(
            account = account,
            balanceInBase = totalInBase,
            baseCurrency = baseCurrency,
            currencyBalances = stats?.currencyBalances ?: emptyMap(),
            isDarkTheme = isDarkTheme,
            isAmoled = isAmoled,
            onClick = { onAccountClick(account.id) },
            modifier = modifier,
            precision = precision
        )
        else -> AeonAccountCardSmall(
            account = account,
            balanceInBase = totalInBase,
            baseCurrency = baseCurrency,
            currencyBalances = stats?.currencyBalances ?: emptyMap(),
            isDarkTheme = isDarkTheme,
            isAmoled = isAmoled,
            onClick = { onAccountClick(account.id) },
            modifier = modifier,
            precision = precision
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowRowScope.AeonAddAccountCard(
    baseCurrency: String,
    currentTheme: String,
    onClick: () -> Unit,
    gridColumns: Int = 2,
    precision: Int = 2,
    modifier: Modifier = Modifier
) {
    AeonAddAccountCardContent(baseCurrency, currentTheme, onClick, gridColumns, precision, modifier)
}

@Composable
fun AeonAddAccountCard(
    baseCurrency: String,
    currentTheme: String,
    onClick: () -> Unit,
    gridColumns: Int = 2,
    precision: Int = 2,
    modifier: Modifier = Modifier
) {
    AeonAddAccountCardContent(baseCurrency, currentTheme, onClick, gridColumns, precision, modifier)
}

@Composable
private fun AeonAddAccountCardContent(
    baseCurrency: String,
    currentTheme: String,
    onClick: () -> Unit,
    gridColumns: Int = 2,
    precision: Int = 2,
    modifier: Modifier = Modifier
) {
    val isDarkTheme = when (currentTheme) {
        "Light" -> false
        "Dark", "Amoled" -> true
        else -> isSystemInDarkTheme()
    }
    val isAmoled = currentTheme == "Amoled"
    val placeholderAccount = Account(
        "empty",
        "GET STARTED",
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        baseCurrency,
        "Default",
        "#808080",
        "Add"
    )

    when (gridColumns) {
        1 -> AeonAccountCardLarge(
            account = placeholderAccount,
            balanceInBase = BigDecimal.ZERO,
            baseCurrency = baseCurrency,
            isDarkTheme = isDarkTheme,
            isAmoled = isAmoled,
            onClick = onClick,
            modifier = modifier,
            precision = precision,
            extraLabel = "Add New"
        )
        2 -> AeonAccountCardMedium(
            account = placeholderAccount,
            balanceInBase = BigDecimal.ZERO,
            baseCurrency = baseCurrency,
            isDarkTheme = isDarkTheme,
            isAmoled = isAmoled,
            onClick = onClick,
            modifier = modifier,
            precision = precision
        )
        else -> AeonAccountCardSmall(
            account = placeholderAccount,
            balanceInBase = BigDecimal.ZERO,
            baseCurrency = baseCurrency,
            isDarkTheme = isDarkTheme,
            isAmoled = isAmoled,
            onClick = onClick,
            modifier = modifier,
            precision = precision
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AeonAccountsSectionPreview() {
    val sampleAccount = Account(
        id = "1",
        name = "Main Bank",
        balance = BigDecimal("1000.00"),
        initialBalance = BigDecimal("1000.00"),
        currencyCode = "USD",
        structureType = "Default",
        colorHex = "#2196F3",
        icon = "bank",
        allowedCurrencies = listOf("USD"),
        isArchived = false,
        displayOrder = 0
    )

    val sampleAccountStats = mapOf(
        "1" to AccountStats(
            currencyBalances = mapOf("USD" to BigDecimal("1000.00")),
            incomeInAccountCurrency = BigDecimal("200.00"),
            expenseInAccountCurrency = BigDecimal("100.00")
        )
    )

    OssWalletTheme {
        Surface {
            AeonAccountsSection(
                onManageClick = {},
                onAccountClick = {},
                onGridColumnsChange = {},
                accounts = listOf(sampleAccount),
                accountStats = sampleAccountStats,
                baseCurrency = "USD",
                exchangeRates = emptyMap(),
                currentTheme = "Light",
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
