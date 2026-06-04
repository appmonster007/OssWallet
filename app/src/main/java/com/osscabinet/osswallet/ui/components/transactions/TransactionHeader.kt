package com.osscabinet.ossWallet.ui.components.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter.formatAuto
import com.osscabinet.ossWallet.ui.theme.AeonSize
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.components.GlassSurface
import com.osscabinet.ossWallet.ui.theme.GlassTokens
import com.osscabinet.ossWallet.ui.theme.HazeLayer
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.theme.PositiveAmountColor
import java.math.BigDecimal

/**
 * Unified transaction header composable that can be used for both weekly and daily transaction summaries.
 * Displays income, expense, net, and balance totals with expandable currency breakdown.
 */
@Composable
fun TransactionHeader(
    title: String,
    subtitle: String,
    incomeByCurrency: Map<String, BigDecimal>,
    expenseByCurrency: Map<String, BigDecimal>,
    openingBalancesByCurrency: Map<String, BigDecimal>,
    closingBalancesByCurrency: Map<String, BigDecimal>,
    allCurrencies: List<String>,
    incomeBase: BigDecimal,
    expenseBase: BigDecimal,
    netBase: BigDecimal,
    totalOpeningBalanceBase: BigDecimal,
    totalClosingBalanceBase: BigDecimal,
    baseCurrency: String,
    showOpeningBalance: Boolean = false,
    showClosingBalance: Boolean = true,
    showOnlyActiveCurrencies: Boolean = false,
    showCurrencyBreakdown: Boolean = true,
    initiallyExpanded: Boolean = false
) {
    var isExpanded by remember { mutableStateOf(initiallyExpanded) }

    val styles = TransactionHeaderStyles.default()
    val isDark = isSystemInDarkTheme()
    val glassShape = RoundedCornerShape(AeonSize.D12.value)
    val glassStyle = GlassTokens.style()

    val activeCurrencies = remember(allCurrencies, incomeByCurrency, expenseByCurrency) {
        allCurrencies.filter { curr ->
            val inc = incomeByCurrency[curr] ?: BigDecimal.ZERO
            val exp = expenseByCurrency[curr] ?: BigDecimal.ZERO
            inc.signum() != 0 || exp.signum() != 0
        }
    }

    val currenciesToDisplay = remember(showOnlyActiveCurrencies, activeCurrencies, allCurrencies) {
        if (showOnlyActiveCurrencies) activeCurrencies else allCurrencies
    }

    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AeonSize.D16.value, vertical = AeonSize.D8.value),
        shape = glassShape,
        hazeLayer = HazeLayer.Surface,
        hazeStyle = glassStyle,
        fallbackColor = AppColors.cardSurfaceProminent,
        shadowElevation = AeonSize.D12.value,
        ambientShadowAlpha = 0.06f,
        spotShadowAlpha = 0.12f,
        borderColor = if (isDark) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.6f)
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = AeonSize.D12.value,
                vertical = AeonSize.D12.value
            )
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = title.uppercase(),
                    style = styles.titleStyle,
                    modifier = Modifier.align(Alignment.CenterStart)
                )
                if (!showCurrencyBreakdown) {
                    Text(
                        text = "∑ ${formatAuto(netBase, baseCurrency, shorten = true)}",
                        style = styles.baseValueStyle,
                        color = if (netBase >= BigDecimal.ZERO) styles.positiveColor else styles.negativeColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                Text(
                    text = subtitle.uppercase(),
                    style = styles.subtitleStyle,
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }

            if (showCurrencyBreakdown) {
                Spacer(modifier = Modifier.height(AeonSize.D4.value))

                    TransactionSummaryBreakdown(
                        incomeByCurrency = incomeByCurrency,
                        expenseByCurrency = expenseByCurrency,
                        incomeBase = incomeBase,
                        expenseBase = expenseBase,
                        netBase = netBase,
                        baseCurrency = baseCurrency,
                        openingBalancesByCurrency = openingBalancesByCurrency,
                        closingBalancesByCurrency = closingBalancesByCurrency,
                        totalOpeningBalanceBase = totalOpeningBalanceBase,
                        totalClosingBalanceBase = totalClosingBalanceBase,
                        showOpeningBalance = showOpeningBalance,
                        showClosingBalance = showClosingBalance,
                        currenciesToDisplay = currenciesToDisplay,
                        labelColumnWidth = styles.labelColumnWidth,
                        showCurrencyBreakdown = true,
                        initiallyExpanded = isExpanded
                    )
                }
            }
        }
}


/**
 * Design system for TransactionHeader to centralize styling choices.
 */
private data class TransactionHeaderStyles(
    val positiveColor: Color,
    val negativeColor: Color,
    val outlineColor: Color,
    val dividerColor: Color,
    val labelColumnWidth: androidx.compose.ui.unit.Dp,
    val titleStyle: androidx.compose.ui.text.TextStyle,
    val subtitleStyle: androidx.compose.ui.text.TextStyle,
    val columnHeaderStyle: androidx.compose.ui.text.TextStyle,
    val totalLabelStyle: androidx.compose.ui.text.TextStyle,
    val baseValueStyle: androidx.compose.ui.text.TextStyle,
    val breakdownLabelStyle: androidx.compose.ui.text.TextStyle,
    val breakdownValueStyle: androidx.compose.ui.text.TextStyle
) {
    companion object {
        @Composable
        fun default() = TransactionHeaderStyles(
            positiveColor = PositiveAmountColor,
            negativeColor = AppColors.error,
            outlineColor = AppColors.border,
            dividerColor = AppColors.divider.copy(alpha = 0.5f),
            labelColumnWidth = AeonSize.D40.value,
            titleStyle = AppTextStyles.transactionSwipeAction.copy(
                fontWeight = FontWeight.Bold,
                color = AppColors.border,
                fontSize = 8.sp,
                letterSpacing = 1.sp,
            ),
            subtitleStyle = AppTextStyles.metaLabel.copy(
                fontWeight = FontWeight.Medium,
                color = AppColors.border,
                fontSize = 8.sp,
                letterSpacing = 0.5.sp,
            ),
            columnHeaderStyle = AppTextStyles.metaLabel.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 8.sp,
                letterSpacing = 0.5.sp
            ),
            totalLabelStyle = AppTextStyles.metaLabel.copy(
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
            ),
            baseValueStyle = AppTextStyles.transactionSwipeAction.copy(
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
            ),
            breakdownLabelStyle = AppTextStyles.metaLabel.copy(
                fontSize = 8.sp,
                fontWeight = FontWeight.Medium,
            ),
            breakdownValueStyle = AppTextStyles.metaLabel.copy(
                fontSize = 8.sp,
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TransactionHeaderPreview() {
    OssWalletTheme {
        TransactionHeader(
            title = "Monday, Jan 22",
            subtitle = "2 Transactions",
            incomeByCurrency = mapOf("USD" to BigDecimal("150.00")),
            expenseByCurrency = mapOf("USD" to BigDecimal("45.50"), "EUR" to BigDecimal("10.00")),
            openingBalancesByCurrency = mapOf("USD" to BigDecimal("1200.00")),
            closingBalancesByCurrency = mapOf("USD" to BigDecimal("1304.50")),
            allCurrencies = listOf("USD", "EUR"),
            incomeBase = BigDecimal("150.00"),
            expenseBase = BigDecimal("56.50"),
            netBase = BigDecimal("93.50"),
            totalOpeningBalanceBase = BigDecimal("1200.00"),
            totalClosingBalanceBase = BigDecimal("1293.50"),
            baseCurrency = "USD",
            showOpeningBalance = true,
            showClosingBalance = true,
            initiallyExpanded = true
        )
    }
}

@Preview(showBackground = true, name = "Floating Transaction Header")
@Composable
fun FloatingTransactionHeaderPreview() {
    OssWalletTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF5F5F5)) // Lighter background to help see the shadow
                .padding(24.dp)
        ) {
            TransactionHeader(
                title = "Jan 22, 2024",
                subtitle = "3 Transactions",
                incomeByCurrency = mapOf("USD" to BigDecimal("200.00")),
                expenseByCurrency = mapOf("USD" to BigDecimal("50.00")),
                openingBalancesByCurrency = emptyMap(),
                closingBalancesByCurrency = emptyMap(),
                allCurrencies = listOf("USD"),
                incomeBase = BigDecimal("200.00"),
                expenseBase = BigDecimal("50.00"),
                netBase = BigDecimal("150.00"),
                totalOpeningBalanceBase = BigDecimal.ZERO,
                totalClosingBalanceBase = BigDecimal("150.00"),
                baseCurrency = "USD",
                showCurrencyBreakdown = false
            )
        }
    }
}
