package com.osscabinet.ossWallet.ui.screens.loans.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.FilterAlt
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Payments
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.domain.model.AmortizationEntry
import com.osscabinet.ossWallet.domain.model.LoanStatus
import com.osscabinet.ossWallet.domain.model.LoanSummary
import com.osscabinet.ossWallet.domain.model.TransactionListState
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.service.filter.TransactionFilterService
import com.osscabinet.ossWallet.domain.service.management.LoanService
import com.osscabinet.ossWallet.domain.service.timeline.TimelineService
import com.osscabinet.ossWallet.ui.components.cards.AeonExpressiveCard
import com.osscabinet.ossWallet.ui.components.scrollBottomPadding
import com.osscabinet.ossWallet.ui.components.navigation.NavigationTab
import com.osscabinet.ossWallet.ui.components.navigation.UnifiedPillNavigation
import com.osscabinet.ossWallet.ui.components.transactions.RecentActivitiesScreen
import com.osscabinet.ossWallet.ui.components.transactions.TransactionItemEvent
import com.osscabinet.ossWallet.ui.components.management.ManagementSectionHeader
import com.osscabinet.ossWallet.ui.components.management.ScopeChip
import com.osscabinet.ossWallet.ui.components.management.ScopeSection
import com.osscabinet.ossWallet.ui.screens.accounts.AccountViewModel
import com.osscabinet.ossWallet.ui.screens.loans.LoanViewModel
import com.osscabinet.ossWallet.ui.screens.loans.detail.components.LoanHeroCard
import com.osscabinet.ossWallet.ui.screens.transactions.TransactionViewModel
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanDetailScreen(
    loanId: Long,
    viewModel: LoanViewModel,
    accountViewModel: AccountViewModel,
    transactionViewModel: TransactionViewModel,
    baseCurrency: String,
    onEditClick: () -> Unit,
    onBackClick: () -> Unit,
    onTransactionClick: (Long) -> Unit,
    onCloneTransactionClick: (Long) -> Unit = {},
    onSplitTransactionClick: (Long) -> Unit = {},
    onRecordsClick: (TransactionFilter) -> Unit
) {
    val loans by viewModel.loans.collectAsStateWithLifecycle()
    val loanSummaries by viewModel.loanSummaries.collectAsStateWithLifecycle()
    val loan = remember(loanId, loans) { loans.find { it.id == loanId } }

    LaunchedEffect(loans) {
        if (loans.isNotEmpty() && loan == null) onBackClick()
    }

    if (loan == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    // Single category source — always populated via TransactionViewModel.
    val categoriesList by transactionViewModel.categories.collectAsStateWithLifecycle()
    val accounts by accountViewModel.accounts.collectAsStateWithLifecycle()
    val absoluteTransactionsWithBalance by transactionViewModel.absoluteTransactionsWithBalance.collectAsStateWithLifecycle()
    val revealedCardIds by transactionViewModel.revealedCardIdsList.collectAsStateWithLifecycle()

    // Use precomputed summary from ViewModel (matches BudgetViewModel.budgetProgress pattern).
    // Fall back to empty summary while the combine flow first emits.
    val summary = loanSummaries[loanId] ?: LoanService.buildSummary(
        loan, emptyList(), System.currentTimeMillis()
    )

    // Activity tab — canonical filter from LoanService guarantees consistency with summary.
    val loanTransactions = remember(loan, absoluteTransactionsWithBalance, categoriesList) {
        val filter = LoanService.buildTransactionFilter(loan, categoriesList)
        android.util.Log.d("LoanDetail", "[${loan.name}] linked filterId=${loan.filterId} | loan fields: categories=${loan.categoryIds} linkedAccountId=${loan.linkedAccountId} tags=${loan.tags} trackTypes=${loan.trackTypes} currencies=${loan.currencies} paymentMethods=${loan.paymentMethods} statuses=${loan.statuses} linkedTxIds=${loan.linkedTransactionIds} timelineRange=${loan.timelineRange}")
        android.util.Log.d("LoanDetail", "[${loan.name}] activity filter: $filter")
        TransactionFilterService.filterTransactionsWithBalance(absoluteTransactionsWithBalance, filter)
    }

    val selectedAccount = remember(accounts, loan.linkedAccountId) {
        accounts.find { it.id == loan.linkedAccountId }
    }
    val selectedCategories = remember(categoriesList, loan.categoryIds) {
        if (loan.categoryIds.isEmpty()) emptyList()
        else categoriesList.filter { it.id in loan.categoryIds }
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
                        loan.name,
                        style = AppTextStyles.cardTitle
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onEditClick) {
                        Icon(Icons.Rounded.Payments, contentDescription = "Edit")
                    }
                    if (loan.status == LoanStatus.ACTIVE) {
                        TextButton(onClick = { viewModel.settleLoan(loan); onBackClick() }) {
                            Text("Settle")
                        }
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
                    0 -> LoanOverviewTab(
                        loan = loan,
                        summary = summary,
                        selectedAccount = selectedAccount,
                        selectedCategories = selectedCategories,
                        allCategoriesCount = categoriesList.size
                    )

                    1 -> {
                        if (loanTransactions.isEmpty()) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(AeonTheme.dimensions.screenPadding),
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
                            val loanListState = remember(loanTransactions, absoluteTransactionsWithBalance, baseCurrency) {
                                val groups = TimelineService.getGroupedData(
                                    transactions = loanTransactions,
                                    absoluteHistory = absoluteTransactionsWithBalance,
                                    groupingType = com.osscabinet.ossWallet.data.local.TransactionGroupingType.BY_DAY,
                                    accounts = accounts,
                                    categories = categoriesList,
                                    baseCurrency = baseCurrency,
                                    exchangeRates = emptyMap()
                                )
                                TransactionListState(groups = groups, isEmpty = groups.isEmpty())
                            }

                            val recentActivitiesBreakdown by viewModel.recentActivitiesBreakdown.collectAsStateWithLifecycle()

                            RecentActivitiesScreen(
                                listState = loanListState,
                                onRecordsClick = { _ ->
                                    onRecordsClick(LoanService.buildTransactionFilter(loan, categoriesList))
                                },
                                baseCurrency = baseCurrency,
                                contentPadding = PaddingValues(0.dp),
                                revealedCardIds = revealedCardIds,
                                showCurrencyBreakdown = recentActivitiesBreakdown,
                                onTransactionEvent = { event ->
                                    when (event) {
                                        is TransactionItemEvent.Click -> onTransactionClick(event.id)
                                        is TransactionItemEvent.Expand -> transactionViewModel.onItemExpanded(event.id)
                                        is TransactionItemEvent.Collapse -> transactionViewModel.onItemCollapsed(event.id)
                                        is TransactionItemEvent.StatusChange -> transactionViewModel.quickUpdateStatus(event.id, event.status)
                                        is TransactionItemEvent.Clone -> onCloneTransactionClick(event.id)
                                        is TransactionItemEvent.Delete -> transactionViewModel.quickDeleteTransaction(event.id)
                                        is TransactionItemEvent.Split -> onSplitTransactionClick(event.id)
                                        is TransactionItemEvent.FullSwipeRight -> transactionViewModel.toggleStatusClearedUncleared(event.id)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            UnifiedPillNavigation(
                selectedTab = pagerState.currentPage,
                onTabSelected = { index -> scope.launch { pagerState.animateScrollToPage(index) } },
                tabs = tabs,
                showFab = false
            )
        }
    }
}

@Composable
private fun LoanOverviewTab(
    loan: com.osscabinet.ossWallet.domain.model.Loan,
    summary: LoanSummary,
    selectedAccount: com.osscabinet.ossWallet.domain.model.Account?,
    selectedCategories: List<com.osscabinet.ossWallet.domain.model.Category>,
    allCategoriesCount: Int
) {
    var showAmortization by remember { mutableStateOf(false) }
    var showAllAmortization by remember { mutableStateOf(false) }

    val amortizationSchedule = remember(loan, showAmortization) {
        if (!showAmortization || loan.interestRate == null) emptyList()
        else {
            val monthlyPayment = loan.principalAmount.divide(BigDecimal(12), 2, RoundingMode.HALF_UP)
            LoanService.getAmortizationSchedule(loan, monthlyPayment, loan.startDate)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = AeonTheme.dimensions.screenPadding, end = AeonTheme.dimensions.screenPadding, top = AeonTheme.dimensions.screenPadding, bottom = scrollBottomPadding()),
        verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingLg)
    ) {
        item {
            LoanHeroCard(
                loan = loan,
                summary = summary,
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
                        label = "LINKED ACCOUNT",
                        icon = Icons.Rounded.AccountBalance,
                        allText = "No linked account",
                        isEmpty = loan.linkedAccountId.isBlank(),
                        items = selectedAccount?.let { account ->
                            listOf<@Composable () -> Unit> {
                                val color = try {
                                    Color(account.colorHex.toColorInt())
                                } catch (_: Exception) {
                                    AppColors.primary
                                }
                                ScopeChip(text = account.name, color = color)
                            }
                        } ?: emptyList()
                    )
                    ScopeSection(
                        label = "CATEGORIES",
                        icon = Icons.Rounded.Category,
                        allText = "All Categories",
                        isEmpty = loan.categoryIds.isEmpty() ||
                                (allCategoriesCount > 0 && loan.categoryIds.size == allCategoriesCount),
                        items = selectedCategories.map { category ->
                            {
                                ScopeChip(
                                    text = category.name,
                                    color = Color(category.colorHex)
                                )
                            }
                        }
                    )
                    ScopeSection(
                        label = "TAGS",
                        icon = Icons.AutoMirrored.Rounded.Label,
                        allText = "No Tags",
                        isEmpty = loan.tags.isEmpty(),
                        items = loan.tags.map { tag ->
                            { ScopeChip(text = tag, color = AppColors.secondary) }
                        }
                    )
                }
            }
        }

        if (loan.interestRate != null) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ManagementSectionHeader(
                        title = "AMORTIZATION SCHEDULE",
                        sectionColor = AppColors.primary
                    )
                    IconButton(onClick = { showAmortization = !showAmortization }) {
                        Icon(
                            if (showAmortization) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                            contentDescription = if (showAmortization) "Collapse" else "Expand"
                        )
                    }
                }
            }
        }

        if (showAmortization && amortizationSchedule.isNotEmpty()) {
            val displayRows = if (showAllAmortization) amortizationSchedule else amortizationSchedule.take(12)
            items(displayRows, key = { it.period }) { entry ->
                AmortizationRow(entry = entry, currency = loan.currency)
            }
            if (amortizationSchedule.size > 12) {
                item {
                    TextButton(
                        onClick = { showAllAmortization = !showAllAmortization },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (showAllAmortization) "Show less" else "Show all (${amortizationSchedule.size} periods)")
                    }
                }
            }
        }
    }
}

@Composable
private fun AmortizationRow(entry: AmortizationEntry, currency: String) {
    val dateStr = remember(entry.date) {
        SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(Date(entry.date))
    }
    Surface(
        color = AppColors.cardSurfaceProminent.copy(alpha = 0.2f),
        shape = RoundedCornerShape(AeonTheme.dimensions.listPadding),
        border = BorderStroke(0.5.dp, AppColors.divider.copy(alpha = 0.08f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AeonTheme.dimensions.listPadding, vertical = AeonTheme.dimensions.spacingSm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${entry.period}",
                style = AppTextStyles.transactionSwipeAction,
                modifier = Modifier.width(AeonTheme.dimensions.spacingXl),
                color = AppColors.contentSecondary
            )
            Text(
                dateStr,
                style = AppTextStyles.caption,
                modifier = Modifier.width(AeonTheme.dimensions.iconSize3xl)
            )
            Text(
                entry.principal.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                style = AppTextStyles.caption
            )
            Text(
                entry.interest.toPlainString(),
                style = AppTextStyles.caption,
                color = AppColors.error
            )
            Text(
                entry.balance.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                style = AppTextStyles.caption,
                color = AppColors.primary
            )
        }
    }
}
