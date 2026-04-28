package com.osscabinet.ossWallet.ui.analytics.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
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
import com.osscabinet.ossWallet.domain.model.settings.IncomeExpensesBookCardConfig
import com.osscabinet.ossWallet.ui.components.AeonFlowSectionHeader
import com.osscabinet.ossWallet.ui.components.cards.AeonCardSettingsSheet
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import com.osscabinet.ossWallet.ui.components.AeonSettingsToggle
import com.osscabinet.ossWallet.ui.theme.labelExtraSmall
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.theme.PositiveAmountColor
import com.osscabinet.ossWallet.ui.theme.NegativeAmountColor
import com.osscabinet.ossWallet.ui.util.formatCurrencyRight
import java.math.BigDecimal

@Composable
fun IncomeExpensesBookCard(
    config: IncomeExpensesBookCardConfig,
    data: IncomeExpensesBookData,
    modifier: Modifier = Modifier,
    headerModifier: Modifier = Modifier,
    onUpdate: (IncomeExpensesBookCardConfig) -> Unit = {},
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
            
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                BookRow("Income", data.incomeTotal, data.incomeCount, PositiveAmountColor, data.currency, data.precision) {
                    onRecordsClick(com.osscabinet.ossWallet.ui.navigation.RecordsFilterContext.Custom(
                        isIncome = true,
                        startDate = config.filter.startDate,
                        endDate = config.filter.endDate,
                        accountIds = config.filter.accountIds,
                        categoryIds = config.filter.categoryIds.map { it.toString() },
                        currencies = config.filter.currencies
                    ))
                }
                BookRow("Expenses", data.expenseTotal, data.expenseCount, NegativeAmountColor, data.currency, data.precision) {
                    onRecordsClick(com.osscabinet.ossWallet.ui.navigation.RecordsFilterContext.Custom(
                        isIncome = false,
                        startDate = config.filter.startDate,
                        endDate = config.filter.endDate,
                        accountIds = config.filter.accountIds,
                        categoryIds = config.filter.categoryIds.map { it.toString() },
                        currencies = config.filter.currencies
                    ))
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("NET BALANCE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Text(
                        formatCurrencyRight(data.incomeTotal.add(data.expenseTotal), data.currency, data.precision),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
                    )
                }
            }
            
            if (config.showShareButton) {
                OutlinedButton(
                    onClick = { /* Share Logic */ },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Share Summary")
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
        ) {
            AeonSettingsToggle(
                label = "SHOW SHARE BUTTON",
                checked = config.showShareButton,
                onCheckedChange = { onUpdate(config.copy(showShareButton = it)) }
            )
        }
    }
}

@Composable
private fun BookRow(label: String, amount: BigDecimal, count: Int, color: Color, currency: String, precision: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("$count Transactions", style = MaterialTheme.typography.labelExtraSmall, color = MaterialTheme.colorScheme.outline)
        }
        Text(
            formatCurrencyRight(amount, currency, precision),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = color
        )
    }
}

data class IncomeExpensesBookData(
    val incomeTotal: BigDecimal,
    val expenseTotal: BigDecimal,
    val incomeCount: Int,
    val expenseCount: Int,
    val currency: String,
    val precision: Int
)

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun IncomeExpensesBookCardPreview() {
    OssWalletTheme {
        IncomeExpensesBookCard(
            config = IncomeExpensesBookCardConfig(cardId = "preview", title = "INCOME & EXPENSES BOOK"),
            data = IncomeExpensesBookData(
                incomeTotal = BigDecimal("5000.00"),
                expenseTotal = BigDecimal("-3450.00"),
                incomeCount = 12,
                expenseCount = 45,
                currency = "USD",
                precision = 2
            )
        )
    }
}
