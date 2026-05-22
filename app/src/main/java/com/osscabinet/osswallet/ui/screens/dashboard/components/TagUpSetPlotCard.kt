package com.osscabinet.ossWallet.ui.screens.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material.icons.filled.ViewStream
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.card.CardRenderData
import com.osscabinet.ossWallet.domain.card.TagUpSetIntersection
import com.osscabinet.ossWallet.domain.card.TagUpSetSet
import androidx.compose.ui.draw.alpha
import com.osscabinet.ossWallet.domain.model.settings.UpSetDynamicFilteringMode
import com.osscabinet.ossWallet.domain.model.settings.UpSetTagSortMode
import com.osscabinet.ossWallet.domain.model.settings.UpSetIntersectionSortMode
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.settings.TagUpSetPlotCardConfig
import com.osscabinet.ossWallet.ui.components.cards.AeonExpressiveCard
import com.osscabinet.ossWallet.ui.components.cards.CardCallbacks
import com.osscabinet.ossWallet.ui.components.cards.CardSettingsSection
import com.osscabinet.ossWallet.ui.components.cards.CardSettingsSheet
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
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
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
                onDelete = callbacks.onDelete,
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
                    dynamicFilteringMode = config.dynamicFilteringMode,
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
                                        fontWeight = FontWeight.Bold
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
                    contentAlignment = Alignment.Center
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
    CardSettingsSheet(title = "Tag Analytics", onDismiss = onDismiss, onDelete = onDelete) {
        CardSettingsSection("Display") {
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
            HorizontalDivider(thickness = 0.5.dp, color = AppColors.divider.copy(alpha = 0.3f))
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
            HorizontalDivider(thickness = 0.5.dp, color = AppColors.divider.copy(alpha = 0.3f))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "DYNAMIC FILTERING",
                    style = AppTextStyles.transactionSwipeAction.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp,
                        fontSize = 11.sp
                    ),
                    color = AppColors.contentSecondary.copy(alpha = 0.6f)
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    UpSetDynamicFilteringMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = config.dynamicFilteringMode == mode,
                            onClick = { onUpdate(config.copy(dynamicFilteringMode = mode)) },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = UpSetDynamicFilteringMode.entries.size
                            ),
                            icon = {},
                            label = {
                                Icon(
                                    imageVector = when (mode) {
                                        UpSetDynamicFilteringMode.OFF -> Icons.Default.Block
                                        UpSetDynamicFilteringMode.HORIZONTAL -> Icons.Default.ViewColumn
                                        UpSetDynamicFilteringMode.VERTICAL -> Icons.Default.ViewStream
                                    },
                                    contentDescription = when (mode) {
                                        UpSetDynamicFilteringMode.OFF -> "Filtering Off"
                                        UpSetDynamicFilteringMode.HORIZONTAL -> "Row Filtering"
                                        UpSetDynamicFilteringMode.VERTICAL -> "Column Filtering"
                                    },
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        )
                    }
                }
            }
        }
        CardSettingsSection("Sorting") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Tags",
                    style = AppTextStyles.transactionTitle,
                    modifier = Modifier.weight(1f)
                )
                SingleChoiceSegmentedButtonRow {
                    val tagPairs = listOf(
                        UpSetTagSortMode.NAME_ASC to UpSetTagSortMode.NAME_DESC,
                        UpSetTagSortMode.DECREASING_ABS to UpSetTagSortMode.INCREASING_ABS,
                        UpSetTagSortMode.DECREASING to UpSetTagSortMode.INCREASING
                    )
                    tagPairs.forEachIndexed { index, (pairA, pairB) ->
                        val isSelected = config.tagSortMode == pairA || config.tagSortMode == pairB
                        val displayed = if (isSelected) config.tagSortMode else pairA
                        SegmentedButton(
                            selected = isSelected,
                            onClick = {
                                onUpdate(config.copy(tagSortMode = if (config.tagSortMode == pairA) pairB else pairA))
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = tagPairs.size),
                            icon = {}
                        ) {
                            Text(
                                text = when (displayed) {
                                    UpSetTagSortMode.NAME_ASC       -> "Az"
                                    UpSetTagSortMode.NAME_DESC      -> "Za"
                                    UpSetTagSortMode.DECREASING_ABS -> "|↓|"
                                    UpSetTagSortMode.INCREASING_ABS -> "|↑|"
                                    UpSetTagSortMode.DECREASING     -> "↓"
                                    UpSetTagSortMode.INCREASING     -> "↑"
                                },
                                style = AppTextStyles.caption.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.alpha(if (isSelected) 1f else 0.45f)
                            )
                        }
                    }
                }
            }
            HorizontalDivider(thickness = 0.5.dp, color = AppColors.divider.copy(alpha = 0.3f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Intersections",
                    style = AppTextStyles.transactionTitle,
                    modifier = Modifier.weight(1f)
                )
                SingleChoiceSegmentedButtonRow {
                    val intPairs = listOf(
                        UpSetIntersectionSortMode.DECREASING_ABS to UpSetIntersectionSortMode.INCREASING_ABS,
                        UpSetIntersectionSortMode.DECREASING to UpSetIntersectionSortMode.INCREASING
                    )
                    intPairs.forEachIndexed { index, (pairA, pairB) ->
                        val isSelected = config.intersectionSortMode == pairA || config.intersectionSortMode == pairB
                        val displayed = if (isSelected) config.intersectionSortMode else pairA
                        SegmentedButton(
                            selected = isSelected,
                            onClick = {
                                onUpdate(config.copy(intersectionSortMode = if (config.intersectionSortMode == pairA) pairB else pairA))
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = intPairs.size),
                            icon = {}
                        ) {
                            Text(
                                text = when (displayed) {
                                    UpSetIntersectionSortMode.DECREASING_ABS -> "|↓|"
                                    UpSetIntersectionSortMode.INCREASING_ABS -> "|↑|"
                                    UpSetIntersectionSortMode.DECREASING     -> "↓"
                                    UpSetIntersectionSortMode.INCREASING     -> "↑"
                                },
                                style = AppTextStyles.caption.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.alpha(if (isSelected) 1f else 0.45f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TagUpSetPlotCardPreview() {
    OssWalletTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            TagUpSetPlotCard(
                renderData = CardRenderData.TagUpSetPlot(
                    config = TagUpSetPlotCardConfig(
                        cardId = "test_tag_upset",
                        title = "Monthly Tag Overlaps"
                    ),
                    sets = listOf(
                        TagUpSetSet("Food", BigDecimal("500"), expense = BigDecimal("500"), transactionCount = 10),
                        TagUpSetSet("Business", BigDecimal("300"), expense = BigDecimal("300"), transactionCount = 5),
                        TagUpSetSet("Travel", BigDecimal("200"), expense = BigDecimal("200"), transactionCount = 3)
                    ),
                    intersections = listOf(
                        TagUpSetIntersection(setOf("Food", "Business"), BigDecimal("100"), expense = BigDecimal("100"), transactionCount = 2),
                        TagUpSetIntersection(setOf("Food"), BigDecimal("400"), expense = BigDecimal("400"), transactionCount = 8),
                        TagUpSetIntersection(setOf("Business"), BigDecimal("200"), expense = BigDecimal("200"), transactionCount = 3)
                    ),
                    baseCurrency = "USD"
                )
            )
        }
    }
}
