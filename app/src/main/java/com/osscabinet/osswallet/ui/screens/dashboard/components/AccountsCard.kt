package com.osscabinet.ossWallet.ui.screens.dashboard.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.card.CardFilterRules
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Filter
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.analytics.AccountSummaryItem
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.ui.components.AeonGridColumnsSettings
import com.osscabinet.ossWallet.ui.components.AeonLayoutToggle
import com.osscabinet.ossWallet.ui.components.AeonSettingsToggle
import com.osscabinet.ossWallet.ui.components.accounts.AeonAccountCardLarge
import com.osscabinet.ossWallet.ui.components.accounts.AeonAccountCardMedium
import com.osscabinet.ossWallet.ui.components.accounts.AeonAccountCardSmall
import com.osscabinet.ossWallet.ui.components.cards.AeonCardFilterButton
import com.osscabinet.ossWallet.ui.components.cards.AeonCardSettingsSheet
import com.osscabinet.ossWallet.ui.components.cards.AeonExpressiveCard
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.theme.AppTextStyles

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AccountsCard(
    onManageClick: () -> Unit,
    onAccountClick: (String) -> Unit,
    onRecordsClick: ((TransactionFilter) -> Unit)? = null,
    onGridColumnsChange: (String, Int) -> Unit,
    accounts: List<Account>,
    accountStats: Map<String, AccountSummaryItem>,
    baseCurrency: String,
    currentTheme: String,
    cardId: String,
    modifier: Modifier = Modifier,
    @Suppress("UNUSED_PARAMETER") headerModifier: Modifier = Modifier,
    gridColumns: Int = 2,
    layout: String = "grid",
    precision: Int = 2,
    filter: TransactionFilter = TransactionFilter(),
    onLayoutChange: (String, String) -> Unit = { _, _ -> },
    onFilterChange: (String, TransactionFilter) -> Unit = { _, _ -> },
    settingsTitle: String = "Accounts View",
    showAddAccountCard: Boolean = true,
    onShowAddAccountCardChange: (String, Boolean) -> Unit = { _, _ -> },
    onDelete: () -> Unit = {},
    filterRules: CardFilterRules = CardFilterRules.DEFAULT,
    baseFilter: TransactionFilter = TransactionFilter(),
    allCategories: List<Category> = emptyList(),
    savedFilters: List<Filter> = emptyList()
) {
    var showDisplaySettings by remember { mutableStateOf(false) }
    var showFilterSettings by remember { mutableStateOf(false) }

    val timelineLabel = when (val tr = filter.timelineRange) {
        is TimelineRange.AllTime -> "All Time"
        is TimelineRange.ThisPeriod -> tr.period.label
        is TimelineRange.LastPeriod -> tr.period.label
        is TimelineRange.CustomRange -> {
            val formatter = DateTimeFormatter.ofPattern("MMM d").withZone(ZoneId.systemDefault())
            val start = tr.start?.let { formatter.format(Instant.ofEpochMilli(it)) } ?: "..."
            val end = tr.end?.let { formatter.format(Instant.ofEpochMilli(it)) } ?: "..."
            "$start - $end"
        }
    }

    AeonExpressiveCard(
        title = "Accounts",
        subtitle = timelineLabel,
        icon = Icons.Rounded.AccountBalanceWallet,
        modifier = modifier,
        headerModifier = headerModifier,
        filter = filter,
        baseFilter = baseFilter,
        filterRules = filterRules,
        headerTrailing = { activeCount ->
            AeonCardFilterButton(
                onClick = { showFilterSettings = true },
                activeFilterCount = activeCount
            )
            IconButton(
                onClick = { showDisplaySettings = true },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Rounded.Tune,
                    contentDescription = "Display Settings",
                    tint = AppColors.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    ) {
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
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                maxItemsInEachRow = gridColumns
            ) {
                accounts.forEach { account ->
                    AccountItemWrapper(
                        account = account,
                        accountStats = accountStats,
                        baseCurrency = baseCurrency,
                        currentTheme = currentTheme,
                        gridColumns = gridColumns,
                        onAccountClick = onAccountClick,
                        onRecordsClick = onRecordsClick,
                        filter = filter,
                        precision = precision,
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
                        currentTheme = currentTheme,
                        gridColumns = 2,
                        onAccountClick = onAccountClick,
                        onRecordsClick = onRecordsClick,
                        filter = filter,
                        precision = precision,
                        modifier = Modifier.width(180.dp)
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
                            modifier = Modifier.width(180.dp)
                        )
                    }
                }
            }
        }
    }

    if (showDisplaySettings) {
        AccountsDisplaySettingsDialog(
            layout = layout,
            gridColumns = gridColumns,
            showAddAccountCard = showAddAccountCard,
            cardId = cardId,
            onLayoutChange = onLayoutChange,
            onGridColumnsChange = onGridColumnsChange,
            onShowAddAccountCardChange = onShowAddAccountCardChange,
            onManageClick = onManageClick,
            onDismiss = { showDisplaySettings = false },
            onDelete = onDelete,
            settingsTitle = settingsTitle
        )
    }

    if (showFilterSettings) {
        AeonCardSettingsSheet(
            title = "Accounts Filter",
            filter = filter,
            onFilterChange = { newFilter -> onFilterChange(cardId, newFilter) },
            onDismiss = { showFilterSettings = false },
            onDelete = onDelete,
            filterRules = filterRules,
            baseFilter = baseFilter,
            accounts = accounts,
            allCategories = allCategories,
            savedFilters = savedFilters
        )
    }
}

@Composable
private fun AccountsDisplaySettingsDialog(
    layout: String,
    gridColumns: Int,
    showAddAccountCard: Boolean,
    cardId: String,
    onLayoutChange: (String, String) -> Unit,
    onGridColumnsChange: (String, Int) -> Unit,
    onShowAddAccountCardChange: (String, Boolean) -> Unit,
    onManageClick: () -> Unit,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    settingsTitle: String
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        dismissButton = {
            TextButton(
                onClick = {
                    onDelete()
                    onDismiss()
                },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = AppColors.error
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete card",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Delete")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
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
                    "Accounts Display",
                    style = AppTextStyles.cardTitle
                )
                IconButton(
                    onClick = {
                        onManageClick()
                        onDismiss()
                    },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = AppColors.accentContainer.copy(alpha = 0.3f),
                        contentColor = AppColors.primary
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
                AeonLayoutToggle(
                    layout = layout,
                    onLayoutChange = { newLayout -> onLayoutChange(cardId, newLayout) }
                )

                if (layout == "grid") {
                    AeonGridColumnsSettings(
                        gridColumns = gridColumns,
                        onGridColumnsChange = { columns -> onGridColumnsChange(cardId, columns) }
                    )
                }

                AeonSettingsToggle(
                    label = "SHOW ADD ACCOUNT CARD",
                    checked = showAddAccountCard,
                    onCheckedChange = { show -> onShowAddAccountCardChange(cardId, show) }
                )
            }
        },
        shape = RoundedCornerShape(32.dp),
        containerColor = AppColors.sheetSurface
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRowScope.AccountItemWrapper(
    account: Account,
    accountStats: Map<String, AccountSummaryItem>,
    baseCurrency: String,
    currentTheme: String,
    gridColumns: Int,
    precision: Int,
    onAccountClick: (String) -> Unit,
    onRecordsClick: ((TransactionFilter) -> Unit)? = null,
    filter: TransactionFilter = TransactionFilter(),
    modifier: Modifier = Modifier
) {
    AccountItemContent(
        account = account,
        accountStats = accountStats,
        baseCurrency = baseCurrency,
        currentTheme = currentTheme,
        gridColumns = gridColumns,
        precision = precision,
        onAccountClick = onAccountClick,
        onRecordsClick = onRecordsClick,
        filter = filter,
        modifier = modifier
    )
}

@Composable
private fun AccountItemWrapper(
    account: Account,
    accountStats: Map<String, AccountSummaryItem>,
    baseCurrency: String,
    currentTheme: String,
    gridColumns: Int,
    precision: Int,
    onAccountClick: (String) -> Unit,
    onRecordsClick: ((TransactionFilter) -> Unit)? = null,
    filter: TransactionFilter = TransactionFilter(),
    modifier: Modifier = Modifier
) {
    AccountItemContent(
        account = account,
        accountStats = accountStats,
        baseCurrency = baseCurrency,
        currentTheme = currentTheme,
        gridColumns = gridColumns,
        precision = precision,
        onAccountClick = onAccountClick,
        onRecordsClick = onRecordsClick,
        filter = filter,
        modifier = modifier
    )
}

@Composable
private fun AccountItemContent(
    account: Account,
    accountStats: Map<String, AccountSummaryItem>,
    baseCurrency: String,
    currentTheme: String,
    gridColumns: Int,
    precision: Int,
    onAccountClick: (String) -> Unit,
    onRecordsClick: ((TransactionFilter) -> Unit)? = null,
    filter: TransactionFilter = TransactionFilter(),
    modifier: Modifier = Modifier
) {
    val stats = accountStats[account.id]
    val balance = stats?.balance ?: BigDecimal.ZERO
    val totalInBase = stats?.balanceInBase ?: BigDecimal.ZERO

    val handleAccountClick = {
        onAccountClick(account.id)
    }

    val isDarkTheme = when (currentTheme) {
        "Light" -> false
        "Dark", "Amoled" -> true
        else -> isSystemInDarkTheme()
    }
    val isAmoled = currentTheme == "Amoled"

    when (gridColumns) {
        1 -> AeonAccountCardLarge(
            account = account,
            balance = balance,
            balanceInBase = totalInBase,
            baseCurrency = baseCurrency,
            currencyBalances = stats?.currencyBalances ?: emptyMap(),
            incomeInAccountCurrency = stats?.incomeInAccountCurrency ?: BigDecimal.ZERO,
            expenseInAccountCurrency = stats?.expenseInAccountCurrency ?: BigDecimal.ZERO,
            isDarkTheme = isDarkTheme,
            isAmoled = isAmoled,
            onClick = handleAccountClick,
            modifier = modifier,
            precision = precision,
            onClickIncome = {
                onRecordsClick?.invoke(
                    filter.copy(
                        accountIds = setOf(account.id),
                        transactionTypes = setOf(TransactionType.INCOME)
                    )
                )
            },
            onClickExpense = {
                onRecordsClick?.invoke(
                    filter.copy(
                        accountIds = setOf(account.id),
                        transactionTypes = setOf(TransactionType.EXPENSE)
                    )
                )
            }
        )
        2 -> AeonAccountCardMedium(
            account = account,
            balance = balance,
            balanceInBase = totalInBase,
            baseCurrency = baseCurrency,
            currencyBalances = stats?.currencyBalances ?: emptyMap(),
            isDarkTheme = isDarkTheme,
            isAmoled = isAmoled,
            onClick = handleAccountClick,
            modifier = modifier,
            precision = precision
        )
        else -> AeonAccountCardSmall(
            account = account,
            balance = balance,
            balanceInBase = totalInBase,
            baseCurrency = baseCurrency,
            currencyBalances = stats?.currencyBalances ?: emptyMap(),
            isDarkTheme = isDarkTheme,
            isAmoled = isAmoled,
            onClick = handleAccountClick,
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
    val seedColor = AppColors.primary
    val placeholderAccount = Account(
        "empty",
        "GET STARTED",
        baseCurrency,
        "Default",
        String.format("#%06X", 0xFFFFFF and seedColor.toArgb()),
        "Add",
        listOf("Add")
    )

    when (gridColumns) {
        1 -> AeonAccountCardLarge(
            account = placeholderAccount,
            balance = BigDecimal.ZERO,
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
            balance = BigDecimal.ZERO,
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
            balance = BigDecimal.ZERO,
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

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun AccountsCardPreview() {
    val accounts = listOf(
        Account("1", "Main Savings", "USD", "Savings", "#004D40", "AccountBalance", emptyList()),
        Account("2", "Travel Fund", "EUR", "Savings", "#B3261E", "Travel", emptyList())
    )
    val stats = mapOf(
        "1" to AccountSummaryItem(accounts[0], mapOf("USD" to BigDecimal("5000.00")), BigDecimal("5000.00"), BigDecimal("5000.00"), BigDecimal("1000.00"), BigDecimal("200.00"), BigDecimal.ZERO),
        "2" to AccountSummaryItem(accounts[1], mapOf("EUR" to BigDecimal("1200.00")), BigDecimal("1200.00"), BigDecimal("1320.00"), BigDecimal("500.00"), BigDecimal("100.00"), BigDecimal.ZERO)
    )

    OssWalletTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            AccountsCard(
                onManageClick = {},
                onAccountClick = {},
                onGridColumnsChange = { _, _ -> },
                accounts = accounts,
                accountStats = stats,
                baseCurrency = "USD",
                currentTheme = "System",
                cardId = "preview",
                gridColumns = 2,
                layout = "grid"
            )
        }
    }
}

