package com.osscabinet.ossWallet.ui.components.accounts

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.CashFlowType
import com.osscabinet.ossWallet.ui.components.AppIconMap
import com.osscabinet.ossWallet.ui.components.AeonSectionHeader
import com.osscabinet.ossWallet.ui.components.AeonFlowBadge
import com.osscabinet.ossWallet.ui.components.AeonHybridSelector
import com.osscabinet.ossWallet.ui.components.AeonSelectMode
import com.osscabinet.ossWallet.ui.components.AeonFullScreenDialog
import com.osscabinet.ossWallet.ui.components.AeonSelectionScreen
import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter.formatAmount
import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter.formatCurrencyRight
import com.osscabinet.ossWallet.ui.util.toContainerColor
import com.osscabinet.ossWallet.ui.util.toOnContainerColor
import java.math.BigDecimal

@Composable
fun AeonAccountCardSmall(
    account: Account,
    balance: BigDecimal,
    balanceInBase: BigDecimal,
    baseCurrency: String,
    currencyBalances: Map<String, BigDecimal> = emptyMap(),
    isDarkTheme: Boolean,
    isAmoled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    precision: Int = 2
) {
    val accountColor = try { Color(android.graphics.Color.parseColor(account.colorHex)) } catch (e: Exception) { MaterialTheme.colorScheme.primary }
    val containerColor = accountColor.toContainerColor(isDarkTheme, isAmoled)
    val contentColor = accountColor.toOnContainerColor(isDarkTheme, isAmoled)
    val icon = AppIconMap[account.icon] ?: Icons.Rounded.AccountBalance

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        contentColor = contentColor,
        border = BorderStroke(0.5.dp, contentColor.copy(alpha = 0.1f))
    ) {
        Box {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = 10.dp)
                    .size(48.dp)
                    .alpha(0.1f),
                tint = contentColor
            )
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    account.name.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 9.sp,
                        letterSpacing = 0.5.sp
                    ),
                    color = contentColor.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Column {
                    Text(
                        formatCurrencyRight(balance, account.currencyCode, precision, shorten = true),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "≈ ${formatCurrencyRight(balanceInBase, baseCurrency, precision, shorten = true)}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = contentColor.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun AeonAccountCardMedium(
    account: Account,
    balance: BigDecimal,
    balanceInBase: BigDecimal,
    baseCurrency: String,
    currencyBalances: Map<String, BigDecimal> = emptyMap(),
    isDarkTheme: Boolean,
    isAmoled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    precision: Int = 2
) {
    val accountColor = try { Color(android.graphics.Color.parseColor(account.colorHex)) } catch (e: Exception) { MaterialTheme.colorScheme.primary }
    val containerColor = accountColor.toContainerColor(isDarkTheme, isAmoled)
    val contentColor = accountColor.toOnContainerColor(isDarkTheme, isAmoled)
    val icon = AppIconMap[account.icon] ?: Icons.Rounded.AccountBalance

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        color = containerColor,
        contentColor = contentColor,
        border = BorderStroke(0.5.dp, contentColor.copy(alpha = 0.1f))
    ) {
        Box {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = 10.dp)
                    .size(80.dp)
                    .alpha(0.1f),
                tint = contentColor
            )
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = contentColor.copy(alpha = 0.7f)
                    )
                    Text(
                        account.name.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 10.sp,
                            letterSpacing = 0.5.sp
                        ),
                        color = contentColor.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    formatCurrencyRight(balance, account.currencyCode, precision, shorten = true),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "≈ ${formatCurrencyRight(balanceInBase, baseCurrency, precision, shorten = true)}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = contentColor.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun AeonAccountCardLarge(
    account: Account,
    balance: BigDecimal,
    balanceInBase: BigDecimal,
    baseCurrency: String,
    exchangeRates: Map<String, BigDecimal> = emptyMap(),
    currencyBalances: Map<String, BigDecimal> = emptyMap(),
    incomeInAccountCurrency: BigDecimal = BigDecimal.ZERO,
    expenseInAccountCurrency: BigDecimal = BigDecimal.ZERO,
    isDarkTheme: Boolean,
    isAmoled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    precision: Int = 2,
    cardNumber: String? = null,
    extraLabel: String? = null,
    onClickIncome: (() -> Unit)? = null,
    onClickExpense: (() -> Unit)? = null
) {
    val accountColor = try { Color(android.graphics.Color.parseColor(account.colorHex)) } catch (e: Exception) { MaterialTheme.colorScheme.primary }
    val containerColor = accountColor.toContainerColor(isDarkTheme, isAmoled)
    val contentColor = accountColor.toOnContainerColor(isDarkTheme, isAmoled)
    val icon = AppIconMap[account.icon] ?: Icons.Rounded.AccountBalance
    
    var cardSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .onSizeChanged { cardSize = it },
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(32.dp),
        border = BorderStroke(0.5.dp, contentColor.copy(alpha = 0.1f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Background Icon
            val iconHeight = with(density) { (cardSize.height * 0.75f).toDp() }
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(iconHeight)
                    .alpha(0.15f),
                tint = contentColor
            )

            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            icon,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = contentColor.copy(alpha = 0.7f)
                        )
                        Text(
                            account.name.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = contentColor.copy(alpha = 0.7f)
                        )
                    }

                    
                    // Income/Expense Badges
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AeonFlowBadge(
                            amount = incomeInAccountCurrency,
                            type = CashFlowType.INCOME,
                            currency = account.currencyCode,
                            precision = precision,
                            onClick = onClickIncome
                        )
                        AeonFlowBadge(
                            amount = expenseInAccountCurrency,
                            type = CashFlowType.EXPENSE,
                            currency = account.currencyCode,
                            precision = precision,
                            onClick = onClickExpense
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        formatCurrencyRight(balance, account.currencyCode, precision = precision, shorten = true),
                        style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                        color = contentColor
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Horizontal scrollable list for subAccounts and currency codes
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = contentColor.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                account.structureType.uppercase(),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                color = contentColor
                            )
                        }
                        
                        val sortedCurrencyBalances = remember(currencyBalances) {
                            currencyBalances.toList().sortedBy { it.first }
                        }
                        
                        sortedCurrencyBalances.forEach { (curr, bal) ->
                            Surface(
                                color = contentColor.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        curr,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.ExtraBold),
                                        color = contentColor.copy(alpha = 0.6f)
                                    )
                                    Text(
                                        formatAmount(bal, precision = precision),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                        color = contentColor
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))

                    Column(horizontalAlignment = Alignment.End) {
                        if (cardNumber != null) {
                            Text(
                                cardNumber, 
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), 
                                color = contentColor.copy(alpha = 0.5f)
                            )
                        }
                        if (extraLabel != null) {
                            Text(
                                extraLabel, 
                                style = MaterialTheme.typography.labelSmall, 
                                color = contentColor.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AccountSelection(
    accounts: List<Account>,
    selectedAccount: Account?,
    onAccountSelected: (Account) -> Unit,
    onDismiss: () -> Unit
) {
    val sortedAccounts = remember(accounts) { accounts.sortedBy { it.name } }
    
    AeonFullScreenDialog(onDismissRequest = onDismiss) {
        AeonSelectionScreen(
            title = "Select Account",
            onBack = onDismiss
        ) { padding ->
            Column(modifier = Modifier.padding(padding).padding(24.dp)) {
                Box(modifier = Modifier.weight(1f, fill = false)) {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        AeonHybridSelector(
                            items = sortedAccounts,
                            mode = AeonSelectMode.SINGLE,
                            selectedIds = if (selectedAccount != null) setOf(selectedAccount.id) else emptySet(),
                            onSelectionChange = { ids ->
                                ids.firstOrNull()?.let { id ->
                                    sortedAccounts.find { it.id == id }?.let { 
                                        onAccountSelected(it)
                                        onDismiss()
                                    }
                                }
                            },
                            idMapper = { it.id },
                            labelMapper = { it.name },
                            detailMapper = { it.structureType },
                            iconMapper = { AppIconMap[it.icon] ?: Icons.Rounded.AccountBalance },
                            colorMapper = { Color(android.graphics.Color.parseColor(it.colorHex)) }
                        )
                    }
                }
            }
        }
    }
}
