package com.osscabinet.ossWallet.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.domain.model.BudgetAlertLevel
import com.osscabinet.ossWallet.domain.model.BudgetTrackType
import com.osscabinet.ossWallet.domain.model.TransactionWithBalance
import com.osscabinet.ossWallet.domain.service.BudgetService
import com.osscabinet.ossWallet.ui.components.cards.AeonExpressiveCard
import com.osscabinet.ossWallet.ui.components.transactions.RecentActivitiesCard
import com.osscabinet.ossWallet.ui.components.navigation.ExpressiveNavigationBar
import com.osscabinet.ossWallet.ui.components.navigation.NavigationTab
import com.osscabinet.ossWallet.ui.navigation.RecordsFilterContext
import com.osscabinet.ossWallet.ui.viewmodel.AccountViewModel
import com.osscabinet.ossWallet.ui.viewmodel.BudgetViewModel
import com.osscabinet.ossWallet.ui.viewmodel.TransactionViewModel
import kotlinx.coroutines.launch
import java.math.BigDecimal

private val TealHealthy = Color(0xFF004D40)
private val RedWarning = Color(0xFFB3261E)

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
    onRecordsClick: (com.osscabinet.ossWallet.ui.navigation.RecordsFilterContext) -> Unit
) {
    val budgets by viewModel.budgets.collectAsStateWithLifecycle()
    val budget = remember(budgetId, budgets) { budgets.find { it.id == budgetId } }

    if (budget == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val progress = remember(budget) { viewModel.getBudgetProgress(budget, baseCurrency, precision) }
    val accentColor = when (progress.alertLevel) {
        BudgetAlertLevel.EXCEEDED, BudgetAlertLevel.WARNING -> RedWarning
        BudgetAlertLevel.NONE -> TealHealthy
    }

    val accounts by accountViewModel.accounts.collectAsStateWithLifecycle()
    val accountNamesMap = remember(accounts) { accounts.associate { it.id to it.name } }
    val accountCurrencyMap = remember(accounts) { accounts.associate { it.id to it.currencyCode } }
    val categoriesList by transactionViewModel.categories.collectAsStateWithLifecycle()
    val categoryMap = remember(categoriesList) { categoriesList.associateBy { it.id } }
    val revealedCardIds by transactionViewModel.revealedCardIdsList.collectAsStateWithLifecycle()

    val budgetTransactions = remember(budget, viewModel.transactions.collectAsStateWithLifecycle().value) {
        viewModel.getBudgetTransactions(budget).map { 
            TransactionWithBalance(it, BigDecimal.ZERO, BigDecimal.ZERO) 
        }
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
                title = { Text(budget.name, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) },
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
        },
        bottomBar = {
            ExpressiveNavigationBar(
                selectedTab = pagerState.currentPage,
                onTabSelected = { index ->
                    scope.launch { pagerState.animateScrollToPage(index) }
                },
                tabs = tabs
            )
        }
    ) { paddingValues ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            verticalAlignment = Alignment.Top
        ) { page ->
            when (page) {
                0 -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            AeonExpressiveCard(
                                title = budget.name,
                                subtitle = "${budget.period.name} Budget",
                                accentColor = accentColor,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "${progress.spent.toPlainString()} / ${budget.limitAmount.toPlainString()} ${budget.currency}",
                                    style = MaterialTheme.typography.displaySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 24.sp
                                    ),
                                    color = accentColor
                                )
                                Spacer(Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { progress.percentage.coerceIn(0f, 1f) },
                                    modifier = Modifier.fillMaxWidth().height(8.dp),
                                    color = accentColor,
                                    trackColor = accentColor.copy(alpha = 0.15f),
                                    strokeCap = StrokeCap.Round
                                )
                                Spacer(Modifier.height(8.dp))

                                if (progress.alertLevel != BudgetAlertLevel.NONE) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            Icons.Rounded.Warning,
                                            contentDescription = null,
                                            tint = RedWarning,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            progress.alertLevel.name,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = RedWarning
                                        )
                                    }
                                    Spacer(Modifier.height(4.dp))
                                }

                                if (progress.remaining > java.math.BigDecimal.ZERO) {
                                    Text(
                                        "${progress.remaining.toPlainString()} ${budget.currency} remaining",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TealHealthy
                                    )
                                } else {
                                    val overspent = progress.spent.subtract(progress.limit).abs()
                                    Text(
                                        "Over by ${overspent.toPlainString()} ${budget.currency}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = RedWarning
                                    )
                                }

                                Text(
                                    "Alert threshold: ${(budget.alertThreshold * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (budget.rollover && progress.rolloverAmount > java.math.BigDecimal.ZERO) {
                                    Text(
                                        "Rollover: +${progress.rolloverAmount.toPlainString()} from last period",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TealHealthy
                                    )
                                }
                            }
                        }
                        
                        item {
                            AeonExpressiveCard(
                                title = "SCOPE",
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("ACCOUNTS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text(
                                    if (budget.accountIds.isEmpty()) "All Accounts" else "${budget.accountIds.size} Accounts selected",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(Modifier.height(8.dp))
                                Text("CATEGORIES", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text(
                                    if (budget.categoryIds.isEmpty()) "All Categories" else "${budget.categoryIds.size} Categories selected",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(Modifier.height(8.dp))
                                Text("LABELS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text(
                                    if (budget.tags.isEmpty()) "All Labels" else budget.tags.joinToString(),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
                1 -> {
                    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        if (budgetTransactions.isEmpty()) {
                            Surface(
                                modifier = Modifier.fillMaxWidth().align(Alignment.Center),
                                shape = RoundedCornerShape(24.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
                            ) {
                                Box(Modifier.padding(32.dp), contentAlignment = Alignment.Center) {
                                    Text(
                                        "No activity in this period",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        } else {
                            val now = System.currentTimeMillis()
                            val isIncome = if (budget.trackTypes.size == 1) {
                                budget.trackTypes.first() == BudgetTrackType.INCOME
                            } else null

                            RecentActivitiesCard(
                                allTransactions = budgetTransactions,
                                accountNamesMap = accountNamesMap,
                                categoryMap = categoryMap,
                                decimalPrecision = precision,
                                precision = precision,
                                onRecordsClick = { _ ->
                                    val (start, end) = BudgetService.getCurrentPeriodRange(budget, now)
                                    onRecordsClick(
                                        RecordsFilterContext.Custom(
                                            accountIds = budget.accountIds,
                                            categoryIds = budget.categoryIds.map { it.toString() },
                                            tags = budget.tags,
                                            startDate = start,
                                            endDate = end,
                                            isIncome = isIncome
                                        )
                                    )
                                },
                                onTransactionClick = onTransactionClick,
                                baseCurrency = baseCurrency,
                                exchangeRates = emptyMap(),
                                accountCurrencyMap = accountCurrencyMap,
                                contentPadding = PaddingValues(0.dp),
                                revealedCardIds = revealedCardIds,
                                onItemExpanded = { transactionViewModel.onItemExpanded(it) },
                                onItemCollapsed = { transactionViewModel.onItemCollapsed(it) },
                                onStatusChange = { id, status -> transactionViewModel.quickUpdateStatus(id, status) },
                                onClone = { },
                                onDelete = { transactionViewModel.quickDeleteTransaction(it) },
                                onSplit = { },
                                onFullSwipeRight = { transactionViewModel.toggleStatusClearedUncleared(it) },
                                applyFilter = false,
                                excludeFuture = false
                            )
                        }
                    }
                }
            }
        }
    }
}
