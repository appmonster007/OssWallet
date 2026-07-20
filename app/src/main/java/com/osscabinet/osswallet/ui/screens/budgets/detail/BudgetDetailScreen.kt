package com.osscabinet.ossWallet.ui.screens.budgets.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Budget
import com.osscabinet.ossWallet.domain.model.BudgetAlertLevel
import com.osscabinet.ossWallet.domain.model.BudgetProgress
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Tag
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionListState
import com.osscabinet.ossWallet.domain.model.TransactionWithBalance
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.service.filter.TransactionFilterService
import com.osscabinet.ossWallet.domain.service.management.BudgetService
import com.osscabinet.ossWallet.domain.service.timeline.TimelineService
import com.osscabinet.ossWallet.ui.components.AeonHazeProvider
import com.osscabinet.ossWallet.ui.components.AeonTopAppBar
import com.osscabinet.ossWallet.ui.components.AeonTopBarActionPill
import com.osscabinet.ossWallet.ui.components.AeonTopBarNavTitlePill
import com.osscabinet.ossWallet.ui.components.AeonTopBarSection
import com.osscabinet.ossWallet.ui.components.AppBarIconButton
import com.osscabinet.ossWallet.ui.components.aeonHazeSource
import com.osscabinet.ossWallet.ui.components.management.ManagementScopeCard
import com.osscabinet.ossWallet.ui.components.navigation.NavigationTab
import com.osscabinet.ossWallet.ui.components.navigation.UnifiedPillNavigation
import com.osscabinet.ossWallet.ui.components.scrollBottomPadding
import com.osscabinet.ossWallet.ui.components.transactions.RecentActivitiesScreen
import com.osscabinet.ossWallet.ui.components.transactions.TransactionItemEvent
import com.osscabinet.ossWallet.ui.screens.accounts.AccountViewModel
import com.osscabinet.ossWallet.ui.screens.budgets.BudgetViewModel
import com.osscabinet.ossWallet.ui.screens.budgets.detail.components.BudgetHeroCard
import com.osscabinet.ossWallet.ui.screens.transactions.TransactionViewModel
import com.osscabinet.ossWallet.ui.theme.AeonSize
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.HazeLayer
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import kotlinx.coroutines.launch
import java.math.BigDecimal


/**
 * A Value Object representing a valid configuration for the Budget Detail screen.
 */
data class BudgetDetailConfig(
    val budgetId: Long,
    val baseCurrency: String
)

/**
 * Grouped actions for the Budget Detail screen.
 */
data class BudgetDetailActions(
    val onEditClick: (Long) -> Unit,
    val onBackClick: () -> Unit,
    val onTransactionClick: (Long) -> Unit,
    val onCloneTransactionClick: (Long) -> Unit = {},
    val onSplitTransactionClick: (Long) -> Unit = {},
    val onRecordsClick: (TransactionFilter) -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetDetailScreen(
    config: BudgetDetailConfig,
    actions: BudgetDetailActions,
    viewModel: BudgetViewModel,
    accountViewModel: AccountViewModel,
    transactionViewModel: TransactionViewModel
) {
    val budgets by viewModel.budgets.collectAsStateWithLifecycle()
    val budgetProgress by viewModel.budgetProgress.collectAsStateWithLifecycle()
    val budget = remember(config.budgetId, budgets) { budgets.find { it.id == config.budgetId } }

    LaunchedEffect(budgets) {
        if (budgets.isNotEmpty() && budget == null) {
            actions.onBackClick()
        }
    }

    if (budget == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val progress = budgetProgress[budget.id] ?: BudgetService.buildProgress(
        budget,
        BigDecimal.ZERO,
        BigDecimal.ZERO
    )

    val accounts by accountViewModel.accounts.collectAsStateWithLifecycle()
    val categoriesList by transactionViewModel.categories.collectAsStateWithLifecycle()
    val allTags by transactionViewModel.allTags.collectAsStateWithLifecycle()
    val revealedCardIds by transactionViewModel.revealedCardIdsList.collectAsStateWithLifecycle()

    val absoluteTransactionsWithBalance by transactionViewModel.absoluteTransactionsWithBalance.collectAsStateWithLifecycle()

    val budgetFilter = remember(budget, categoriesList) {
        BudgetService.buildTransactionFilter(
            budget,
            System.currentTimeMillis(),
            categoriesList
        )
    }

    val budgetTransactions =
        remember(budgetFilter, absoluteTransactionsWithBalance) {
            TransactionFilterService.filterTransactionsWithBalance(
                absoluteTransactionsWithBalance,
                budgetFilter
            )
        }

    val recentActivitiesBreakdown by viewModel.recentActivitiesBreakdown.collectAsStateWithLifecycle()

    BudgetDetailContent(
        budget = budget,
        progress = progress,
        budgetFilter = budgetFilter,
        budgetTransactions = budgetTransactions,
        absoluteTransactionsWithBalance = absoluteTransactionsWithBalance,
        accounts = accounts,
        categoriesList = categoriesList,
        allTags = allTags,
        baseCurrency = config.baseCurrency,
        revealedCardIds = revealedCardIds,
        recentActivitiesBreakdown = recentActivitiesBreakdown,
        onEditClick = { actions.onEditClick(budget.id) },
        onBackClick = actions.onBackClick,
        onTransactionEvent = { event, availableIds ->
            when (event) {
                is TransactionItemEvent.Click -> {
                    if (event.id.startsWith("tx_")) {
                        actions.onTransactionClick(event.id.removePrefix("tx_").toLong())
                    }
                }

                is TransactionItemEvent.Expand -> transactionViewModel.onItemExpanded(event.id)
                is TransactionItemEvent.Collapse -> transactionViewModel.onItemCollapsed(event.id)
                is TransactionItemEvent.StatusChange -> transactionViewModel.quickUpdateStatus(
                    event.id,
                    event.status
                )

                is TransactionItemEvent.Clone -> actions.onCloneTransactionClick(event.id)
                is TransactionItemEvent.Delete -> transactionViewModel.quickDeleteTransaction(event.id)
                is TransactionItemEvent.Split -> actions.onSplitTransactionClick(event.id)
                is TransactionItemEvent.Jump -> transactionViewModel.handleJumpRequest(
                    event.id,
                    availableIds
                )

                is TransactionItemEvent.FullSwipeRight -> transactionViewModel.toggleStatusClearedUncleared(
                    event.id
                )

                is TransactionItemEvent.ConfirmPlanned -> transactionViewModel.confirmPlannedPayment(
                    event.paymentId,
                    event.scheduledDate
                )

                is TransactionItemEvent.CancelPlanned -> transactionViewModel.cancelPlannedPayment(
                    event.paymentId,
                    event.scheduledDate
                )
            }
        },
        onRecordsClick = actions.onRecordsClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetDetailContent(
    budget: Budget,
    progress: BudgetProgress,
    budgetFilter: TransactionFilter,
    budgetTransactions: List<TransactionWithBalance>,
    absoluteTransactionsWithBalance: List<TransactionWithBalance>,
    accounts: List<Account>,
    categoriesList: List<Category>,
    allTags: List<Tag>,
    baseCurrency: String,
    revealedCardIds: List<String>,
    recentActivitiesBreakdown: Boolean,
    onEditClick: () -> Unit,
    onBackClick: () -> Unit,
    onTransactionEvent: (TransactionItemEvent, Set<Long>?) -> Unit,
    onRecordsClick: (TransactionFilter) -> Unit
) {
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = 0) { 2 }
    val tabs = listOf(
        NavigationTab(
            icon = Icons.Rounded.Info,
            selectedIcon = Icons.Rounded.Info,
            label = "Overview",
            contentDescription = "Overview tab"
        ),
        NavigationTab(
            icon = Icons.Rounded.Receipt,
            selectedIcon = Icons.Rounded.Receipt,
            label = "Activity",
            contentDescription = "Activity tab"
        )
    )

    val pillHeight = AeonSize.D64.value

    AeonHazeProvider { hazeLayers ->
        Box(modifier = Modifier.fillMaxSize()) {
            // 1. Content Layer
            Scaffold(
                containerColor = Color.Transparent
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .aeonHazeSource(hazeLayers.states[HazeLayer.Overlay])
                        .padding(bottom = paddingValues.calculateBottomPadding())
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.Top
                    ) { page ->
                        when (page) {
                            0 -> {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(
                                        start = AeonTheme.dimensions.screenPadding,
                                        end = AeonTheme.dimensions.screenPadding,
                                        top = AeonTheme.dimensions.screenPadding + pillHeight,
                                        bottom = scrollBottomPadding()
                                    ),
                                    verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingXl)
                                ) {
                                    item {
                                        BudgetHeroCard(
                                            budget = budget,
                                            progress = progress,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }

                                    item {
                                        ManagementScopeCard(
                                            filter = budgetFilter,
                                            allAccounts = accounts,
                                            allCategories = categoriesList
                                        )
                                    }

                                    if (budget.rollover && progress.rolloverAmount > BigDecimal.ZERO) {
                                        item {
                                            Surface(
                                                color = AppColors.primary.copy(alpha = 0.05f),
                                                shape = RoundedCornerShape(AeonTheme.dimensions.containerCornerRadius),
                                                border = BorderStroke(
                                                    1.dp,
                                                    AppColors.primary.copy(alpha = 0.1f)
                                                )
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .padding(AeonTheme.dimensions.cardPadding)
                                                        .fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(
                                                        AeonTheme.dimensions.spacingMd
                                                    )
                                                ) {
                                                    Icon(
                                                        Icons.Rounded.Autorenew,
                                                        contentDescription = null,
                                                        tint = AppColors.primary
                                                    )
                                                    Column {
                                                        Text(
                                                            "Rollover Applied",
                                                            style = AppTextStyles.managementItemTitle.copy(
                                                                fontWeight = FontWeight.Bold
                                                            ),
                                                            color = AppColors.primary
                                                        )
                                                        Text(
                                                            "Remaining budget from previous period was added to this month.",
                                                            style = AppTextStyles.caption,
                                                            color = AppColors.contentSecondary.copy(
                                                                alpha = 0.7f
                                                            )
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            1 -> {
                                if (budgetTransactions.isEmpty()) {
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = pillHeight + AeonTheme.dimensions.screenPadding)
                                            .padding(horizontal = AeonTheme.dimensions.screenPadding),
                                        shape = RoundedCornerShape(AeonTheme.dimensions.cardCornerRadius),
                                        color = AppColors.cardSurface,
                                        border = BorderStroke(
                                            0.5.dp,
                                            AppColors.divider.copy(alpha = 0.15f)
                                        )
                                    ) {
                                        Box(
                                            Modifier.padding(AeonTheme.dimensions.spacing2xl),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                "No related transactions",
                                                style = AppTextStyles.transactionTitle,
                                                color = AppColors.contentSecondary.copy(alpha = 0.6f)
                                            )
                                        }
                                    }
                                } else {
                                    val budgetListState = remember(
                                        budgetTransactions,
                                        absoluteTransactionsWithBalance,
                                        baseCurrency,
                                        allTags
                                    ) {
                                        val groups = TimelineService.getGroupedData(
                                            transactions = budgetTransactions,
                                            absoluteHistory = absoluteTransactionsWithBalance,
                                            groupingType = com.osscabinet.ossWallet.domain.model.settings.TransactionGroupingType.BY_DAY,
                                            accounts = accounts,
                                            categories = categoriesList,
                                            allTags = allTags,
                                            baseCurrency = baseCurrency,
                                            exchangeRates = com.osscabinet.ossWallet.domain.service.currency.ExchangeRateSnapshot.EMPTY
                                        )
                                        com.osscabinet.ossWallet.ui.mapper.TransactionUiMapper.mapToUiState(
                                            domainState = TransactionListState(
                                                groups = groups,
                                                isEmpty = groups.isEmpty()
                                            ),
                                            baseCurrency = baseCurrency
                                        )
                                    }

                                    RecentActivitiesScreen(
                                        listState = budgetListState,
                                        onRecordsClick = { _ ->
                                            onRecordsClick(budgetFilter)
                                        },
                                        baseCurrency = baseCurrency,
                                        contentPadding = PaddingValues(top = pillHeight + AeonTheme.dimensions.spacingMd),
                                        revealedCardIds = revealedCardIds,
                                        showCurrencyBreakdown = recentActivitiesBreakdown,
                                        onTransactionEvent = { event ->
                                            onTransactionEvent(
                                                event,
                                                budgetListState.getAllTransactionIds()
                                            )
                                        },
                                        hasStickyHeader = false,
                                        showHeader = false
                                    )
                                }
                            }
                        }
                    }

                }
            }

            // 2. Pill Navigation Layer (sibling of Scaffold — must be outside hazeSource content)
            UnifiedPillNavigation(
                selectedTab = pagerState.currentPage,
                onTabSelected = { index -> scope.launch { pagerState.animateScrollToPage(index) } },
                tabs = tabs,
                showFab = false
            )

            // 3. Top Bar Layer
            AeonTopAppBar(
                left = AeonTopBarSection(
                    content = {
                        AeonTopBarNavTitlePill(
                            title = budget.name,
                            onBackClick = onBackClick,
                            tabs = tabs.map { it.label },
                            selectedTabIndex = pagerState.currentPage,
                            onTabSelected = { index ->
                                scope.launch { pagerState.animateScrollToPage(index) }
                            }
                        )
                    }
                ),
                right = AeonTopBarSection(
                    content = {
                        AeonTopBarActionPill {
                            when (pagerState.currentPage) {
                                0 -> AppBarIconButton(
                                    icon = Icons.Rounded.Edit,
                                    onClick = onEditClick
                                )

                                1 -> AppBarIconButton(
                                    icon = Icons.Rounded.History,
                                    onClick = { onRecordsClick(budgetFilter) }
                                )
                            }
                        }
                    }
                )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BudgetDetailPreview() {
    val sampleBudget = Budget(
        id = 1L,
        name = "Monthly Groceries",
        limitAmount = BigDecimal("500"),
        currency = "USD",
        timelineRange = TimelineRange.AllTime(),
        categoryIds = setOf(1L, 2L),
        accountIds = emptySet(),
        tags = emptySet(),
        profileId = 1
    )

    val sampleProgress = BudgetProgress(
        budget = sampleBudget,
        spent = BigDecimal("350"),
        limit = BigDecimal("500"),
        remaining = BigDecimal("150"),
        percentage = 0.7f,
        alertLevel = BudgetAlertLevel.NONE
    )

    OssWalletTheme {
        BudgetDetailContent(
            budget = sampleBudget,
            progress = sampleProgress,
            budgetFilter = TransactionFilter(),
            budgetTransactions = emptyList(),
            absoluteTransactionsWithBalance = emptyList(),
            accounts = emptyList(),
            categoriesList = emptyList(),
            allTags = emptyList(),
            baseCurrency = "USD",
            revealedCardIds = emptyList(),
            recentActivitiesBreakdown = false,
            onEditClick = {},
            onBackClick = {},
            onTransactionEvent = { _, _ -> },
            onRecordsClick = {}
        )
    }
}
