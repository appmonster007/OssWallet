package com.osscabinet.ossWallet.ui.screens.dashboard.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.data.local.TransactionGroupingType
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import com.osscabinet.ossWallet.ui.components.transactions.TransactionItemEvent
import com.osscabinet.ossWallet.domain.model.TransactionListState
import com.osscabinet.ossWallet.domain.model.TransactionWithBalance
import com.osscabinet.ossWallet.domain.model.settings.RecentActivitiesCardConfig
import com.osscabinet.ossWallet.ui.components.cards.AeonExpressiveCard
import com.osscabinet.ossWallet.ui.components.cards.CardCallbacks
import com.osscabinet.ossWallet.ui.components.cards.FilterSelectionContext
import com.osscabinet.ossWallet.ui.components.transactions.RecentActivitiesScreen
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import java.math.BigDecimal
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles

@Composable
fun RecentActivitiesCard(
    listState: TransactionListState,
    config: RecentActivitiesCardConfig,
    callbacks: CardCallbacks.RecentActivities,
    modifier: Modifier = Modifier,
    headerModifier: Modifier = Modifier,
    baseCurrency: String = "USD",
    exchangeRates: Map<String, BigDecimal> = emptyMap(),
    filterContext: FilterSelectionContext = FilterSelectionContext(),
    baseFilter: TransactionFilter = TransactionFilter(),
    inheritedFilter: TransactionFilter = TransactionFilter(),
    showFilterButton: Boolean = true,
) {
    val displayFilter = if (inheritedFilter.timelineRange !is com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.AllTime)
        config.filter.copy(timelineRange = inheritedFilter.timelineRange)
    else config.filter

    AeonExpressiveCard(
        title = config.title,
        icon = Icons.Rounded.History,
        modifier = modifier,
        headerModifier = headerModifier,
        filterEnabled = showFilterButton,
        filter = displayFilter,
        baseFilter = baseFilter,
        filterRules = config.filterRules,
        filterContext = filterContext,
        onClick = { callbacks.onRecordsClick(config.filter) },
        onFilterChange = { callbacks.onUpdate(config.copy(filter = it)) },
        onSaveFilterClick = callbacks.onSaveFilterClick,
        displaySettings = { onDismiss ->
            RecentActivitiesDisplaySettingsDialog(config = config, onUpdate = callbacks.onUpdate, onDelete = callbacks.onDelete, onDismiss = onDismiss)
        }
    ) {
        RecentActivitiesScreen(
            listState = listState,
            onRecordsClick = callbacks.onRecordsClick,
            baseCurrency = baseCurrency,
            modifier = Modifier.height(300.dp),
            showHeader = false,
            showCurrencyBreakdown = config.showCurrencyBreakdown,
            revealedCardIds = emptyList(),
            onTransactionEvent = { event ->
                when (event) {
                    is TransactionItemEvent.Click -> callbacks.onTransactionClick(event.id)
                    is TransactionItemEvent.Expand -> callbacks.onItemExpanded(event.id)
                    is TransactionItemEvent.Collapse -> callbacks.onItemCollapsed(event.id)
                    is TransactionItemEvent.StatusChange -> callbacks.onStatusChange(event.id, event.status)
                    is TransactionItemEvent.Clone -> callbacks.onClone(event.id)
                    is TransactionItemEvent.Delete -> callbacks.onDeleteTransaction(event.id)
                    is TransactionItemEvent.Split -> callbacks.onSplit(event.id)
                    is TransactionItemEvent.FullSwipeRight -> callbacks.onFullSwipeRight(event.id)
                }
            }
        )
    }
}

@Composable
private fun RecentActivitiesDisplaySettingsDialog(
    config: RecentActivitiesCardConfig,
    onUpdate: (RecentActivitiesCardConfig) -> Unit,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        dismissButton = {
            TextButton(
                onClick = {
                    onDelete()
                    onDismiss()
                },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = AppColors.error
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete card",
                    modifier = Modifier.size(AeonTheme.dimensions.iconSizeSm)
                )
                Spacer(modifier = Modifier.width(AeonTheme.dimensions.spacingXs))
                Text("Delete")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        },
        title = {
            Text(
                "Recent Activities Display",
                style = AppTextStyles.cardTitle
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingLg)) {
                // Group By Selection
                Surface(
                    color = AppColors.insetSurface.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(AeonTheme.dimensions.containerCornerRadius),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(AeonTheme.dimensions.cardPadding),
                        verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)
                    ) {
                        Text(
                            "GROUP BY",
                            style = AppTextStyles.transactionSwipeAction.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = AppColors.primary
                        )

                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            TransactionGroupingType.entries.forEachIndexed { index, type ->
                                SegmentedButton(
                                    selected = config.groupBy == type,
                                    onClick = { onUpdate(config.copy(groupBy = type)) },
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = TransactionGroupingType.entries.size
                                    ),
                                    label = {
                                        Text(
                                            when (type) {
                                                TransactionGroupingType.BY_DAY -> "Day"
                                                TransactionGroupingType.BY_WEEK -> "Week"
                                            }
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        shape = RoundedCornerShape(AeonTheme.dimensions.dialogCornerRadius),
        containerColor = AppColors.sheetSurface
    )
}
