package com.osscabinet.ossWallet.ui.screens.budgets.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.domain.model.TransactionListState
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.service.management.BudgetService
import com.osscabinet.ossWallet.domain.service.timeline.TimelineService
import com.osscabinet.ossWallet.ui.components.cards.AeonExpressiveCard
import com.osscabinet.ossWallet.ui.components.navigation.NavigationTab
import com.osscabinet.ossWallet.ui.components.navigation.UnifiedPillNavigation
import com.osscabinet.ossWallet.ui.components.transactions.RecentActivitiesScreen
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.ui.screens.budgets.detail.components.BudgetHeroCard
import com.osscabinet.ossWallet.ui.components.management.ScopeChip
import com.osscabinet.ossWallet.ui.components.management.ScopeSection
import com.osscabinet.ossWallet.ui.screens.accounts.AccountViewModel
import com.osscabinet.ossWallet.ui.screens.budgets.BudgetViewModel
import com.osscabinet.ossWallet.ui.screens.transactions.TransactionViewModel
import kotlinx.coroutines.launch
import java.math.BigDecimal

private val TealHealthy = Color(0xFF004D40)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetDetailScreen(
    budgetId: Long,
    viewModel: BudgetViewModel,
    accountViewModel: AccountViewModel,
    transactionViewModel: TransactionViewModel,
    baseCurrency: String,
    precision: Int,
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
    val revealedCardIds by transactionViewModel.revealedCardIdsList.collectAsStateWithLifecycle()

    val absoluteTransactionsWithBalance by transactionViewModel.absoluteTransactionsWithBalance.collectAsStateWithLifecycle()

    val budgetTransactions =
        remember(budget, absoluteTransactionsWithBalance) {
            absoluteTransactionsWithBalance.filter { item ->
                val transaction = item.transaction
                val accountMatch = budget.accountIds.isEmpty() || transaction.accountId in budget.accountIds
                val categoryMatch = budget.categoryIds.isEmpty() || transaction.categoryId in budget.categoryIds
                val tagMatch = budget.tags.isEmpty() || transaction.tags.any { it in budget.tags }
                accountMatch && categoryMatch && tagMatch
            }
        }

    val selectedAccounts = remember(accounts, budget.accountIds) {
        if (budget.accountIds.isEmpty()) emptyList()
        else accounts.filter { it.id in budget.accountIds }
    }
    val selectedCategories = remember(categoriesList, budget.categoryIds) {
        if (budget.categoryIds.isEmpty()) emptyList()
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
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
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
        Box(modifier = Modifier.fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                verticalAlignment = Alignment.Top
            ) { page ->
                when (page) {
                    0 -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            item {
                                BudgetHeroCard(
                                    budget = budget,
                                    progress = progress,
                                    precision = precision,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            item {
                                AeonExpressiveCard(
                                    title = "SCOPE & CONTEXT",
                                    icon = Icons.Rounded.FilterAlt,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                                        ScopeSection(
                                            label = "ACCOUNTS",
                                            icon = Icons.Rounded.AccountBalance,
                                            allText = "All Accounts",
                                            isEmpty = budget.accountIds.isEmpty(),
                                            items = selectedAccounts.map { account ->
                                                {
                                                    val color = try {
                                                        Color(account.colorHex.toColorInt())
                                                    } catch (_: Exception) {
                                                        MaterialTheme.colorScheme.primary
                                                    }
                                                    ScopeChip(text = account.name, color = color)
                                                }
                                            }
                                        )
                                        ScopeSection(
                                            label = "CATEGORIES",
                                            icon = Icons.Rounded.Category,
                                            allText = "All Categories",
                                            isEmpty = budget.categoryIds.isEmpty(),
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
                                            allText = "All Tags",
                                            isEmpty = budget.tags.isEmpty(),
                                            items = budget.tags.map { tag ->
                                                {
                                                    ScopeChip(
                                                        text = tag,
                                                        color = MaterialTheme.colorScheme.secondary
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
                                        color = TealHealthy.copy(alpha = 0.05f),
                                        shape = RoundedCornerShape(20.dp),
                                        border = BorderStroke(1.dp, TealHealthy.copy(alpha = 0.1f))
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .padding(16.dp)
                                                .fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Icon(
                                                Icons.Rounded.Autorenew,
                                                contentDescription = null,
                                                tint = TealHealthy
                                            )
                                            Column {
                                                Text(
                                                    "Rollover Applied",
                                                    style = MaterialTheme.typography.titleSmall.copy(
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    color = TealHealthy
                                                )
                                                Text(
                                                    "+${progress.rolloverAmount.toPlainString()} ${budget.currency} carried forward",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = TealHealthy.copy(alpha = 0.7f)
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
                                shape = RoundedCornerShape(24.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                border = BorderStroke(
                                    0.5.dp,
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)
                                )
                            ) {
                                Box(Modifier.padding(32.dp), contentAlignment = Alignment.Center) {
                                    Text(
                                        "No activity in this period",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                            alpha = 0.6f
                                        )
                                    )
                                }
                            }
                        } else {
                            val now = System.currentTimeMillis()
                            val transactionType = if (budget.trackTypes.size == 1) {
                                budget.trackTypes.first()
                            } else null

                            val budgetListState = remember(budgetTransactions, absoluteTransactionsWithBalance, precision, baseCurrency) {
                                val groups = TimelineService.getGroupedData(
                                    transactions = budgetTransactions,
                                    absoluteHistory = absoluteTransactionsWithBalance,
                                    groupingType = com.osscabinet.ossWallet.data.local.TransactionGroupingType.BY_DAY,
                                    accounts = accounts,
                                    categories = categoriesList,
                                    baseCurrency = baseCurrency,
                                    exchangeRates = emptyMap(),
                                    precision = precision
                                )
                                TransactionListState(groups = groups, isEmpty = groups.isEmpty())
                            }

                            RecentActivitiesScreen(
                                listState = budgetListState,
                                onRecordsClick = { _ ->
                                    val (start, end) = BudgetService.getCurrentPeriodRange(
                                        budget,
                                        now
                                    )
                                    onRecordsClick(
                                        TransactionFilter(
                                            accountIds = budget.accountIds,
                                            categoryIds = budget.categoryIds,
                                            tags = budget.tags,
                                            timelineRange = TimelineRange.CustomRange(start, end),
                                            transactionType = transactionType
                                        )
                                    )
                                },
                                onTransactionClick = onTransactionClick,
                                baseCurrency = baseCurrency,
                                contentPadding = PaddingValues(0.dp),
                                revealedCardIds = revealedCardIds,
                                onItemExpanded = { transactionViewModel.onItemExpanded(it) },
                                onItemCollapsed = { transactionViewModel.onItemCollapsed(it) },
                                onStatusChange = { id, status ->
                                    transactionViewModel.quickUpdateStatus(
                                        id,
                                        status
                                    )
                                },
                                onClone = { onCloneTransactionClick(it) },
                                onDelete = { transactionViewModel.quickDeleteTransaction(it) },
                                onSplit = { onSplitTransactionClick(it) },
                                onFullSwipeRight = {
                                    transactionViewModel.toggleStatusClearedUncleared(
                                        it
                                    )
                                },
                                precision = precision
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

