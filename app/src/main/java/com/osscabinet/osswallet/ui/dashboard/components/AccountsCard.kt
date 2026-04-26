package com.osscabinet.ossWallet.ui.dashboard.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.service.TimelineService
import com.osscabinet.ossWallet.domain.usecase.AccountSummaryItem
import com.osscabinet.ossWallet.ui.components.*
import com.osscabinet.ossWallet.ui.components.accounts.*
import com.osscabinet.ossWallet.ui.navigation.RecordsFilterContext
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Serializable
data class AccountsSectionSettings(
    val layout: String = "grid", // "scroll" or "grid"
    val gridColumns: Int = 2,
    val showAddAccountCard: Boolean = true,
    val timelineRange: TimelineRange = TimelineRange.AllTime
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AccountsCard(
    onManageClick: () -> Unit,
    onAccountClick: (String) -> Unit,
    onRecordsClick: ((com.osscabinet.ossWallet.ui.navigation.RecordsFilterContext) -> Unit)? = null,
    onGridColumnsChange: (String, Int) -> Unit,
    accounts: List<Account>,
    accountStats: Map<String, AccountSummaryItem>,
    baseCurrency: String,
    currentTheme: String,
    cardId: String,
    modifier: Modifier = Modifier,
    headerModifier: Modifier = Modifier,
    gridColumns: Int = 2,
    layout: String = "grid",
    precision: Int = 2,
    timelineRange: TimelineRange = TimelineRange.AllTime,
    onLayoutChange: (String, String) -> Unit = { _, _ -> },
    onTimelineRangeChange: (String, TimelineRange) -> Unit = { _, _ -> },
    onHeaderClick: (() -> Unit)? = null,
    settingsTitle: String = "Account Settings",
    showAddAccountCard: Boolean = true,
    onShowAddAccountCardChange: (String, Boolean) -> Unit = { _, _ -> },
    onDelete: () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }

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

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            AeonFlowSectionHeader(
                title = "ACCOUNTS",
                modifier = if (onHeaderClick != null) headerModifier.clickable(onClick = onHeaderClick) else headerModifier,
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
                            onClick = { showMenu = true },
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

            if (showMenu) {
                AlertDialog(
                    onDismissRequest = { showMenu = false },
                    dismissButton = {
                        TextButton(
                            onClick = onDelete,
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
                                        onRangeSelected = { range -> onTimelineRangeChange(cardId, range) },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            AeonLayoutToggle(
                                layout = layout,
                                onLayoutChange = { newLayout -> onLayoutChange(cardId, newLayout) }
                            )

                            if (layout == "grid") {
                                AeonGridColumnsSettings(
                                    gridColumns = gridColumns,
                                    onGridColumnsChange = { columns -> onGridColumnsChange(cardId, columns) }
                                )
                            }

                            AeonSettingsToggle(
                                label = "SHOW ADD ACCOUNT CARD",
                                checked = showAddAccountCard,
                                onCheckedChange = { show -> onShowAddAccountCardChange(cardId, show) }
                            )
                        }
                    },
                    shape = RoundedCornerShape(32.dp),
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            }

            if (accounts.isEmpty()) {
                AeonAddAccountCard(
                    baseCurrency = baseCurrency,
                    currentTheme = currentTheme,
                    onClick = onManageClick,
                    gridColumns = if (layout == "grid") gridColumns else 2,
                    precision = precision,
                    modifier = Modifier.fillMaxWidth()
                )
            } else if (layout == "grid") {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    maxItemsInEachRow = gridColumns
                ) {
                    accounts.forEach { account ->
                        AccountItemWrapper(
                            account = account,
                            accountStats = accountStats,
                            baseCurrency = baseCurrency,
                            currentTheme = currentTheme,
                            gridColumns = gridColumns,
                            onAccountClick = onAccountClick,
                            onRecordsClick = onRecordsClick,
                            timelineRange = timelineRange,
                            precision = precision,
                            modifier = if (gridColumns == 1) Modifier.fillMaxWidth() else Modifier.weight(1f)
                        )
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
                        repeat(gridColumns - (accounts.size % gridColumns)) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 4.dp)
                ) {
                    items(accounts) { account ->
                        AccountItemWrapper(
                            account = account,
                            accountStats = accountStats,
                            baseCurrency = baseCurrency,
                            currentTheme = currentTheme,
                            gridColumns = 2,
                            onAccountClick = onAccountClick,
                            onRecordsClick = onRecordsClick,
                            timelineRange = timelineRange,
                            precision = precision,
                            modifier = Modifier.width(160.dp)
                        )
                    }
                    if (showAddAccountCard) {
                        item {
                            AeonAddAccountCard(
                                baseCurrency = baseCurrency,
                                currentTheme = currentTheme,
                                onClick = onManageClick,
                                gridColumns = 2,
                                precision = precision,
                                modifier = Modifier.width(160.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRowScope.AccountItemWrapper(
    account: Account,
    accountStats: Map<String, AccountSummaryItem>,
    baseCurrency: String,
    currentTheme: String,
    gridColumns: Int,
    precision: Int,
    onAccountClick: (String) -> Unit,
    onRecordsClick: ((RecordsFilterContext) -> Unit)? = null,
    timelineRange: TimelineRange = TimelineRange.AllTime,
    modifier: Modifier = Modifier
) {
    AccountItemContent(
        account = account,
        accountStats = accountStats,
        baseCurrency = baseCurrency,
        currentTheme = currentTheme,
        gridColumns = gridColumns,
        precision = precision,
        onAccountClick = onAccountClick,
        onRecordsClick = onRecordsClick,
        timelineRange = timelineRange,
        modifier = modifier
    )
}

@Composable
private fun AccountItemWrapper(
    account: Account,
    accountStats: Map<String, AccountSummaryItem>,
    baseCurrency: String,
    currentTheme: String,
    gridColumns: Int,
    precision: Int,
    onAccountClick: (String) -> Unit,
    onRecordsClick: ((RecordsFilterContext) -> Unit)? = null,
    timelineRange: TimelineRange = TimelineRange.AllTime,
    modifier: Modifier = Modifier
) {
    AccountItemContent(
        account = account,
        accountStats = accountStats,
        baseCurrency = baseCurrency,
        currentTheme = currentTheme,
        gridColumns = gridColumns,
        precision = precision,
        onAccountClick = onAccountClick,
        onRecordsClick = onRecordsClick,
        timelineRange = timelineRange,
        modifier = modifier
    )
}

@Composable
private fun AccountItemContent(
    account: Account,
    accountStats: Map<String, AccountSummaryItem>,
    baseCurrency: String,
    currentTheme: String,
    gridColumns: Int,
    precision: Int,
    onAccountClick: (String) -> Unit,
    onRecordsClick: ((RecordsFilterContext) -> Unit)? = null,
    timelineRange: TimelineRange = TimelineRange.AllTime,
    modifier: Modifier = Modifier
) {
    val stats = accountStats[account.id]
    val totalInBase = stats?.balanceInBase ?: BigDecimal.ZERO

    val handleAccountClick = {
        onAccountClick(account.id)
    }

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
            currencyBalances = stats?.currencyBalances ?: emptyMap(),
            incomeInAccountCurrency = stats?.incomeInAccountCurrency ?: BigDecimal.ZERO,
            expenseInAccountCurrency = stats?.expenseInAccountCurrency ?: BigDecimal.ZERO,
            isDarkTheme = isDarkTheme,
            isAmoled = isAmoled,
            onClick = handleAccountClick,
            modifier = modifier,
            precision = precision
        )
        2 -> AeonAccountCardMedium(
            account = account,
            balanceInBase = totalInBase,
            baseCurrency = baseCurrency,
            currencyBalances = stats?.currencyBalances ?: emptyMap(),
            isDarkTheme = isDarkTheme,
            isAmoled = isAmoled,
            onClick = handleAccountClick,
            modifier = modifier,
            precision = precision
        )
        else -> AeonAccountCardSmall(
            account = account,
            balanceInBase = totalInBase,
            baseCurrency = baseCurrency,
            currencyBalances = stats?.currencyBalances ?: emptyMap(),
            isDarkTheme = isDarkTheme,
            isAmoled = isAmoled,
            onClick = handleAccountClick,
            modifier = modifier,
            precision = precision
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowRowScope.AeonAddAccountCard(
    baseCurrency: String,
    currentTheme: String,
    onClick: () -> Unit,
    gridColumns: Int = 2,
    precision: Int = 2,
    modifier: Modifier = Modifier
) {
    AeonAddAccountCardContent(baseCurrency, currentTheme, onClick, gridColumns, precision, modifier)
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
    AeonAddAccountCardContent(baseCurrency, currentTheme, onClick, gridColumns, precision, modifier)
}

@Composable
private fun AeonAddAccountCardContent(
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
        baseCurrency,
        "Default",
        "#808080",
        "Add",
        listOf("Add")
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
