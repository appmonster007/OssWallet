package com.osscabinet.ossWallet.ui.components.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.ui.components.*
import com.osscabinet.ossWallet.ui.util.convertAmount
import com.osscabinet.ossWallet.ui.util.formatCurrencyRight
import java.math.BigDecimal

@Composable
fun CurrencyAggregationSection(
    currencyTotals: Map<String, BigDecimal>,
    baseCurrency: String,
    exchangeRates: Map<String, BigDecimal>,
    modifier: Modifier = Modifier,
    headerModifier: Modifier = Modifier,
    precision: Int = 2,
    layout: String = "scroll",
    showBaseConversion: Boolean = true,
    onLayoutChange: (String) -> Unit = {},
    onShowBaseConversionChange: (Boolean) -> Unit = {}
) {
    var showSettings by remember { mutableStateOf(false) }

    if (currencyTotals.isNotEmpty()) {
        Card(
            modifier = modifier,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AeonFlowSectionHeader(
                    title = "AGGREGATED CURRENCY BALANCES",
                    modifier = headerModifier,
                    trailing = {
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
                )

                val sortedTotals = currencyTotals.toList().sortedBy { it.first }

                if (layout == "grid") {
                    val chunks = sortedTotals.chunked(3)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        chunks.forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowItems.forEach { (currency, amount) ->
                                    AeonCurrencyBalanceItem(
                                        currency = currency,
                                        amount = amount,
                                        baseCurrency = baseCurrency,
                                        exchangeRates = exchangeRates,
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
                        items(sortedTotals) { (currency, amount) ->
                            AeonCurrencyBalanceItem(
                                currency = currency,
                                amount = amount,
                                baseCurrency = baseCurrency,
                                exchangeRates = exchangeRates,
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
                    "Currency Settings",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    AeonLayoutToggle(
                        layout = layout,
                        onLayoutChange = onLayoutChange
                    )

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

@Composable
fun AeonCurrencyBalanceItem(
    currency: String,
    amount: BigDecimal,
    baseCurrency: String,
    exchangeRates: Map<String, BigDecimal>,
    precision: Int = 2,
    showBaseConversion: Boolean = true,
    modifier: Modifier = Modifier
) {
    val inBase = convertAmount(amount, currency, baseCurrency, exchangeRates, precision)

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
                currency,
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
                    formatCurrencyRight(amount, currency, precision),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFeatureSettings = "tnum",
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )
                
                if (showBaseConversion && currency != baseCurrency) {
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
