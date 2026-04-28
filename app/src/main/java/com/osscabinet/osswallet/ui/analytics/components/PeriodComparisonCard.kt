package com.osscabinet.ossWallet.ui.analytics.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Filter
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.settings.PeriodComparisonCardConfig
import com.osscabinet.ossWallet.ui.components.AeonFlowSectionHeader
import com.osscabinet.ossWallet.ui.components.AeonSettingsToggle
import com.osscabinet.ossWallet.ui.components.cards.AeonCardSettingsSheet
import com.osscabinet.ossWallet.ui.components.visualization.charts.AeonLineChart
import com.osscabinet.ossWallet.ui.components.visualization.charts.AeonLineChartSeries
import com.osscabinet.ossWallet.ui.components.visualization.charts.AeonLineChartPoint
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.theme.labelExtraSmall
import com.osscabinet.ossWallet.ui.theme.PositiveAmountColor
import com.osscabinet.ossWallet.ui.theme.NegativeAmountColor
import androidx.compose.foundation.background
import com.osscabinet.ossWallet.ui.util.formatCurrencyRight
import java.math.BigDecimal

@Composable
fun PeriodComparisonCard(
    config: PeriodComparisonCardConfig,
    data: PeriodComparisonData,
    modifier: Modifier = Modifier,
    headerModifier: Modifier = Modifier,
    onUpdate: (PeriodComparisonCardConfig) -> Unit = {},
    onDelete: () -> Unit = {},
    onRecordsClick: (com.osscabinet.ossWallet.ui.navigation.RecordsFilterContext) -> Unit = {},
    accounts: List<Account> = emptyList(),
    allCategories: List<Category> = emptyList(),
    savedFilters: List<Filter> = emptyList()
) {
    var showSettings by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                onRecordsClick(com.osscabinet.ossWallet.ui.navigation.RecordsFilterContext.Custom(
                    startDate = config.filter.startDate,
                    endDate = config.filter.endDate,
                    isIncome = when(config.selectedTab) {
                        "Income" -> true
                        "Expenses" -> false
                        else -> null
                    }
                ))
            },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            AeonFlowSectionHeader(
                title = config.title,
                modifier = headerModifier,
                filter = config.filter,
                trailing = {
                    Row {
                        IconButton(onClick = { showSettings = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(20.dp))
                        }
                    }
                }
            )
            
            // Tab Selection
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                listOf("Income", "Expenses", "Cash flow").forEachIndexed { index, label ->
                    SegmentedButton(
                        selected = config.selectedTab == label,
                        onClick = { onUpdate(config.copy(selectedTab = label)) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = 3),
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
            
            // Summary Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text("CURRENT PERIOD", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        formatCurrencyRight(data.currentValue, data.currency, data.precision),
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold)
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    val delta = data.deltaPercentage
                    val color = if (delta >= 0) PositiveAmountColor else NegativeAmountColor
                    Surface(
                        color = color.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "${if (delta >= 0) "+" else ""}${String.format(java.util.Locale.US, "%.1f%%", delta)}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = color
                        )
                    }
                    Text("vs Previous", style = MaterialTheme.typography.labelExtraSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            
            // Chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                if (data.chartData.isNotEmpty()) {
                    val series = mutableListOf<AeonLineChartSeries>()
                    
                    series.add(
                        AeonLineChartSeries(
                            name = "Current",
                            color = MaterialTheme.colorScheme.primary,
                            dataPoints = data.chartData.mapIndexed { i, v -> 
                                AeonLineChartPoint(i.toFloat(), v.toBigDecimal()) 
                            }
                        )
                    )
                    
                    if (config.showGhosting && data.ghostData != null) {
                        series.add(
                            AeonLineChartSeries(
                                name = "Previous",
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                dataPoints = data.ghostData.mapIndexed { i, v -> 
                                    AeonLineChartPoint(i.toFloat(), v.toBigDecimal()) 
                                },
                                isDashed = true,
                                fillGradient = false
                            )
                        )
                    }
                    
                    AeonLineChart(
                        series = series,
                        currency = data.currency,
                        precision = data.precision,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No data for comparison", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Legend
            if (config.showGhosting && data.ghostData != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    LegendItem("Current Period", MaterialTheme.colorScheme.primary)
                    LegendItem("Previous Period", MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), isLine = true)
                }
            }
        }
    }

    if (showSettings) {
        AeonCardSettingsSheet(
            title = config.title,
            filter = config.filter,
            onFilterChange = { newFilter -> onUpdate(config.copy(filter = newFilter)) },
            onDismiss = { showSettings = false },
            onDelete = onDelete,
            accounts = accounts,
            allCategories = allCategories,
            savedFilters = savedFilters
        ) {
            AeonSettingsToggle(
                label = "SHOW GHOSTING",
                description = "Show previous period comparison",
                checked = config.showGhosting,
                onCheckedChange = { onUpdate(config.copy(showGhosting = it)) }
            )
        }
    }
}

@Composable
private fun LegendItem(label: String, color: androidx.compose.ui.graphics.Color, isLine: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .size(if (isLine) 16.dp else 12.dp, if (isLine) 2.dp else 12.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

data class PeriodComparisonData(
    val currentValue: BigDecimal,
    val currency: String,
    val precision: Int,
    val chartData: List<Double>,
    val ghostData: List<Double>? = null,
    val runRate: BigDecimal = BigDecimal.ZERO,
    val deltaPercentage: Float = 0f
)

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun PeriodComparisonCardPreview() {
    OssWalletTheme {
        PeriodComparisonCard(
            config = PeriodComparisonCardConfig(
                cardId = "preview",
                title = "PERIOD COMPARISON"
            ),
            data = PeriodComparisonData(
                currentValue = BigDecimal("1200.00"),
                currency = "USD",
                precision = 2,
                chartData = listOf(100.0, 200.0, 150.0, 400.0, 350.0),
                ghostData = listOf(80.0, 150.0, 200.0, 300.0, 320.0),
                deltaPercentage = 15.5f
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}
