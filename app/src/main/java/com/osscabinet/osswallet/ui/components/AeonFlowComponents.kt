package com.osscabinet.ossWallet.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.items
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.CashFlowType
import com.osscabinet.ossWallet.ui.viewmodel.AccountStats
import com.osscabinet.ossWallet.ui.util.calculateTotalInCurrency
import com.osscabinet.ossWallet.ui.util.convertAmount
import com.osscabinet.ossWallet.ui.util.formatAmount
import com.osscabinet.ossWallet.ui.util.formatCurrencyRight
import java.math.BigDecimal
import java.math.RoundingMode

@Composable
fun AeonFlowBreakdownItem(
    curr: String,
    amount: BigDecimal,
    inBase: BigDecimal,
    baseCurrency: String,
    precision: Int = 2
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
            if (curr != baseCurrency) {
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
fun AeonFlowBadge(
    amount: BigDecimal,
    type: CashFlowType,
    modifier: Modifier = Modifier,
    incomeColor: Color = Color(0xFF2E7D32),
    expenseColor: Color = Color(0xFFC62828),
    precision: Int = 2
) {
    val isIncome = type == CashFlowType.INCOME
    val flowColor = if (isIncome) incomeColor else expenseColor
    val icon = if (isIncome) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward

    // Following Material 3 tonal container principles
    val containerColor = flowColor.copy(alpha = 0.12f)
    val contentColor = flowColor // High contrast against the pale background

    Surface(
        modifier = modifier,
        color = containerColor,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, flowColor.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = formatAmount(amount, precision),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                ),
                color = contentColor
            )
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
    incomeColor: Color = Color(0xFF2E7D32),
    expenseColor: Color = Color(0xFFC62828),
    precision: Int = 2,
    content: @Composable ColumnScope.() -> Unit = {}
) {
    val isAmoled = currentTheme == "Amoled"

    val isIncome = type == CashFlowType.INCOME
    val flowColor = if (isIncome) incomeColor else expenseColor

    // M3 Tonal Palette approach: 
    // Container is very light/desaturated, content is the bold brand/semantic color
    val containerColor = if (isAmoled) Color.Black else flowColor.copy(alpha = 0.08f)
    val borderColor =
        if (isAmoled) BorderStroke(1.dp, flowColor.copy(alpha = 0.3f)) else BorderStroke(
            1.dp,
            flowColor.copy(alpha = 0.15f)
        )
    val icon = if (isIncome) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward

    var cardSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current

    Surface(
        modifier = modifier.onSizeChanged { cardSize = it },
        shape = RoundedCornerShape(24.dp),
        color = containerColor,
        border = borderColor
    ) {
        Box {
            val iconHeight = with(density) { (cardSize.width * 0.5f).toDp() }
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.TopEnd)
//                    .offset { IntOffset(x = (cardSize.width * 0.25f).toInt(), y = 0) }
                    .size(iconHeight)
                    .alpha(0.2f),
                tint = flowColor
            )
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = flowColor,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatCurrencyRight(totalInBase, baseCurrency, precision),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
                )
                Spacer(modifier = Modifier.height(12.dp))
                content()
            }
        }
    }
}

@Composable
fun AeonCashFlowSummary(
    transactions: List<Transaction>,
    baseCurrency: String,
    exchangeRates: Map<String, BigDecimal>,
    currentTheme: String,
    modifier: Modifier = Modifier,
    headerModifier: Modifier = Modifier,
    showTitle: Boolean = true,
    precision: Int = 2
) {
    val allSupportedCurrencies = remember(transactions) {
        transactions.map { it.currency }.distinct().sorted()
    }

    val flowData by remember(
        allSupportedCurrencies,
        transactions,
        exchangeRates,
        baseCurrency,
        precision
    ) {
        derivedStateOf {
            allSupportedCurrencies.sorted().mapNotNull { curr ->
                val txsForCurr = transactions.filter { it.currency == curr }
                val income = txsForCurr.filter { it.isIncome }
                    .fold(BigDecimal.ZERO) { acc, t -> acc.add(t.amount) }
                val expense = txsForCurr.filter { !it.isIncome }
                    .fold(BigDecimal.ZERO) { acc, t -> acc.add(t.amount) }

                if (income.signum() == 0 && expense.signum() == 0) null
                else {
                    val incomeInBase = convertAmount(income, curr, baseCurrency, exchangeRates, precision + 4)
                    val expenseInBase = convertAmount(expense, curr, baseCurrency, exchangeRates, precision + 4)
                    Triple(curr, Pair(income, incomeInBase), Pair(expense, expenseInBase))
                }
            }
        }
    }

    val totalIncomeInBase =
        flowData.fold(BigDecimal.ZERO) { acc, data -> acc.add(data.second.second) }.setScale(precision, RoundingMode.HALF_UP)
    val totalExpenseInBase =
        flowData.fold(BigDecimal.ZERO) { acc, data -> acc.add(data.third.second) }.setScale(precision, RoundingMode.HALF_UP)

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (showTitle) {
                AeonFlowSectionHeader(
                    title = "CASH FLOW SUMMARY",
                    modifier = headerModifier
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
                        flowData.forEach { (curr, income, _) ->
                            AeonFlowBreakdownItem(
                                curr,
                                income.first,
                                income.second,
                                baseCurrency,
                                precision
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
                        flowData.forEach { (curr, _, expense) ->
                            AeonFlowBreakdownItem(
                                curr,
                                expense.first,
                                expense.second,
                                baseCurrency,
                                precision
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AeonAccountsSection(
    onManageClick: () -> Unit,
    onAccountClick: (String) -> Unit,
    onGridColumnsChange: (Int) -> Unit,
    accounts: List<com.osscabinet.ossWallet.domain.model.Account>,
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            AeonFlowSectionHeader(
                title = "Accounts",
                modifier = if (onHeaderClick != null) headerModifier.clickable(onClick = onHeaderClick) else headerModifier,
                trailing = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                Icons.Default.MoreHoriz,
                                contentDescription = "Grid Settings",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

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

                                        Surface(
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                            shape = RoundedCornerShape(20.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(16.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    "SHOW ADD ACCOUNT CARD",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        letterSpacing = 1.sp
                                                    ),
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Switch(
                                                    checked = showAddAccountCard,
                                                    onCheckedChange = onShowAddAccountCardChange
                                                )
                                            }
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(32.dp),
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        }
                    }
                }
            )

            if (accounts.isEmpty()) {
                AeonAddAccountCard(
                    baseCurrency = baseCurrency,
                    currentTheme = currentTheme,
                    onClick = onManageClick,
                    precision = precision,
                    gridColumns = gridColumns,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    maxItemsInEachRow = gridColumns
                ) {
                    accounts.forEach { account ->
                        val balanceInBase = convertAmount(account.balance, account.currencyCode, baseCurrency, exchangeRates, precision)
                        val isDarkTheme = when (currentTheme) {
                            "Light" -> false
                            "Dark", "Amoled" -> true
                            else -> isSystemInDarkTheme()
                        }
                        val isAmoled = currentTheme == "Amoled"
                        val stats = accountStats[account.id]

                        when (gridColumns) {
                            1 -> AeonAccountCardLarge(
                                account = account,
                                balanceInBase = balanceInBase,
                                baseCurrency = baseCurrency,
                                exchangeRates = exchangeRates,
                                currencyBalances = stats?.currencyBalances ?: emptyMap(),
                                incomeInAccountCurrency = stats?.incomeInAccountCurrency ?: BigDecimal.ZERO,
                                expenseInAccountCurrency = stats?.expenseInAccountCurrency ?: BigDecimal.ZERO,
                                isDarkTheme = isDarkTheme,
                                isAmoled = isAmoled,
                                onClick = { onAccountClick(account.id) },
                                modifier = Modifier.fillMaxWidth(),
                                precision = precision
                            )
                            2 -> AeonAccountCardMedium(
                                account = account,
                                balanceInBase = balanceInBase,
                                baseCurrency = baseCurrency,
                                isDarkTheme = isDarkTheme,
                                isAmoled = isAmoled,
                                onClick = { onAccountClick(account.id) },
                                modifier = Modifier.weight(1f),
                                precision = precision
                            )
                            3 -> AeonAccountCardSmall(
                                account = account,
                                balanceInBase = balanceInBase,
                                baseCurrency = baseCurrency,
                                isDarkTheme = isDarkTheme,
                                isAmoled = isAmoled,
                                onClick = { onAccountClick(account.id) },
                                modifier = Modifier.weight(1f),
                                precision = precision
                            )
                            else -> AeonAccountCardSmall(
                                account = account,
                                balanceInBase = balanceInBase,
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
                            precision = precision,
                            gridColumns = gridColumns,
                            modifier = if (gridColumns == 1) Modifier.fillMaxWidth() else Modifier.weight(1f)
                        )
                    }

                    val totalItems = accounts.size + if (showAddAccountCard) 1 else 0
                    if (totalItems % gridColumns != 0) {
                        repeat(gridColumns - (totalItems % gridColumns)) {
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
    precision: Int,
    gridColumns: Int,
    modifier: Modifier = Modifier
) {
    val isDarkTheme = when (currentTheme) {
        "Light" -> false
        "Dark", "Amoled" -> true
        else -> isSystemInDarkTheme()
    }
    val isAmoled = currentTheme == "Amoled"
    val placeholderAccount = com.osscabinet.ossWallet.domain.model.Account(
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

@Composable
fun AeonFlowSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = {}
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            ),
            color = MaterialTheme.colorScheme.outline
        )
        trailing()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetWorthSection(
    accounts: List<com.osscabinet.ossWallet.domain.model.Account>,
    baseCurrency: String,
    modifier: Modifier = Modifier,
    headerModifier: Modifier = Modifier,
    exchangeRates: Map<String, BigDecimal> = emptyMap(),
    precision: Int = 2,
    fontSize: String = "Medium",
    onFontSizeChange: (String) -> Unit = {}
) {
    var showSettings by remember { mutableStateOf(false) }

    val totalBalanceInBase = remember(accounts, exchangeRates, baseCurrency, precision) {
        val currencyTotals = accounts.groupBy { it.currencyCode }
            .mapValues { (_, accs) -> accs.fold(BigDecimal.ZERO) { acc, a -> acc.add(a.balance) } }
        calculateTotalInCurrency(currencyTotals, baseCurrency, exchangeRates, precision)
    }

    val headlineStyle = when (fontSize) {
        "Small" -> MaterialTheme.typography.displaySmall
        "Medium" -> MaterialTheme.typography.displayMedium
        "Large" -> MaterialTheme.typography.displayLarge
        else -> MaterialTheme.typography.displayMedium
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            AeonFlowSectionHeader(
                title = "TOTAL NET WORTH",
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
            Text(
                formatCurrencyRight(totalBalanceInBase, baseCurrency, precision),
                style = headlineStyle.copy(fontWeight = FontWeight.Bold)
            )
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
                    "Net Worth Settings",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
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
                                "FONT SIZE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )

                            val sizes = listOf("Small", "Medium", "Large")
                            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                sizes.forEachIndexed { index, size ->
                                    SegmentedButton(
                                        selected = fontSize == size,
                                        onClick = { onFontSizeChange(size) },
                                        shape = SegmentedButtonDefaults.itemShape(
                                            index = index,
                                            count = sizes.size
                                        ),
                                        label = {
                                            val iconSize = when(size) {
                                                "Small" -> 16.sp
                                                "Medium" -> 20.sp
                                                "Large" -> 24.sp
                                                else -> 20.sp
                                            }
                                            Text("A", fontSize = iconSize, fontWeight = FontWeight.Black)
                                        }
                                    )
                                }
                            }
                        }
                    }
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
                
                // Always reserve space for the base currency conversion to maintain uniform height
                if (currency != baseCurrency) {
                    Text(
                        "≈ ${formatCurrencyRight(inBase, baseCurrency, precision)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1
                    )
                } else {
                    // Transparent placeholder for height consistency
                    Text(
                        "",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.height(14.dp) // Approximate height of labelSmall
                    )
                }
            }
        }
    }
}

@Composable
fun CurrencyAggregationSection(
    currencyTotals: Map<String, BigDecimal>,
    baseCurrency: String,
    exchangeRates: Map<String, BigDecimal>,
    modifier: Modifier = Modifier,
    headerModifier: Modifier = Modifier,
    precision: Int = 2,
    layout: String = "scroll",
    onLayoutChange: (String) -> Unit = {}
) {
    var showSettings by remember { mutableStateOf(false) }

    if (currencyTotals.isNotEmpty()) {
        Card(
            modifier = modifier,
            shape = RoundedCornerShape(24.dp),
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
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 4.dp)
                    ) {
                        items(sortedTotals) { (currency, amount) ->
                            AeonCurrencyBalanceItem(
                                currency = currency,
                                amount = amount,
                                baseCurrency = baseCurrency,
                                exchangeRates = exchangeRates,
                                precision = precision
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
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "LAYOUT",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    if (layout == "grid") "Grid Layout" else "Horizontal Overflow",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = layout == "grid",
                                onCheckedChange = { onLayoutChange(if (it) "grid" else "scroll") },
                                thumbContent = {
                                    if (layout == "grid") {
                                        Icon(
                                            imageVector = Icons.Default.GridView,
                                            contentDescription = null,
                                            modifier = Modifier.size(SwitchDefaults.IconSize)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.ViewCarousel,
                                            contentDescription = null,
                                            modifier = Modifier.size(SwitchDefaults.IconSize)
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            },
            shape = RoundedCornerShape(32.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}
