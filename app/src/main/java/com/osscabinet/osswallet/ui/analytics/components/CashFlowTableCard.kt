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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Filter
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.settings.CashFlowTableCardConfig
import com.osscabinet.ossWallet.ui.components.AeonFlowSectionHeader
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import com.osscabinet.ossWallet.ui.components.cards.AeonCardSettingsSheet
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.theme.PositiveAmountColor
import com.osscabinet.ossWallet.ui.theme.NegativeAmountColor
import com.osscabinet.ossWallet.ui.util.formatCurrencyRight
import java.math.BigDecimal

@Composable
fun CashFlowTableCard(
    config: CashFlowTableCardConfig,
    data: CashFlowTableData,
    modifier: Modifier = Modifier,
    headerModifier: Modifier = Modifier,
    onUpdate: (CashFlowTableCardConfig) -> Unit = {},
    onDelete: () -> Unit = {},
    onRecordsClick: (com.osscabinet.ossWallet.ui.navigation.RecordsFilterContext) -> Unit = {},
    accounts: List<Account> = emptyList(),
    allCategories: List<Category> = emptyList(),
    savedFilters: List<Filter> = emptyList()
) {
    var showSettings by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
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
            
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TableHeader()
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                TableRow("Income", data.incomeCount, data.incomeAvgDay, data.incomeAvgRecord, data.incomeTotal, PositiveAmountColor, data.currency, data.precision) {
                    onRecordsClick(com.osscabinet.ossWallet.ui.navigation.RecordsFilterContext.Custom(
                        isIncome = true,
                        startDate = config.filter.startDate,
                        endDate = config.filter.endDate,
                        accountIds = config.filter.accountIds,
                        categoryIds = config.filter.categoryIds.map { it.toString() },
                        currencies = config.filter.currencies
                    ))
                }
                TableRow("Expenses", data.expenseCount, data.expenseAvgDay, data.expenseAvgRecord, data.expenseTotal, NegativeAmountColor, data.currency, data.precision) {
                    onRecordsClick(com.osscabinet.ossWallet.ui.navigation.RecordsFilterContext.Custom(
                        isIncome = false,
                        startDate = config.filter.startDate,
                        endDate = config.filter.endDate,
                        accountIds = config.filter.accountIds,
                        categoryIds = config.filter.categoryIds.map { it.toString() },
                        currencies = config.filter.currencies
                    ))
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                TableRow("Net Flow", -1, null, null, data.netCashFlow, if (data.netCashFlow >= BigDecimal.ZERO) PositiveAmountColor else NegativeAmountColor, data.currency, data.precision, isTotal = true) {}
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

@Composable
private fun TableHeader() {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text("METRIC", modifier = Modifier.weight(1.5f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("QTY", modifier = Modifier.weight(0.5f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("AVG/DAY", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("TOTAL", modifier = Modifier.weight(1.2f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.End)
    }
}

@Composable
private fun TableRow(
    label: String,
    count: Int,
    avgDay: BigDecimal?,
    avgRecord: BigDecimal?,
    total: BigDecimal,
    color: Color,
    currency: String,
    precision: Int,
    isTotal: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp, horizontal = 4.dp), 
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1.5f), style = if (isTotal) MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold) else MaterialTheme.typography.labelMedium)
        Text(if (count >= 0) count.toString() else "-", modifier = Modifier.weight(0.5f), style = MaterialTheme.typography.labelMedium)
        Text(avgDay?.let { formatCurrencyRight(it, currency, precision) } ?: "-", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
        Text(
            formatCurrencyRight(total, currency, precision),
            modifier = Modifier.weight(1.2f),
            style = if (isTotal) MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold) else MaterialTheme.typography.labelMedium,
            color = color,
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}

data class CashFlowTableData(
    val incomeCount: Int,
    val expenseCount: Int,
    val incomeAvgDay: BigDecimal,
    val expenseAvgDay: BigDecimal,
    val incomeAvgRecord: BigDecimal,
    val expenseAvgRecord: BigDecimal,
    val incomeTotal: BigDecimal,
    val expenseTotal: BigDecimal,
    val netCashFlow: BigDecimal,
    val currency: String,
    val precision: Int
)

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun CashFlowTableCardPreview() {
    OssWalletTheme {
        CashFlowTableCard(
            config = CashFlowTableCardConfig(cardId = "preview", title = "CASH FLOW TABLE"),
            data = CashFlowTableData(
                incomeCount = 10,
                expenseCount = 20,
                incomeAvgDay = BigDecimal("100.00"),
                expenseAvgDay = BigDecimal("50.00"),
                incomeAvgRecord = BigDecimal("500.00"),
                expenseAvgRecord = BigDecimal("200.00"),
                incomeTotal = BigDecimal("5000.00"),
                expenseTotal = BigDecimal("-3000.00"),
                netCashFlow = BigDecimal("2000.00"),
                currency = "USD",
                precision = 2
            )
        )
    }
}
