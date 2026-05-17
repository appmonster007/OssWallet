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
import com.osscabinet.ossWallet.ui.components.transactions.AdvancedFilterSection
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
    filterContext: FilterSelectionContext = FilterSelectionContext(),
    filterRules: CardFilterRules = CardFilterRules.DEFAULT,
    baseFilter: TransactionFilter = TransactionFilter()
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = AppColors.cardSurfaceProminent,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                title,
                style = AppTextStyles.heroAmount,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Surface(
                color = AppColors.insetSurface.copy(alpha = 0.3f),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (filterRules.allowFutureTransactionsToggle) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Include future transactions", style = AppTextStyles.chartTooltipValue)
                                Text(
                                    "Show scheduled entries in this card",
                                    style = AppTextStyles.caption,
                                    color = AppColors.border
                                )
                            }
                            Switch(
                                checked = !filter.excludeFuture,
                                onCheckedChange = { include ->
                                    onFilterChange(filter.copy(excludeFuture = !include))
                                }
                            )
                        }
                        HorizontalDivider(thickness = 0.5.dp, color = AppColors.divider.copy(alpha = 0.5f))
                    }
                    AdvancedFilterSection(
                        filter = filter,
                        onFilterChange = onFilterChange,
                        accounts = filterContext.accounts,
                        allCategories = filterContext.allCategories,
                        savedFilters = filterContext.savedFilters,
                        filterRules = filterRules,
                        baseFilter = baseFilter
                    )
                }
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Done")
            }
        }
    }
}
