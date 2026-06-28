package com.osscabinet.ossWallet.ui.components.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.settings.FastScrollSide
import com.osscabinet.ossWallet.ui.screens.transactions.model.TransactionListUiState
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.AeonSize

@Composable
fun RecentActivitiesScreen(
    listState: TransactionListUiState,
    onRecordsClick: (TransactionFilter) -> Unit,
    modifier: Modifier = Modifier,
    lazyListState: LazyListState = rememberLazyListState(),
    onTransactionEvent: (TransactionItemEvent) -> Unit = {},
    baseCurrency: String = "USD",
    contentPadding: PaddingValues = PaddingValues(bottom = AeonSize.D24.value),
    revealedCardIds: List<String> = emptyList(),
    filter: TransactionFilter = TransactionFilter(),
    showHeader: Boolean = true,
    headerTitle: String = "Recent Activities",
    showHeaderOpeningBalance: Boolean = false,
    showHeaderClosingBalance: Boolean = true,
    showItemOpeningBalance: Boolean = false,
    showItemClosingBalance: Boolean = true,
    showCurrencyBreakdown: Boolean = true,
    allTxnBreakdown: Boolean = true,
    useNoteAsHeader: Boolean = false,
    showAccount: Boolean = true,
    showCategory: Boolean = true,
    showNote: Boolean = true,
    showTags: Boolean = true,
    showTimestamp: Boolean = true,
    showIcon: Boolean = true,
    tagColors: Map<String, Int> = emptyMap(),
    hasStickyHeader: Boolean = true,
    scrollToId: String? = null,
    highlightedId: String? = null,
    onScrollComplete: () -> Unit = {},
    fastScrollSide: FastScrollSide = FastScrollSide.RIGHT
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        if (showHeader) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AeonSize.D16.value, vertical = AeonSize.D12.value),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AeonSize.D8.value)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.History,
                        contentDescription = null,
                        tint = AppColors.primary,
                        modifier = Modifier.size(AeonSize.D20.value)
                    )
                    Text(
                        text = headerTitle,
                        style = AppTextStyles.screenSubTitle.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                TextButton(
                    onClick = { onRecordsClick(filter) },
                    contentPadding = PaddingValues(horizontal = AeonSize.D8.value, vertical = 0.dp),
                    modifier = Modifier.height(AeonSize.D32.value)
                ) {
                    Text(
                        text = "VIEW ALL",
                        style = AppTextStyles.transactionSwipeAction.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            fontSize = 12.sp
                        )
                    )
                }
            }
        }

        if (listState.isEmpty) {
            TransactionListEmptyState(
                modifier = Modifier.padding(vertical = 32.dp)
            )
        } else {
            AeonTransactionList(
                listState = listState,
                onTransactionEvent = onTransactionEvent,
                lazyListState = lazyListState,
                modifier = Modifier.weight(1f),
                contentPadding = contentPadding,
                useNoteAsHeader = useNoteAsHeader,
                showHeaderOpeningBalance = showHeaderOpeningBalance,
                showHeaderClosingBalance = showHeaderClosingBalance,
                showItemOpeningBalance = showItemOpeningBalance,
                showItemClosingBalance = showItemClosingBalance,
                showCurrencyBreakdown = showCurrencyBreakdown,
                allTxnBreakdown = allTxnBreakdown,
                showAccount = showAccount,
                showCategory = showCategory,
                showNote = showNote,
                showTags = showTags,
                showTimestamp = showTimestamp,
                showIcon = showIcon,
                baseCurrency = baseCurrency,
                revealedCardIds = revealedCardIds,
                highlightedId = highlightedId,
                scrollToId = scrollToId,
                onScrollComplete = onScrollComplete,
                hasStickyHeader = hasStickyHeader,
                fastScrollSide = fastScrollSide
            )
        }
    }
}
