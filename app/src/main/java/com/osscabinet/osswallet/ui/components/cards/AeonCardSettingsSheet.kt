package com.osscabinet.ossWallet.ui.components.cards

import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import com.osscabinet.ossWallet.ui.theme.LocalGlassmorphismEnabled
import com.osscabinet.ossWallet.domain.card.CardFilterRules
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.ui.components.transactions.FilterEvent
import com.osscabinet.ossWallet.ui.components.transactions.FilterSheetContent
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme

/**
 * Standardized bottom sheet for configuring card filters.
 *
 * Uses [Modifier.statusBarsPadding] on the ModalBottomSheet modifier so the sheet stops at
 * the status bar when fully expanded. [contentWindowInsets] is set to [WindowInsets.navigationBars]
 * so M3 natively applies nav bar bottom padding to the content.
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
    val glassEnabled = LocalGlassmorphismEnabled.current
    val glassActive = glassEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = if (glassActive) AppColors.sheetSurface.copy(alpha = 0.5f)
                         else AppColors.sheetSurface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        contentWindowInsets = { WindowInsets.navigationBars },
        modifier = Modifier.statusBarsPadding()
    ) {
        val view = LocalView.current
        SideEffect {
            if (glassActive) {
                (view.parent as? DialogWindowProvider)?.window?.setBackgroundBlurRadius(120)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
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
