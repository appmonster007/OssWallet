package com.osscabinet.ossWallet.ui.screens.loans.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.domain.model.Loan
import com.osscabinet.ossWallet.domain.model.LoanStatus
import com.osscabinet.ossWallet.domain.model.LoanType
import com.osscabinet.ossWallet.ui.components.AeonHazeProvider
import com.osscabinet.ossWallet.ui.components.AeonTopAppBar
import com.osscabinet.ossWallet.ui.components.AeonTopBarNavTitlePill
import com.osscabinet.ossWallet.ui.components.AeonTopBarSection
import com.osscabinet.ossWallet.ui.components.ContourBackground
import com.osscabinet.ossWallet.ui.components.management.ManagementListItem
import com.osscabinet.ossWallet.ui.components.management.ManagementSectionHeader
import com.osscabinet.ossWallet.ui.components.navigation.UnifiedPillNavigation
import com.osscabinet.ossWallet.ui.components.scrollBottomPadding
import com.osscabinet.ossWallet.ui.screens.loans.LoanViewModel
import com.osscabinet.ossWallet.ui.theme.AeonSize
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.HazeLayer
import dev.chrisbanes.haze.hazeSource
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.WarningColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanListScreen(
    onBackClick: () -> Unit,
    viewModel: LoanViewModel,
    onAddClick: () -> Unit,
    onLoanClick: (Long) -> Unit
) {
    val navigationBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val loans by viewModel.loans.collectAsStateWithLifecycle()
    val active = loans.filter { it.status == LoanStatus.ACTIVE }
    val settled = loans.filter { it.status == LoanStatus.SETTLED }

    AeonHazeProvider { hazeLayers ->
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                containerColor = Color.Transparent
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AppColors.screenBackground)
                        .hazeSource(hazeLayers.states[HazeLayer.Overlay])
                ) {
                    ContourBackground()

                    if (loans.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .statusBarsPadding()
                                .padding(top = AeonSize.D64.value),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = AppColors.primary.copy(alpha = 0.08f),
                                    modifier = Modifier.size(AeonTheme.dimensions.iconSize3xl)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Rounded.AccountBalance,
                                            contentDescription = null,
                                            modifier = Modifier.size(AeonTheme.dimensions.iconSizeXl),
                                            tint = AppColors.primary
                                        )
                                    }
                                }
                                Text(
                                    "No Loans Tracked",
                                    style = AppTextStyles.calculatorKey,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Track money you've lent or borrowed",
                                    style = AppTextStyles.transactionTitle,
                                    color = AppColors.contentSecondary.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = AeonTheme.dimensions.screenPadding,
                                end = AeonTheme.dimensions.screenPadding,
                                top = AeonSize.D64.value + AeonTheme.dimensions.screenPadding + 48.dp, // approx status bar
                                bottom = innerPadding.calculateBottomPadding() + scrollBottomPadding()
                            ),
                            verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.listPadding)
                        ) {
                            if (active.isNotEmpty()) {
                                item {
                                    ManagementSectionHeader(
                                        title = "ACTIVE",
                                        sectionColor = AppColors.primary
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
                                        sectionColor = AppColors.contentSecondary.copy(alpha = 0.4f)
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
                }
            }

            UnifiedPillNavigation(
                selectedTab = 0,
                onTabSelected = {},
                tabs = emptyList(),
                fabPaddingBottom = 16.dp + navigationBarHeight,
                onFabClick = onAddClick,
                fabIcon = Icons.Rounded.AccountBalance,
                showFab = true
            )

            AeonTopAppBar(
                left = AeonTopBarSection(
                    content = {
                        AeonTopBarNavTitlePill(
                            title = "Loans",
                            onBackClick = onBackClick
                        )
                    }
                )
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
    val accentColor = if (loan.loanType == LoanType.LENT) AppColors.primary else WarningColor
    val loanIcon =
        if (loan.loanType == LoanType.LENT) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward

    ManagementListItem(
        onClick = onEdit,
        icon = loanIcon,
        iconTint = accentColor,
        title = loan.name,
        subtitle = "$typeLabel ${loan.lenderOrBorrowerName}",
        trailingContent = {
            Text(
                "${loan.principalAmount.toPlainString()} ${loan.currency}",
                style = AppTextStyles.listItemTitle,
                color = accentColor
            )
            Surface(
                shape = CircleShape,
                color = accentColor.copy(alpha = 0.1f),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    if (loan.status == LoanStatus.ACTIVE) "Active" else "Settled",
                    style = AppTextStyles.formSectionLabel,
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
                text = { Text("Delete", color = AppColors.error) },
                onClick = { onDismiss(); onDelete() }
            )
        }
    )
}
