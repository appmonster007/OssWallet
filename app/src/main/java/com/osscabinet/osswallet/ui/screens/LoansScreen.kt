package com.osscabinet.ossWallet.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.usecase.LoanAnalysis
import com.osscabinet.ossWallet.ui.components.AeonFlowSectionHeader
import com.osscabinet.ossWallet.ui.components.AeonFloatingActionButton
import com.osscabinet.ossWallet.ui.components.AeonBudgetProgress
import com.osscabinet.ossWallet.ui.viewmodel.LoanViewModel
import com.osscabinet.ossWallet.ui.viewmodel.SettingsViewModel
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import com.osscabinet.ossWallet.ui.util.formatCurrencyRight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoansScreen(
    viewModel: LoanViewModel,
    settingsViewModel: SettingsViewModel,
    onBackClick: () -> Unit,
    onAddLoanClick: () -> Unit,
    onLoanClick: (String) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val baseCurrency by settingsViewModel.currency.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Loans & Debts", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            AeonFloatingActionButton(
                onClick = onAddLoanClick,
                icon = Icons.Rounded.Add,
                contentDescription = "New Loan"
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
                AeonFlowSectionHeader(title = "ACTIVE LIABILITIES")
            }

            if (state.loans.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Text("No active loans", color = MaterialTheme.colorScheme.outline)
                    }
                }
            } else {
                items(state.loans) { analysis ->
                    LoanAnalysisCard(
                        analysis = analysis,
                        baseCurrency = baseCurrency,
                        onClick = { onLoanClick(analysis.loan.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun LoanAnalysisCard(
    analysis: LoanAnalysis,
    baseCurrency: String,
    onClick: () -> Unit
) {
    val loan = analysis.loan
    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
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
                Text(
                    loan.title.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.outline
                )
                
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (loan.loanType == com.osscabinet.ossWallet.domain.model.LoanType.CREDIT) 
                           MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        if (loan.loanType == com.osscabinet.ossWallet.domain.model.LoanType.CREDIT) "DEBT" else "CREDIT",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (loan.loanType == com.osscabinet.ossWallet.domain.model.LoanType.CREDIT) 
                               MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
            }

            AeonBudgetProgress(
                label = "PRINCIPAL PAID",
                current = analysis.principalPaid,
                total = loan.principal,
                color = if (loan.loanType == com.osscabinet.ossWallet.domain.model.LoanType.CREDIT) 
                       MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                baseCurrency = baseCurrency,
                showLabels = true
            )
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "BALANCE: ${formatCurrencyRight(loan.currentBalance, loan.currency)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "${analysis.remainingMaturityDays} DAYS LEFT",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
