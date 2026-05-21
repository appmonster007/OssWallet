package com.osscabinet.ossWallet.ui.components.transactions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter.formatCurrencyRight
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.PositiveAmountColor
import java.math.BigDecimal

/**
 * Reusable component for displaying financial totals (Income, Expense, Net, and optional Balance)
 * with an expandable per-currency breakdown.
 */
@Composable
fun TransactionSummaryBreakdown(
    incomeByCurrency: Map<String, BigDecimal>,
    expenseByCurrency: Map<String, BigDecimal>,
    incomeBase: BigDecimal,
    expenseBase: BigDecimal,
    netBase: BigDecimal,
    baseCurrency: String,
    modifier: Modifier = Modifier,
    openingBalancesByCurrency: Map<String, BigDecimal> = emptyMap(),
    closingBalancesByCurrency: Map<String, BigDecimal> = emptyMap(),
    totalOpeningBalanceBase: BigDecimal = BigDecimal.ZERO,
    totalClosingBalanceBase: BigDecimal = BigDecimal.ZERO,
    showOpeningBalance: Boolean = false,
    showClosingBalance: Boolean = false,
    currenciesToDisplay: List<String>? = null,
    labelColumnWidth: Dp = 40.dp,
    showCurrencyBreakdown: Boolean = true,
    initiallyExpanded: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    var isExpanded by remember { mutableStateOf(initiallyExpanded) }

    val positiveColor = PositiveAmountColor
    val negativeColor = AppColors.error
    
    val currencies = remember(incomeByCurrency, expenseByCurrency, openingBalancesByCurrency, closingBalancesByCurrency, currenciesToDisplay) {
        currenciesToDisplay ?: (incomeByCurrency.keys + expenseByCurrency.keys + openingBalancesByCurrency.keys + closingBalancesByCurrency.keys).distinct().sorted()
    }

    val canExpand = showCurrencyBreakdown && currencies.isNotEmpty()
    
    val balanceWeight = when {
        showOpeningBalance && showClosingBalance -> 2f
        showOpeningBalance || showClosingBalance -> 1f
        else -> 0f
    }

    Column(modifier = modifier.padding(contentPadding)) {
        // Headers
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.width(labelColumnWidth))
            listOf("INCOME", "EXPENSE", "NET").forEach { label ->
                Text(
                    label,
                    style = AppTextStyles.metaLabel.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 8.sp,
                        letterSpacing = 0.5.sp
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                    textAlign = TextAlign.End
                )
            }
            if (balanceWeight > 0) {
                Text(
                    "BALANCE",
                    style = AppTextStyles.metaLabel.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 8.sp,
                        letterSpacing = 0.5.sp
                    ),
                    modifier = Modifier
                        .weight(balanceWeight)
                        .padding(start = 8.dp),
                    textAlign = TextAlign.End
                )
            }
        }

        // Totals Row (Clickable for expansion)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (canExpand) Modifier.clickable { isExpanded = !isExpanded } else Modifier)
                .padding(vertical = 1.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.width(labelColumnWidth),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TOTAL",
                    style = AppTextStyles.metaLabel.copy(
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                    ),
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
                style = AppTextStyles.transactionSwipeAction.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                color = positiveColor,
                modifier = baseColumnModifier,
                textAlign = TextAlign.End
            )
            Text(
                formatCurrencyRight(expenseBase, baseCurrency, shorten = true),
                style = AppTextStyles.transactionSwipeAction.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                color = negativeColor,
                modifier = baseColumnModifier,
                textAlign = TextAlign.End
            )
            Text(
                formatCurrencyRight(netBase, baseCurrency, shorten = true),
                style = AppTextStyles.transactionSwipeAction.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                color = if (netBase >= BigDecimal.ZERO) positiveColor else negativeColor,
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
                    style = AppTextStyles.transactionSwipeAction.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
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

        // Breakdown Rows
        AnimatedVisibility(visible = isExpanded) {
            Column(modifier = Modifier.padding(top = 2.dp)) {
                currencies.forEach { curr ->
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
                            style = AppTextStyles.metaLabel.copy(fontSize = 8.sp, fontWeight = FontWeight.Medium),
                            modifier = Modifier.width(labelColumnWidth),
                            color = AppColors.border.copy(alpha = 0.8f)
                        )

                        val breakdownColumnModifier = Modifier.weight(1f).padding(start = 8.dp)

                        Text(
                            formatCurrencyRight(inc, curr, shorten = true),
                            style = AppTextStyles.metaLabel.copy(fontSize = 8.sp),
                            color = positiveColor.copy(alpha = 0.7f),
                            modifier = breakdownColumnModifier,
                            textAlign = TextAlign.End
                        )
                        Text(
                            formatCurrencyRight(exp, curr, shorten = true),
                            style = AppTextStyles.metaLabel.copy(fontSize = 8.sp),
                            color = negativeColor.copy(alpha = 0.7f),
                            modifier = breakdownColumnModifier,
                            textAlign = TextAlign.End
                        )
                        Text(
                            formatCurrencyRight(net, curr, shorten = true),
                            style = AppTextStyles.metaLabel.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                            color = (if (net >= BigDecimal.ZERO) positiveColor else negativeColor).copy(alpha = 0.7f),
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
                                style = AppTextStyles.metaLabel.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
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

@Preview(showBackground = true)
@Composable
fun TransactionSummaryBreakdownPreview() {
    OssWalletTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            TransactionSummaryBreakdown(
                incomeByCurrency = mapOf("USD" to BigDecimal("1500.00"), "EUR" to BigDecimal("200.00")),
                expenseByCurrency = mapOf("USD" to BigDecimal("800.00"), "EUR" to BigDecimal("50.00")),
                incomeBase = BigDecimal("1700.00"),
                expenseBase = BigDecimal("850.00"),
                netBase = BigDecimal("850.00"),
                baseCurrency = "USD",
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
