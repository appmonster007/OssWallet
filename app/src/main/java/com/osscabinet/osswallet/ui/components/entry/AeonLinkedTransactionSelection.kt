package com.osscabinet.ossWallet.ui.components.entry

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.R
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.CashFlowType
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.ui.components.AeonFullScreenDialog
import com.osscabinet.ossWallet.ui.components.AeonSelectionScreen
import com.osscabinet.ossWallet.ui.components.AppIconMap
import com.osscabinet.ossWallet.ui.components.AeonSearchField
import com.osscabinet.ossWallet.ui.components.transactions.AeonTransactionItem
import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter
import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter.formatCurrencyRight
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.delay
import java.util.*
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles

/**
 * Full-screen selection for Linking Transactions.
 */
@Composable
fun AeonLinkedTransactionSelection(
    onSearch: (String) -> Flow<List<Transaction>>,
    currentTransactionId: Long?,
    linkedIds: Set<Long>,
    accounts: List<Account>,
    categories: List<Category>,
    onLinkedTransactionsChanged: (Set<Long>) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedIds by remember { mutableStateOf(linkedIds) }
    var query by remember { mutableStateOf("") }
    var debouncedQuery by remember { mutableStateOf("") }

    LaunchedEffect(query) {
        if (query.isEmpty()) {
            debouncedQuery = ""
        } else {
            delay(300)
            debouncedQuery = query
        }
    }

    var searchResults by remember { mutableStateOf<List<Transaction>>(emptyList()) }
    val searchFlow = remember(debouncedQuery) { onSearch(debouncedQuery) }
    LaunchedEffect(debouncedQuery) {
        searchFlow.collect { results ->
            searchResults = results
        }
    }

    val filteredTransactions = remember(searchResults, currentTransactionId) {
        searchResults.filter { it.id != currentTransactionId }
    }

    AeonFullScreenDialog(onDismissRequest = onDismiss) {
        AeonSelectionScreen(
            title = stringResource(R.string.link_records_title),
            onBack = onDismiss,
            actions = {
                if (selectedIds.isNotEmpty()) {
                    TextButton(onClick = { selectedIds = emptySet() }) {
                        Text(
                            stringResource(R.string.action_clear_all).uppercase(),
                            style = AppTextStyles.listSectionHeader
                        )
                    }
                }
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = stringResource(R.string.link_records_hint),
                        style = AppTextStyles.transactionTitle,
                        color = AppColors.contentSecondary.copy(alpha = 0.7f),
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                    )
                    
                    AeonSearchField(
                        query = query,
                        onQueryChange = { query = it },
                        placeholder = stringResource(R.string.link_records_search_placeholder),
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            filteredTransactions,
                            key = { it.id }
                        ) { tx ->
                            val isSelected = tx.id in selectedIds
                            val account = accounts.find { it.id == tx.accountId }
                            val category = categories.find { it.id == tx.categoryId }
                            val icon = AppIconMap[category?.iconName] ?: Icons.Rounded.Category
                            val iconBg = category?.colorHex?.let { Color(it) }

                            Surface(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) AppColors.primary.copy(alpha = 0.08f)
                                else AppColors.panelSurface,
                                border = if (isSelected) BorderStroke(
                                    1.dp,
                                    AppColors.primary.copy(alpha = 0.3f)
                                ) else null
                            ) {
                                Box {
                                    AeonTransactionItem(
                                        category = category?.name ?: stringResource(R.string.unknown),
                                        account = account?.name ?: stringResource(R.string.unknown),
                                        amount = formatCurrencyRight(tx.amount, tx.currency, precision = CurrencyFormatter.precision),
                                        balanceBefore = "",
                                        balanceAfter = "",
                                        type = when (tx.transactionType) {
                                            TransactionType.TRANSFER -> CashFlowType.TRANSFER
                                            TransactionType.INCOME -> CashFlowType.INCOME
                                            else -> CashFlowType.EXPENSE
                                        },
                                        timestamp = com.osscabinet.ossWallet.ui.util.TimeUtils.formatDateTime(tx.timestamp),
                                        note = tx.note,
                                        tags = tx.tags,
                                        icon = icon,
                                        iconBg = iconBg,
                                        showOpeningBalance = false,
                                        showClosingBalance = false,
                                        showDividers = false,
                                        status = tx.status,
                                        containerColor = AppColors.transparent,
                                        modifier = Modifier.padding(end = if (isSelected) 48.dp else 0.dp),
                                        onClick = {
                                            selectedIds = if (isSelected) {
                                                selectedIds - tx.id
                                            } else {
                                                selectedIds + tx.id
                                            }
                                        }
                                    )

                                    if (isSelected) {
                                        Surface(
                                            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp).size(24.dp),
                                            shape = CircleShape,
                                            color = AppColors.primary,
                                            shadowElevation = 2.dp
                                        ) {
                                            Icon(
                                                Icons.Rounded.Check,
                                                contentDescription = stringResource(R.string.selected_desc),
                                                tint = AppColors.onPrimary,
                                                modifier = Modifier.padding(4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        
                        item { Spacer(modifier = Modifier.height(100.dp)) }
                    }
                }

                Surface(
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                    color = AppColors.panelSurface.copy(alpha = 0.95f)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp).windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        Button(
                            onClick = {
                                onLinkedTransactionsChanged(selectedIds)
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(
                                pluralStringResource(
                                    R.plurals.link_records_button_count,
                                    selectedIds.size,
                                    selectedIds.size
                                ),
                                style = AppTextStyles.listItemTitle
                            )
                        }
                    }
                }
            }
        }
    }
}
