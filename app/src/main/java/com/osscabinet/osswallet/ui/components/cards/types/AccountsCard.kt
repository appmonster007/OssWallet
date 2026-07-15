package com.osscabinet.ossWallet.ui.components.cards.types

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
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.CardRenderData
import com.osscabinet.ossWallet.domain.card.CardUpdateHandler
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.AccountStructureType
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.analytics.AccountFlowMetrics
import com.osscabinet.ossWallet.domain.model.analytics.AccountSummaryItem
import com.osscabinet.ossWallet.domain.model.settings.AccountsCardConfig
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.ui.components.AeonGridColumnsSettings
import com.osscabinet.ossWallet.ui.components.AeonLayoutToggle
import com.osscabinet.ossWallet.ui.components.AeonLazyRow
import com.osscabinet.ossWallet.ui.components.AeonSettingsToggle
import com.osscabinet.ossWallet.ui.components.accounts.AeonAccountCardLarge
import com.osscabinet.ossWallet.ui.components.accounts.AeonAccountCardMedium
import com.osscabinet.ossWallet.ui.components.accounts.AeonAccountCardSmall
import com.osscabinet.ossWallet.ui.components.cards.AeonExpressiveCard
import com.osscabinet.ossWallet.ui.components.cards.AeonInteractionCallbacks
import com.osscabinet.ossWallet.ui.components.cards.CardCallbacks
import com.osscabinet.ossWallet.ui.components.cards.CardPerspectiveSettings
import com.osscabinet.ossWallet.ui.components.cards.CardSettingsSection
import com.osscabinet.ossWallet.ui.components.cards.AeonCardSettingsScaffold
import com.osscabinet.ossWallet.ui.components.cards.CardLayoutProps
import com.osscabinet.ossWallet.ui.components.cards.CardWithCallbacks
import com.osscabinet.ossWallet.ui.components.cards.resolveDrillDown
import com.osscabinet.ossWallet.ui.components.cards.resolveSeeAll
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import java.math.BigDecimal

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AccountsCard(
    accounts: List<Account>,
    accountSummaries: Map<String, AccountSummaryItem>,
    baseCurrency: String,
    config: AccountsCardConfig,
    previousSummaries: Map<String, AccountSummaryItem>? = null,
    callbacks: CardCallbacks.Accounts = CardCallbacks.Accounts(),
    layoutProps: CardLayoutProps = CardLayoutProps(),
) {
    val displayFilter = layoutProps.computeDisplayFilter(config.filter)

    AeonExpressiveCard(
        title = config.title,
        icon = Icons.Rounded.AccountBalanceWallet,
        modifier = layoutProps.modifier,
        headerModifier = layoutProps.headerModifier.fillMaxWidth(),
        onClick = callbacks.onSeeAll,
        filterEnabled = layoutProps.showFilterButton,
        filter = displayFilter,
        baseFilter = layoutProps.baseFilter,
        filterRules = layoutProps.filterRules ?: config.filterRules,
        filterContext = layoutProps.filterContext,
        onFilterChange = { callbacks.onFilterChange(config.cardId, it) },
        onSaveFilterClick = callbacks.onSaveFilterClick,
        onDelete = callbacks.onDelete,
        settingsContent = {
            AccountsSettingsContent(config = config, callbacks = callbacks)
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
                        accountSummaries = accountSummaries,
                        previousSummaries = if (config.showComparison) previousSummaries else null,
                        baseCurrency = baseCurrency,
                        config = config,
                        onAccountClick = callbacks.onAccountClick,
                        onRecordsClick = callbacks.onRecordsClick,
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
            val rowItemWidth = when (config.gridColumns) {
                1 -> 280.dp
                2 -> 180.dp
                else -> 120.dp
            }
            AeonLazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 4.dp)
            ) {
                items(accounts) { account ->
                    AccountItemWrapper(
                        account = account,
                        accountSummaries = accountSummaries,
                        previousSummaries = if (config.showComparison) previousSummaries else null,
                        baseCurrency = baseCurrency,
                        config = config,
                        onAccountClick = callbacks.onAccountClick,
                        onRecordsClick = callbacks.onRecordsClick,
                        modifier = Modifier.width(rowItemWidth)
                    )
                }
                if (config.showAddAccountCard) {
                    item {
                        AeonAddAccountCard(
                            baseCurrency = baseCurrency,
                            onClick = callbacks.onManageClick,
                            gridColumns = config.gridColumns,
                            modifier = Modifier.width(rowItemWidth)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountsSettingsContent(
    config: AccountsCardConfig,
    callbacks: CardCallbacks.Accounts,
) {
    CardSettingsSection("Layout") {
        AeonLayoutToggle(
            layout = config.layout,
            onLayoutChange = { callbacks.onLayoutChange(config.cardId, it) },
            showBackground = false
        )
        AeonGridColumnsSettings(
            gridColumns = config.gridColumns,
            onGridColumnsChange = { callbacks.onGridColumnsChange(config.cardId, it) },
            showBackground = false
        )
        AeonSettingsToggle(
            label = "Show Comparison",
            description = "Show change from previous period",
            checked = config.showComparison,
            onCheckedChange = { callbacks.onShowComparisonChange(config.cardId, it) },
            showBackground = false
        )
        AeonSettingsToggle(
            label = "Show add account card",
            checked = config.showAddAccountCard,
            onCheckedChange = { callbacks.onShowAddAccountCardChange(config.cardId, it) },
            showBackground = false
        )
    }
    CardPerspectiveSettings(
        perspective = config.perspective,
        onPerspectiveChange = { newPerspective -> callbacks.onPerspectiveChange(config.cardId, newPerspective) },
        excludeInternalTransfers = config.excludeInternalTransfers,
        onExcludeInternalTransfersChange = {
            callbacks.onExcludeInternalTransfersChange(
                config.cardId,
                it
            )
        }
    )
    OutlinedButton(
        onClick = { callbacks.onManageClick() },
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRowScope.AccountItemWrapper(
    account: Account,
    accountSummaries: Map<String, AccountSummaryItem>,
    previousSummaries: Map<String, AccountSummaryItem>? = null,
    baseCurrency: String,
    config: AccountsCardConfig,
    onAccountClick: (String) -> Unit,
    onRecordsClick: ((TransactionFilter) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    AccountItemContent(
        account = account,
        accountSummaries = accountSummaries,
        previousSummaries = previousSummaries,
        baseCurrency = baseCurrency,
        gridColumns = config.gridColumns,
        perspective = config.perspective,
        onAccountClick = onAccountClick,
        onRecordsClick = onRecordsClick,
        filter = config.filter,
        modifier = modifier
    )
}

@Composable
private fun AccountItemWrapper(
    modifier: Modifier = Modifier,
    account: Account,
    accountSummaries: Map<String, AccountSummaryItem>,
    previousSummaries: Map<String, AccountSummaryItem>? = null,
    baseCurrency: String,
    config: AccountsCardConfig,
    onAccountClick: (String) -> Unit,
    onRecordsClick: ((TransactionFilter) -> Unit)? = null,
) {
    AccountItemContent(
        account = account,
        accountSummaries = accountSummaries,
        previousSummaries = previousSummaries,
        baseCurrency = baseCurrency,
        gridColumns = config.gridColumns,
        perspective = config.perspective,
        onAccountClick = onAccountClick,
        onRecordsClick = onRecordsClick,
        filter = config.filter,
        modifier = modifier
    )
}

@Composable
private fun AccountItemContent(
    account: Account,
    accountSummaries: Map<String, AccountSummaryItem>,
    previousSummaries: Map<String, AccountSummaryItem>? = null,
    baseCurrency: String,
    gridColumns: Int,
    perspective: com.osscabinet.ossWallet.domain.model.settings.AccountingPerspective = com.osscabinet.ossWallet.domain.model.settings.AccountingPerspective.Accounting,
    onAccountClick: (String) -> Unit,
    onRecordsClick: ((TransactionFilter) -> Unit)? = null,
    filter: TransactionFilter = TransactionFilter(),
    modifier: Modifier = Modifier
) {
    val stats = accountSummaries[account.id] ?: AccountSummaryItem(account)
    val prevStats = previousSummaries?.get(account.id)

    when (gridColumns) {
        1 -> AeonAccountCardLarge(
            summary = stats,
            previousSummary = prevStats,
            baseCurrency = baseCurrency,
            perspective = perspective,
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
            },
            onClickInflow = {
                onRecordsClick?.invoke(
                    filter.copy(
                        accountIds = setOf(account.id),
                        transactionTypes = setOf(TransactionType.INCOME) + TransactionType.TRANSFER_TYPES
                    )
                )
            },
            onClickOutflow = {
                onRecordsClick?.invoke(
                    filter.copy(
                        accountIds = setOf(account.id),
                        transactionTypes = setOf(TransactionType.EXPENSE) + TransactionType.TRANSFER_TYPES
                    )
                )
            },
            onClickTransfer = {
                onRecordsClick?.invoke(
                    filter.copy(
                        accountIds = setOf(account.id),
                        transactionTypes = TransactionType.TRANSFER_TYPES
                    )
                )
            },
            onClickNet = {
                onRecordsClick?.invoke(
                    filter.copy(
                        accountIds = setOf(account.id)
                    )
                )
            },
            onClickNetFlow = {
                onRecordsClick?.invoke(
                    filter.copy(
                        accountIds = setOf(account.id)
                    )
                )
            }
        )

        2 -> AeonAccountCardMedium(
            summary = stats,
            baseCurrency = baseCurrency,
            onClick = { onAccountClick(account.id) },
            modifier = modifier
        )

        else -> AeonAccountCardSmall(
            summary = stats,
            baseCurrency = baseCurrency,
            onClick = { onAccountClick(account.id) },
            modifier = modifier
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
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
    val summary = AccountSummaryItem(placeholderAccount)

    when (gridColumns) {
        1 -> AeonAccountCardLarge(
            summary = summary,
            baseCurrency = baseCurrency,
            onClick = onClick,
            modifier = modifier,
            extraLabel = "Add New"
        )

        2 -> AeonAccountCardMedium(
            summary = summary,
            baseCurrency = baseCurrency,
            onClick = onClick,
            modifier = modifier
        )

        else -> AeonAccountCardSmall(
            summary = summary,
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
            currentPeriodFlow = AccountFlowMetrics(
                income = BigDecimal("1000.00"),
                expense = BigDecimal("200.00"),
                transferIn = BigDecimal.ZERO
            )
        ),
        "2" to AccountSummaryItem(
            accounts[1],
            mapOf("EUR" to BigDecimal("1200.00")),
            BigDecimal("1200.00"),
            BigDecimal("1320.00"),
            currentPeriodFlow = AccountFlowMetrics(
                income = BigDecimal("500.00"),
                expense = BigDecimal("100.00"),
                transferIn = BigDecimal.ZERO
            )
        )
    )

    OssWalletTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            AccountsCard(
                accounts = accounts,
                accountSummaries = stats,
                baseCurrency = "USD",
                config = AccountsCardConfig(cardId = "preview"),
                callbacks = CardCallbacks.Accounts(onManageClick = {}, onAccountClick = {})
            )
        }
    }
}

/**
 * Maps [AccountsCardConfig] + [CardRenderData.Accounts] to [CardWithCallbacks.Accounts].
 */
fun mapAccounts(
    config: AccountsCardConfig,
    renderData: CardRenderData.Accounts,
    input: CardInput,
    updateHandler: CardUpdateHandler,
    onDelete: ((String) -> Unit)?,
    onRecordsClick: (TransactionFilter) -> Unit,
    callbacks: AeonInteractionCallbacks,
    filterSheetEnabled: Boolean
) = CardWithCallbacks.Accounts(
    renderData = renderData,
    callbacks = CardCallbacks.Accounts(
        onGridColumnsChange = updateHandler::updateCardAccountsGridColumns,
        onLayoutChange = updateHandler::updateCardAccountsLayout,
        onShowComparisonChange = updateHandler::updateCardShowComparison,
        onFilterChange = updateHandler::updateCardAccountsFilter,
        onShowAddAccountCardChange = updateHandler::updateCardAccountsShowAddAccountCard,
        onPerspectiveChange = updateHandler::updateCardPerspective,
        onExcludeInternalTransfersChange = updateHandler::updateCardExcludeInternalTransfers,
        onManageClick = callbacks.accountActions.onManageClick,
        onAccountClick = callbacks.accountActions.onClick,
        onRecordsClick = { drillDown ->
            onRecordsClick(
                resolveDrillDown(
                    input,
                    config,
                    drillDown
                )
            )
        },
        onClickInflow = { drillDown -> onRecordsClick(resolveDrillDown(input, config, drillDown)) },
        onClickOutflow = { drillDown ->
            onRecordsClick(
                resolveDrillDown(
                    input,
                    config,
                    drillDown
                )
            )
        },
        onClickNet = { drillDown -> onRecordsClick(resolveDrillDown(input, config, drillDown)) },
        onClickNetFlow = { drillDown ->
            onRecordsClick(
                resolveDrillDown(
                    input,
                    config,
                    drillDown
                )
            )
        },
        onClickTransfer = { drillDown ->
            onRecordsClick(
                resolveDrillDown(
                    input,
                    config,
                    drillDown
                )
            )
        },
        onSeeAll = { onRecordsClick(resolveSeeAll(input, config)) },
        onDelete = onDelete?.let { { it(config.cardId) } },
        onSaveFilterClick = callbacks.filterActions.onSaveClick
    ),
    filterRules = config.filterRules,
    baseFilter = input.filter,
    filterSheetEnabled = filterSheetEnabled
)
