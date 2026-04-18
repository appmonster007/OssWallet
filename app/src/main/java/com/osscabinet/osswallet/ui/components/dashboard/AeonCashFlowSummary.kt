package com.osscabinet.ossWallet.ui.components.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.model.CashFlowType
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.usecase.CashFlowResult
import com.osscabinet.ossWallet.ui.components.*
import com.osscabinet.ossWallet.ui.util.convertAmount
import com.osscabinet.ossWallet.ui.util.formatCurrencyRight
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

import androidx.compose.ui.tooling.preview.Preview
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme

@Serializable
data class CashFlowSettings(
    val showBaseConversion: Boolean = true,
    val timelineRange: TimelineRange = TimelineRange.AllTime
)

@Composable
fun AeonCashFlowSummary(
    cashFlowResult: CashFlowResult?,
    baseCurrency: String,
    currentTheme: String,
    modifier: Modifier = Modifier,
    headerModifier: Modifier = Modifier,
    showTitle: Boolean = true,
    precision: Int = 2,
    showBaseConversion: Boolean = true,
    timelineRange: TimelineRange = TimelineRange.AllTime,
    onShowBaseConversionChange: (Boolean) -> Unit = {},
    onTimelineRangeChange: (TimelineRange) -> Unit = {}
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

    val totalIncomeInBase = cashFlowResult?.totalIncomeInBase ?: BigDecimal.ZERO
    val totalExpenseInBase = cashFlowResult?.totalExpensesInBase ?: BigDecimal.ZERO

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (showTitle) {
                AeonFlowSectionHeader(
                    title = "CASH FLOW SUMMARY",
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
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AeonFlowCard(
                    label = "Income",
                    totalInBase = totalIncomeInBase,
                    baseCurrency = baseCurrency,
                    type = CashFlowType.INCOME,
                    currentTheme = currentTheme,
                    precision = precision,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        cashFlowResult?.breakdown?.forEach { item ->
                            AeonFlowBreakdownItem(
                                curr = item.currency,
                                amount = item.income,
                                inBase = item.incomeInBase,
                                baseCurrency = baseCurrency,
                                precision = precision,
                                showBaseConversion = showBaseConversion
                            )
                        }
                    }
                }

                AeonFlowCard(
                    label = "Expense",
                    totalInBase = totalExpenseInBase,
                    baseCurrency = baseCurrency,
                    type = CashFlowType.EXPENSE,
                    currentTheme = currentTheme,
                    precision = precision,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        cashFlowResult?.breakdown?.forEach { item ->
                            AeonFlowBreakdownItem(
                                curr = item.currency,
                                amount = item.expense,
                                inBase = item.expenseInBase,
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
            confirmButton = {
                TextButton(onClick = { showSettings = false }) {
                    Text("Done")
                }
            },
            title = {
                Text(
                    "Cash Flow Settings",
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

                    AeonBaseConversionToggle(
                        baseCurrency = baseCurrency,
                        showBaseConversion = showBaseConversion,
                        onShowBaseConversionChange = onShowBaseConversionChange
                    )
                }
            },
            shape = RoundedCornerShape(32.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AeonCashFlowSummaryPreview() {
    val sampleTransactions = listOf(
        Transaction(
            id = 1L,
            title = "Salary",
            amount = BigDecimal("5000.00"),
            category = "Salary",
            timestamp = System.currentTimeMillis(),
            currency = "USD",
            isIncome = true,
            accountId = "1",
            note = "Monthly salary"
        ),
        Transaction(
            id = 2L,
            title = "Rent",
            amount = BigDecimal("1200.00"),
            category = "Rent",
            timestamp = System.currentTimeMillis(),
            currency = "USD",
            isIncome = false,
            accountId = "1",
            note = "Apartment rent"
        )
    )

    OssWalletTheme {
        Surface {
            AeonCashFlowSummary(
                cashFlowResult = null,
                baseCurrency = "USD",
                currentTheme = "Light",
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Composable
internal fun AeonFlowBreakdownItem(
    curr: String,
    amount: BigDecimal,
    inBase: BigDecimal,
    baseCurrency: String,
    precision: Int = 2,
    showBaseConversion: Boolean = true
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            curr,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Black
            ),
            color = MaterialTheme.colorScheme.outline
        )
        Column(horizontalAlignment = Alignment.End) {
            Text(
                formatCurrencyRight(amount, curr, precision),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black)
            )
            if (showBaseConversion && curr != baseCurrency) {
                Text(
                    "≈ ${formatCurrencyRight(inBase, baseCurrency, precision)}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
fun AeonFlowCard(
    label: String,
    totalInBase: BigDecimal,
    baseCurrency: String,
    type: CashFlowType,
    modifier: Modifier = Modifier,
    currentTheme: String = "System",
    incomeColor: Color = Color(0xFF4CAF50),
    expenseColor: Color = Color(0xFFF44336),
    precision: Int = 2,
    content: @Composable ColumnScope.() -> Unit
) {
    val color = if (type == CashFlowType.INCOME) incomeColor else expenseColor
    val icon = if (type == CashFlowType.INCOME) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward
    
    var cardSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.1f)),
        modifier = modifier.onSizeChanged { cardSize = it }
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            val iconHeight = with(density) { (cardSize.height * 0.6f).toDp() }
            
            if (cardSize.height > 0) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(iconHeight)
                        .alpha(0.1f),
                    tint = color
                )
            }

            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        label.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = color
                    )
                    AeonFlowBadge(totalInBase, type, precision = precision)
                }

                content()
            }
        }
    }
}
