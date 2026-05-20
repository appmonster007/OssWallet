package com.osscabinet.ossWallet.ui.components.cards

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.ui.components.transactions.FilterEvent
import com.osscabinet.ossWallet.ui.components.transactions.FilterSheetContent
import com.osscabinet.ossWallet.domain.card.CardFilterRules
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles

/**
 * Standardised bottom sheet for configuring card filters.
 * Scoped to filtering only — card deletion lives in the display settings dialog.
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
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = AppColors.sheetSurface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                title,
                style = AppTextStyles.heroAmount,
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .align(Alignment.CenterHorizontally)
            )
            
            FilterSheetContent(
                filter = filter,
                accounts = filterContext.accounts,
                allCategories = filterContext.allCategories,
                allTags = filterContext.allTags,
                onSearchTransactions = filterContext.onSearchTransactions,
                savedFilters = filterContext.savedFilters,
                onFilterEvent = { event ->
                    when (event) {
                        is FilterEvent.Apply -> onFilterChange(event.filter)
                        is FilterEvent.Reset -> onFilterChange(baseFilter)
                        else -> {}
                    }
                },
                onSaveFilterClick = onSaveFilterClick,
                onClose = onDismiss,
                backgroundColor = AppColors.transparent,
                filterRules = filterRules,
                baseFilter = baseFilter
            )
        }
    }
}
