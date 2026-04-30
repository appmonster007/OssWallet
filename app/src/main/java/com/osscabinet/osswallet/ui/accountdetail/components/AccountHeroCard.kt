package com.osscabinet.ossWallet.ui.accountdetail.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.service.CurrencyService
import com.osscabinet.ossWallet.ui.components.AccountIconMap
import com.osscabinet.ossWallet.ui.util.formatCurrencyRight
import com.osscabinet.ossWallet.ui.util.isDark
import java.math.BigDecimal
import androidx.core.graphics.toColorInt

import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Tune
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.ui.components.cards.AeonCardSettingsSheet
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

import com.osscabinet.ossWallet.domain.card.CardFilterRules

@Composable
fun AccountHeroCard(
    account: Account,
    balance: BigDecimal,
    netBalance: BigDecimal,
    baseCurrency: String,
    exchangeRates: Map<String, BigDecimal>,
    modifier: Modifier = Modifier,
    precision: Int = 2,
    showCurrencyBadges: Boolean = true,
    filter: TransactionFilter = TransactionFilter(),
    onShowCurrencyBadgesChange: (Boolean) -> Unit = {},
    onFilterChange: (TransactionFilter) -> Unit = {},
    onClick: (() -> Unit)? = null,
    onDelete: () -> Unit = {},
    filterRules: CardFilterRules = CardFilterRules.DEFAULT
) {
    var showDisplaySettings by remember { mutableStateOf(false) }
    var showFilterSettings by remember { mutableStateOf(false) }

    val accountColor = try {
        Color(account.colorHex.toColorInt())
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    val isDark = accountColor.isDark()
    val contentColor = if (isDark) Color.White else Color.Black
    val icon = AccountIconMap[account.icon] ?: Icons.Rounded.AccountBalance
    
    var cardSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current

    val timelineLabel = when (val tr = filter.timelineRange) {
        is com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.AllTime -> "All Time"
        is com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.ThisPeriod -> tr.period.label
        is com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.LastPeriod -> tr.period.label
        is com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.CustomRange -> {
            val formatter = DateTimeFormatter.ofPattern("MMM d").withZone(ZoneId.systemDefault())
            val start = formatter.format(Instant.ofEpochMilli(tr.start))
            val end = formatter.format(Instant.ofEpochMilli(tr.end))
            "$start - $end"
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { cardSize = it }
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(32.dp),
        color = accountColor,
        contentColor = contentColor,
        shadowElevation = 0.dp
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
                    .alpha(0.12f),
                tint = contentColor
            )

            // Settings Buttons
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = timelineLabel.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        fontSize = 10.sp
                    ),
                    color = contentColor.copy(alpha = 0.6f)
                )
                IconButton(
                    onClick = { showFilterSettings = true },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.FilterList,
                        contentDescription = "Filter",
                        tint = contentColor.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = { showDisplaySettings = true },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Tune,
                        contentDescription = "Display Settings",
                        tint = contentColor.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(24.dp)) {
                // First Row: Account Icon, Name, Structure Type and Balance
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = contentColor.copy(alpha = 0.9f)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = account.name.uppercase(),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = contentColor
                        )
                        Spacer(modifier = Modifier.width(12.dp))
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
                    }
                    Text(
                        text = formatCurrencyRight(balance, account.currencyCode, precision),
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 36.sp
                        )
                    )
                    if (account.currencyCode != baseCurrency) {
                        val convertedBalance = CurrencyService.convertAmount(balance, account.currencyCode, baseCurrency, exchangeRates, precision)
                        Text(
                            text = "≈ ${formatCurrencyRight(convertedBalance, baseCurrency, precision)}",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = contentColor.copy(alpha = 0.7f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Monthly Net and Account ID
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "NET FLOW",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = contentColor.copy(alpha = 0.7f)
                        )
                        Text(
                            text = formatCurrencyRight(netBalance, account.currencyCode, precision),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black)
                        )
                    }
                    
                    Text(
                        text = account.id.takeLast(4).let { "**** $it" },
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = contentColor.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }

    if (showDisplaySettings) {
        AlertDialog(
            onDismissRequest = { showDisplaySettings = false },
            dismissButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDisplaySettings = false
                    },
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
                TextButton(onClick = { showDisplaySettings = false }) {
                    Text("Done")
                }
            },
            title = {
                Text(
                    "Account Summary Settings",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Show Currency Badges", style = MaterialTheme.typography.bodyLarge)
                            Text("Display currency breakdown badges on balance", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                        Switch(
                            checked = showCurrencyBadges,
                            onCheckedChange = { onShowCurrencyBadgesChange(it) }
                        )
                    }
                }
            },
            shape = RoundedCornerShape(32.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    }

    if (showFilterSettings) {
        AeonCardSettingsSheet(
            title = "Account Hero Filter",
            filter = filter,
            onFilterChange = onFilterChange,
            onDismiss = { showFilterSettings = false },
            onDelete = onDelete,
            filterRules = filterRules
        )
    }
}
