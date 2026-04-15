package com.osscabinet.ossWallet.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.ui.util.convertAmount
import com.osscabinet.ossWallet.ui.util.formatCurrencyRight
import com.osscabinet.ossWallet.ui.util.isDark
import java.math.BigDecimal

import androidx.core.graphics.toColorInt

@Composable
fun AeonAccountHeroCard(
    account: Account,
    balance: BigDecimal,
    netBalance: BigDecimal,
    baseCurrency: String,
    exchangeRates: Map<String, BigDecimal>,
    modifier: Modifier = Modifier,
    precision: Int = 2
) {
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

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { cardSize = it },
        shape = RoundedCornerShape(32.dp),
        color = accountColor,
        contentColor = contentColor,
        shadowElevation = 0.dp
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Background Icon - Standardized with Large Card
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

            Column(modifier = Modifier.padding(24.dp)) {
                // First Row: Account Icon and Name
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
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
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Structure Type and Balance
                Column {
                    Text(
                        text = account.structureType.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.5.sp
                        ),
                        color = contentColor.copy(alpha = 0.7f)
                    )
                    Text(
                        text = formatCurrencyRight(balance, account.currencyCode, precision),
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 36.sp
                        )
                    )
                    if (account.currencyCode != baseCurrency) {
                        val convertedBalance = convertAmount(balance, account.currencyCode, baseCurrency, exchangeRates, precision)
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
                            text = "MONTHLY NET",
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
}
