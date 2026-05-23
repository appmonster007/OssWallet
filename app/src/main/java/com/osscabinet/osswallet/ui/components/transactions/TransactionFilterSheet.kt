package com.osscabinet.ossWallet.ui.components.transactions

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Filter
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Transaction filter sheet implementation.
 * Uses Modifier.statusBarsPadding() on the sheet to stop at the status bar.
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
    accountNames: Map<String, String> = emptyMap(),
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        containerColor = backgroundColor,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        scrimColor = Color.Black.copy(alpha = 0.5f),
        modifier = Modifier.statusBarsPadding(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            FilterSheetContent(
                filter = filter,
                accounts = accounts,
                allCategories = categories,
                onSearchTransactions = onSearchTransactions,
                onFilterEvent = onFilterEvent,
                onClose = onClose,
                allTags = allTags,
                savedFilters = savedFilters,
                onSaveFilterClick = onSaveFilterClick,
                backgroundColor = Color.Transparent,
                accountNames = accountNames,
                onAddTag = onAddTag,
                modifier = Modifier.weight(1f)
            )
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
