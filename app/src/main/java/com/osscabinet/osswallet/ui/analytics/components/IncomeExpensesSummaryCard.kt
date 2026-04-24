package com.osscabinet.ossWallet.ui.analytics.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.model.settings.IncomeExpensesSummaryCardConfig
import com.osscabinet.ossWallet.ui.components.AeonFlowSectionHeader
import com.osscabinet.ossWallet.ui.theme.PositiveAmountColor
import com.osscabinet.ossWallet.ui.theme.NegativeAmountColor
import com.osscabinet.ossWallet.ui.theme.AccentBlue
import com.osscabinet.ossWallet.ui.theme.AccentOrange
import com.osscabinet.ossWallet.ui.theme.AccentPurple
import com.osscabinet.ossWallet.ui.util.formatCurrencyRight
import java.math.BigDecimal

data class CategoryItem(
    val name: String,
    val amount: BigDecimal,
    val change: String,
    val color: Color,
    val icon: ImageVector
)

@Composable
fun IncomeExpensesSummaryCard(
    config: IncomeExpensesSummaryCardConfig,
    data: IncomeExpensesSummaryData,
    modifier: Modifier = Modifier,
    onUpdate: (IncomeExpensesSummaryCardConfig) -> Unit = {},
    onDelete: () -> Unit = {},
    onCategoryClick: ((String) -> Unit)? = null
) {
    var showSettings by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            AeonFlowSectionHeader(
                title = config.title,
                trailing = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
            
            // Main value with trend indicator
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    formatCurrencyRight(data.totalAmount, data.currency, data.precision),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.weight(1f))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (data.percentageChange.startsWith("+")) 
                        PositiveAmountColor.copy(alpha = 0.1f) 
                    else 
                        NegativeAmountColor.copy(alpha = 0.1f)
                ) {
                    Text(
                        data.percentageChange,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        color = if (data.percentageChange.startsWith("+")) PositiveAmountColor else NegativeAmountColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
            
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
            
            // Category items with interactivity
            data.categories.forEach { item ->
                val isSelected = selectedCategory == item.name
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (onCategoryClick != null) {
                                Modifier.clickable {
                                    selectedCategory = if (isSelected) null else item.name
                                    onCategoryClick(item.name)
                                }
                            } else Modifier
                        ),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) 
                            item.color.copy(alpha = 0.15f) 
                        else 
                            MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(item.color, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                item.icon,
                                null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(
                                item.name,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                item.change,
                                color = if (item.change.startsWith("+")) PositiveAmountColor else NegativeAmountColor,
                                fontSize = 11.sp
                            )
                        }
                        Text(
                            formatCurrencyRight(item.amount, data.currency, data.precision),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
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
                    Icon(Icons.Default.Delete, contentDescription = "Delete card", modifier = Modifier.size(18.dp))
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
                Text(config.title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
            },
            text = {
                Text("Configure income & expenses summary settings")
            }
        )
    }
}

data class IncomeExpensesSummaryData(
    val totalAmount: BigDecimal,
    val percentageChange: String,
    val currency: String,
    val precision: Int,
    val categories: List<CategoryItem>
)

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

data class PlannedPaymentsTimelineData(
    val total: BigDecimal,
    val currency: String,
    val precision: Int,
    val weeklyData: List<Pair<String, BigDecimal>>
)

data class AccountBalanceItem(
    val accountName: String,
    val balance: BigDecimal,
    val currency: String,
    val isNegative: Boolean
)

data class BalanceByAccountsData(
    val totalBalance: BigDecimal,
    val currency: String,
    val precision: Int,
    val accountBalances: List<AccountBalanceItem>
)

data class BalanceTrendData(
    val currentBalance: BigDecimal,
    val percentageChange: String,
    val currency: String,
    val precision: Int,
    val weeklyBalances: List<Double>
)
