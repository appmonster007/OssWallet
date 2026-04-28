package com.osscabinet.ossWallet.ui.analytics.components

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Filter
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.settings.PlannedPaymentsCardConfig
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import java.text.SimpleDateFormat
import java.util.*
import com.osscabinet.ossWallet.ui.components.AeonFlowSectionHeader
import androidx.compose.foundation.clickable
import com.osscabinet.ossWallet.ui.components.cards.AeonCardSettingsSheet
import com.osscabinet.ossWallet.ui.components.visualization.AeonProgressBar
import com.osscabinet.ossWallet.ui.theme.PositiveAmountColor
import com.osscabinet.ossWallet.ui.theme.AccentOrange
import com.osscabinet.ossWallet.ui.theme.AccentPurple
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.util.formatCurrencyRight
import java.math.BigDecimal

@Composable
fun PlannedPaymentsCard(
    config: PlannedPaymentsCardConfig,
    data: PlannedPaymentsData,
    modifier: Modifier = Modifier,
    headerModifier: Modifier = Modifier,
    onUpdate: (PlannedPaymentsCardConfig) -> Unit = {},
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
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                config.subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Liquidity Risk Overview
            if (data.payments.any { it.isAtRisk }) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        Text(
                            "Liquidity Risk: ${data.payments.count { it.isAtRisk }} payments exceed current cash",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            // Heatmap Timeline
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "OUTGOING VOLUME HEATMAP",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    color = MaterialTheme.colorScheme.primary
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .drawBehind {
                            val blocks = 14 // 2 weeks
                            val blockWidth = size.width / blocks
                            val maxVolume = data.dailyVolumeHeatmap.maxOfOrNull { it.second } ?: 1.0
                            
                            data.dailyVolumeHeatmap.takeLast(blocks).forEachIndexed { i, (_, volume) ->
                                val alpha = (volume / maxVolume).toFloat().coerceIn(0.1f, 1.0f)
                                drawRect(
                                    color = AccentOrange.copy(alpha = alpha),
                                    topLeft = Offset(i * blockWidth + 2.dp.toPx(), 0f),
                                    size = Size(blockWidth - 4.dp.toPx(), size.height)
                                )
                            }
                        }
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Total
            Column {
                Text(
                    "Total Planned",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    formatCurrencyRight(data.total, data.currency, data.precision),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Payment items
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                data.payments.forEach { payment ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                onRecordsClick(com.osscabinet.ossWallet.ui.navigation.RecordsFilterContext.Custom(
                                    queryNote = payment.name,
                                    startDate = config.filter.startDate,
                                    endDate = config.filter.endDate,
                                    accountIds = config.filter.accountIds,
                                    categoryIds = config.filter.categoryIds.map { it.toString() },
                                    currencies = config.filter.currencies
                                ))
                            }
                            .padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    payment.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (payment.isAtRisk) {
                                    Icon(
                                        Icons.Default.Warning,
                                        null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            Text(
                                formatCurrencyRight(payment.amount, data.currency, data.precision),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (payment.isAtRisk) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        AeonProgressBar(
                            progress = payment.progress,
                            color = payment.color,
                            height = 8.dp
                        )
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
        )
    }
}

data class PlannedPaymentsData(
    val total: BigDecimal,
    val currency: String,
    val precision: Int,
    val payments: List<PaymentItem>,
    val currentLiquidCash: BigDecimal = BigDecimal.ZERO,
    val dailyVolumeHeatmap: List<Pair<Long, Double>> = emptyList() // Timestamp to volume
)

data class PaymentItem(
    val name: String,
    val amount: BigDecimal,
    val progress: Float,
    val color: Color,
    val isAtRisk: Boolean = false
)

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun PlannedPaymentsCardPreview() {
    OssWalletTheme {
        PlannedPaymentsCard(
            config = PlannedPaymentsCardConfig(
                cardId = "preview",
                title = "PLANNED PAYMENTS",
                subtitle = "Upcoming obligations"
            ),
            data = PlannedPaymentsData(
                total = BigDecimal("1250.00"),
                currency = "USD",
                precision = 2,
                payments = listOf(
                    PaymentItem("Rent", BigDecimal("800.00"), 1.0f, AccentPurple),
                    PaymentItem("Electricity", BigDecimal("120.00"), 0.5f, AccentOrange),
                    PaymentItem("Internet", BigDecimal("80.00"), 0.2f, PositiveAmountColor)
                )
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}
