package com.osscabinet.ossWallet.ui.screens.loans.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.domain.model.*
import com.osscabinet.ossWallet.domain.service.filter.TransactionFilterService
import com.osscabinet.ossWallet.domain.service.management.LoanService
import com.osscabinet.ossWallet.domain.service.timeline.TimelineService
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
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


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

    val categoriesList by transactionViewModel.categories.collectAsStateWithLifecycle()
    val accounts by accountViewModel.accounts.collectAsStateWithLifecycle()
    val allTags by transactionViewModel.allTags.collectAsStateWithLifecycle()
    val absoluteTransactionsWithBalance by transactionViewModel.absoluteTransactionsWithBalance.collectAsStateWithLifecycle()
    val revealedCardIds by transactionViewModel.revealedCardIdsList.collectAsStateWithLifecycle()

    val summary = loanSummaries[loanId] ?: LoanService.buildSummary(
        loan, emptyList(), System.currentTimeMillis()
    )

    val loanFilter = remember(loan, categoriesList) {
        LoanService.buildTransactionFilter(loan, categoriesList)
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
        loanFilter = loanFilter,
        loanTransactions = loanTransactions,
        absoluteTransactionsWithBalance = absoluteTransactionsWithBalance,
        accounts = accounts,
        categoriesList = categoriesList,
        allTags = allTags,
        baseCurrency = baseCurrency,
        revealedCardIds = revealedCardIds,
        recentActivitiesBreakdown = recentActivitiesBreakdown,
        onEditClick = onEditClick,
        onBackClick = onBackClick,
        onSettleClick = { viewModel.settleLoan(loan); onBackClick() },
        onTransactionEvent = { event ->
            when (event) {
                is TransactionItemEvent.Click -> onTransactionClick(event.id.substringAfter("_").toLongOrNull() ?: -1L)
                is TransactionItemEvent.Expand -> transactionViewModel.onItemExpanded(event.id)
                is TransactionItemEvent.Collapse -> transactionViewModel.onItemCollapsed(event.id)
                is TransactionItemEvent.StatusChange -> transactionViewModel.quickUpdateStatus(
                    event.id,
                    event.status
                )
                is TransactionItemEvent.Clone -> onCloneTransactionClick(event.id)
                is TransactionItemEvent.Delete -> transactionViewModel.quickDeleteTransaction(event.id)
                is TransactionItemEvent.Split -> onSplitTransactionClick(event.id)
                is TransactionItemEvent.FullSwipeRight -> transactionViewModel.toggleStatusClearedUncleared(
                    event.id
                )
                is TransactionItemEvent.ConfirmPlanned -> transactionViewModel.confirmPlannedPayment(event.paymentId, event.scheduledDate)
                is TransactionItemEvent.CancelPlanned -> transactionViewModel.cancelPlannedPayment(event.paymentId, event.scheduledDate)
            }
        },
        onRecordsClick = onRecordsClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanDetailContent(
    loan: Loan,
    summary: LoanSummary,
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
    onTransactionEvent: (TransactionItemEvent) -> Unit,
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
                        Button(
                            onClick = onSettleClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppColors.primary.copy(alpha = 0.1f),
                                contentColor = AppColors.primary
                            ),
                            elevation = ButtonDefaults.buttonElevation(0.dp),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                "Settle",
                                style = AppTextStyles.caption.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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
                        loanFilter = loanFilter,
                        accounts = accounts,
                        categoriesList = categoriesList
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
                                    exchangeRates = emptyMap()
                                )
                                TransactionListState(groups = groups, isEmpty = groups.isEmpty())
                            }

                            RecentActivitiesScreen(
                                listState = loanListState,
                                onRecordsClick = { _ ->
                                    onRecordsClick(loanFilter)
                                },
                                baseCurrency = baseCurrency,
                                contentPadding = PaddingValues(0.dp),
                                revealedCardIds = revealedCardIds,
                                showCurrencyBreakdown = recentActivitiesBreakdown,
                                onTransactionEvent = onTransactionEvent
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
            onTransactionEvent = {},
            onRecordsClick = {}
        )
    }
}

@Composable
private fun LoanOverviewTab(
    loan: Loan,
    summary: LoanSummary,
    loanFilter: TransactionFilter,
    accounts: List<Account>,
    categoriesList: List<Category>
) {
    var showAmortization by remember { mutableStateOf(false) }
    var showAllAmortization by remember { mutableStateOf(false) }

    val amortizationSchedule = remember(loan, showAmortization) {
        if (!showAmortization || loan.interestRate == null) emptyList()
        else {
            val monthlyPayment =
                loan.principalAmount.divide(BigDecimal(12), 2, RoundingMode.HALF_UP)
            LoanService.getAmortizationSchedule(loan, monthlyPayment, loan.startDate)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = AeonTheme.dimensions.screenPadding,
            end = AeonTheme.dimensions.screenPadding,
            top = AeonTheme.dimensions.screenPadding,
            bottom = scrollBottomPadding()
        ),
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
        }

        if (showAmortization && amortizationSchedule.isNotEmpty()) {
            val displayRows =
                if (showAllAmortization) amortizationSchedule else amortizationSchedule.take(12)
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
        color = AppColors.cardSurfaceProminent.copy(alpha = 0.3f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(0.5.dp, AppColors.divider.copy(alpha = 0.1f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = AppColors.primary.copy(alpha = 0.1f),
                        shape = CircleShape,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                entry.period.toString(),
                                style = AppTextStyles.caption.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                color = AppColors.primary
                            )
                        }
                    }
                    Text(
                        dateStr,
                        style = AppTextStyles.managementItemTitle.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Text(
                    text = "Balance: ${entry.balance.setScale(2, RoundingMode.HALF_UP).toPlainString()} $currency",
                    style = AppTextStyles.caption.copy(fontWeight = FontWeight.Medium),
                    color = AppColors.primary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AmortizationDetailItem(
                    label = "PRINCIPAL",
                    value = entry.principal.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                    color = AppColors.contentPrimary,
                    modifier = Modifier.weight(1f)
                )
                AmortizationDetailItem(
                    label = "INTEREST",
                    value = entry.interest.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                    color = AppColors.error,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun AmortizationDetailItem(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            label,
            style = AppTextStyles.caption.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
                letterSpacing = 1.sp
            ),
            color = AppColors.contentSecondary.copy(alpha = 0.5f)
        )
        Text(
            value,
            style = AppTextStyles.caption.copy(fontWeight = FontWeight.Bold),
            color = color
        )
    }
}
