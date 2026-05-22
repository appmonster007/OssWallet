package com.osscabinet.ossWallet.ui.components.cards

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.osscabinet.ossWallet.domain.card.CardFilterRules
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.ui.components.transactions.FilterEvent
import com.osscabinet.ossWallet.ui.components.transactions.FilterSheetContent
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme

/**
 * Standardized bottom sheet for configuring card filters.
 * Uses Modifier.statusBarsPadding() on the sheet to stop at the status bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AeonCardSettingsSheet(
    title: String,
    filter: TransactionFilter,
    onFilterChange: (TransactionFilter) -> Unit,
    onDismiss: () -> Unit,
    onSaveFilterClick: (TransactionFilter) -> Unit = {},
    filterContext: FilterSelectionContext = FilterSelectionContext(),
    filterRules: CardFilterRules = CardFilterRules.DEFAULT,
    baseFilter: TransactionFilter = TransactionFilter()
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppColors.sheetSurface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = Modifier.statusBarsPadding(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(
                text = title,
                style = AppTextStyles.heroAmount,
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .align(Alignment.CenterHorizontally)
            )

            FilterSheetContent(
                filter = filter,
                accounts = filterContext.accounts,
                allCategories = filterContext.allCategories,
                onSearchTransactions = filterContext.onSearchTransactions,
                onFilterEvent = { event ->
                    when (event) {
                        is FilterEvent.Apply -> onFilterChange(event.filter)
                        is FilterEvent.Reset -> onFilterChange(baseFilter)
                        else -> {}
                    }
                },
                onClose = onDismiss,
                modifier = Modifier.weight(1f),
                allTags = filterContext.allTags,
                savedFilters = filterContext.savedFilters,
                onSaveFilterClick = onSaveFilterClick,
                backgroundColor = Color.Transparent,
                filterRules = filterRules,
                baseFilter = baseFilter
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AeonCardSettingsSheetPreview() {
    OssWalletTheme {
        AeonCardSettingsSheet(
            title = "Card Filter Settings",
            filter = TransactionFilter(),
            onFilterChange = {},
            onDismiss = {},
            filterContext = FilterSelectionContext()
        )
    }
}
