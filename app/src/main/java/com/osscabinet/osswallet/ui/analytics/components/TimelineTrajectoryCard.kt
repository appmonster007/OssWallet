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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Filter
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.analytics.TimelineTrajectoryData
import com.osscabinet.ossWallet.domain.model.settings.TimelineTrajectoryCardConfig
import com.osscabinet.ossWallet.ui.components.AeonFlowSectionHeader
import com.osscabinet.ossWallet.ui.components.AeonSettingsToggle
import com.osscabinet.ossWallet.ui.components.cards.AeonCardSettingsSheet
import com.osscabinet.ossWallet.ui.components.visualization.charts.AeonLineChart
import com.osscabinet.ossWallet.ui.components.visualization.charts.AeonLineChartSeries
import com.osscabinet.ossWallet.ui.components.visualization.charts.AeonLineChartPoint
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.theme.PositiveAmountColor
import com.osscabinet.ossWallet.ui.theme.NegativeAmountColor
import com.osscabinet.ossWallet.ui.theme.AccentBlue
import java.math.BigDecimal

@Composable
fun TimelineTrajectoryCard(
    config: TimelineTrajectoryCardConfig,
    data: List<TimelineTrajectoryData>,
    modifier: Modifier = Modifier,
    headerModifier: Modifier = Modifier,
    onUpdate: (TimelineTrajectoryCardConfig) -> Unit = {},
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
                    endDate = config.filter.endDate
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
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
            ) {
                if (data.isNotEmpty()) {
                    val series = mutableListOf<AeonLineChartSeries>()
                    
                    val dtf = java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault())
                    if (config.showBalance) {
                        series.add(
                            AeonLineChartSeries(
                                name = "Balance",
                                color = AccentBlue,
                                dataPoints = data.mapIndexed { i, d -> AeonLineChartPoint(i.toFloat(), d.balance, dtf.format(java.util.Date(d.timestamp))) }
                            )
                        )
                    }
                    
                    if (config.showIncome) {
                        series.add(
                            AeonLineChartSeries(
                                name = "Income",
                                color = PositiveAmountColor,
                                dataPoints = data.mapIndexed { i, d -> AeonLineChartPoint(i.toFloat(), d.income, dtf.format(java.util.Date(d.timestamp))) },
                                fillGradient = false
                            )
                        )
                    }
                    
                    if (config.showExpense) {
                        series.add(
                            AeonLineChartSeries(
                                name = "Expense",
                                color = NegativeAmountColor,
                                dataPoints = data.mapIndexed { i, d -> AeonLineChartPoint(i.toFloat(), d.expense.abs(), dtf.format(java.util.Date(d.timestamp))) },
                                fillGradient = false
                            )
                        )
                    }
                    
                    AeonLineChart(
                        series = series,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                        Text("No trajectory data", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
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
                label = "SHOW BALANCE",
                checked = config.showBalance,
                onCheckedChange = { onUpdate(config.copy(showBalance = it)) }
            )
            
            AeonSettingsToggle(
                label = "SHOW INCOME",
                checked = config.showIncome,
                onCheckedChange = { onUpdate(config.copy(showIncome = it)) }
            )
            
            AeonSettingsToggle(
                label = "SHOW EXPENSE",
                checked = config.showExpense,
                onCheckedChange = { onUpdate(config.copy(showExpense = it)) }
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun TimelineTrajectoryCardPreview() {
    val now = System.currentTimeMillis()
    val day = 24 * 60 * 60 * 1000L
    val sampleData = listOf(
        TimelineTrajectoryData(now - 2 * day, BigDecimal("1000.00"), BigDecimal("200.00"), BigDecimal("5000.00")),
        TimelineTrajectoryData(now - 1 * day, BigDecimal("1200.00"), BigDecimal("300.00"), BigDecimal("5900.00")),
        TimelineTrajectoryData(now, BigDecimal("800.00"), BigDecimal("400.00"), BigDecimal("6300.00"))
    )

    OssWalletTheme {
        TimelineTrajectoryCard(
            config = TimelineTrajectoryCardConfig(cardId = "preview", title = "TIMELINE TRAJECTORY"),
            data = sampleData
        )
    }
}
