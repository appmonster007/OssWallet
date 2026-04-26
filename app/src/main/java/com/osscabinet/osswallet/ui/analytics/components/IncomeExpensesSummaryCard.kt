package com.osscabinet.ossWallet.ui.analytics.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.data.local.entity.CategoryEntity
import com.osscabinet.ossWallet.data.local.entity.FilterEntity
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.settings.IncomeExpensesSummaryCardConfig
import com.osscabinet.ossWallet.ui.components.AeonFlowSectionHeader
import com.osscabinet.ossWallet.ui.components.AeonSettingsToggle
import com.osscabinet.ossWallet.ui.components.cards.AeonCardSettingsSheet
import com.osscabinet.ossWallet.ui.theme.labelExtraSmall
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.theme.PositiveAmountColor
import com.osscabinet.ossWallet.ui.theme.NegativeAmountColor
import com.osscabinet.ossWallet.ui.theme.AccentBlue
import com.osscabinet.ossWallet.ui.util.formatCurrencyRight
import java.math.BigDecimal

@Composable
fun IncomeExpensesSummaryCard(
    config: IncomeExpensesSummaryCardConfig,
    data: IncomeExpensesSummaryData,
    modifier: Modifier = Modifier,
    headerModifier: Modifier = Modifier,
    onUpdate: (IncomeExpensesSummaryCardConfig) -> Unit = {},
    onDelete: () -> Unit = {},
    onRecordsClick: (com.osscabinet.ossWallet.ui.navigation.RecordsFilterContext) -> Unit = {},
    accounts: List<Account> = emptyList(),
    allCategories: List<CategoryEntity> = emptyList(),
    savedFilters: List<FilterEntity> = emptyList()
) {
    var showSettings by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { 
                onRecordsClick(com.osscabinet.ossWallet.ui.navigation.RecordsFilterContext.Custom(
                    startDate = config.filter.startDate,
                    endDate = config.filter.endDate
                ))
            },
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
            
            // Net Flow Summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "NET FLOW",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        formatCurrencyRight(data.totalAmount, data.currency, data.precision),
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = if (data.totalAmount >= BigDecimal.ZERO) PositiveAmountColor else NegativeAmountColor
                    )
                }
                
                if (config.showPercentage) {
                    Surface(
                        color = (if (data.percentageChange.startsWith("+")) PositiveAmountColor else NegativeAmountColor).copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            data.percentageChange,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (data.percentageChange.startsWith("+")) PositiveAmountColor else NegativeAmountColor
                        )
                    }
                }
            }
            
            // Savings Rate Indicator (if income > 0)
            if (config.showSavingsRate && data.incomeTotal > BigDecimal.ZERO) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("SAVINGS RATE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${(data.savingsRate * 100).toInt()}%", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    }
                    LinearProgressIndicator(
                        progress = { data.savingsRate.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = AccentBlue,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
            
            // Category Quick List
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                data.categories.forEach { category ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onRecordsClick(com.osscabinet.ossWallet.ui.navigation.RecordsFilterContext.Custom(
                                    queryPayee = category.name, 
                                    startDate = config.filter.startDate,
                                    endDate = config.filter.endDate
                                ))
                            },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Surface(
                                modifier = Modifier.size(32.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = category.color.copy(alpha = 0.1f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(category.icon, null, modifier = Modifier.size(16.dp), tint = category.color)
                                }
                            }
                            Column {
                                Text(category.name, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                Text(category.change, style = MaterialTheme.typography.labelExtraSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Text(
                            formatCurrencyRight(category.amount, data.currency, data.precision),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
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
        ) {
            AeonSettingsToggle(
                label = "SHOW PERCENTAGE",
                description = "Show change vs previous period",
                checked = config.showPercentage,
                onCheckedChange = { onUpdate(config.copy(showPercentage = it)) }
            )

            AeonSettingsToggle(
                label = "SHOW SAVINGS RATE",
                description = "Efficiency ratio based on income",
                checked = config.showSavingsRate,
                onCheckedChange = { onUpdate(config.copy(showSavingsRate = it)) }
            )
        }
    }
}

data class IncomeExpensesSummaryData(
    val totalAmount: BigDecimal,
    val percentageChange: String,
    val currency: String,
    val precision: Int,
    val categories: List<CategoryItem>,
    val savingsRate: Float = 0f,
    val incomeTotal: BigDecimal = BigDecimal.ZERO,
    val expenseTotal: BigDecimal = BigDecimal.ZERO
)

data class CategoryItem(
    val name: String,
    val amount: BigDecimal,
    val change: String,
    val color: Color,
    val icon: ImageVector
)

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun IncomeExpensesSummaryCardPreview() {
    OssWalletTheme {
        IncomeExpensesSummaryCard(
            config = IncomeExpensesSummaryCardConfig(
                cardId = "preview",
                title = "INCOME & EXPENSES"
            ),
            data = IncomeExpensesSummaryData(
                totalAmount = BigDecimal("3450.00"),
                percentageChange = "+12.5%",
                currency = "USD",
                precision = 2,
                categories = listOf(
                    CategoryItem("Salary", BigDecimal("5000.00"), "+5%", PositiveAmountColor, Icons.Default.AttachMoney),
                    CategoryItem("Rent", BigDecimal("-1200.00"), "0%", NegativeAmountColor, Icons.Default.Home),
                    CategoryItem("Food", BigDecimal("-450.00"), "-2%", NegativeAmountColor, Icons.Default.Restaurant)
                ),
                savingsRate = 0.35f,
                incomeTotal = BigDecimal("5000.00")
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}
