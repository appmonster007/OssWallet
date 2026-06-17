package com.osscabinet.ossWallet.ui.screens.dashboard.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material.icons.filled.ViewStream
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.card.CardFilterRules
import com.osscabinet.ossWallet.domain.card.CardRenderData
import com.osscabinet.ossWallet.domain.card.TagUpSetIntersection
import com.osscabinet.ossWallet.domain.card.TagUpSetSet
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import com.osscabinet.ossWallet.domain.model.settings.ChartMetric
import com.osscabinet.ossWallet.domain.model.settings.TagUpSetPlotCardConfig
import com.osscabinet.ossWallet.domain.model.settings.UpSetDynamicFilteringMode
import com.osscabinet.ossWallet.domain.model.settings.UpSetIntersectionSortMode
import com.osscabinet.ossWallet.domain.model.settings.UpSetTagSortMode
import com.osscabinet.ossWallet.ui.components.AeonStepperField
import com.osscabinet.ossWallet.ui.components.cards.AeonExpressiveCard
import com.osscabinet.ossWallet.ui.components.cards.CardCallbacks
import com.osscabinet.ossWallet.ui.components.cards.CardSettingsSection
import com.osscabinet.ossWallet.ui.components.cards.CardSettingsSheet
import com.osscabinet.ossWallet.ui.components.cards.FilterSelectionContext
import com.osscabinet.ossWallet.ui.components.transactions.TransactionSummaryBreakdown
import com.osscabinet.ossWallet.ui.components.visualization.charts.AeonUpSetData
import com.osscabinet.ossWallet.ui.components.visualization.charts.AeonUpSetIntersection
import com.osscabinet.ossWallet.ui.components.visualization.charts.AeonUpSetPlot
import com.osscabinet.ossWallet.ui.components.visualization.charts.AeonUpSetSet
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
    filterRules: CardFilterRules? = null,
    onRecordsClick: (TransactionFilter) -> Unit = {},
    screenOwnsTimeline: Boolean = false,
    showFilterButton: Boolean = true,
    modifier: Modifier = Modifier,
    headerModifier: Modifier = Modifier,
) {
    val config = renderData.config
    var selectedIntersection by remember { mutableStateOf<AeonUpSetIntersection?>(null) }

    val displayFilter =
        if (screenOwnsTimeline || inheritedFilter.timelineRange !is com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.AllTime)
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
        filterRules = filterRules ?: config.filterRules,
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
                    currency = renderData.currency,
                    maxVisibleRows = config.maxTags,
                    maxVisibleCols = config.maxIntersections,
                    dynamicFilteringMode = config.dynamicFilteringMode,
                    topStartContent = {
                        CompactMetricSelector(
                            selectedMetric = config.selectedMetric,
                            onMetricChange = { metric ->
                                callbacks.onUpdate(config.copy(selectedMetric = metric))
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(end = 8.dp)
                        )
                    },
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        color = AppColors.primaryContainer.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
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
                                        val tags =
                                            intersection.setIndices.map { uiSets[it].name }.toSet()
                                        onRecordsClick(displayFilter.copy(tags = tags))
                                    }
                                )
                            }

                            HorizontalDivider(
                                color = AppColors.border.copy(alpha = 0.1f),
                                thickness = 0.5.dp
                            )

                            TransactionSummaryBreakdown(
                                incomeByCurrency = intersection.incomeByCurrency,
                                expenseByCurrency = intersection.expenseByCurrency,
                                incomeBase = intersection.income,
                                expenseBase = intersection.expense,
                                netBase = intersection.income.subtract(intersection.expense),
                                baseCurrency = renderData.currency,
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

@Composable
private fun CompactMetricSelector(
    selectedMetric: ChartMetric,
    onMetricChange: (ChartMetric) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ChartMetric.entries.forEach { metric ->
            val isSelected = selectedMetric == metric
            Surface(
                onClick = { onMetricChange(metric) },
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) AppColors.primary else AppColors.cardSurfaceProminent,
                tonalElevation = if (isSelected) 0.dp else 2.dp,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .alpha(if (isSelected) 1f else 0.8f)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = metric.name,
                        style = AppTextStyles.caption.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        ),
                        color = if (isSelected) AppColors.onPrimary else AppColors.contentPrimary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TagUpSetSettingsSheet(
    config: TagUpSetPlotCardConfig,
    onUpdate: (TagUpSetPlotCardConfig) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    CardSettingsSheet(title = "Tag Analytics", onDismiss = onDismiss, onDelete = onDelete) {
        CardSettingsSection("Display") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (config.maxTags != 7) {
                        IconButton(
                            onClick = { onUpdate(config.copy(maxTags = 7)) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Reset to 7",
                                tint = AppColors.contentSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    AeonStepperField(
                        value = config.maxTags,
                        onDecrement = { if (config.maxTags > 1) onUpdate(config.copy(maxTags = config.maxTags - 1)) },
                        onIncrement = { onUpdate(config.copy(maxTags = config.maxTags + 1)) },
                        min = 1
                    )
                }
            }
            HorizontalDivider(
                thickness = 0.5.dp,
                color = AppColors.divider.copy(alpha = 0.3f),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
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
                val screenWidthDp = LocalConfiguration.current.screenWidthDp
                val defaultIntersections = maxOf(1, (screenWidthDp - 32 - 120 - 8) / 24)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (config.maxIntersections != defaultIntersections) {
                        IconButton(
                            onClick = { onUpdate(config.copy(maxIntersections = defaultIntersections)) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Reset to default",
                                tint = AppColors.contentSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    AeonStepperField(
                        value = config.maxIntersections,
                        onDecrement = { if (config.maxIntersections > 1) onUpdate(config.copy(maxIntersections = config.maxIntersections - 1)) },
                        onIncrement = { onUpdate(config.copy(maxIntersections = config.maxIntersections + 1)) },
                        min = 1
                    )
                }
            }
            HorizontalDivider(
                thickness = 0.5.dp,
                color = AppColors.divider.copy(alpha = 0.3f),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
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
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = tagPairs.size
                            ),
                            icon = {}
                        ) {
                            Text(
                                text = when (displayed) {
                                    UpSetTagSortMode.NAME_ASC -> "Az"
                                    UpSetTagSortMode.NAME_DESC -> "Za"
                                    UpSetTagSortMode.DECREASING_ABS -> "|↓|"
                                    UpSetTagSortMode.INCREASING_ABS -> "|↑|"
                                    UpSetTagSortMode.DECREASING -> "↓"
                                    UpSetTagSortMode.INCREASING -> "↑"
                                },
                                style = AppTextStyles.caption.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.alpha(if (isSelected) 1f else 0.45f)
                            )
                        }
                    }
                }
            }
            HorizontalDivider(
                thickness = 0.5.dp,
                color = AppColors.divider.copy(alpha = 0.3f),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
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
                        val isSelected =
                            config.intersectionSortMode == pairA || config.intersectionSortMode == pairB
                        val displayed = if (isSelected) config.intersectionSortMode else pairA
                        SegmentedButton(
                            selected = isSelected,
                            onClick = {
                                onUpdate(config.copy(intersectionSortMode = if (config.intersectionSortMode == pairA) pairB else pairA))
                            },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = intPairs.size
                            ),
                            icon = {}
                        ) {
                            Text(
                                text = when (displayed) {
                                    UpSetIntersectionSortMode.DECREASING_ABS -> "|↓|"
                                    UpSetIntersectionSortMode.INCREASING_ABS -> "|↑|"
                                    UpSetIntersectionSortMode.DECREASING -> "↓"
                                    UpSetIntersectionSortMode.INCREASING -> "↑"
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
                        TagUpSetSet(
                            "Food",
                            BigDecimal("500"),
                            expense = BigDecimal("500"),
                            transactionCount = 10
                        ),
                        TagUpSetSet(
                            "Business",
                            BigDecimal("300"),
                            expense = BigDecimal("300"),
                            transactionCount = 5
                        ),
                        TagUpSetSet(
                            "Travel",
                            BigDecimal("200"),
                            expense = BigDecimal("200"),
                            transactionCount = 3
                        )
                    ),
                    intersections = listOf(
                        TagUpSetIntersection(
                            setOf("Food", "Business"),
                            BigDecimal("100"),
                            expense = BigDecimal("100"),
                            transactionCount = 2
                        ),
                        TagUpSetIntersection(
                            setOf("Food"),
                            BigDecimal("400"),
                            expense = BigDecimal("400"),
                            transactionCount = 8
                        ),
                        TagUpSetIntersection(
                            setOf("Business"),
                            BigDecimal("200"),
                            expense = BigDecimal("200"),
                            transactionCount = 3
                        )
                    ),
                    currency = "USD"
                )
            )
        }
    }
}
