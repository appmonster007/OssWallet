package com.osscabinet.ossWallet.ui.screens.loans.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.domain.model.AmortizationEntry
import com.osscabinet.ossWallet.domain.model.LoanStatus
import com.osscabinet.ossWallet.domain.model.LoanType
import com.osscabinet.ossWallet.domain.model.TransactionListState
import com.osscabinet.ossWallet.domain.service.filter.TransactionFilterService
import com.osscabinet.ossWallet.domain.service.management.LoanService
import com.osscabinet.ossWallet.domain.service.timeline.TimelineService
import com.osscabinet.ossWallet.ui.components.cards.AeonExpressiveCard
import com.osscabinet.ossWallet.ui.components.navigation.NavigationTab
import com.osscabinet.ossWallet.ui.components.navigation.UnifiedPillNavigation
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.ui.components.transactions.RecentActivitiesScreen
import com.osscabinet.ossWallet.ui.screens.loans.detail.components.LoanHeroCard
import com.osscabinet.ossWallet.ui.components.management.ManagementSectionHeader
import com.osscabinet.ossWallet.ui.components.management.ScopeChip
import com.osscabinet.ossWallet.ui.components.management.ScopeSection
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.screens.accounts.AccountViewModel
import com.osscabinet.ossWallet.ui.screens.loans.LoanViewModel
import com.osscabinet.ossWallet.ui.screens.transactions.TransactionViewModel
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val TealHealthy = Color(0xFF004D40)
private val RedWarning = Color(0xFFB3261E)
private val LentColor = Color(0xFF004D40)
private val BorrowedColor = Color(0xFFE65100)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanDetailScreen(
    loanId: Long,
    viewModel: LoanViewModel,
    accountViewModel: AccountViewModel,
    transactionViewModel: TransactionViewModel,
    baseCurrency: String,
    precision: Int,
    onEditClick: () -> Unit,
    onBackClick: () -> Unit,
    onTransactionClick: (Long) -> Unit,
    onCloneTransactionClick: (Long) -> Unit = {},
    onSplitTransactionClick: (Long) -> Unit = {},
    onRecordsClick: (TransactionFilter) -> Unit
) {
    val loans by viewModel.loans.collectAsStateWithLifecycle()
    val loan = remember(loanId, loans) { loans.find { it.id == loanId } }
    
    LaunchedEffect(loans) {
        if (loans.isNotEmpty() && loan == null) {
            onBackClick()
        }
    }

    val allTransactions by viewModel.transactions.collectAsStateWithLifecycle()
    val exchangeRates by viewModel.exchangeRates.collectAsStateWithLifecycle()
    val allCategories by viewModel.categories.collectAsStateWithLifecycle()
    val now = System.currentTimeMillis()

    val summary = remember(loan, allTransactions, exchangeRates, now) {
        if (loan == null) return@remember null
        LoanService.buildSummaryFromTransactions(loan, allTransactions, exchangeRates, now, precision, allCategories)
    }
    
    if (loan == null || summary == null) return

    if (loan.principalAmount > BigDecimal.ZERO)
        (summary.totalPaid.divide(loan.principalAmount, 4, RoundingMode.HALF_UP)).toFloat()
            .coerceIn(0f, 1f)
    else 0f
    if (loan.loanType == LoanType.LENT) LentColor else BorrowedColor


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

    val absoluteTransactionsWithBalance by transactionViewModel.absoluteTransactionsWithBalance.collectAsStateWithLifecycle()

    // Get loan-related transactions for Activity tab
    val loanTransactions =
        remember(loan, absoluteTransactionsWithBalance) {
            val loanFilter = TransactionFilter(
                categoryIds = loan.categoryIds,
                tags = loan.tags,
                accountIds = if (loan.linkedAccountId.isBlank()) emptySet() else setOf(loan.linkedAccountId),
                paymentMethods = loan.paymentMethods,
                statuses = loan.statuses,
                linkedTransactionIds = loan.linkedTransactionIds
            )
            absoluteTransactionsWithBalance.filter { item ->
                TransactionFilterService.matches(item.transaction, loanFilter)
            }
        }

    val accounts by accountViewModel.accounts.collectAsStateWithLifecycle()
    val categoriesList by transactionViewModel.categories.collectAsStateWithLifecycle()
    val revealedCardIds by transactionViewModel.revealedCardIdsList.collectAsStateWithLifecycle()

    val selectedAccount = remember(accounts, loan.linkedAccountId) {
        accounts.find { it.id == loan.linkedAccountId }
    }
    val selectedCategories = remember(categoriesList, loan.categoryIds) {
        if (loan.categoryIds.isEmpty()) emptyList()
        else categoriesList.filter { it.id in loan.categoryIds }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        loan.name,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
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
                        allCategoriesCount = categoriesList.size,
                        precision = precision
                    )

                    1 -> {
                        if (loanTransactions.isEmpty()) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(AeonTheme.dimensions.screenPadding),
                                shape = RoundedCornerShape(AeonTheme.dimensions.cardCornerRadius),
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                border = BorderStroke(
                                    0.5.dp,
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)
                                )
                            ) {
                                Box(Modifier.padding(AeonTheme.dimensions.spacing2xl), contentAlignment = Alignment.Center) {
                                    Text(
                                        "No related transactions",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                            alpha = 0.6f
                                        )
                                    )
                                }
                            }
                        } else {
                            val loanListState = remember(loanTransactions, absoluteTransactionsWithBalance, precision, baseCurrency) {
                                val groups = TimelineService.getGroupedData(
                                    transactions = loanTransactions,
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
                                listState = loanListState,
                                onRecordsClick = { _ ->
                                    onRecordsClick(
                                        TransactionFilter(
                                            accountIds = if (loan.linkedAccountId.isBlank()) emptySet() else setOf(
                                                loan.linkedAccountId
                                            ),
                                            categoryIds = loan.categoryIds,
                                            tags = loan.tags
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

@Composable
private fun LoanOverviewTab(
    loan: com.osscabinet.ossWallet.domain.model.Loan,
    summary: com.osscabinet.ossWallet.domain.model.LoanSummary,
    selectedAccount: com.osscabinet.ossWallet.domain.model.Account?,
    selectedCategories: List<com.osscabinet.ossWallet.domain.model.Category>,
    allCategoriesCount: Int,
    precision: Int
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
        contentPadding = PaddingValues(AeonTheme.dimensions.screenPadding),
        verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingLg)
    ) {
        item {
            LoanHeroCard(
                loan = loan,
                summary = summary,
                precision = precision,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // SCOPE & CONTEXT Card
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
                                    MaterialTheme.colorScheme.primary
                                }
                                ScopeChip(text = account.name, color = color)
                            }
                        } ?: emptyList()
                    )
                    ScopeSection(
                        label = "CATEGORIES",
                        icon = Icons.Rounded.Category,
                        allText = "All Categories",
                        isEmpty = loan.categoryIds.isEmpty() || (allCategoriesCount > 0 && loan.categoryIds.size == allCategoriesCount),
                        items = selectedCategories.map { category ->
                            {
                                val color = Color(category.colorHex)
                                ScopeChip(text = category.name, color = color)
                            }
                        }
                    )
                    ScopeSection(
                        label = "TAGS",
                        icon = Icons.AutoMirrored.Rounded.Label,
                        allText = "No Tags",
                        isEmpty = loan.tags.isEmpty(),
                        items = loan.tags.map { tag ->
                            {
                                ScopeChip(text = tag, color = MaterialTheme.colorScheme.secondary)
                            }
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
                        sectionColor = MaterialTheme.colorScheme.primary
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
private fun DataRow(
    label: String,
    value: String,
    valueColor: Color? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = valueColor ?: MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun AmortizationRow(entry: AmortizationEntry, currency: String) {
    val dateStr = remember(entry.date) {
        SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(Date(entry.date))
    }
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.2f),
        shape = RoundedCornerShape(AeonTheme.dimensions.listPadding),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.08f)),
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
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.width(AeonTheme.dimensions.spacingXl),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                dateStr,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.width(AeonTheme.dimensions.iconSize3xl)
            )
            Text(
                entry.principal.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                entry.interest.toPlainString(),
                style = MaterialTheme.typography.bodySmall,
                color = RedWarning
            )
            Text(
                entry.balance.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                style = MaterialTheme.typography.bodySmall,
                color = TealHealthy
            )
        }
    }
}


