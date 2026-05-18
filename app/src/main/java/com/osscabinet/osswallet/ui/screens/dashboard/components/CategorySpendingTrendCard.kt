package com.osscabinet.ossWallet.ui.screens.dashboard.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.card.CardRenderData
import com.osscabinet.ossWallet.domain.card.CategorySummary
import com.osscabinet.ossWallet.domain.model.CategoryNature
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.settings.CategoryNatureFilter
import com.osscabinet.ossWallet.domain.model.settings.CategorySpendingTrendCardConfig
import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.ui.components.cards.AeonExpressiveCard
import com.osscabinet.ossWallet.ui.components.cards.CardCallbacks
import com.osscabinet.ossWallet.ui.components.cards.FilterSelectionContext
import com.osscabinet.ossWallet.ui.components.visualization.charts.AeonBarChart
import com.osscabinet.ossWallet.ui.components.visualization.charts.AeonBarChartBar
import com.osscabinet.ossWallet.ui.components.visualization.charts.AeonBarChartGroup
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.ChartColorOrange
import java.math.BigDecimal

@Composable
fun CategorySpendingTrendCard(
    renderData: CardRenderData.CategorySpendingTrend,
    callbacks: CardCallbacks.CategorySpendingTrend = CardCallbacks.CategorySpendingTrend(),
    filterContext: FilterSelectionContext = FilterSelectionContext(),
    baseFilter: TransactionFilter = TransactionFilter(),
    modifier: Modifier = Modifier,
    headerModifier: Modifier = Modifier,
) {
    val config = renderData.config
    var selectedNature by rememberSaveable { mutableStateOf(config.selectedNature) }
    var categoriesExpanded by rememberSaveable { mutableStateOf(false) }

    AeonExpressiveCard(
        title = config.title,
        icon = Icons.Default.BarChart,
        modifier = modifier,
        headerModifier = headerModifier,
        filter = config.filter,
        baseFilter = baseFilter,
        filterRules = config.filterRules,
        filterContext = filterContext,
        onFilterChange = { newFilter ->
            callbacks.onUpdate(config.copy(filter = newFilter))
        },
        onSaveFilterClick = callbacks.onSaveFilterClick,
        displaySettings = { onDismiss ->
            CategorySpendingDisplaySettingsDialog(
                config = config,
                callbacks = callbacks,
                onDismiss = onDismiss
            )
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Nature segmented button: ALL / MUST / NEED / WANT
            val natureOptions = listOf(
                CategoryNatureFilter.ALL to "All",
                CategoryNatureFilter.MUST to "Must",
                CategoryNatureFilter.NEED to "Need",
                CategoryNatureFilter.WANT to "Want"
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                natureOptions.forEachIndexed { index, (nature, label) ->
                    SegmentedButton(
                        selected = selectedNature == nature,
                        onClick = {
                            selectedNature = nature
                            callbacks.onUpdate(config.copy(selectedNature = nature))
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = natureOptions.size),
                        label = {
                            Text(
                                text = label,
                                style = AppTextStyles.caption.copy(fontSize = 11.sp)
                            )
                        }
                    )
                }
            }

            // Resolve nature colors in composable scope
            val mustColor = AppColors.error
            val needColor = ChartColorOrange
            val wantColor = AppColors.accent

            // Stacked bar chart
            val chartGroups = buildChartGroups(renderData, mustColor, needColor, wantColor)
            if (chartGroups.isNotEmpty()) {
                AeonBarChart(
                    groups = chartGroups,
                    modifier = Modifier.fillMaxWidth().height(220.dp),
                    currency = renderData.baseCurrency,
                    showGrid = config.showGridLines,
                    showXAxisLabels = true
                )
            }

            // Expandable category list header
            if (renderData.activeCategories.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { categoriesExpanded = !categoriesExpanded }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Categories (${renderData.activeCategories.size})",
                        style = AppTextStyles.formSectionLabel,
                        color = AppColors.contentPrimary
                    )
                    Icon(
                        imageVector = if (categoriesExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (categoriesExpanded) "Collapse" else "Expand",
                        tint = AppColors.contentSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                AnimatedVisibility(visible = categoriesExpanded) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        renderData.activeCategories.forEach { summary ->
                            CategorySummaryRow(
                                summary = summary,
                                currency = renderData.baseCurrency
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategorySummaryRow(
    summary: CategorySummary,
    currency: String
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(Color(summary.colorHex or 0xFF000000.toInt()), CircleShape)
            )
            Text(
                text = summary.name,
                style = AppTextStyles.transactionSwipeAction,
                color = AppColors.contentPrimary,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            NatureChip(nature = summary.nature)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = CurrencyFormatter.formatCurrencyRight(summary.totalAmount, currency, CurrencyService.precision),
            style = AppTextStyles.formSectionLabel,
            color = AppColors.contentPrimary
        )
    }
}

@Composable
private fun NatureChip(nature: CategoryNature) {
    val (label, color) = when (nature) {
        CategoryNature.MUST -> "Must" to AppColors.error
        CategoryNature.NEED -> "Need" to ChartColorOrange
        CategoryNature.WANT -> "Want" to AppColors.accent
    }
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = label,
            style = AppTextStyles.caption.copy(fontSize = 9.sp),
            color = color,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun CategorySpendingDisplaySettingsDialog(
    config: CategorySpendingTrendCardConfig,
    callbacks: CardCallbacks.CategorySpendingTrend,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Display Settings", style = AppTextStyles.cardSectionTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Show Grid Lines", style = AppTextStyles.transactionSwipeAction)
                    Switch(
                        checked = config.showGridLines,
                        onCheckedChange = { callbacks.onUpdate(config.copy(showGridLines = it)) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        },
        dismissButton = {
            TextButton(onClick = { callbacks.onDelete(); onDismiss() }) {
                Text("Delete Card", color = AppColors.error)
            }
        }
    )
}

private fun buildChartGroups(
    renderData: CardRenderData.CategorySpendingTrend,
    mustColor: Color,
    needColor: Color,
    wantColor: Color
): List<AeonBarChartGroup> {
    return renderData.buckets.map { bucket ->
        val bars = bucket.categoryAmounts
            .filter { it.amount > BigDecimal.ZERO }
            .map { catAmount ->
                val color = when (catAmount.nature) {
                    CategoryNature.MUST -> mustColor
                    CategoryNature.NEED -> needColor
                    CategoryNature.WANT -> wantColor
                }
                AeonBarChartBar(
                    value = catAmount.amount,
                    label = catAmount.categoryName,
                    color = color
                )
            }
        // Empty buckets get a zero placeholder so the chart always renders axes and labels
        AeonBarChartGroup(
            label = bucket.label,
            bars = bars.ifEmpty { listOf(AeonBarChartBar(BigDecimal.ZERO, "", Color.Transparent)) }
        )
    }
}
