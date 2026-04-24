package com.osscabinet.ossWallet.ui.analytics.components

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.model.settings.PeriodComparisonCardConfig
import com.osscabinet.ossWallet.ui.components.AeonFlowSectionHeader
import com.osscabinet.ossWallet.ui.theme.AccentBlue
import com.osscabinet.ossWallet.ui.util.formatCurrencyRight
import java.math.BigDecimal

@Composable
fun PeriodComparisonCard(
    config: PeriodComparisonCardConfig,
    data: PeriodComparisonData,
    modifier: Modifier = Modifier,
    onUpdate: (PeriodComparisonCardConfig) -> Unit = {},
    onDelete: () -> Unit = {}
) {
    var showSettings by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(config.selectedTab) }
    
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
            
            // Current value
            Text(
                formatCurrencyRight(data.currentValue, data.currency, data.precision),
                fontSize = 24.sp,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            
            // Tabs
            Row(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                listOf("Cash flow", "Expenses", "Income").forEach { tab ->
                    val isSelected = tab == selectedTab
                    Text(
                        tab,
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (isSelected) AccentBlue else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(8.dp),
                        textAlign = TextAlign.Center,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }
            
            // Simple placeholder for line chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Chart visualization coming soon",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                Text("Configure period comparison settings")
            }
        )
    }
}

data class PeriodComparisonData(
    val currentValue: BigDecimal,
    val currency: String,
    val precision: Int,
    val chartData: List<Double>
)
