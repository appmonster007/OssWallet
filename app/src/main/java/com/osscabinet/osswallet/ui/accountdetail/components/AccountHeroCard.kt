package com.osscabinet.ossWallet.ui.accountdetail.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.service.CurrencyService
import com.osscabinet.ossWallet.ui.components.AccountIconMap
import com.osscabinet.ossWallet.ui.util.formatAmount
import com.osscabinet.ossWallet.ui.util.formatCurrencyRight
import com.osscabinet.ossWallet.ui.util.isDark
import java.math.BigDecimal
import androidx.core.graphics.toColorInt
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.card.CardFilterRules

import androidx.compose.foundation.isSystemInDarkTheme
import com.osscabinet.ossWallet.ui.components.accounts.AeonAccountCardLarge

@Composable
fun AccountHeroCard(
    account: Account,
    balance: BigDecimal,
    income: BigDecimal,
    expense: BigDecimal,
    baseCurrency: String,
    exchangeRates: Map<String, BigDecimal>,
    modifier: Modifier = Modifier,
    currencyBalances: Map<String, BigDecimal> = emptyMap(),
    precision: Int = 2,
    showCurrencyBadges: Boolean = true,
    filter: TransactionFilter = TransactionFilter(),
    onShowCurrencyBadgesChange: (Boolean) -> Unit = {},
    onFilterChange: (TransactionFilter) -> Unit = {},
    onClick: (() -> Unit)? = null,
    onDelete: () -> Unit = {},
    filterRules: CardFilterRules = CardFilterRules.DEFAULT
) {
    val isDark = isSystemInDarkTheme()
    
    // Calculate balance in base currency
    val balanceInBase = CurrencyService.convertAmount(
        amount = balance,
        fromCurrency = account.currencyCode,
        toCurrency = baseCurrency,
        exchangeRates = exchangeRates,
        precision = precision
    )

    AeonAccountCardLarge(
        account = account,
        balance = balance,
        balanceInBase = balanceInBase,
        baseCurrency = baseCurrency,
        exchangeRates = exchangeRates,
        currencyBalances = currencyBalances,
        incomeInAccountCurrency = income,
        expenseInAccountCurrency = expense,
        isDarkTheme = isDark,
        isAmoled = false, // Or pass it if available
        onClick = { onClick?.invoke() },
        modifier = modifier,
        precision = precision,
        extraLabel = account.structureType.uppercase()
    )
}
