package com.osscabinet.ossWallet.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.domain.model.AmortizationEntry
import com.osscabinet.ossWallet.domain.model.LoanPayment
import com.osscabinet.ossWallet.domain.model.LoanStatus
import com.osscabinet.ossWallet.domain.model.LoanType
import com.osscabinet.ossWallet.domain.model.TransactionWithBalance
import com.osscabinet.ossWallet.domain.service.LoanService
import com.osscabinet.ossWallet.ui.components.AeonDateField
import com.osscabinet.ossWallet.ui.components.cards.AeonExpressiveCard
import com.osscabinet.ossWallet.ui.components.navigation.ExpressiveNavigationBar
import com.osscabinet.ossWallet.ui.components.navigation.NavigationTab
import com.osscabinet.ossWallet.ui.components.transactions.RecentActivitiesScreen
import com.osscabinet.ossWallet.ui.navigation.RecordsFilterContext
import com.osscabinet.ossWallet.ui.viewmodel.AccountViewModel
import com.osscabinet.ossWallet.ui.viewmodel.LoanViewModel
import com.osscabinet.ossWallet.ui.viewmodel.TransactionViewModel
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.*

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
    onRecordsClick: (RecordsFilterContext) -> Unit
) {
    val loans by viewModel.loans.collectAsStateWithLifecycle()
    val loan = remember(loanId, loans) { loans.find { it.id == loanId } } ?: return
    val allTransactions by viewModel.transactions.collectAsStateWithLifecycle()

    val now = System.currentTimeMillis()
    val summary = remember(loan, allTransactions, now) {
        LoanService.buildSummaryFromTransactions(loan, allTransactions, now, precision)
    }

    val progress = if (loan.principalAmount > BigDecimal.ZERO)
        (summary.totalPaid.divide(loan.principalAmount, 4, RoundingMode.HALF_UP)).toFloat().coerceIn(0f, 1f)
    else 0f
    val accentColor = if (loan.loanType == LoanType.LENT) LentColor else BorrowedColor


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

    // Get loan-related transactions for Activity tab
    val loanTransactions = remember(loan, viewModel.transactions.collectAsStateWithLifecycle().value) {
        viewModel.getLoanTransactions(loan).map {
            TransactionWithBalance(it, BigDecimal.ZERO, BigDecimal.ZERO)
        }
    }

    val accounts by accountViewModel.accounts.collectAsStateWithLifecycle()
    val accountNamesMap = remember(accounts) { accounts.associate { it.id to it.name } }
    val categoriesList by transactionViewModel.categories.collectAsStateWithLifecycle()
    val categoryMap = remember(categoriesList) { categoriesList.associateBy { it.id } }
    val revealedCardIds by transactionViewModel.revealedCardIdsList.collectAsStateWithLifecycle()

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
                0 -> LoanOverviewTab(
                    loan = loan,
                    summary = summary,
                    progress = progress,
                    accentColor = accentColor
                )
                1 -> {
                    if (loanTransactions.isEmpty()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
                        ) {
                            Box(Modifier.padding(32.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    "No related transactions",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }
                    } else {
                        RecentActivitiesScreen(
                            allTransactions = loanTransactions,
                            accountNamesMap = accountNamesMap,
                            categoryMap = categoryMap,
                            onRecordsClick = { _ ->
                                onRecordsClick(
                                    RecordsFilterContext.Custom(
                                        accountIds = if (loan.linkedAccountId.isBlank()) emptyList() else listOf(loan.linkedAccountId),
                                        categoryIds = loan.categoryIds.map { it.toString() },
                                        tags = loan.tags
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
                            onStatusChange = { id, status -> transactionViewModel.quickUpdateStatus(id, status) },
                            onClone = { },
                            onDelete = { transactionViewModel.quickDeleteTransaction(it) },
                            onSplit = { },
                            onFullSwipeRight = { transactionViewModel.toggleStatusClearedUncleared(it) },
                            applyFilter = false,
                            excludeFuture = false,
                            precision = precision
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoanOverviewTab(
    loan: com.osscabinet.ossWallet.domain.model.Loan,
    summary: com.osscabinet.ossWallet.domain.model.LoanSummary,
    progress: Float,
    accentColor: Color
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
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            AeonExpressiveCard(
                title = loan.name,
                subtitle = "${loan.loanType.name.lowercase().replaceFirstChar { it.uppercase() }} — ${loan.currency}",
                accentColor = accentColor,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Loan type badge
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = accentColor.copy(alpha = 0.1f),
                    border = BorderStroke(0.5.dp, accentColor.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (loan.loanType == LoanType.LENT) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = accentColor
                        )
                        Text(
                            if (loan.loanType == LoanType.LENT) "LENT" else "BORROWED",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = accentColor
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                // Remaining balance prominent
                Text(
                    summary.remainingBalance.toPlainString(),
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                    color = accentColor
                )
                Text(
                    "of ${loan.principalAmount.toPlainString()} ${loan.currency} remaining",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Spacer(Modifier.height(8.dp))
                // Data rows wrapped in surface
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DataRow(
                            label = "PRINCIPAL",
                            value = "${loan.principalAmount.toPlainString()} ${loan.currency}"
                        )
                        DataRow(
                            label = "TOTAL PAID",
                            value = "${summary.totalPaid.toPlainString()} ${loan.currency}"
                        )
                        DataRow(
                            label = "REMAINING",
                            value = "${summary.remainingBalance.toPlainString()} ${loan.currency}",
                            valueColor = accentColor
                        )
                        if (loan.interestRate != null) {
                            DataRow(
                                label = "INTEREST ACCRUED",
                                value = "${summary.interestAccrued.toPlainString()} ${loan.currency}"
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                // Progress bar
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = accentColor,
                    trackColor = accentColor.copy(alpha = 0.15f),
                    strokeCap = StrokeCap.Round
                )
            }
        }

        // SCOPE Card
        item {
            AeonExpressiveCard(
                title = "SCOPE",
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("LINKED ACCOUNT", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(
                    if (loan.linkedAccountId.isBlank()) "Not linked to any account" else "Linked to account",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(8.dp))
                Text("CATEGORIES", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(
                    if (loan.categoryIds.isEmpty()) "All Categories" else "${loan.categoryIds.size} Categories selected",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(8.dp))
                Text("LABELS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(
                    if (loan.tags.isEmpty()) "All Labels" else loan.tags.joinToString(),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        if (loan.interestRate != null) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LoanSectionHeaderInline(
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
private fun LoanSectionHeaderInline(title: String, sectionColor: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(sectionColor)
        )
        Text(
            title,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 11.sp,
                letterSpacing = 1.5.sp
            ),
            color = sectionColor
        )
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
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.08f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${entry.period}",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.width(28.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(dateStr, style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(70.dp))
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


