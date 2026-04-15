package com.osscabinet.ossWallet.ui.components.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.ui.components.*
import com.osscabinet.ossWallet.ui.components.accounts.*
import com.osscabinet.ossWallet.ui.viewmodel.AccountStats
import java.math.BigDecimal

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AeonAccountsSection(
    onManageClick: () -> Unit,
    onAccountClick: (String) -> Unit,
    onGridColumnsChange: (Int) -> Unit,
    accounts: List<Account>,
    accountStats: Map<String, AccountStats>,
    baseCurrency: String,
    exchangeRates: Map<String, BigDecimal>,
    currentTheme: String,
    modifier: Modifier = Modifier,
    headerModifier: Modifier = Modifier,
    gridColumns: Int = 2,
    precision: Int = 2,
    onHeaderClick: (() -> Unit)? = null,
    settingsTitle: String = "Account Settings",
    showAddAccountCard: Boolean = true,
    onShowAddAccountCardChange: (Boolean) -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            AeonFlowSectionHeader(
                title = "ACCOUNTS",
                modifier = if (onHeaderClick != null) headerModifier.clickable(onClick = onHeaderClick) else headerModifier,
                trailing = {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.MoreHoriz,
                            contentDescription = "Grid Settings",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )

            if (showMenu) {
                AlertDialog(
                    onDismissRequest = { showMenu = false },
                    confirmButton = {
                        TextButton(onClick = { showMenu = false }) {
                            Text("Done")
                        }
                    },
                    title = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                settingsTitle,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            IconButton(
                                onClick = {
                                    onManageClick()
                                    showMenu = false
                                },
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                    contentColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(
                                    Icons.Default.Settings,
                                    contentDescription = "Manage Accounts"
                                )
                            }
                        }
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
                                        "ACCOUNT GRID COLUMNS",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        ),
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                        listOf(1, 2, 3).forEachIndexed { index, count ->
                                            SegmentedButton(
                                                selected = gridColumns == count,
                                                onClick = { onGridColumnsChange(count) },
                                                shape = SegmentedButtonDefaults.itemShape(
                                                    index = index,
                                                    count = 3
                                                ),
                                                label = { Text(count.toString()) }
                                            )
                                        }
                                    }
                                }
                            }

                            AeonSettingsToggle(
                                label = "SHOW ADD ACCOUNT CARD",
                                checked = showAddAccountCard,
                                onCheckedChange = onShowAddAccountCardChange
                            )
                        }
                    },
                    shape = RoundedCornerShape(32.dp),
                    containerColor = MaterialTheme.colorScheme.surface
                )
            }

            if (accounts.isEmpty()) {
                AeonAddAccountCard(
                    baseCurrency = baseCurrency,
                    currentTheme = currentTheme,
                    onClick = onManageClick,
                    gridColumns = gridColumns,
                    precision = precision,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    maxItemsInEachRow = gridColumns
                ) {
                    accounts.forEach { account ->
                        val stats = accountStats[account.id]
                        val totalInBase = stats?.currencyBalances?.let { balances ->
                            com.osscabinet.ossWallet.ui.util.calculateTotalInCurrency(
                                balances,
                                baseCurrency,
                                exchangeRates,
                                precision
                            )
                        } ?: BigDecimal.ZERO

                        val isDarkTheme = when (currentTheme) {
                            "Light" -> false
                            "Dark", "Amoled" -> true
                            else -> isSystemInDarkTheme()
                        }
                        val isAmoled = currentTheme == "Amoled"

                        when (gridColumns) {
                            1 -> AeonAccountCardLarge(
                                account = account,
                                balanceInBase = totalInBase,
                                baseCurrency = baseCurrency,
                                isDarkTheme = isDarkTheme,
                                isAmoled = isAmoled,
                                onClick = { onAccountClick(account.id) },
                                modifier = Modifier.fillMaxWidth(),
                                precision = precision
                            )
                            2 -> AeonAccountCardMedium(
                                account = account,
                                balanceInBase = totalInBase,
                                baseCurrency = baseCurrency,
                                isDarkTheme = isDarkTheme,
                                isAmoled = isAmoled,
                                onClick = { onAccountClick(account.id) },
                                modifier = Modifier.weight(1f),
                                precision = precision
                            )
                            else -> AeonAccountCardSmall(
                                account = account,
                                balanceInBase = totalInBase,
                                baseCurrency = baseCurrency,
                                isDarkTheme = isDarkTheme,
                                isAmoled = isAmoled,
                                onClick = { onAccountClick(account.id) },
                                modifier = Modifier.weight(1f),
                                precision = precision
                            )
                        }
                    }

                    if (showAddAccountCard) {
                        AeonAddAccountCard(
                            baseCurrency = baseCurrency,
                            currentTheme = currentTheme,
                            onClick = onManageClick,
                            gridColumns = gridColumns,
                            precision = precision,
                            modifier = if (gridColumns == 1) Modifier.fillMaxWidth() else Modifier.weight(1f)
                        )
                    } else if (gridColumns > 1 && accounts.size % gridColumns != 0) {
                        // Fill remaining space if grid not full
                        repeat(gridColumns - (accounts.size % gridColumns)) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AeonAddAccountCard(
    baseCurrency: String,
    currentTheme: String,
    onClick: () -> Unit,
    gridColumns: Int = 2,
    precision: Int = 2,
    modifier: Modifier = Modifier
) {
    val isDarkTheme = when (currentTheme) {
        "Light" -> false
        "Dark", "Amoled" -> true
        else -> isSystemInDarkTheme()
    }
    val isAmoled = currentTheme == "Amoled"
    val placeholderAccount = Account(
        "empty",
        "GET STARTED",
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        baseCurrency,
        "Default",
        "#808080",
        "Add"
    )

    when (gridColumns) {
        1 -> AeonAccountCardLarge(
            account = placeholderAccount,
            balanceInBase = BigDecimal.ZERO,
            baseCurrency = baseCurrency,
            isDarkTheme = isDarkTheme,
            isAmoled = isAmoled,
            onClick = onClick,
            modifier = modifier,
            precision = precision,
            extraLabel = "Add New"
        )
        2 -> AeonAccountCardMedium(
            account = placeholderAccount,
            balanceInBase = BigDecimal.ZERO,
            baseCurrency = baseCurrency,
            isDarkTheme = isDarkTheme,
            isAmoled = isAmoled,
            onClick = onClick,
            modifier = modifier,
            precision = precision
        )
        else -> AeonAccountCardSmall(
            account = placeholderAccount,
            balanceInBase = BigDecimal.ZERO,
            baseCurrency = baseCurrency,
            isDarkTheme = isDarkTheme,
            isAmoled = isAmoled,
            onClick = onClick,
            modifier = modifier,
            precision = precision
        )
    }
}
