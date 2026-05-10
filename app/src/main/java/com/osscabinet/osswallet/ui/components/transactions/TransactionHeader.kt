package com.osscabinet.ossWallet.ui.components.transactions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter.formatCurrencyRight
import java.math.BigDecimal

/**
 * Unified transaction header composable that can be used for both weekly and daily transaction summaries.
 * Displays income, expense, net, and balance totals with expandable currency breakdown.
 */
@Composable
fun TransactionHeader(
    title: String,
    subtitle: String,
    incomeByCurrency: Map<String, BigDecimal>,
    expenseByCurrency: Map<String, BigDecimal>,
    closingBalancesByCurrency: Map<String, BigDecimal>,
    allCurrencies: List<String>,
    incomeBase: BigDecimal,
    expenseBase: BigDecimal,
    netBase: BigDecimal,
    totalClosingBalanceBase: BigDecimal,
    baseCurrency: String,
    precision: Int
) {
    var isExpanded by remember { mutableStateOf(true) }
    val positiveColor = Color(0xFF4CAF50)
    val negativeColor = MaterialTheme.colorScheme.error
    val surfaceColor = MaterialTheme.colorScheme.surfaceContainer
    val outlineColor = MaterialTheme.colorScheme.outline
    val dividerColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = surfaceColor
    ) {
        Column {
            HorizontalDivider(thickness = 0.5.dp, color = dividerColor)
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                // Row 1: Title Info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = outlineColor,
                            letterSpacing = 1.sp,
                            fontSize = 8.sp
                        )
                    )
                    Text(
                        text = subtitle.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Medium,
                            color = outlineColor.copy(alpha = 0.6f),
                            letterSpacing = 0.5.sp,
                            fontSize = 7.sp
                        )
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))

                // Row 2: Headers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.width(40.dp))
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("INCOME", "EXPENSE", "NET").forEach { label ->
                            Text(
                                label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.5.sp
                                ),
                                color = outlineColor.copy(alpha = 0.5f),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Text(
                        "BALANCE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 7.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        ),
                        color = outlineColor.copy(alpha = 0.5f),
                        modifier = Modifier.width(70.dp),
                        textAlign = TextAlign.End
                    )
                }

                // Row 3: Aggregate (Clickable)
                val canExpand = allCurrencies.isNotEmpty()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (canExpand) Modifier.clickable { isExpanded = !isExpanded } else Modifier)
                        .padding(vertical = 1.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.width(40.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TOTAL",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 8.sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (canExpand) {
                            Icon(
                                if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                contentDescription = null,
                                modifier = Modifier.size(10.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            formatCurrencyRight(incomeBase, baseCurrency, precision, shorten = true),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                            color = positiveColor,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            formatCurrencyRight(expenseBase, baseCurrency, precision, shorten = true),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                            color = negativeColor,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            formatCurrencyRight(netBase, baseCurrency, precision, shorten = true),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                            color = if (netBase >= BigDecimal.ZERO) positiveColor else negativeColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Text(
                        formatCurrencyRight(totalClosingBalanceBase, baseCurrency, precision, shorten = true),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                        modifier = Modifier.width(70.dp),
                        textAlign = TextAlign.End,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Row 4+: Breakdown (Expandable)
                AnimatedVisibility(visible = isExpanded) {
                    Column(modifier = Modifier.padding(top = 2.dp)) {
                        allCurrencies.forEach { curr ->
                            val inc = incomeByCurrency[curr] ?: BigDecimal.ZERO
                            val exp = expenseByCurrency[curr] ?: BigDecimal.ZERO
                            val net = inc.add(exp)
                            val bal = closingBalancesByCurrency[curr]

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 1.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = curr,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp
                                    ),
                                    modifier = Modifier.width(40.dp),
                                    color = outlineColor.copy(alpha = 0.8f)
                                )

                                Row(
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        formatCurrencyRight(inc, curr, precision, shorten = true),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        color = positiveColor.copy(alpha = 0.7f),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        formatCurrencyRight(exp, curr, precision, shorten = true),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        color = negativeColor.copy(alpha = 0.7f),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        formatCurrencyRight(net, curr, precision, shorten = true),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp
                                        ),
                                        color = (if (net >= BigDecimal.ZERO) positiveColor else negativeColor).copy(alpha = 0.7f),
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Text(
                                    if (bal != null) formatCurrencyRight(bal, curr, precision, shorten = true) else "0.00 $curr",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp
                                    ),
                                    modifier = Modifier.width(70.dp),
                                    textAlign = TextAlign.End,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TransactionHeaderPreview() {
    MaterialTheme {
        TransactionHeader(
            title = "WEEK 1",
            subtitle = "Jan 1 - Jan 7",
            incomeByCurrency = mapOf("USD" to BigDecimal("5000.00"), "EUR" to BigDecimal("2500.00")),
            expenseByCurrency = mapOf("USD" to BigDecimal("3200.00"), "EUR" to BigDecimal("1800.00")),
            closingBalancesByCurrency = mapOf("USD" to BigDecimal("15000.00"), "EUR" to BigDecimal("8000.00")),
            allCurrencies = listOf("EUR", "USD"),
            incomeBase = BigDecimal("7500.00"),
            expenseBase = BigDecimal("5000.00"),
            netBase = BigDecimal("2500.00"),
            totalClosingBalanceBase = BigDecimal("23000.00"),
            baseCurrency = "USD",
            precision = 2
        )
    }
}
