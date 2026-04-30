package com.osscabinet.ossWallet.ui.dashboard.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.model.analytics.CurrencyAggregationItem
import com.osscabinet.ossWallet.ui.components.*
import com.osscabinet.ossWallet.ui.components.cards.AeonCardSettingsSheet
import com.osscabinet.ossWallet.ui.util.formatCurrencyRight
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

import com.osscabinet.ossWallet.domain.card.CardFilterRules

@Composable
fun CurrencyBalancesCard(
    currencyTotals: List<CurrencyAggregationItem>,
    baseCurrency: String,
    cardId: String,
    modifier: Modifier = Modifier,
    headerModifier: Modifier = Modifier,
    title: String = "CURRENCY BALANCES",
    precision: Int = 2,
    layout: String = "scroll", // "scroll" or "grid"
    showBaseConversion: Boolean = true,
    filter: TransactionFilter = TransactionFilter(),
    onLayoutChange: (String, String) -> Unit = { _, _ -> },
    onShowBaseConversionChange: (String, Boolean) -> Unit = { _, _ -> },
    onFilterChange: (String, TransactionFilter) -> Unit = { _, _ -> },
    onDelete: () -> Unit = {},
    onCurrencyClick: (String, TimelineRange) -> Unit = { _, _ -> },
    filterRules: CardFilterRules = CardFilterRules.DEFAULT
) {
    var showDisplaySettings by remember { mutableStateOf(false) }
    var showFilterSettings by remember { mutableStateOf(false) }

    val timelineLabel = when (val tr = filter.timelineRange) {
        is TimelineRange.AllTime -> "All Time"
        is TimelineRange.ThisPeriod -> tr.period.label
        is TimelineRange.LastPeriod -> tr.period.label
        is TimelineRange.CustomRange -> {
            val formatter = DateTimeFormatter.ofPattern("MMM d").withZone(ZoneId.systemDefault())
            val start = formatter.format(Instant.ofEpochMilli(tr.start))
            val end = formatter.format(Instant.ofEpochMilli(tr.end))
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
                    filter = filter,
                    trailing = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
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
                                onClick = { showFilterSettings = true },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.FilterList,
                                    contentDescription = "Filter",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = { showDisplaySettings = true },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.Tune,
                                    contentDescription = "Display Settings",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
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
                                        onClick = { onCurrencyClick(item.currency, filter.timelineRange) },
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
                                showBaseConversion = showBaseConversion,
                                onClick = { onCurrencyClick(item.currency, filter.timelineRange) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDisplaySettings) {
        CurrencyDisplaySettingsDialog(
            layout = layout,
            showBaseConversion = showBaseConversion,
            baseCurrency = baseCurrency,
            cardId = cardId,
            onLayoutChange = onLayoutChange,
            onShowBaseConversionChange = onShowBaseConversionChange,
            onDismiss = { showDisplaySettings = false },
            onDelete = onDelete
        )
    }

    if (showFilterSettings) {
        AeonCardSettingsSheet(
            title = "Currency Balances Filter",
            filter = filter,
            onFilterChange = { newFilter -> onFilterChange(cardId, newFilter) },
            onDismiss = { showFilterSettings = false },
            onDelete = onDelete,
            filterRules = filterRules
        )
    }
}

@Composable
private fun CurrencyDisplaySettingsDialog(
    layout: String,
    showBaseConversion: Boolean,
    baseCurrency: String,
    cardId: String,
    onLayoutChange: (String, String) -> Unit,
    onShowBaseConversionChange: (String, Boolean) -> Unit,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        dismissButton = {
            TextButton(
                onClick = {
                    onDelete()
                    onDismiss()
                },
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
                "Currency Display",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
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

@Composable
fun CurrencyBalanceItem(
    curr: String,
    amount: BigDecimal,
    inBase: BigDecimal,
    baseCurrency: String,
    precision: Int = 2,
    showBaseConversion: Boolean = true,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
        modifier = modifier.clickable { onClick() }
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
