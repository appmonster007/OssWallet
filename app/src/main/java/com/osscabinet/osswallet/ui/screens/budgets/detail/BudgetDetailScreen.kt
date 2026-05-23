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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FilterAlt
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Label
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionListState
import com.osscabinet.ossWallet.domain.service.filter.TransactionFilterService
import com.osscabinet.ossWallet.domain.service.management.BudgetService
import com.osscabinet.ossWallet.domain.service.timeline.TimelineService
import com.osscabinet.ossWallet.ui.components.cards.AeonExpressiveCard
import com.osscabinet.ossWallet.ui.components.management.ScopeChip
import com.osscabinet.ossWallet.ui.components.management.ScopeSection
import com.osscabinet.ossWallet.ui.components.navigation.NavigationTab
import com.osscabinet.ossWallet.ui.components.navigation.UnifiedPillNavigation
import com.osscabinet.ossWallet.ui.components.scrollBottomPadding
import com.osscabinet.ossWallet.ui.components.transactions.RecentActivitiesScreen
import com.osscabinet.ossWallet.ui.components.transactions.TransactionItemEvent
import com.osscabinet.ossWallet.ui.screens.accounts.AccountViewModel
import com.osscabinet.ossWallet.ui.screens.budgets.BudgetViewModel
import com.osscabinet.ossWallet.ui.screens.budgets.detail.components.BudgetHeroCard
import com.osscabinet.ossWallet.ui.screens.transactions.TransactionViewModel
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import kotlinx.coroutines.launch
import java.math.BigDecimal


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetDetailScreen(
    budgetId: Long,
    viewModel: BudgetViewModel,
    accountViewModel: AccountViewModel,
    transactionViewModel: TransactionViewModel,
    baseCurrency: String,
    onEditClick: (Long) -> Unit,
    onBackClick: () -> Unit,
    onTransactionClick: (Long) -> Unit,
    onCloneTransactionClick: (Long) -> Unit = {},
    onSplitTransactionClick: (Long) -> Unit = {},
    onRecordsClick: (TransactionFilter) -> Unit
) {
    val budgets by viewModel.budgets.collectAsStateWithLifecycle()
    val budgetProgress by viewModel.budgetProgress.collectAsStateWithLifecycle()
    val budget = remember(budgetId, budgets) { budgets.find { it.id == budgetId } }

    LaunchedEffect(budgets) {
        if (budgets.isNotEmpty() && budget == null) {
            onBackClick()
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

    val budgetTransactions =
        remember(budget, absoluteTransactionsWithBalance, categoriesList) {
            if (budget == null) return@remember emptyList()
            val filter = BudgetService.buildTransactionFilter(
                budget,
                System.currentTimeMillis(),
                categoriesList
            )
            android.util.Log.d(
                "BudgetDetail",
                "[${budget.name}] linked filterId=${budget.filterId} | budget fields: categories=${budget.categoryIds} accounts=${budget.accountIds} tags=${budget.tags} trackTypes=${budget.trackTypes} currencies=${budget.currencies} paymentMethods=${budget.paymentMethods} statuses=${budget.statuses} linkedTxIds=${budget.linkedTransactionIds} timelineRange=${budget.timelineRange}"
            )
            android.util.Log.d("BudgetDetail", "[${budget.name}] activity filter: $filter")
            TransactionFilterService.filterTransactionsWithBalance(
                absoluteTransactionsWithBalance,
                filter
            )
        }

    val selectedAccounts = remember(accounts, budget?.accountIds) {
        if (budget == null || budget.accountIds.isEmpty()) emptyList()
        else accounts.filter { it.id in budget.accountIds }
    }
    val selectedCategories = remember(categoriesList, budget?.categoryIds) {
        if (budget == null || budget.categoryIds.isEmpty()) emptyList()
        else categoriesList.filter { it.id in budget.categoryIds }
    }

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

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        budget.name,
                        style = AppTextStyles.cardTitle
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onEditClick(budget.id) }) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Edit")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)) {
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
                                top = AeonTheme.dimensions.screenPadding,
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
                                AeonExpressiveCard(
                                    title = "SCOPE & CONTEXT",
                                    icon = Icons.Rounded.FilterAlt,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingXl)) {
                                        ScopeSection(
                                            label = "ACCOUNTS",
                                            icon = Icons.Rounded.AccountBalance,
                                            allText = "All Accounts",
                                            isEmpty = budget.accountIds.isEmpty() || (accounts.isNotEmpty() && budget.accountIds.size == accounts.size),
                                            items = selectedAccounts.map { account ->
                                                {
                                                    val color = try {
                                                        Color(account.colorHex.toColorInt())
                                                    } catch (_: Exception) {
                                                        AppColors.primary
                                                    }
                                                    ScopeChip(text = account.name, color = color)
                                                }
                                            }
                                        )
                                        ScopeSection(
                                            label = "CATEGORIES",
                                            icon = Icons.Rounded.Category,
                                            allText = "All Categories",
                                            isEmpty = budget.categoryIds.isEmpty() || (categoriesList.isNotEmpty() && budget.categoryIds.size == categoriesList.size),
                                            items = selectedCategories.map { category ->
                                                {
                                                    val color = Color(category.colorHex)
                                                    ScopeChip(text = category.name, color = color)
                                                }
                                            }
                                        )
                                        ScopeSection(
                                            label = "TAGS",
                                            icon = Icons.Rounded.Label,
                                            allText = "No Tags",
                                            isEmpty = budget.tags.isEmpty(),
                                            items = budget.tags.map { tag ->
                                                {
                                                    ScopeChip(
                                                        text = tag,
                                                        color = AppColors.secondary
                                                    )
                                                }
                                            }
                                        )
                                    }
                                }
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
                                            horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)
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
                                                    "+${progress.rolloverAmount.toPlainString()} ${budget.currency} carried forward",
                                                    style = AppTextStyles.caption,
                                                    color = AppColors.primary.copy(alpha = 0.7f)
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
                                modifier = Modifier.fillMaxWidth(),
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
                                        "No activity in this period",
                                        style = AppTextStyles.transactionTitle,
                                        color = AppColors.contentSecondary.copy(
                                            alpha = 0.6f
                                        )
                                    )
                                }
                            }
                        } else {
                            val now = System.currentTimeMillis()

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
                                    exchangeRates = emptyMap()
                                )
                                TransactionListState(groups = groups, isEmpty = groups.isEmpty())
                            }

                            val recentActivitiesBreakdown by viewModel.recentActivitiesBreakdown.collectAsStateWithLifecycle()

                            RecentActivitiesScreen(
                                listState = budgetListState,
                                onRecordsClick = { _ ->
                                    onRecordsClick(
                                        BudgetService.buildTransactionFilter(
                                            budget,
                                            now,
                                            categoriesList
                                        )
                                    )
                                },
                                baseCurrency = baseCurrency,
                                contentPadding = PaddingValues(0.dp),
                                revealedCardIds = revealedCardIds,
                                showCurrencyBreakdown = recentActivitiesBreakdown,
                                onTransactionEvent = { event ->
                                    when (event) {
                                        is TransactionItemEvent.Click -> onTransactionClick(event.id)
                                        is TransactionItemEvent.Expand -> transactionViewModel.onItemExpanded(
                                            event.id
                                        )

                                        is TransactionItemEvent.Collapse -> transactionViewModel.onItemCollapsed(
                                            event.id
                                        )

                                        is TransactionItemEvent.StatusChange -> transactionViewModel.quickUpdateStatus(
                                            event.id,
                                            event.status
                                        )

                                        is TransactionItemEvent.Clone -> onCloneTransactionClick(
                                            event.id
                                        )

                                        is TransactionItemEvent.Delete -> transactionViewModel.quickDeleteTransaction(
                                            event.id
                                        )

                                        is TransactionItemEvent.Split -> onSplitTransactionClick(
                                            event.id
                                        )

                                        is TransactionItemEvent.FullSwipeRight -> transactionViewModel.toggleStatusClearedUncleared(
                                            event.id
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }

            UnifiedPillNavigation(
                selectedTab = pagerState.currentPage,
                onTabSelected = { index ->
                    scope.launch { pagerState.animateScrollToPage(index) }
                },
                tabs = tabs,
                showFab = false
            )
        }
    }
}

