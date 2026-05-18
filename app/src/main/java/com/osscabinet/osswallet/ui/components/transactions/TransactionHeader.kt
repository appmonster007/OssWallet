package com.osscabinet.ossWallet.ui.components.transactions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.osscabinet.ossWallet.ui.theme.PositiveAmountColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter.formatCurrencyRight
import java.math.BigDecimal
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.theme.labelExtraSmall
import com.osscabinet.ossWallet.ui.theme.AppTextStyles

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
    val balanceWeight = when {
        showOpeningBalance && showClosingBalance -> 2f
        showOpeningBalance || showClosingBalance -> 1f
        else -> 0f
    }

    val activeCurrencies = remember(allCurrencies, incomeByCurrency, expenseByCurrency) {
        allCurrencies.filter { curr ->
            val inc = incomeByCurrency[curr] ?: BigDecimal.ZERO
            val exp = expenseByCurrency[curr] ?: BigDecimal.ZERO
            inc.signum() != 0 || exp.signum() != 0
        }
    }

    val currenciesToDisplay = if (showOnlyActiveCurrencies) activeCurrencies else allCurrencies
    val canExpand = showCurrencyBreakdown && currenciesToDisplay.isNotEmpty()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = styles.surfaceColor
    ) {
        Column {
            HorizontalDivider(thickness = 0.5.dp, color = styles.dividerColor)
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                // Row 1: Title Info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title.uppercase(),
                        style = styles.titleStyle
                    )
                    Text(
                        text = subtitle.uppercase(),
                        style = styles.subtitleStyle
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Row 2: Headers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.width(styles.labelColumnWidth))
                    listOf("INCOME", "EXPENSE", "NET").forEach { label ->
                        Text(
                            label,
                            style = styles.columnHeaderStyle,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 8.dp),
                            textAlign = TextAlign.End
                        )
                    }
                    if (balanceWeight > 0) {
                        Text(
                            "BALANCE",
                            style = styles.columnHeaderStyle,
                            modifier = Modifier
                                .weight(balanceWeight)
                                .padding(start = 8.dp),
                            textAlign = TextAlign.End
                        )
                    }
                }

                // Row 3: Aggregate (Clickable)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (canExpand) Modifier.clickable { isExpanded = !isExpanded } else Modifier)
                        .padding(vertical = 1.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.width(styles.labelColumnWidth),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TOTAL",
                            style = styles.totalLabelStyle,
                            color = AppColors.primary
                        )
                        if (canExpand) {
                            Icon(
                                if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                contentDescription = null,
                                modifier = Modifier.size(10.dp),
                                tint = AppColors.primary.copy(alpha = 0.7f)
                            )
                        }
                    }

                    val baseColumnModifier = Modifier.weight(1f).padding(start = 8.dp)

                    Text(
                        formatCurrencyRight(incomeBase, baseCurrency, shorten = true),
                        style = styles.baseValueStyle,
                        color = styles.positiveColor,
                        modifier = baseColumnModifier,
                        textAlign = TextAlign.End
                    )
                    Text(
                        formatCurrencyRight(expenseBase, baseCurrency, shorten = true),
                        style = styles.baseValueStyle,
                        color = styles.negativeColor,
                        modifier = baseColumnModifier,
                        textAlign = TextAlign.End
                    )
                    Text(
                        formatCurrencyRight(netBase, baseCurrency, shorten = true),
                        style = styles.baseValueStyle,
                        color = if (netBase >= BigDecimal.ZERO) styles.positiveColor else styles.negativeColor,
                        modifier = baseColumnModifier,
                        textAlign = TextAlign.End
                    )

                    if (balanceWeight > 0) {
                        val openingBaseText = formatCurrencyRight(totalOpeningBalanceBase, baseCurrency, shorten = true)
                        val closingBaseText = formatCurrencyRight(totalClosingBalanceBase, baseCurrency, shorten = true)
                        val balanceText = when {
                            showOpeningBalance && showClosingBalance -> "$openingBaseText → $closingBaseText"
                            showOpeningBalance -> openingBaseText
                            else -> closingBaseText
                        }
                        Text(
                            text = balanceText,
                            style = styles.baseValueStyle,
                            modifier = Modifier
                                .weight(balanceWeight)
                                .padding(start = 8.dp),
                            textAlign = TextAlign.End,
                            color = AppColors.contentPrimary,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                // Row 4+: Breakdown (Expandable)
                AnimatedVisibility(visible = showCurrencyBreakdown && isExpanded) {
                    Column(modifier = Modifier.padding(top = 2.dp)) {
                        currenciesToDisplay.forEach { curr ->
                            val inc = incomeByCurrency[curr] ?: BigDecimal.ZERO
                            val exp = expenseByCurrency[curr] ?: BigDecimal.ZERO
                            val net = inc.add(exp)
                            val balAfter = closingBalancesByCurrency[curr]
                            val balBefore = openingBalancesByCurrency[curr] ?: BigDecimal.ZERO

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 1.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = curr,
                                    style = styles.breakdownLabelStyle,
                                    modifier = Modifier.width(styles.labelColumnWidth),
                                    color = styles.outlineColor.copy(alpha = 0.8f)
                                )

                                val breakdownColumnModifier = Modifier.weight(1f).padding(start = 8.dp)

                                Text(
                                    formatCurrencyRight(inc, curr, shorten = true),
                                    style = styles.breakdownValueStyle,
                                    color = styles.positiveColor.copy(alpha = 0.7f),
                                    modifier = breakdownColumnModifier,
                                    textAlign = TextAlign.End
                                )
                                Text(
                                    formatCurrencyRight(exp, curr, shorten = true),
                                    style = styles.breakdownValueStyle,
                                    color = styles.negativeColor.copy(alpha = 0.7f),
                                    modifier = breakdownColumnModifier,
                                    textAlign = TextAlign.End
                                )
                                Text(
                                    formatCurrencyRight(net, curr, shorten = true),
                                    style = styles.breakdownValueStyle.copy(fontWeight = FontWeight.Bold),
                                    color = (if (net >= BigDecimal.ZERO) styles.positiveColor else styles.negativeColor).copy(alpha = 0.7f),
                                    modifier = breakdownColumnModifier,
                                    textAlign = TextAlign.End
                                )

                                if (balanceWeight > 0) {
                                    val balAfterText = if (balAfter != null) formatCurrencyRight(balAfter, curr, shorten = true) else "0.00 $curr"
                                    val balBeforeText = formatCurrencyRight(balBefore, curr, shorten = true)

                                    val balanceText = when {
                                        showOpeningBalance && showClosingBalance -> "$balBeforeText → $balAfterText"
                                        showOpeningBalance -> balBeforeText
                                        else -> balAfterText
                                    }

                                    Text(
                                        text = balanceText,
                                        style = styles.breakdownValueStyle.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier
                                            .weight(balanceWeight)
                                            .padding(start = 8.dp),
                                        textAlign = TextAlign.End,
                                        color = AppColors.contentPrimary.copy(alpha = 0.6f),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }
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
    val surfaceColor: Color,
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
            surfaceColor = AppColors.sheetSurface,
            outlineColor = AppColors.border,
            dividerColor = AppColors.divider.copy(alpha = 0.5f),
            labelColumnWidth = 40.dp,
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

@Preview(showBackground = true, heightDp = 100)
@Composable
fun TransactionHeaderPreview() {
    OssWalletTheme {
        TransactionHeader(
            title = "WEEK 1",
            subtitle = "Jan 1 - Jan 7",
            incomeByCurrency = mapOf("USD" to BigDecimal("5000.00"), "EUR" to BigDecimal("2500.00")),
            expenseByCurrency = mapOf("USD" to BigDecimal("3200.00"), "EUR" to BigDecimal("1800.00")),
            openingBalancesByCurrency = mapOf("USD" to BigDecimal("10000.00"), "EUR" to BigDecimal("7300.00")),
            closingBalancesByCurrency = mapOf("USD" to BigDecimal("15000.00"), "EUR" to BigDecimal("8000.00")),
            allCurrencies = listOf("EUR", "USD"),
            incomeBase = BigDecimal("7500.00"),
            expenseBase = BigDecimal("5000.00"),
            netBase = BigDecimal("2500.00"),
            totalOpeningBalanceBase = BigDecimal("17300.00"),
            totalClosingBalanceBase = BigDecimal("23000.00"),
            baseCurrency = "USD"
        )
    }
}
