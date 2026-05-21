package com.osscabinet.ossWallet.ui.components.transactions

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.AccountStructureType
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Filter
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.math.BigDecimal
import com.osscabinet.ossWallet.ui.theme.AppColors

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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        containerColor = backgroundColor,
        dragHandle = {
            FilterSheetDragHandle(
                isExpanded = true,
                onClick = onClose,
                backgroundColor = backgroundColor
            )
        },
        scrimColor = Color.Black.copy(alpha = 0.5f),
    ) {
        Box(modifier = Modifier.statusBarsPadding()) {
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
                backgroundColor = backgroundColor,
                showAccountFilter = showAccountFilter,
                accountNames = accountNames,
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

