package com.osscabinet.ossWallet.ui.screens

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
import com.osscabinet.ossWallet.domain.model.BudgetTrackType
import com.osscabinet.ossWallet.domain.model.TransactionWithBalance
import com.osscabinet.ossWallet.domain.service.management.BudgetService
import com.osscabinet.ossWallet.ui.components.cards.AeonExpressiveCard
import com.osscabinet.ossWallet.ui.components.navigation.NavigationTab
import com.osscabinet.ossWallet.ui.components.navigation.UnifiedPillNavigation
import com.osscabinet.ossWallet.ui.components.transactions.RecentActivitiesScreen
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.ui.screens.budgets.detail.components.BudgetHeroCard
import com.osscabinet.ossWallet.ui.viewmodel.AccountViewModel
import com.osscabinet.ossWallet.ui.viewmodel.BudgetViewModel
import com.osscabinet.ossWallet.ui.viewmodel.TransactionViewModel
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
    val accountNamesMap = remember(accounts) { accounts.associate { it.id to it.name } }
    val categoriesList by transactionViewModel.categories.collectAsStateWithLifecycle()
    val categoryMap = remember(categoriesList) { categoriesList.associateBy { it.id } }
    val revealedCardIds by transactionViewModel.revealedCardIdsList.collectAsStateWithLifecycle()

    val budgetTransactions =
        remember(budget, viewModel.transactions.collectAsStateWithLifecycle().value) {
            viewModel.getBudgetTransactions(budget).map {
                TransactionWithBalance(it, BigDecimal.ZERO, BigDecimal.ZERO)
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
                                if (budget.trackTypes.first() == BudgetTrackType.INCOME) "income" else "expense"
                            } else null

                            RecentActivitiesScreen(
                                allTransactions = budgetTransactions,
                                accountNamesMap = accountNamesMap,
                                categoryMap = categoryMap,
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
                                exchangeRates = emptyMap(),
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
                                onClone = { },
                                onDelete = { transactionViewModel.quickDeleteTransaction(it) },
                                onSplit = { },
                                onFullSwipeRight = {
                                    transactionViewModel.toggleStatusClearedUncleared(
                                        it
                                    )
                                },
                                applyFilter = false,
                                excludeFuture = false,
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ScopeSection(
    label: String,
    icon: ImageVector,
    allText: String,
    isEmpty: Boolean,
    items: List<@Composable () -> Unit>
) {
    var isExpanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Text(
                label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            )
        }

        if (isEmpty) {
            Text(
                allText,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 56.dp)
            )
        } else {
            FlowRow(
                modifier = Modifier.padding(start = 56.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val displayCount = if (isExpanded) items.size else 5
                items.take(displayCount).forEach { it() }

                if (items.size > 5 && !isExpanded) {
                    Surface(
                        onClick = { isExpanded = true },
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(
                            0.5.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        )
                    ) {
                        Text(
                            text = "+${items.size - 5} more",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScopeChip(
    text: String,
    color: Color
) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(0.5.dp, color.copy(alpha = 0.2f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
