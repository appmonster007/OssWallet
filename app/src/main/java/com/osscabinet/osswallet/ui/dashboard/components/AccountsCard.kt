package com.osscabinet.ossWallet.ui.dashboard.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Filter
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.model.analytics.AccountSummaryItem
import com.osscabinet.ossWallet.ui.components.*
import com.osscabinet.ossWallet.ui.components.accounts.*
import com.osscabinet.ossWallet.ui.components.cards.AeonCardSettingsSheet
import com.osscabinet.ossWallet.ui.navigation.RecordsFilterContext
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

import com.osscabinet.ossWallet.domain.card.CardFilterRules

import com.osscabinet.ossWallet.ui.components.cards.AeonExpressiveCard

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AccountsCard(
    onManageClick: () -> Unit,
    onAccountClick: (String) -> Unit,
    onRecordsClick: ((RecordsFilterContext) -> Unit)? = null,
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
    onHeaderClick: (() -> Unit)? = null,
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
            val start = formatter.format(Instant.ofEpochMilli(tr.start))
            val end = formatter.format(Instant.ofEpochMilli(tr.end))
            "$start - $end"
        }
    }

    AeonExpressiveCard(
        title = "ACCOUNTS",
        modifier = if (onHeaderClick != null) modifier.clickable(onClick = onHeaderClick) else modifier,
        filter = filter,
        headerTrailing = {
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
                onClick = { showFilterSettings = true },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Default.FilterList,
                    contentDescription = "Filter",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(
                onClick = { showDisplaySettings = true },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Default.Tune,
                    contentDescription = "Display Settings",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
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
                        timelineRange = filter.timelineRange,
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
                        timelineRange = filter.timelineRange,
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
                    contentColor = MaterialTheme.colorScheme.error
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
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                IconButton(
                    onClick = {
                        onManageClick()
                        onDismiss()
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
        containerColor = MaterialTheme.colorScheme.surfaceContainer
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
    onRecordsClick: ((RecordsFilterContext) -> Unit)? = null,
    timelineRange: TimelineRange = TimelineRange.AllTime,
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
        timelineRange = timelineRange,
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
    onRecordsClick: ((RecordsFilterContext) -> Unit)? = null,
    timelineRange: TimelineRange = TimelineRange.AllTime,
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
        timelineRange = timelineRange,
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
    onRecordsClick: ((RecordsFilterContext) -> Unit)? = null,
    timelineRange: TimelineRange = TimelineRange.AllTime,
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
            precision = precision
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
    val placeholderAccount = Account(
        "empty",
        "GET STARTED",
        baseCurrency,
        "Default",
        "#808080",
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
