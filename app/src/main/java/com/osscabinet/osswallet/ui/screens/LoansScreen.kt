package com.osscabinet.ossWallet.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
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
import com.osscabinet.ossWallet.ui.viewmodel.LoanViewModel
import com.osscabinet.ossWallet.ui.components.navigation.UnifiedPillNavigation

private val LentColor = Color(0xFF004D40)
private val BorrowedColor = Color(0xFFE65100)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoansScreen(
    viewModel: LoanViewModel,
    onAddClick: () -> Unit,
    onLoanClick: (Long) -> Unit,
    onBackClick: () -> Unit
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
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
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
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Rounded.AccountBalance,
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp),
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
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (active.isNotEmpty()) {
                        item {
                            LoanSectionHeader(
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
                            LoanSectionHeader(
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
private fun LoanSectionHeader(title: String, sectionColor: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
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
private fun LoanItem(
    loan: Loan,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSettle: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val typeLabel = if (loan.loanType == LoanType.LENT) "Lent to" else "Borrowed from"
    val accentColor = if (loan.loanType == LoanType.LENT) LentColor else BorrowedColor
    val loanIcon = if (loan.loanType == LoanType.LENT) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward

    Surface(
        onClick = onEdit,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Left icon circle
            Surface(
                shape = CircleShape,
                color = accentColor.copy(alpha = 0.1f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        loanIcon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = accentColor
                    )
                }
            }
            // Center info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    loan.name,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "$typeLabel ${loan.lenderOrBorrowerName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
            // Trailing: amount + status badge
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
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
            }
            // MoreVert menu
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Rounded.MoreVert,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    if (loan.status == LoanStatus.ACTIVE) {
                        DropdownMenuItem(
                            text = { Text("Mark Settled") },
                            onClick = { showMenu = false; onSettle() }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        onClick = { showMenu = false; onDelete() }
                    )
                }
            }
        }
    }
}
