package com.osscabinet.ossWallet.ui.components.transactions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Filter
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import com.osscabinet.ossWallet.ui.theme.AppColors

/**
 * General purpose transaction filter sheet rewritten with best-practice architecture.
 * Ensures smooth scrolling and immersive status bar support.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionFilterSheet(
    filter: TransactionFilter,
    accounts: List<Account>,
    categories: List<Category>,
    allTags: List<com.osscabinet.ossWallet.domain.model.Tag> = emptyList(),
    onSearchTransactions: (String) -> Flow<List<Transaction>>,
    savedFilters: List<Filter> = emptyList(),
    onFilterEvent: (FilterEvent) -> Unit,
    onSaveFilterClick: (TransactionFilter) -> Unit = {},
    onAddTag: (String, Int) -> Unit = { _, _ -> },
    onClose: () -> Unit,
    backgroundColor: Color = AppColors.sheetSurface,
    showAccountFilter: Boolean = true,
    accountNames: Map<String, String> = emptyMap(),
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        containerColor = backgroundColor,
        dragHandle = {
            BottomSheetDefaults.DragHandle()
        },
        scrimColor = Color.Black.copy(alpha = 0.5f),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding(), // Ensures content stays below status bar at all times
            contentPadding = PaddingValues(
                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp
            )
        ) {
            item {
                FilterSheetContent(
                    filter = filter,
                    accounts = accounts,
                    allCategories = categories,
                    allTags = allTags,
                    onSearchTransactions = onSearchTransactions,
                    savedFilters = savedFilters,
                    onFilterEvent = onFilterEvent,
                    onSaveFilterClick = onSaveFilterClick,
                    onAddTag = onAddTag,
                    onClose = onClose,
                    backgroundColor = Color.Transparent,
                    showAccountFilter = showAccountFilter,
                    accountNames = accountNames,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TransactionFilterSheetPreview() {
    OssWalletTheme {
        TransactionFilterSheet(
            filter = TransactionFilter(),
            accounts = emptyList(),
            categories = emptyList(),
            onSearchTransactions = { _ -> flowOf(emptyList()) },
            onFilterEvent = {},
            onClose = {}
        )
    }
}
