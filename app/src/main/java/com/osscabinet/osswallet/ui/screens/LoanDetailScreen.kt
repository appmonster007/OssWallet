package com.osscabinet.ossWallet.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.domain.model.*
import com.osscabinet.ossWallet.domain.usecase.LoanAnalysis
import com.osscabinet.ossWallet.ui.components.*
import com.osscabinet.ossWallet.ui.components.transactions.AeonTransactionItem
import com.osscabinet.ossWallet.ui.util.formatCurrencyRight
import com.osscabinet.ossWallet.ui.viewmodel.LoanViewModel
import com.osscabinet.ossWallet.ui.viewmodel.TransactionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanDetailScreen(
    accountId: String,
    loanViewModel: LoanViewModel,
    transactionViewModel: TransactionViewModel,
    onEditClick: (String) -> Unit,
    onBackClick: () -> Unit
) {
    val state by loanViewModel.state.collectAsStateWithLifecycle()
    val allTransactionsWithBalance by transactionViewModel.transactionsWithBalance.collectAsStateWithLifecycle()

    val analysis = remember(accountId, state.loans) {
        state.loans.find { it.loan.account.id == accountId }
    }

    if (analysis == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val loan = analysis.loan

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(loan.title, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onEditClick(loan.id) }) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Edit")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                LoanAnalysisCard(analysis)
            }

            if (analysis.repayments.isNotEmpty()) {
                item {
                    AeonFlowSectionHeader(title = "REPAYMENTS")
                }
                items(analysis.repayments.sortedByDescending { it.timestamp }) { tx ->
                    // Find balance info from allTransactionsWithBalance
                    val txWithBalance = allTransactionsWithBalance.find { it.transaction.id == tx.id }
                    
                    AeonTransactionItem(
                        category = tx.category,
                        account = tx.accountId,
                        amount = formatCurrencyRight(tx.amount, tx.currency),
                        balanceBefore = txWithBalance?.balanceBefore?.let { formatCurrencyRight(it, tx.currency) } ?: "0.00",
                        balanceAfter = txWithBalance?.balanceAfter?.let { formatCurrencyRight(it, tx.currency) } ?: "0.00",
                        type = CashFlowType.INCOME, // Repayment is income to the loan account
                        timestamp = formatDateTime(tx.timestamp),
                        note = tx.note,
                        tags = tx.tags,
                        status = tx.status,
                        onClick = {}
                    )
                }
            }

            if (analysis.disbursements.isNotEmpty()) {
                item {
                    AeonFlowSectionHeader(title = "DISBURSEMENTS")
                }
                items(analysis.disbursements.sortedByDescending { it.timestamp }) { tx ->
                    val txWithBalance = allTransactionsWithBalance.find { it.transaction.id == tx.id }

                    AeonTransactionItem(
                        category = tx.category,
                        account = tx.accountId,
                        amount = formatCurrencyRight(tx.amount, tx.currency),
                        balanceBefore = txWithBalance?.balanceBefore?.let { formatCurrencyRight(it, tx.currency) } ?: "0.00",
                        balanceAfter = txWithBalance?.balanceAfter?.let { formatCurrencyRight(it, tx.currency) } ?: "0.00",
                        type = CashFlowType.EXPENSE, // Disbursement is expense from the loan account
                        timestamp = formatDateTime(tx.timestamp),
                        note = tx.note,
                        tags = tx.tags,
                        status = tx.status,
                        onClick = {}
                    )
                }
            }
        }
    }
}

@Composable
fun LoanAnalysisCard(analysis: LoanAnalysis) {
    val loan = analysis.loan
    Surface(
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("REMAINING BALANCE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    Text(
                        formatCurrencyRight(loan.currentBalance, loan.currency),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Text(
                        "${loan.interestRate}% APR",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            AeonBudgetProgress(
                label = "Principal Paid",
                current = analysis.principalPaid,
                total = loan.principal,
                color = MaterialTheme.colorScheme.primary,
                baseCurrency = loan.currency,
                showLabels = true
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text("INTEREST PAID", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    Text(
                        formatCurrencyRight(analysis.interestPaid, loan.currency),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("MATURITY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    Text(
                        "${analysis.remainingMaturityDays} DAYS LEFT",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (analysis.remainingMaturityDays < 30) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            
            Text(
                text = "Total Principal: ${formatCurrencyRight(loan.principal, loan.currency)}\nMaturity Date: ${formatDate(loan.maturityDate)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
