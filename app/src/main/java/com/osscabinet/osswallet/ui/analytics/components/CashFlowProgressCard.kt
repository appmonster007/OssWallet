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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.model.settings.CashFlowProgressCardConfig
import com.osscabinet.ossWallet.ui.components.AeonFlowSectionHeader
import com.osscabinet.ossWallet.ui.components.TimelineFilterBar
import com.osscabinet.ossWallet.ui.components.visualization.AeonProgressBar
import com.osscabinet.ossWallet.ui.theme.PositiveAmountColor
import com.osscabinet.ossWallet.ui.theme.NegativeAmountColor
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.util.formatCurrencyRight
import java.math.BigDecimal

@Composable
fun CashFlowProgressCard(
    config: CashFlowProgressCardConfig,
    data: CashFlowProgressData,
    modifier: Modifier = Modifier,
    headerModifier: Modifier = Modifier,
    onUpdate: (CashFlowProgressCardConfig) -> Unit = {},
    onDelete: () -> Unit = {}
) {
    var showSettings by remember { mutableStateOf(false) }
    
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
                modifier = headerModifier,
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
            
            // Income progress
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Income",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        formatCurrencyRight(data.income, data.currency, data.precision),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                val incomeProgress = if (data.expenses.abs().signum() > 0) {
                    data.income.divide(data.expenses.abs(), 2, java.math.RoundingMode.HALF_UP).toFloat().coerceIn(0f, 1f)
                } else 0f
                
                AeonProgressBar(
                    progress = incomeProgress,
                    color = PositiveAmountColor,
                    height = 12.dp
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Expenses progress
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Expenses",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        formatCurrencyRight(data.expenses, data.currency, data.precision),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                AeonProgressBar(
                    progress = 1f,
                    color = NegativeAmountColor,
                    height = 12.dp
                )
            }
        }
    }
    
    if (showSettings) {
        CashFlowProgressSettingsDialog(
            config = config,
            onUpdate = onUpdate,
            onDismiss = { showSettings = false },
            onDelete = onDelete
        )
    }
}

@Composable
private fun CashFlowProgressSettingsDialog(
    config: CashFlowProgressCardConfig,
    onUpdate: (CashFlowProgressCardConfig) -> Unit,
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

data class CashFlowProgressData(
    val income: BigDecimal,
    val expenses: BigDecimal,
    val currency: String,
    val precision: Int
)

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun CashFlowProgressCardPreview() {
    OssWalletTheme {
        CashFlowProgressCard(
            config = CashFlowProgressCardConfig(
                cardId = "preview",
                title = "CASH FLOW PROGRESS",
                subtitle = "Monthly target vs actual"
            ),
            data = CashFlowProgressData(
                income = BigDecimal("5000.00"),
                expenses = BigDecimal("3500.00"),
                currency = "USD",
                precision = 2
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}
