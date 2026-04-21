package com.osscabinet.ossWallet.ui.dashboard.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.usecase.CurrencyAggregationItem
import com.osscabinet.ossWallet.ui.components.*
import com.osscabinet.ossWallet.ui.util.formatCurrencyRight
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Serializable
data class CurrencyBalancesSettings(
    val layout: String = "scroll", // "scroll" or "grid"
    val showBaseConversion: Boolean = true,
    val timelineRange: TimelineRange = TimelineRange.AllTime
)

@Composable
fun CurrencyBalancesCard(
    currencyTotals: List<CurrencyAggregationItem>,
    baseCurrency: String,
    cardId: String,
    modifier: Modifier = Modifier,
    headerModifier: Modifier = Modifier,
    title: String = "AGGREGATED CURRENCY BALANCES",
    precision: Int = 2,
    layout: String = "scroll", // "scroll" or "grid"
    showBaseConversion: Boolean = true,
    timelineRange: TimelineRange = TimelineRange.AllTime,
    onLayoutChange: (String, String) -> Unit = { _, _ -> },
    onShowBaseConversionChange: (String, Boolean) -> Unit = { _, _ -> },
    onTimelineRangeChange: (String, TimelineRange) -> Unit = { _, _ -> },
    onDelete: () -> Unit = {}
) {
    var showSettings by remember { mutableStateOf(false) }

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

    if (currencyTotals.isNotEmpty()) {
        Card(
            modifier = modifier,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AeonFlowSectionHeader(
                    title = title,
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

                if (layout == "grid") {
                    val chunks = currencyTotals.chunked(3)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        chunks.forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowItems.forEach { item ->
                                    CurrencyBalanceItem(
                                        curr = item.currency,
                                        amount = item.amount,
                                        inBase = item.amountInBase,
                                        baseCurrency = baseCurrency,
                                        precision = precision,
                                        showBaseConversion = showBaseConversion,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (rowItems.size < 3) {
                                    repeat(3 - rowItems.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 4.dp)
                    ) {
                        items(currencyTotals) { item ->
                            CurrencyBalanceItem(
                                curr = item.currency,
                                amount = item.amount,
                                inBase = item.amountInBase,
                                baseCurrency = baseCurrency,
                                precision = precision,
                                showBaseConversion = showBaseConversion
                            )
                        }
                    }
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
                TextButton(onClick = { showSettings = false }) {
                    Text("Done")
                }
            },
            title = {
                Text(
                    "Currency Settings",
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
                                onRangeSelected = { range -> onTimelineRangeChange(cardId, range) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    AeonLayoutToggle(
                        layout = layout,
                        onLayoutChange = { newLayout -> onLayoutChange(cardId, newLayout) }
                    )

                    AeonBaseConversionToggle(
                        baseCurrency = baseCurrency,
                        showBaseConversion = showBaseConversion,
                        onShowBaseConversionChange = { show -> onShowBaseConversionChange(cardId, show) }
                    )
                }
            },
            shape = RoundedCornerShape(32.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    }
}

@Composable
fun CurrencyBalanceItem(
    curr: String,
    amount: BigDecimal,
    inBase: BigDecimal,
    baseCurrency: String,
    precision: Int = 2,
    showBaseConversion: Boolean = true,
    modifier: Modifier = Modifier
) {

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                curr,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.fillMaxWidth()
            )
            
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    formatCurrencyRight(amount, curr, precision),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFeatureSettings = "tnum",
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )
                
                if (showBaseConversion && curr != baseCurrency) {
                    Text(
                        "≈ ${formatCurrencyRight(inBase, baseCurrency, precision)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1
                    )
                } else if (showBaseConversion) {
                    Text(
                        "",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.height(14.dp)
                    )
                }
            }
        }
    }
}
