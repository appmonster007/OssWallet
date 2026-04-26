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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.data.local.entity.CategoryEntity
import com.osscabinet.ossWallet.data.local.entity.FilterEntity
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.settings.BalanceForecastCardConfig
import com.osscabinet.ossWallet.ui.components.AeonFlowSectionHeader
import com.osscabinet.ossWallet.ui.components.AeonSettingsToggle
import com.osscabinet.ossWallet.ui.components.cards.AeonCardSettingsSheet
import com.osscabinet.ossWallet.ui.components.visualization.charts.AeonLineChart
import com.osscabinet.ossWallet.ui.components.visualization.charts.AeonLineChartSeries
import com.osscabinet.ossWallet.ui.components.visualization.charts.AeonLineChartPoint
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.theme.AccentBlue
import com.osscabinet.ossWallet.ui.theme.labelExtraSmall
import com.osscabinet.ossWallet.ui.util.formatCurrencyRight
import java.math.BigDecimal

@Composable
fun BalanceForecastCard(
    config: BalanceForecastCardConfig,
    data: BalanceForecastData,
    modifier: Modifier = Modifier,
    headerModifier: Modifier = Modifier,
    onUpdate: (BalanceForecastCardConfig) -> Unit = {},
    onDelete: () -> Unit = {},
    onRecordsClick: (com.osscabinet.ossWallet.ui.navigation.RecordsFilterContext) -> Unit = {},
    accounts: List<Account> = emptyList(),
    allCategories: List<CategoryEntity> = emptyList(),
    savedFilters: List<FilterEntity> = emptyList()
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
            
            Column {
                Text(
                    "PROJECTED BALANCE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    formatCurrencyRight(data.forecastBalance, data.currency, data.precision),
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold)
                )
                Text(
                    "In ${config.forecastDays} days",
                    style = MaterialTheme.typography.labelExtraSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                if (data.chartData.isNotEmpty()) {
                    val series = listOf(
                        AeonLineChartSeries(
                            name = "Realistic",
                            color = AccentBlue,
                            dataPoints = data.chartData.mapIndexed { i, d -> AeonLineChartPoint(i.toFloat(), d.second.toBigDecimal()) },
                            confidenceIntervals = data.chartData.map { d -> d.third.toBigDecimal() to d.first.toBigDecimal() }
                        )
                    )
                    
                    AeonLineChart(
                        series = series,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            
            if (data.projectedZeroDate != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Warning: Projected to reach zero on ${java.text.DateFormat.getDateInstance().format(data.projectedZeroDate)}",
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
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
            // Forecast Days Slider
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("FORECAST HORIZON", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Slider(
                    value = config.forecastDays.toFloat(),
                    onValueChange = { onUpdate(config.copy(forecastDays = it.toInt())) },
                    valueRange = 7f..90f,
                    steps = 83
                )
                Text("${config.forecastDays} Days", style = MaterialTheme.typography.labelSmall)
            }

            AeonSettingsToggle(
                label = "INCLUDE PLANNED",
                description = "Consider upcoming Obligations",
                checked = config.includePlannedPayments,
                onCheckedChange = { onUpdate(config.copy(includePlannedPayments = it)) }
            )
        }
    }
}

data class BalanceForecastData(
    val forecastBalance: BigDecimal,
    val currency: String,
    val precision: Int,
    val chartData: List<Triple<Double, Double, Double>>, // Optimistic, Realistic, Pessimistic
    val projectedZeroDate: Long? = null,
    val currentLiquidCash: BigDecimal = BigDecimal.ZERO
)

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun BalanceForecastCardPreview() {
    OssWalletTheme {
        BalanceForecastCard(
            config = BalanceForecastCardConfig(cardId = "preview", title = "BALANCE FORECAST"),
            data = BalanceForecastData(
                forecastBalance = BigDecimal("15000.00"),
                currency = "USD",
                precision = 2,
                chartData = listOf(
                    Triple(11000.0, 10000.0, 9000.0),
                    Triple(13000.0, 12000.0, 11000.0),
                    Triple(16000.0, 15000.0, 14000.0)
                )
            )
        )
    }
}
