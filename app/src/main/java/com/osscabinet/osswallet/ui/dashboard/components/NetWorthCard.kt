package com.osscabinet.ossWallet.ui.dashboard.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.ui.components.AeonFlowSectionHeader
import com.osscabinet.ossWallet.ui.components.cards.AeonCardSettingsSheet
import com.osscabinet.ossWallet.ui.theme.NegativeAmountColor
import com.osscabinet.ossWallet.ui.theme.PositiveAmountColor
import com.osscabinet.ossWallet.ui.util.formatCurrencyRight
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

import com.osscabinet.ossWallet.domain.card.CardFilterRules

@Composable
fun NetWorthCard(
    netWorth: BigDecimal,
    netIncome: BigDecimal,
    netExpense: BigDecimal,
    baseCurrency: String,
    precision: Int,
    cardId: String,
    fontSize: String,
    filter: TransactionFilter,
    onFontSizeChange: (String, String) -> Unit,
    onFilterChange: (String, TransactionFilter) -> Unit,
    onDelete: () -> Unit = {},
    onClickIncome: (TimelineRange) -> Unit = {},
    onClickExpense: (TimelineRange) -> Unit = {},
    modifier: Modifier = Modifier,
    headerModifier: Modifier = Modifier,
    filterRules: CardFilterRules = CardFilterRules.DEFAULT
) {
    var showDisplaySettings by remember { mutableStateOf(false) }
    var showFilterSettings by remember { mutableStateOf(false) }

    val textStyle = when (fontSize) {
        "Small" -> MaterialTheme.typography.displaySmall
        "Large" -> MaterialTheme.typography.displayLarge
        "X-Large" -> MaterialTheme.typography.displayLarge.copy(fontSize = 64.sp)
        else -> MaterialTheme.typography.displayMedium
    }

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

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AeonFlowSectionHeader(
                title = "NET WORTH",
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
                        color = PositiveAmountColor,
                        isIncome = true,
                        onClick = { onClickIncome(filter.timelineRange) }
                    )
                    FlowBadge(
                        label = "EXPENSE",
                        amount = netExpense,
                        currency = baseCurrency,
                        precision = precision,
                        color = NegativeAmountColor,
                        isIncome = false,
                        onClick = { onClickExpense(filter.timelineRange) }
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

    if (showDisplaySettings) {
        NetWorthDisplaySettingsDialog(
            fontSize = fontSize,
            cardId = cardId,
            onFontSizeChange = onFontSizeChange,
            onDismiss = { showDisplaySettings = false },
            onDelete = onDelete
        )
    }

    if (showFilterSettings) {
        AeonCardSettingsSheet(
            title = "Net Worth Filter",
            filter = filter,
            onFilterChange = { newFilter -> onFilterChange(cardId, newFilter) },
            onDismiss = { showFilterSettings = false },
            onDelete = onDelete,
            filterRules = filterRules
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
    isIncome: Boolean,
    onClick: () -> Unit = {}
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
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
private fun NetWorthDisplaySettingsDialog(
    fontSize: String,
    cardId: String,
    onFontSizeChange: (String, String) -> Unit,
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
                "Net Worth Display",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
                                    onClick = { onFontSizeChange(cardId, size) },
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
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    )
}
