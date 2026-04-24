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
import com.osscabinet.ossWallet.domain.model.settings.CashFlowTrendCardConfig
import com.osscabinet.ossWallet.ui.components.AeonFlowSectionHeader
import com.osscabinet.ossWallet.ui.theme.PositiveAmountColor
import com.osscabinet.ossWallet.ui.theme.NegativeAmountColor
import com.osscabinet.ossWallet.ui.theme.AccentBlue

@Composable
fun CashFlowTrendCard(
    config: CashFlowTrendCardConfig,
    data: CashFlowTrendData,
    modifier: Modifier = Modifier,
    onUpdate: (CashFlowTrendCardConfig) -> Unit = {},
    onDelete: () -> Unit = {}
) {
    var showSettings by remember { mutableStateOf(false) }
    var selectedBarIndex by remember { mutableStateOf<Int?>(null) }
    
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
            AeonFlowSectionHeader(
                title = config.title,
                trailing = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
            
            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(PositiveAmountColor, RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Income", style = MaterialTheme.typography.labelSmall)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(NegativeAmountColor, RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Expenses", style = MaterialTheme.typography.labelSmall)
                }
            }
            
            // Interactive bar + line chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { selectedBarIndex = null },
                contentAlignment = Alignment.Center
            ) {
                if (data.barData.isNotEmpty() && data.lineData.isNotEmpty()) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        // Bar chart for cash flow
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            data.barData.forEachIndexed { index, value ->
                                val maxValue = data.barData.maxOrNull() ?: 1.0
                                val normalizedHeight = if (maxValue > 0) {
                                    (value / maxValue).toFloat()
                                } else {
                                    0.1f
                                }
                                val isSelected = selectedBarIndex == index
                                
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(16.dp)
                                            .height(120.dp * normalizedHeight.coerceIn(0.1f, 1.0f))
                                            .background(
                                                if (value >= 0) PositiveAmountColor else NegativeAmountColor,
                                                RoundedCornerShape(4.dp)
                                            )
                                            .clickable { selectedBarIndex = index },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Text(
                                                value.toString(),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Line chart for trend overlay
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            data.lineData.forEachIndexed { index, value ->
                                val maxValue = data.lineData.maxOrNull() ?: 1.0
                                val normalizedHeight = if (maxValue > 0) {
                                    (value / maxValue).toFloat()
                                } else {
                                    0.1f
                                }
                                
                                Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(
                                                AccentBlue,
                                                CircleShape
                                            )
                                    )
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
                Text("Configure cash flow trend settings")
            }
        )
    }
}

data class CashFlowTrendData(
    val barData: List<Double>,
    val lineData: List<Double>
)
