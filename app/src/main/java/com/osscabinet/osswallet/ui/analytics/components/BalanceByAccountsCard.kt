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
import com.osscabinet.ossWallet.domain.model.settings.BalanceByAccountsCardConfig
import com.osscabinet.ossWallet.ui.components.AeonFlowSectionHeader
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import com.osscabinet.ossWallet.ui.components.cards.AeonCardSettingsSheet
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.theme.PositiveAmountColor
import com.osscabinet.ossWallet.ui.theme.NegativeAmountColor
import com.osscabinet.ossWallet.ui.util.formatCurrencyRight
import java.math.BigDecimal

@Composable
fun BalanceByAccountsCard(
    config: BalanceByAccountsCardConfig,
    data: BalanceByAccountsData,
    modifier: Modifier = Modifier,
    headerModifier: Modifier = Modifier,
    onUpdate: (BalanceByAccountsCardConfig) -> Unit = {},
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
            
            Column {
                Text("TOTAL LIQUIDITY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    formatCurrencyRight(data.totalBalance, data.currency, data.precision),
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold)
                )
            }
            
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                data.accountBalances.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                // Find accountId by name if possible, or just use filter
                                val accountId = accounts.find { it.name == item.accountName }?.id
                                onRecordsClick(com.osscabinet.ossWallet.ui.navigation.RecordsFilterContext.Custom(
                                    accountIds = if (accountId != null) listOf(accountId) else config.filter.accountIds,
                                    startDate = config.filter.startDate,
                                    endDate = config.filter.endDate,
                                    categoryIds = config.filter.categoryIds.map { it.toString() },
                                    currencies = listOf(item.currency)
                                ))
                            }
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(item.accountName, style = MaterialTheme.typography.labelMedium)
                        Text(
                            formatCurrencyRight(item.balance, item.currency, data.precision),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (item.isNegative) NegativeAmountColor else MaterialTheme.colorScheme.onSurface
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
        )
    }
}

data class BalanceByAccountsData(
    val totalBalance: BigDecimal,
    val currency: String,
    val precision: Int,
    val accountBalances: List<AccountBalanceItem>
)

data class AccountBalanceItem(
    val accountName: String,
    val balance: BigDecimal,
    val currency: String,
    val isNegative: Boolean
)

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun BalanceByAccountsCardPreview() {
    OssWalletTheme {
        BalanceByAccountsCard(
            config = BalanceByAccountsCardConfig(cardId = "preview", title = "BALANCE BY ACCOUNTS"),
            data = BalanceByAccountsData(
                totalBalance = BigDecimal("15000.00"),
                currency = "USD",
                precision = 2,
                accountBalances = listOf(
                    AccountBalanceItem("Checking", BigDecimal("5000.00"), "USD", false),
                    AccountBalanceItem("Savings", BigDecimal("10000.00"), "USD", false)
                )
            )
        )
    }
}
