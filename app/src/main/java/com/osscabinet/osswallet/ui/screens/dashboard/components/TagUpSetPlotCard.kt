package com.osscabinet.ossWallet.ui.screens.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.card.CardRenderData
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.ui.components.cards.AeonExpressiveCard
import com.osscabinet.ossWallet.ui.components.cards.CardCallbacks
import com.osscabinet.ossWallet.ui.components.cards.FilterSelectionContext
import com.osscabinet.ossWallet.ui.components.visualization.charts.AeonUpSetData
import com.osscabinet.ossWallet.ui.components.visualization.charts.AeonUpSetIntersection
import com.osscabinet.ossWallet.ui.components.visualization.charts.AeonUpSetPlot
import com.osscabinet.ossWallet.ui.components.visualization.charts.AeonUpSetSet
import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.ui.components.transactions.TransactionSummaryBreakdown
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import java.math.BigDecimal

@Composable
fun TagUpSetPlotCard(
    renderData: CardRenderData.TagUpSetPlot,
    callbacks: CardCallbacks.TagUpSetPlot = CardCallbacks.TagUpSetPlot(),
    filterContext: FilterSelectionContext = FilterSelectionContext(),
    baseFilter: TransactionFilter = TransactionFilter(),
    inheritedFilter: TransactionFilter = TransactionFilter(),
    onRecordsClick: (TransactionFilter) -> Unit = {},
    screenOwnsTimeline: Boolean = false,
    showFilterButton: Boolean = true,
    modifier: Modifier = Modifier,
    headerModifier: Modifier = Modifier,
) {
    val config = renderData.config
    var selectedIntersection by remember { mutableStateOf<AeonUpSetIntersection?>(null) }

    val displayFilter = if (screenOwnsTimeline || inheritedFilter.timelineRange !is com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.AllTime)
        config.filter.copy(timelineRange = inheritedFilter.timelineRange)
    else config.filter

    AeonExpressiveCard(
        title = config.title,
        icon = Icons.Default.Analytics,
        modifier = modifier,
        headerModifier = headerModifier,
        filterEnabled = showFilterButton,
        filter = displayFilter,
        baseFilter = baseFilter,
        filterRules = config.filterRules,
        filterContext = filterContext,
        onFilterChange = { newFilter ->
            callbacks.onUpdate(config.copy(filter = newFilter))
        },
        onSaveFilterClick = callbacks.onSaveFilterClick,
        displaySettings = { onDismiss ->
            TagUpSetSettingsSheet(
                config = config,
                onUpdate = { newConfig ->
                    callbacks.onUpdate(newConfig)
                },
                onDelete = {
                    callbacks.onDelete()
                    onDismiss()
                },
                onDismiss = onDismiss
            )
        }
    ) {
        val uiSets = renderData.sets.map { 
            AeonUpSetSet(
                name = it.tagName,
                total = it.totalAmount,
                income = it.income,
                expense = it.expense,
                incomeByCurrency = it.incomeByCurrency,
                expenseByCurrency = it.expenseByCurrency,
                transactionCount = it.transactionCount
            ) 
        }
        val setIndexMap = uiSets.withIndex().associate { it.value.name to it.index }
        
        val uiIntersections = renderData.intersections.mapNotNull { intersection ->
            val indices = intersection.tags.mapNotNull { setIndexMap[it] }.toSet()
            if (indices.isEmpty()) null
            else AeonUpSetIntersection(
                setIndices = indices,
                value = intersection.amount,
                income = intersection.income,
                expense = intersection.expense,
                incomeByCurrency = intersection.incomeByCurrency,
                expenseByCurrency = intersection.expenseByCurrency,
                count = intersection.transactionCount
            )
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            if (uiSets.isNotEmpty()) {
                AeonUpSetPlot(
                    data = AeonUpSetData(uiSets, uiIntersections),
                    currency = renderData.baseCurrency,
                    maxVisibleRows = config.maxTags,
                    maxVisibleCols = config.maxIntersections,
                    dynamicFiltering = config.dynamicTagFiltering,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    onIntersectionClick = { intersection ->
                        selectedIntersection = intersection
                    },
                    onSetClick = { index ->
                        val set = uiSets[index]
                        selectedIntersection = AeonUpSetIntersection(
                            setIndices = setOf(index),
                            value = set.total,
                            income = set.income,
                            expense = set.expense,
                            incomeByCurrency = set.incomeByCurrency,
                            expenseByCurrency = set.expenseByCurrency,
                            count = set.transactionCount
                        )
                    }
                )
                
                selectedIntersection?.let { intersection ->
                    val isSet = intersection.setIndices.size == 1
                    val titleText = if (isSet) {
                        uiSets[intersection.setIndices.first()].name
                    } else {
                        intersection.setIndices.map { uiSets[it].name }.joinToString(", ")
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        color = AppColors.primaryContainer.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = titleText,
                                    style = AppTextStyles.caption.copy(fontWeight = FontWeight.Bold),
                                    color = AppColors.primary
                                )
                                Text(
                                    text = "${intersection.count} Transactions",
                                    style = AppTextStyles.caption.copy(
                                        fontWeight = FontWeight.Bold,
                                        textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                                    ),
                                    color = AppColors.primary,
                                    modifier = Modifier.clickable {
                                        val tags = intersection.setIndices.map { uiSets[it].name }.toSet()
                                        onRecordsClick(displayFilter.copy(tags = tags))
                                    }
                                )
                            }

                            HorizontalDivider(color = AppColors.border.copy(alpha = 0.1f), thickness = 0.5.dp)

                            TransactionSummaryBreakdown(
                                incomeByCurrency = intersection.incomeByCurrency,
                                expenseByCurrency = intersection.expenseByCurrency,
                                incomeBase = intersection.income,
                                expenseBase = intersection.expense,
                                netBase = intersection.value,
                                baseCurrency = renderData.baseCurrency,
                                labelColumnWidth = 32.dp,
                                showCurrencyBreakdown = true
                            )
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Text(
                        text = "No tagged transactions in this period",
                        style = AppTextStyles.caption,
                        color = AppColors.contentSecondary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TagUpSetSettingsSheet(
    config: com.osscabinet.ossWallet.domain.model.settings.TagUpSetPlotCardConfig,
    onUpdate: (com.osscabinet.ossWallet.domain.model.settings.TagUpSetPlotCardConfig) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
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
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Tag Analytics",
                    style = AppTextStyles.heroAmount
                )
                IconButton(
                    onClick = { onDelete(); onDismiss() },
                    colors = IconButtonDefaults.iconButtonColors(contentColor = AppColors.error)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete card")
                }
            }

            // Display controls
            Surface(
                color = AppColors.insetSurface.copy(alpha = 0.3f),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "DISPLAY",
                        style = AppTextStyles.transactionSwipeAction.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp,
                            fontSize = 11.sp
                        ),
                        color = AppColors.contentSecondary.copy(alpha = 0.6f)
                    )
                    
                    // Max Tags Control
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Maximum visible tags", style = AppTextStyles.transactionTitle)
                            Text(
                                "Showing top ${config.maxTags} tags",
                                style = AppTextStyles.caption,
                                color = AppColors.contentSecondary
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = AppColors.sheetSurface,
                            modifier = Modifier.height(40.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { if (config.maxTags > 1) onUpdate(config.copy(maxTags = config.maxTags - 1)) },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(Icons.Rounded.Remove, contentDescription = "Fewer", tint = AppColors.primary)
                                }
                                
                                Text(
                                    text = "${config.maxTags}",
                                    style = AppTextStyles.calculatorKey.copy(fontSize = 14.sp),
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                                
                                IconButton(
                                    onClick = { onUpdate(config.copy(maxTags = config.maxTags + 1)) },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = "More", tint = AppColors.primary)
                                }
                            }
                        }
                    }

                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = AppColors.divider.copy(alpha = 0.3f)
                    )

                    // Max Intersections Control
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Maximum intersections", style = AppTextStyles.transactionTitle)
                            Text(
                                "Showing top ${config.maxIntersections} sets",
                                style = AppTextStyles.caption,
                                color = AppColors.contentSecondary
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = AppColors.sheetSurface,
                            modifier = Modifier.height(40.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { if (config.maxIntersections > 1) onUpdate(config.copy(maxIntersections = config.maxIntersections - 1)) },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(Icons.Rounded.Remove, contentDescription = "Fewer", tint = AppColors.primary)
                                }
                                
                                Text(
                                    text = "${config.maxIntersections}",
                                    style = AppTextStyles.calculatorKey.copy(fontSize = 14.sp),
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                                
                                IconButton(
                                    onClick = { onUpdate(config.copy(maxIntersections = config.maxIntersections + 1)) },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = "More", tint = AppColors.primary)
                                }
                            }
                        }
                    }

                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = AppColors.divider.copy(alpha = 0.3f)
                    )

                    // Dynamic Tag Filtering Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Dynamic Tag Filtering", style = AppTextStyles.transactionTitle)
                            Text(
                                "Show only tags in visible columns",
                                style = AppTextStyles.caption,
                                color = AppColors.contentSecondary
                            )
                        }
                        Switch(
                            checked = config.dynamicTagFiltering,
                            onCheckedChange = { onUpdate(config.copy(dynamicTagFiltering = it)) }
                        )
                    }
                }
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Done") }
        }
    }
}
