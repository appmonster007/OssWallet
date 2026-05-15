package com.osscabinet.ossWallet.ui.screens.loans.list

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.domain.model.Loan
import com.osscabinet.ossWallet.domain.model.LoanStatus
import com.osscabinet.ossWallet.domain.model.LoanType
import com.osscabinet.ossWallet.ui.components.management.ManagementListItem
import com.osscabinet.ossWallet.ui.components.management.ManagementSectionHeader
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.screens.loans.LoanViewModel
import com.osscabinet.ossWallet.ui.components.navigation.UnifiedPillNavigation

private val LentColor = Color(0xFF004D40)
private val BorrowedColor = Color(0xFFE65100)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanListScreen(
    onBackClick: () -> Unit,
    viewModel: LoanViewModel,
    onAddClick: () -> Unit,
    onLoanClick: (Long) -> Unit
) {
    val loans by viewModel.loans.collectAsStateWithLifecycle()
    val active = loans.filter { it.status == LoanStatus.ACTIVE }
    val settled = loans.filter { it.status == LoanStatus.SETTLED }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Loans",
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (loans.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            modifier = Modifier.size(AeonTheme.dimensions.iconSize3xl)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Rounded.AccountBalance,
                                    contentDescription = null,
                                    modifier = Modifier.size(AeonTheme.dimensions.iconSizeXl),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Text(
                            "No Loans Tracked",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Track money you've lent or borrowed",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(AeonTheme.dimensions.screenPadding),
                    verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.listPadding)
                ) {
                    if (active.isNotEmpty()) {
                        item {
                            ManagementSectionHeader(
                                title = "ACTIVE",
                                sectionColor = MaterialTheme.colorScheme.primary
                            )
                        }
                        items(active, key = { it.id }) { loan ->
                            LoanItem(
                                loan = loan,
                                onEdit = { onLoanClick(loan.id) },
                                onDelete = { viewModel.deleteLoan(loan.id) },
                                onSettle = { viewModel.settleLoan(loan) }
                            )
                        }
                    }
                    if (settled.isNotEmpty()) {
                        item {
                            ManagementSectionHeader(
                                title = "SETTLED",
                                sectionColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }
                        items(settled, key = { it.id }) { loan ->
                            LoanItem(
                                loan = loan,
                                onEdit = { onLoanClick(loan.id) },
                                onDelete = { viewModel.deleteLoan(loan.id) },
                                onSettle = {}
                            )
                        }
                    }
                }
            }

            UnifiedPillNavigation(
                selectedTab = 0,
                onTabSelected = {},
                tabs = emptyList(),
                onFabClick = onAddClick,
                fabIcon = Icons.Rounded.AccountBalance,
                showFab = true
            )
        }
    }
}

@Composable
private fun LoanItem(
    loan: Loan,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSettle: () -> Unit
) {
    val typeLabel = if (loan.loanType == LoanType.LENT) "Lent to" else "Borrowed from"
    val accentColor = if (loan.loanType == LoanType.LENT) LentColor else BorrowedColor
    val loanIcon = if (loan.loanType == LoanType.LENT) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward

    ManagementListItem(
        onClick = onEdit,
        icon = loanIcon,
        iconTint = accentColor,
        title = loan.name,
        subtitle = "$typeLabel ${loan.lenderOrBorrowerName}",
        trailingContent = {
            Text(
                "${loan.principalAmount.toPlainString()} ${loan.currency}",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = accentColor
            )
            Surface(
                shape = CircleShape,
                color = accentColor.copy(alpha = 0.1f),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    if (loan.status == LoanStatus.ACTIVE) "Active" else "Settled",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = accentColor,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        },
        menuItems = { onDismiss ->
            if (loan.status == LoanStatus.ACTIVE) {
                DropdownMenuItem(
                    text = { Text("Mark Settled") },
                    onClick = { onDismiss(); onSettle() }
                )
            }
            DropdownMenuItem(
                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                onClick = { onDismiss(); onDelete() }
            )
        }
    )
}
