package com.osscabinet.ossWallet.ui.screens.dashboard.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
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
            TagUpSetDisplaySettingsDialog(
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
                                    style = AppTextStyles.caption,
                                    color = AppColors.contentSecondary
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

@Composable
private fun TagUpSetDisplaySettingsDialog(
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Card Settings", style = AppTextStyles.cardSectionTitle) },
        text = {
            Text("This card shows how your tags intersect and their total amounts.", style = AppTextStyles.caption)
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        },
        dismissButton = {
            TextButton(onClick = onDelete) {
                Text("Delete Card", color = AppColors.error)
            }
        }
    )
}
