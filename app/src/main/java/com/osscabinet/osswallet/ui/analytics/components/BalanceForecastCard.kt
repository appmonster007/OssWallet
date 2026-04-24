package com.osscabinet.ossWallet.ui.analytics.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import com.osscabinet.ossWallet.ui.theme.PositiveAmountColor
import com.osscabinet.ossWallet.ui.theme.NegativeAmountColor
import com.osscabinet.ossWallet.ui.theme.AccentBlue
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
    var selectedPointIndex by remember { mutableStateOf<Int?>(null) }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Column {
                AeonFlowSectionHeader(
                    title = config.title,
                    trailing = {
                        IconButton(onClick = { showSettings = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                )
                Text(
                    config.subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Forecast value with trend
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    formatCurrencyRight(data.forecastBalance, data.currency, data.precision),
                    fontSize = 24.sp,
                    color = if (data.forecastBalance >= java.math.BigDecimal.ZERO) 
                        PositiveAmountColor 
                    else 
                        NegativeAmountColor,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "in $forecastDays days",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Forecast days slider
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Forecast Days: $forecastDays",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium
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
            
            // Interactive waterfall chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { selectedPointIndex = null },
                contentAlignment = Alignment.Center
            ) {
                if (data.chartData.isNotEmpty()) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        val maxBalance = data.chartData.maxOfOrNull { it.first } ?: 1.0
                        val minBalance = data.chartData.minOfOrNull { it.third } ?: 0.0
                        val range = maxBalance - minBalance
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            data.chartData.forEachIndexed { index, (income, expense, balance) ->
                                val normalizedHeight = if (range > 0) {
                                    ((balance - minBalance) / range).toFloat()
                                } else {
                                    0.5f
                                }
                                val isSelected = selectedPointIndex == index
                                
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // Income bar (top)
                                    if (income > 0) {
                                        val incomeHeight = (income / maxBalance).toFloat().coerceIn(0f, 0.3f)
                                        Box(
                                            modifier = Modifier
                                                .width(10.dp)
                                                .height(100.dp * incomeHeight)
                                                .background(
                                                    PositiveAmountColor.copy(alpha = 0.7f),
                                                    RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                                )
                                        )
                                    }
                                    
                                    // Balance bar (middle)
                                    Box(
                                        modifier = Modifier
                                            .width(10.dp)
                                            .height(100.dp * normalizedHeight.coerceIn(0.1f, 1.0f))
                                            .background(
                                                if (balance >= 0) AccentBlue else NegativeAmountColor,
                                                RoundedCornerShape(4.dp)
                                            )
                                            .clickable { selectedPointIndex = index },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Text(
                                                balance.toInt().toString(),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White
                                            )
                                        }
                                    }
                                    
                                    // Expense bar (bottom)
                                    if (expense > 0) {
                                        val expenseHeight = (expense / maxBalance).toFloat().coerceIn(0f, 0.3f)
                                        Box(
                                            modifier = Modifier
                                                .width(10.dp)
                                                .height(100.dp * expenseHeight)
                                                .background(
                                                    NegativeAmountColor.copy(alpha = 0.7f),
                                                    RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp)
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        "No data available",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
    
    if (showSettings) {
        AlertDialog(
            onDismissRequest = { showSettings = false },
            dismissButton = {
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete card", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete")
                }
            },
            confirmButton = {
                TextButton(onClick = { showSettings = false }) {
                    Text("Done")
                }
            },
            title = {
                Text(config.title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
            },
            text = {
                Text("Configure balance forecast settings")
            }
        )
    }
}

data class BalanceForecastData(
    val forecastBalance: BigDecimal,
    val currency: String,
    val precision: Int,
    val chartData: List<Triple<Double, Double, Double>>
)
