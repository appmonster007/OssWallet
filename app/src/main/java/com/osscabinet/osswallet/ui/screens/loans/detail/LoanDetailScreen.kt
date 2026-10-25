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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Loan
import com.osscabinet.ossWallet.domain.model.LoanStatus
import com.osscabinet.ossWallet.domain.model.LoanSummary
import com.osscabinet.ossWallet.domain.model.LoanType
import com.osscabinet.ossWallet.domain.model.Tag
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionListState
import com.osscabinet.ossWallet.ui.screens.loans.detail.components.LoanAmortizationScheduleSection
import com.osscabinet.ossWallet.domain.model.TransactionWithBalance
import com.osscabinet.ossWallet.domain.service.filter.TransactionFilterService
import com.osscabinet.ossWallet.domain.service.management.LoanService
import com.osscabinet.ossWallet.domain.service.timeline.TimelineService
import com.osscabinet.ossWallet.ui.components.AeonHazeProvider
import com.osscabinet.ossWallet.ui.components.AeonTopAppBar
import com.osscabinet.ossWallet.ui.components.AeonTopBarActionPill
import com.osscabinet.ossWallet.ui.components.AeonTopBarNavTitlePill
import com.osscabinet.ossWallet.ui.components.AeonTopBarSection
import com.osscabinet.ossWallet.ui.components.AppBarIconButton
import com.osscabinet.ossWallet.ui.components.aeonHazeSource
import com.osscabinet.ossWallet.ui.components.management.ManagementScopeCard
import com.osscabinet.ossWallet.ui.components.management.ManagementSectionHeader
import com.osscabinet.ossWallet.ui.components.navigation.NavigationTab
import com.osscabinet.ossWallet.ui.components.navigation.UnifiedPillNavigation
import com.osscabinet.ossWallet.ui.components.scrollBottomPadding
import com.osscabinet.ossWallet.ui.components.transactions.RecentActivitiesScreen
import com.osscabinet.ossWallet.ui.components.transactions.TransactionItemEvent
import com.osscabinet.ossWallet.ui.screens.accounts.AccountViewModel
import com.osscabinet.ossWallet.ui.screens.loans.LoanViewModel
import com.osscabinet.ossWallet.ui.screens.loans.detail.components.LoanHeroCard
import com.osscabinet.ossWallet.ui.screens.transactions.TransactionViewModel
import com.osscabinet.ossWallet.ui.theme.AeonSize
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.HazeLayer
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


/**
 * A Value Object representing a valid configuration for the Loan Detail screen.
 */
data class LoanDetailConfig(
    val loanId: Long,
    val baseCurrency: String
)

/**
 * Grouped actions for the Loan Detail screen.
 */
data class LoanDetailActions(
    val onEditClick: () -> Unit,
    val onBackClick: () -> Unit,
    val onTransactionClick: (Long) -> Unit,
    val onCloneTransactionClick: (Long) -> Unit = {},
    val onSplitTransactionClick: (Long) -> Unit = {},
    val onRecordsClick: (TransactionFilter) -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanDetailScreen(
    config: LoanDetailConfig,
    actions: LoanDetailActions,
    viewModel: LoanViewModel,
    accountViewModel: AccountViewModel,
    transactionViewModel: TransactionViewModel
) {
    val loans by viewModel.loans.collectAsStateWithLifecycle()
    val loanSummaries by viewModel.loanSummaries.collectAsStateWithLifecycle()
    val loan = remember(config.loanId, loans) { loans.find { it.id == config.loanId } }

    LaunchedEffect(loans) {
        if (loans.isNotEmpty() && loan == null) actions.onBackClick()
    }

    if (loan == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val categoriesList by transactionViewModel.categories.collectAsStateWithLifecycle()
    val accounts by accountViewModel.accounts.collectAsStateWithLifecycle()
    val allTags by transactionViewModel.allTags.collectAsStateWithLifecycle()
    val absoluteTransactionsWithBalance by transactionViewModel.absoluteTransactionsWithBalance.collectAsStateWithLifecycle()
    val revealedCardIds by transactionViewModel.revealedCardIdsList.collectAsStateWithLifecycle()

    val selectedOffsets by viewModel.selectedOffsets.collectAsStateWithLifecycle()
    val periodHistoryMap by viewModel.periodHistory.collectAsStateWithLifecycle()

    val currentOffset = selectedOffsets[loan.id] ?: 0
    val history = periodHistoryMap[loan.id] ?: emptyList()

    val evalLoan = if (currentOffset != 0) {
        loan.copy(timelineRange = TimelineService.shiftRange(loan.timelineRange, currentOffset))
    } else loan

    val summary = loanSummaries[config.loanId] ?: LoanService.buildSummary(
        evalLoan, emptyList(), System.currentTimeMillis()
    )

    val loanFilter = remember(evalLoan, categoriesList) {
        LoanService.buildTransactionFilter(evalLoan, categoriesList)
    }

    val loanTransactions = remember(loanFilter, absoluteTransactionsWithBalance) {
        TransactionFilterService.filterTransactionsWithBalance(
            absoluteTransactionsWithBalance,
            loanFilter
        )
    }

    val recentActivitiesBreakdown by viewModel.recentActivitiesBreakdown.collectAsStateWithLifecycle()

    LoanDetailContent(
        loan = loan,
        summary = summary,
        offset = currentOffset,
        onOffsetChange = { viewModel.setPeriodOffset(loan.id, it) },
        history = history,
        loanFilter = loanFilter,
        loanTransactions = loanTransactions,
        absoluteTransactionsWithBalance = absoluteTransactionsWithBalance,
        accounts = accounts,
        categoriesList = categoriesList,
        allTags = allTags,
        baseCurrency = config.baseCurrency,
        revealedCardIds = revealedCardIds,
        recentActivitiesBreakdown = recentActivitiesBreakdown,
        onEditClick = actions.onEditClick,
        onBackClick = actions.onBackClick,
        onSettleClick = { viewModel.settleLoan(loan); actions.onBackClick() },
        onTransactionEvent = { event, availableIds ->
            when (event) {
                is TransactionItemEvent.Click -> actions.onTransactionClick(
                    event.id.substringAfter(
                        "_"
                    ).toLongOrNull() ?: -1L
                )

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
fun LoanDetailContent(
    loan: Loan,
    summary: LoanSummary,
    offset: Int = 0,
    onOffsetChange: ((Int) -> Unit)? = null,
    history: List<LoanSummary> = emptyList(),
    loanFilter: TransactionFilter,
    loanTransactions: List<TransactionWithBalance>,
    absoluteTransactionsWithBalance: List<TransactionWithBalance>,
    accounts: List<Account>,
    categoriesList: List<Category>,
    allTags: List<Tag>,
    baseCurrency: String,
    revealedCardIds: List<String>,
    recentActivitiesBreakdown: Boolean,
    onEditClick: () -> Unit,
    onBackClick: () -> Unit,
    onSettleClick: () -> Unit,
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
                            0 -> LoanOverviewTab(
                                loan = loan,
                                summary = summary,
                                offset = offset,
                                onOffsetChange = onOffsetChange,
                                history = history,
                                loanFilter = loanFilter,
                                accounts = accounts,
                                categoriesList = categoriesList,
                                topPadding = pillHeight
                            )

                            1 -> {
                                if (loanTransactions.isEmpty()) {
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
                                    val loanListState = remember(
                                        loanTransactions,
                                        absoluteTransactionsWithBalance,
                                        baseCurrency,
                                        allTags
                                    ) {
                                        val groups = TimelineService.getGroupedData(
                                            transactions = loanTransactions,
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
                                        listState = loanListState,
                                        onRecordsClick = { _ ->
                                            onRecordsClick(loanFilter)
                                        },
                                        baseCurrency = baseCurrency,
                                        contentPadding = PaddingValues(top = pillHeight + AeonTheme.dimensions.spacingMd),
                                        revealedCardIds = revealedCardIds,
                                        showCurrencyBreakdown = recentActivitiesBreakdown,
                                        onTransactionEvent = { event ->
                                            onTransactionEvent(
                                                event,
                                                loanListState.getAllTransactionIds()
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
                onTabSelected = { index ->
                    scope.launch { pagerState.animateScrollToPage(index) }
                },
                tabs = tabs,
                showFab = false
            )

            // 3. Top Bar Layer
            AeonTopAppBar(
                left = AeonTopBarSection(
                    content = {
                        AeonTopBarNavTitlePill(
                            title = loan.name,
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
                                0 -> {
                                    if (loan.status == LoanStatus.ACTIVE) {
                                        AppBarIconButton(
                                            icon = Icons.Rounded.CheckCircle,
                                            onClick = onSettleClick
                                        )
                                    }
                                    AppBarIconButton(
                                        icon = Icons.Rounded.Payments,
                                        onClick = onEditClick
                                    )
                                }

                                1 -> {
                                    AppBarIconButton(
                                        icon = Icons.Rounded.History,
                                        onClick = { onRecordsClick(loanFilter) }
                                    )
                                }
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
fun LoanDetailPreview() {
    val sampleLoan = Loan(
        id = 1L,
        name = "Home Mortgage",
        principalAmount = BigDecimal("250000"),
        currency = "USD",
        interestRate = 3.5f,
        startDate = System.currentTimeMillis() - 1000000000L,
        lenderOrBorrowerName = "Chase Bank",
        loanType = LoanType.BORROWED,
        linkedAccountId = "acc1",
        expectedSettlementDate = System.currentTimeMillis() + 5000000000L,
        status = LoanStatus.ACTIVE,
        profileId = 1,
        categoryIds = setOf(1L)
    )

    val sampleSummary = LoanSummary(
        loan = sampleLoan,
        totalPaid = BigDecimal("15000"),
        remainingBalance = BigDecimal("235000"),
        interestAccrued = BigDecimal("5000")
    )

    OssWalletTheme {
        LoanDetailContent(
            loan = sampleLoan,
            summary = sampleSummary,
            loanFilter = TransactionFilter(),
            loanTransactions = emptyList(),
            absoluteTransactionsWithBalance = emptyList(),
            accounts = emptyList(),
            categoriesList = emptyList(),
            allTags = emptyList(),
            baseCurrency = "USD",
            revealedCardIds = emptyList(),
            recentActivitiesBreakdown = false,
            onEditClick = {},
            onBackClick = {},
            onSettleClick = {},
            onTransactionEvent = { _, _ -> },
            onRecordsClick = {}
        )
    }
}

@Composable
private fun LoanOverviewTab(
    loan: Loan,
    summary: LoanSummary,
    offset: Int = 0,
    onOffsetChange: ((Int) -> Unit)? = null,
    history: List<LoanSummary> = emptyList(),
    loanFilter: TransactionFilter,
    accounts: List<Account>,
    categoriesList: List<Category>,
    topPadding: androidx.compose.ui.unit.Dp = 0.dp
) {
    var showAmortization by remember { mutableStateOf(false) }

    val monthlyPayment = remember(loan) {
        LoanService.calculateStandardMonthlyPayment(loan)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = AeonTheme.dimensions.cardListEdgeSpacing,
            end = AeonTheme.dimensions.cardListEdgeSpacing,
            top = AeonTheme.dimensions.screenPadding + topPadding,
            bottom = scrollBottomPadding()
        ),
        // Matches the standard card-to-card gap used on Dashboard/Analytics/Budget-list/Loan-list
        verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.cardListGap)
    ) {
        item {
            LoanHeroCard(
                loan = loan,
                summary = summary,
                offset = offset,
                onOffsetChange = onOffsetChange,
                history = history,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            ManagementScopeCard(
                filter = loanFilter,
                allAccounts = accounts,
                allCategories = categoriesList
            )
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

            if (showAmortization) {
                item {
                    LoanAmortizationScheduleSection(
                        loan = loan,
                        monthlyPayment = monthlyPayment
                    )
                }
            }
        }
    }
}
