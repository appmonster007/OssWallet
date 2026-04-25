package com.osscabinet.ossWallet.ui.analytics.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.model.analytics.TimelineTrajectoryData
import com.osscabinet.ossWallet.domain.model.settings.TimelineTrajectoryCardConfig
import com.osscabinet.ossWallet.ui.components.AeonFlowSectionHeader
import com.osscabinet.ossWallet.ui.components.TimelineFilterBar
import com.osscabinet.ossWallet.ui.components.visualization.charts.AeonLineChart
import com.osscabinet.ossWallet.ui.components.visualization.charts.AeonLineChartPoint
import com.osscabinet.ossWallet.ui.components.visualization.charts.AeonLineChartSeries
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.theme.PositiveAmountColor
import com.osscabinet.ossWallet.ui.theme.NegativeAmountColor
import com.osscabinet.ossWallet.ui.theme.AccentBlue
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TimelineTrajectoryCard(
    config: TimelineTrajectoryCardConfig,
    data: List<TimelineTrajectoryData>,
    modifier: Modifier = Modifier,
    headerModifier: Modifier = Modifier,
    onUpdate: (TimelineTrajectoryCardConfig) -> Unit = {},
    onDelete: () -> Unit = {}
) {
    var showSettings by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier.fillMaxWidth(),
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
                modifier = headerModifier.fillMaxWidth(),
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
                    val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
                    val series = mutableListOf<AeonLineChartSeries>()
                    
                    if (config.showBalance) {
                        series.add(
                            AeonLineChartSeries(
                                name = "Balance",
                                color = AccentBlue,
                                dataPoints = data.mapIndexed { index, point ->
                                    AeonLineChartPoint(index.toFloat(), point.balance, dateFormat.format(Date(point.timestamp)))
                                }
                            )
                        )
                    }
                    
                    if (config.showIncome) {
                        series.add(
                            AeonLineChartSeries(
                                name = "Income",
                                color = PositiveAmountColor,
                                dataPoints = data.mapIndexed { index, point ->
                                    AeonLineChartPoint(index.toFloat(), point.income, dateFormat.format(Date(point.timestamp)))
                                },
                                fillGradient = false
                            )
                        )
                    }
                    
                    if (config.showExpense) {
                        series.add(
                            AeonLineChartSeries(
                                name = "Expense",
                                color = NegativeAmountColor,
                                dataPoints = data.mapIndexed { index, point ->
                                    AeonLineChartPoint(index.toFloat(), point.expense.abs(), dateFormat.format(Date(point.timestamp)))
                                },
                                fillGradient = false,
                                isDashed = true
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
            
            // Legend
            TimelineLegend(
                showBalance = config.showBalance,
                showExpense = config.showExpense,
                showIncome = config.showIncome
            )
        }
    }
    
    if (showSettings) {
        TimelineTrajectorySettingsDialog(
            config = config,
            onUpdate = onUpdate,
            onDismiss = { showSettings = false },
            onDelete = onDelete
        )
    }
}

@Composable
fun TimelineLegend(
    showBalance: Boolean,
    showExpense: Boolean,
    showIncome: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        if (showBalance) LegendItem("Balance", AccentBlue)
        if (showIncome) LegendItem("Income", PositiveAmountColor)
        if (showExpense) LegendItem("Expense", NegativeAmountColor, isDashed = true)
    }
}

@Composable
private fun LegendItem(label: String, color: Color, isDashed: Boolean = false) {
    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .size(width = 16.dp, height = 3.dp)
                .then(
                    if (!isDashed) {
                        Modifier.background(color, RoundedCornerShape(2.dp))
                    } else {
                        Modifier.drawBehind {
                            val dashWidth = 4.dp.toPx()
                            val spaceWidth = 2.dp.toPx()
                            drawLine(
                                color = color,
                                start = Offset(0f, size.height / 2),
                                end = Offset(size.width, size.height / 2),
                                strokeWidth = size.height,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashWidth, spaceWidth))
                            )
                        }
                    }
                )
        )
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TimelineTrajectorySettingsDialog(
    config: TimelineTrajectoryCardConfig,
    onUpdate: (TimelineTrajectoryCardConfig) -> Unit,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
        dismissButton = {
            TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Delete")
            }
        },
        title = { Text(config.title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("TIMELINE", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp), color = MaterialTheme.colorScheme.primary)
                        TimelineFilterBar(
                            currentRange = config.timelineRange,
                            onRangeSelected = { range -> onUpdate(config.copy(timelineRange = range)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                
                // Visibility Toggles
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("VISIBILITY", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.outline)
                    ToggleRow("Show Balance", config.showBalance) { onUpdate(config.copy(showBalance = it)) }
                    ToggleRow("Show Income", config.showIncome) { onUpdate(config.copy(showIncome = it)) }
                    ToggleRow("Show Expense", config.showExpense) { onUpdate(config.copy(showExpense = it)) }
                }
            }
        }
    )
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun TimelineTrajectoryCardPreview() {
    OssWalletTheme {
        TimelineTrajectoryCard(
            config = TimelineTrajectoryCardConfig(
                cardId = "preview",
                title = "TIMELINE TRAJECTORY"
            ),
            data = listOf(
                TimelineTrajectoryData(System.currentTimeMillis() - 86400000 * 3, BigDecimal("1000.00"), BigDecimal("500.00"), BigDecimal("200.00")),
                TimelineTrajectoryData(System.currentTimeMillis() - 86400000 * 2, BigDecimal("1300.00"), BigDecimal("400.00"), BigDecimal("100.00")),
                TimelineTrajectoryData(System.currentTimeMillis() - 86400000 * 1, BigDecimal("1200.00"), BigDecimal("100.00"), BigDecimal("200.00")),
                TimelineTrajectoryData(System.currentTimeMillis(), BigDecimal("1500.00"), BigDecimal("600.00"), BigDecimal("300.00"))
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}
