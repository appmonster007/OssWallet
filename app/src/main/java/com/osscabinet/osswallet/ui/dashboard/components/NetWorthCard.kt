package com.osscabinet.ossWallet.ui.dashboard.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.ui.components.AeonFlowSectionHeader
import com.osscabinet.ossWallet.ui.components.TimelineFilterBar
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.ui.util.formatCurrencyRight
import com.osscabinet.ossWallet.ui.viewmodel.NetWorthUiState
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Serializable
data class NetWorthSettings(
    val fontSize: String = "Medium", // "Small", "Medium", "Large"
    val timelineRange: TimelineRange = TimelineRange.AllTime
)

@Composable
fun NetWorthCard(
    uiState: NetWorthUiState,
    onFontSizeChange: (String) -> Unit,
    onTimelineRangeChange: (TimelineRange) -> Unit,
    modifier: Modifier = Modifier,
    headerModifier: Modifier = Modifier
) {
    when (val state = uiState) {
        is NetWorthUiState.Loading -> {
            Spacer(modifier = modifier)
        }
        is NetWorthUiState.Success -> {
            NetWorthContent(
                netWorth = state.netWorth,
                netIncome = state.netIncome,
                netExpense = state.netExpense,
                baseCurrency = state.baseCurrency,
                precision = state.precision,
                fontSize = state.fontSize,
                timelineRange = state.timelineRange,
                onFontSizeChange = onFontSizeChange,
                onTimelineRangeChange = onTimelineRangeChange,
                modifier = modifier,
                headerModifier = headerModifier
            )
        }
        is NetWorthUiState.Error -> {
            Text("Error: ${state.message}")
        }
    }
}

@Composable
private fun NetWorthContent(
    netWorth: BigDecimal,
    netIncome: BigDecimal,
    netExpense: BigDecimal,
    baseCurrency: String,
    precision: Int,
    fontSize: String,
    timelineRange: TimelineRange,
    onFontSizeChange: (String) -> Unit,
    onTimelineRangeChange: (TimelineRange) -> Unit,
    modifier: Modifier = Modifier,
    headerModifier: Modifier = Modifier
) {
    var showSettings by remember { mutableStateOf(false) }

    val textStyle = when (fontSize) {
        "Small" -> MaterialTheme.typography.displaySmall
        "Large" -> MaterialTheme.typography.displayLarge
        "X-Large" -> MaterialTheme.typography.displayLarge.copy(fontSize = 64.sp)
        else -> MaterialTheme.typography.displayMedium
    }

    val timelineLabel = when (timelineRange) {
        is TimelineRange.AllTime -> "All Time"
        is TimelineRange.ThisPeriod -> timelineRange.period.label
        is TimelineRange.LastPeriod -> timelineRange.period.label
        is TimelineRange.CustomRange -> {
            val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy").withZone(ZoneId.systemDefault())
            val start = formatter.format(Instant.ofEpochMilli(timelineRange.start))
            val end = formatter.format(Instant.ofEpochMilli(timelineRange.end))
            "$start - $end"
        }
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AeonFlowSectionHeader(
                title = "NET WORTH",
                modifier = headerModifier,
                trailing = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = timelineLabel.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                fontSize = 10.sp
                            ),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        )
                        IconButton(
                            onClick = { showSettings = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.MoreHoriz,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    FlowBadge(
                        label = "INCOME",
                        amount = netIncome,
                        currency = baseCurrency,
                        precision = precision,
                        color = Color(0xFF4CAF50),
                        isIncome = true
                    )
                    FlowBadge(
                        label = "EXPENSE",
                        amount = netExpense,
                        currency = baseCurrency,
                        precision = precision,
                        color = Color(0xFFF44336),
                        isIncome = false
                    )
                }

                Text(
                    text = formatCurrencyRight(netWorth, baseCurrency, precision),
                    style = textStyle.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-1).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }

    if (showSettings) {
        NetWorthSettingsDialog(
            fontSize = fontSize,
            timelineRange = timelineRange,
            onFontSizeChange = onFontSizeChange,
            onTimelineRangeChange = onTimelineRangeChange,
            onDismiss = { showSettings = false }
        )
    }
}

@Composable
private fun FlowBadge(
    label: String,
    amount: BigDecimal,
    currency: String,
    precision: Int,
    color: Color,
    isIncome: Boolean
) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                if (isIncome) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                contentDescription = null,
                modifier = Modifier.size(10.dp),
                tint = color
            )
            Text(
                text = "${label}: ${formatCurrencyRight(amount, currency, precision)}",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    letterSpacing = 0.5.sp
                ),
                color = color
            )
        }
    }
}

@Composable
private fun NetWorthSettingsDialog(
    fontSize: String,
    timelineRange: TimelineRange,
    onFontSizeChange: (String) -> Unit,
    onTimelineRangeChange: (TimelineRange) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        },
        title = {
            Text(
                "Net Worth Settings",
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
                            currentRange = timelineRange,
                            onRangeSelected = onTimelineRangeChange,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Font Size Selection
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
                            "FONT SIZE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )

                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            listOf("Small", "Medium", "Large", "X-Large").forEachIndexed { index, size ->
                                SegmentedButton(
                                    selected = fontSize == size,
                                    onClick = { onFontSizeChange(size) },
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = 4
                                    ),
                                    label = { 
                                        Text(
                                            when(size) {
                                                "Small" -> "S"
                                                "Medium" -> "M"
                                                "Large" -> "L"
                                                else -> "XL"
                                            }
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        shape = RoundedCornerShape(32.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}
