package com.osscabinet.ossWallet.ui.analytics.components

import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Filter
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.settings.CashFlowTrendCardConfig
import com.osscabinet.ossWallet.ui.components.AeonFlowSectionHeader
import com.osscabinet.ossWallet.ui.components.AeonSettingsToggle
import com.osscabinet.ossWallet.ui.components.cards.AeonCardSettingsSheet
import com.osscabinet.ossWallet.ui.components.visualization.charts.AeonBarLineChart
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.theme.PositiveAmountColor
import com.osscabinet.ossWallet.ui.theme.NegativeAmountColor
import com.osscabinet.ossWallet.ui.theme.AccentBlue
import com.osscabinet.ossWallet.ui.theme.labelExtraSmall
import com.osscabinet.ossWallet.ui.util.formatCurrencyRight
import androidx.compose.material.icons.filled.Info
import java.math.BigDecimal

@Composable
fun CashFlowTrendCard(
    config: CashFlowTrendCardConfig,
    data: CashFlowTrendData,
    modifier: Modifier = Modifier,
    headerModifier: Modifier = Modifier,
    onUpdate: (CashFlowTrendCardConfig) -> Unit = {},
    onDelete: () -> Unit = {},
    onRecordsClick: (com.osscabinet.ossWallet.ui.navigation.RecordsFilterContext) -> Unit = {},
    accounts: List<Account> = emptyList(),
    allCategories: List<Category> = emptyList(),
    savedFilters: List<Filter> = emptyList()
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

            if (data.percentageChange != "0.0%") {
                val isNegative = data.percentageChange.startsWith("-")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Surface(
                        color = (if (isNegative) NegativeAmountColor else PositiveAmountColor).copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "${data.percentageChange} vs Previous Period",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isNegative) NegativeAmountColor else PositiveAmountColor
                        )
                    }
                }
            }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
            ) {
                if (data.barData.isNotEmpty() && data.lineData.isNotEmpty()) {
                    AeonBarLineChart(
                        barData = data.barData.map { it.toBigDecimal() },
                        lineData = data.lineData.map { it.toBigDecimal() },
                        smaData = if (config.showSMA) data.smaData else null,
                        barColorPositive = PositiveAmountColor,
                        barColorNegative = NegativeAmountColor,
                        lineColor = AccentBlue,
                        modifier = Modifier.fillMaxSize(),
                        onNavigateToRecords = { _ ->
                            onRecordsClick(com.osscabinet.ossWallet.ui.navigation.RecordsFilterContext.Custom(
                                startDate = config.filter.startDate,
                                endDate = config.filter.endDate,
                                accountIds = config.filter.accountIds,
                                categoryIds = config.filter.categoryIds.map { it.toString() },
                                currencies = config.filter.currencies
                            ))
                        }
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                        Text("No cash flow data", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            
            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                LegendItem("Income", PositiveAmountColor)
                LegendItem("Expenses", NegativeAmountColor)
                LegendItem("Net Trend", AccentBlue, isLine = true)
                if (config.showSMA) LegendItem("SMA", AccentBlue.copy(alpha = 0.5f), isLine = true)
            }
            
            // Volatility Analysis
            data.volatility?.let { vol ->
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                        Column {
                            Text(
                                "Cash Flow Volatility: $vol",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.secondary
                            )
                            if (vol.toDouble() > 1000) {
                                Text(
                                    "High volatility detected. Consider increasing emergency buffer.",
                                    style = MaterialTheme.typography.labelExtraSmall,
                                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
                                )
                            }
                        }
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
            // SMA Toggle
            AeonSettingsToggle(
                label = "SHOW SMA",
                description = "Simple Moving Average for smoothing",
                checked = config.showSMA,
                onCheckedChange = { onUpdate(config.copy(showSMA = it)) }
            )
        }
    }
}

@Composable
private fun LegendItem(label: String, color: Color, isLine: Boolean = false) {
    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .size(if (isLine) 16.dp else 12.dp, if (isLine) 3.dp else 12.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

data class CashFlowTrendData(
    val barData: List<Double>,
    val lineData: List<Double>,
    val smaData: List<Double>? = null,
    val volatility: BigDecimal? = null,
    val percentageChange: String = "0.0%"
)

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun CashFlowTrendCardPreview() {
    OssWalletTheme {
        CashFlowTrendCard(
            config = CashFlowTrendCardConfig(
                cardId = "preview",
                title = "CASH FLOW TREND"
            ),
            data = CashFlowTrendData(
                barData = listOf(500.0, -200.0, 800.0, 400.0),
                lineData = listOf(500.0, 300.0, 1100.0, 1500.0)
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}
