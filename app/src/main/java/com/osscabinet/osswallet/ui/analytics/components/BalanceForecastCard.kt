package com.osscabinet.ossWallet.ui.analytics.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.model.settings.BalanceForecastCardConfig
import com.osscabinet.ossWallet.ui.components.AeonFlowSectionHeader
import com.osscabinet.ossWallet.ui.components.TimelineFilterBar
import com.osscabinet.ossWallet.ui.components.visualization.charts.AeonLineChart
import com.osscabinet.ossWallet.ui.components.visualization.charts.AeonLineChartPoint
import com.osscabinet.ossWallet.ui.components.visualization.charts.AeonLineChartSeries
import com.osscabinet.ossWallet.ui.theme.PositiveAmountColor
import com.osscabinet.ossWallet.ui.theme.NegativeAmountColor
import com.osscabinet.ossWallet.ui.theme.AccentBlue
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.util.formatCurrencyRight
import java.math.BigDecimal

@Composable
fun BalanceForecastCard(
    config: BalanceForecastCardConfig,
    data: BalanceForecastData,
    modifier: Modifier = Modifier,
    onUpdate: (BalanceForecastCardConfig) -> Unit = {},
    onDelete: () -> Unit = {}
) {
    var showSettings by remember { mutableStateOf(false) }
    var forecastDays by remember { mutableStateOf(config.forecastDays) }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Header
            AeonFlowSectionHeader(
                title = config.title,
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
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                config.subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Forecast value with trend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        "Expected Balance",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        formatCurrencyRight(data.forecastBalance, data.currency, data.precision),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = if (data.forecastBalance >= BigDecimal.ZERO) 
                                PositiveAmountColor 
                            else 
                                NegativeAmountColor
                        )
                    )
                }
                
                Text(
                    "in $forecastDays days",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Forecast days slider
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Forecast Horizon",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "$forecastDays Days",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Slider(
                    value = forecastDays.toFloat(),
                    onValueChange = { 
                        forecastDays = it.toInt()
                        onUpdate(config.copy(forecastDays = it.toInt()))
                    },
                    valueRange = 7f..90f,
                    steps = 83,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Interactive visualization
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            ) {
                if (data.chartData.isNotEmpty()) {
                    val series = listOf(
                        AeonLineChartSeries(
                            name = "Optimistic",
                            color = PositiveAmountColor,
                            isDashed = true,
                            dataPoints = data.chartData.mapIndexed { index, (optimistic, _, _) ->
                                AeonLineChartPoint(index.toFloat(), optimistic.toBigDecimal())
                            }
                        ),
                        AeonLineChartSeries(
                            name = "Realistic",
                            color = AccentBlue,
                            dataPoints = data.chartData.mapIndexed { index, (_, realistic, _) ->
                                val daysLabel = (index * forecastDays / (data.chartData.size - 1)).toInt()
                                AeonLineChartPoint(index.toFloat(), realistic.toBigDecimal(), "+${daysLabel}d")
                            }
                        ),
                        AeonLineChartSeries(
                            name = "Pessimistic",
                            color = NegativeAmountColor,
                            isDashed = true,
                            dataPoints = data.chartData.mapIndexed { index, (_, _, pessimistic) ->
                                AeonLineChartPoint(index.toFloat(), pessimistic.toBigDecimal())
                            }
                        )
                    )
                    
                    AeonLineChart(
                        series = series,
                        currency = data.currency,
                        precision = data.precision,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No forecast data available",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
    
    if (showSettings) {
        BalanceForecastSettingsDialog(
            config = config,
            onUpdate = onUpdate,
            onDismiss = { showSettings = false },
            onDelete = onDelete
        )
    }
}

@Composable
private fun BalanceForecastSettingsDialog(
    config: BalanceForecastCardConfig,
    onUpdate: (BalanceForecastCardConfig) -> Unit,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        dismissButton = {
            TextButton(
                onClick = onDelete,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete card",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
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
                config.title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Timeline Range Selection
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "TIMELINE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )

                        TimelineFilterBar(
                            currentRange = config.timelineRange,
                            onRangeSelected = { range -> onUpdate(config.copy(timelineRange = range)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    )
}

data class BalanceForecastData(
    val forecastBalance: BigDecimal,
    val currency: String,
    val precision: Int,
    val chartData: List<Triple<Double, Double, Double>>
)

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun BalanceForecastCardPreview() {
    OssWalletTheme {
        BalanceForecastCard(
            config = BalanceForecastCardConfig(
                cardId = "preview",
                title = "BALANCE FORECAST",
                subtitle = "Expected savings trend",
                forecastDays = 30
            ),
            data = BalanceForecastData(
                forecastBalance = BigDecimal("18500.00"),
                currency = "USD",
                precision = 2,
                chartData = listOf(
                    Triple(15000.0, 15000.0, 15000.0),
                    Triple(16500.0, 16000.0, 15500.0),
                    Triple(18000.0, 17200.0, 16400.0),
                    Triple(20000.0, 18500.0, 17000.0)
                )
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}
