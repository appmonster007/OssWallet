package com.osscabinet.ossWallet.ui.screens.dashboard.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.card.CardFilterRules
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.AccountStructureType
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.analytics.AccountSummaryItem
import com.osscabinet.ossWallet.domain.model.settings.AccountsCardConfig
import com.osscabinet.ossWallet.ui.components.AeonGridColumnsSettings
import com.osscabinet.ossWallet.ui.components.AeonLayoutToggle
import com.osscabinet.ossWallet.ui.components.AeonSettingsToggle
import com.osscabinet.ossWallet.ui.components.accounts.AeonAccountCardLarge
import com.osscabinet.ossWallet.ui.components.accounts.AeonAccountCardMedium
import com.osscabinet.ossWallet.ui.components.accounts.AeonAccountCardSmall
import com.osscabinet.ossWallet.ui.components.cards.AeonExpressiveCard
import com.osscabinet.ossWallet.ui.components.cards.CardCallbacks
import com.osscabinet.ossWallet.ui.components.cards.CardSettingsSection
import com.osscabinet.ossWallet.ui.components.cards.CardSettingsSheet
import com.osscabinet.ossWallet.ui.components.cards.FilterSelectionContext
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import java.math.BigDecimal

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AccountsCard(
    accounts: List<Account>,
    accountStats: Map<String, AccountSummaryItem>,
    baseCurrency: String,
    config: AccountsCardConfig,
    callbacks: CardCallbacks.Accounts = CardCallbacks.Accounts(),
    filterContext: FilterSelectionContext = FilterSelectionContext(),
    baseFilter: TransactionFilter = TransactionFilter(),
    inheritedFilter: TransactionFilter = TransactionFilter(),
    filterRules: CardFilterRules? = null,
    screenOwnsTimeline: Boolean = false,
    showFilterButton: Boolean = true,
    modifier: Modifier = Modifier,
    headerModifier: Modifier = Modifier,
) {
    val displayFilter =
        if (screenOwnsTimeline || inheritedFilter.timelineRange !is com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.AllTime)
            config.filter.copy(timelineRange = inheritedFilter.timelineRange)
        else config.filter

    AeonExpressiveCard(
        title = config.title,
        icon = Icons.Rounded.AccountBalanceWallet,
        modifier = modifier,
        headerModifier = headerModifier,
        onClick = callbacks.onSeeAll,
        filterEnabled = showFilterButton,
        filter = displayFilter,
        baseFilter = baseFilter,
        filterRules = filterRules ?: config.filterRules,
        filterContext = filterContext,
        onFilterChange = { callbacks.onFilterChange(config.cardId, it) },
        onSaveFilterClick = callbacks.onSaveFilterClick,
        displaySettings = { onDismiss ->
            AccountsSettingsSheet(config = config, callbacks = callbacks, onDismiss = onDismiss)
        }
    ) {
        if (accounts.isEmpty()) {
            AeonAddAccountCard(
                baseCurrency = baseCurrency,
                onClick = callbacks.onManageClick,
                gridColumns = if (config.layout == "grid") config.gridColumns else 2,
                modifier = Modifier.fillMaxWidth()
            )
        } else if (config.layout == "grid") {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                maxItemsInEachRow = config.gridColumns
            ) {
                accounts.forEach { account ->
                    AccountItemWrapper(
                        account = account,
                        accountStats = accountStats,
                        baseCurrency = baseCurrency,
                        gridColumns = config.gridColumns,
                        onAccountClick = callbacks.onAccountClick,
                        onRecordsClick = callbacks.onRecordsClick,
                        filter = config.filter,
                        modifier = if (config.gridColumns == 1) Modifier.fillMaxWidth() else Modifier.weight(
                            1f
                        )
                    )
                }

                if (config.showAddAccountCard) {
                    AeonAddAccountCard(
                        baseCurrency = baseCurrency,
                        onClick = callbacks.onManageClick,
                        gridColumns = config.gridColumns,
                        modifier = if (config.gridColumns == 1) Modifier.fillMaxWidth() else Modifier.weight(
                            1f
                        )
                    )
                } else if (config.gridColumns > 1 && accounts.size % config.gridColumns != 0) {
                    repeat(config.gridColumns - (accounts.size % config.gridColumns)) {
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
                        gridColumns = 2,
                        onAccountClick = callbacks.onAccountClick,
                        onRecordsClick = callbacks.onRecordsClick,
                        filter = config.filter,
                        modifier = Modifier.width(180.dp)
                    )
                }
                if (config.showAddAccountCard) {
                    item {
                        AeonAddAccountCard(
                            baseCurrency = baseCurrency,
                            onClick = callbacks.onManageClick,
                            gridColumns = 2,
                            modifier = Modifier.width(180.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountsSettingsSheet(
    config: AccountsCardConfig,
    callbacks: CardCallbacks.Accounts,
    onDismiss: () -> Unit
) {
    CardSettingsSheet(title = "Accounts", onDismiss = onDismiss, onDelete = callbacks.onDelete) {
        CardSettingsSection("Display") {
            AeonLayoutToggle(
                layout = config.layout,
                onLayoutChange = { callbacks.onLayoutChange(config.cardId, it) }
            )
            if (config.layout == "grid") {
                AeonGridColumnsSettings(
                    gridColumns = config.gridColumns,
                    onGridColumnsChange = { callbacks.onGridColumnsChange(config.cardId, it) }
                )
            }
            AeonSettingsToggle(
                label = "SHOW ADD ACCOUNT CARD",
                checked = config.showAddAccountCard,
                onCheckedChange = { callbacks.onShowAddAccountCardChange(config.cardId, it) }
            )
        }
        OutlinedButton(
            onClick = { callbacks.onManageClick(); onDismiss() },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                Icons.Default.Settings,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Manage Accounts")
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRowScope.AccountItemWrapper(
    account: Account,
    accountStats: Map<String, AccountSummaryItem>,
    baseCurrency: String,
    gridColumns: Int,
    onAccountClick: (String) -> Unit,
    onRecordsClick: ((TransactionFilter) -> Unit)? = null,
    filter: TransactionFilter = TransactionFilter(),
    modifier: Modifier = Modifier
) {
    AccountItemContent(
        account = account,
        accountStats = accountStats,
        baseCurrency = baseCurrency,
        gridColumns = gridColumns,
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
    gridColumns: Int,
    onAccountClick: (String) -> Unit,
    onRecordsClick: ((TransactionFilter) -> Unit)? = null,
    filter: TransactionFilter = TransactionFilter(),
    modifier: Modifier = Modifier
) {
    AccountItemContent(
        account = account,
        accountStats = accountStats,
        baseCurrency = baseCurrency,
        gridColumns = gridColumns,
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
    gridColumns: Int,
    onAccountClick: (String) -> Unit,
    onRecordsClick: ((TransactionFilter) -> Unit)? = null,
    filter: TransactionFilter = TransactionFilter(),
    modifier: Modifier = Modifier
) {
    val stats = accountStats[account.id]
    val balance = stats?.balance ?: BigDecimal.ZERO
    val totalInBase = stats?.balanceInBase ?: BigDecimal.ZERO

    when (gridColumns) {
        1 -> AeonAccountCardLarge(
            account = account,
            balance = balance,
            balanceInBase = totalInBase,
            baseCurrency = baseCurrency,
            currencyBalances = stats?.currencyBalances ?: emptyMap(),
            incomeInAccountCurrency = stats?.incomeInAccountCurrency ?: BigDecimal.ZERO,
            expenseInAccountCurrency = stats?.expenseInAccountCurrency ?: BigDecimal.ZERO,
            onClick = { onAccountClick(account.id) },
            modifier = modifier,
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
            onClick = { onAccountClick(account.id) },
            modifier = modifier
        )

        else -> AeonAccountCardSmall(
            account = account,
            balance = balance,
            balanceInBase = totalInBase,
            baseCurrency = baseCurrency,
            currencyBalances = stats?.currencyBalances ?: emptyMap(),
            onClick = { onAccountClick(account.id) },
            modifier = modifier
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowRowScope.AeonAddAccountCard(
    baseCurrency: String,
    onClick: () -> Unit,
    gridColumns: Int = 2,
    modifier: Modifier = Modifier
) {
    AeonAddAccountCardContent(baseCurrency, onClick, gridColumns, modifier)
}

@Composable
fun AeonAddAccountCard(
    baseCurrency: String,
    onClick: () -> Unit,
    gridColumns: Int = 2,
    modifier: Modifier = Modifier
) {
    AeonAddAccountCardContent(baseCurrency, onClick, gridColumns, modifier)
}

@Composable
private fun AeonAddAccountCardContent(
    baseCurrency: String,
    onClick: () -> Unit,
    gridColumns: Int = 2,
    modifier: Modifier = Modifier
) {
    val seedColor = AppColors.primary
    val placeholderAccount = Account(
        "empty",
        "GET STARTED",
        baseCurrency,
        AccountStructureType.SAVINGS,
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
            onClick = onClick,
            modifier = modifier,
            extraLabel = "Add New"
        )

        2 -> AeonAccountCardMedium(
            account = placeholderAccount,
            balance = BigDecimal.ZERO,
            balanceInBase = BigDecimal.ZERO,
            baseCurrency = baseCurrency,
            onClick = onClick,
            modifier = modifier
        )

        else -> AeonAccountCardSmall(
            account = placeholderAccount,
            balance = BigDecimal.ZERO,
            balanceInBase = BigDecimal.ZERO,
            baseCurrency = baseCurrency,
            onClick = onClick,
            modifier = modifier
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun AccountsCardPreview() {
    val accounts = listOf(
        Account(
            "1",
            "Main Savings",
            "USD",
            AccountStructureType.SAVINGS,
            "#004D40",
            "AccountBalance",
            emptyList()
        ),
        Account(
            "2",
            "Travel Fund",
            "EUR",
            AccountStructureType.SAVINGS,
            "#B3261E",
            "Travel",
            emptyList()
        )
    )
    val stats = mapOf(
        "1" to AccountSummaryItem(
            accounts[0],
            mapOf("USD" to BigDecimal("5000.00")),
            BigDecimal("5000.00"),
            BigDecimal("5000.00"),
            BigDecimal("1000.00"),
            BigDecimal("200.00"),
            BigDecimal.ZERO
        ),
        "2" to AccountSummaryItem(
            accounts[1],
            mapOf("EUR" to BigDecimal("1200.00")),
            BigDecimal("1200.00"),
            BigDecimal("1320.00"),
            BigDecimal("500.00"),
            BigDecimal("100.00"),
            BigDecimal.ZERO
        )
    )

    OssWalletTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            AccountsCard(
                accounts = accounts,
                accountStats = stats,
                baseCurrency = "USD",
                config = AccountsCardConfig(cardId = "preview"),
                callbacks = CardCallbacks.Accounts(onManageClick = {}, onAccountClick = {})
            )
        }
    }
}
