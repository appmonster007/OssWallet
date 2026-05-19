package com.osscabinet.ossWallet.ui.screens.dashboard.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.analytics.CurrencyAggregationItem
import com.osscabinet.ossWallet.domain.model.settings.CurrencyBalancesCardConfig
import com.osscabinet.ossWallet.ui.components.*
import com.osscabinet.ossWallet.ui.components.cards.AeonExpressiveCard
import com.osscabinet.ossWallet.ui.components.cards.CardCallbacks
import com.osscabinet.ossWallet.ui.components.cards.FilterSelectionContext
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter.formatCurrencyRight
import java.math.BigDecimal
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles

@Composable
fun CurrencyBalancesCard(
    currencyTotals: List<CurrencyAggregationItem>,
    baseCurrency: String,
    config: CurrencyBalancesCardConfig,
    callbacks: CardCallbacks.CurrencyBalances = CardCallbacks.CurrencyBalances(),
    filterContext: FilterSelectionContext = FilterSelectionContext(),
    baseFilter: TransactionFilter = TransactionFilter(),
    inheritedFilter: TransactionFilter = TransactionFilter(),
    showFilterButton: Boolean = true,
    modifier: Modifier = Modifier,
    headerModifier: Modifier = Modifier,
) {
    val displayFilter = if (inheritedFilter.timelineRange !is com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.AllTime)
        config.filter.copy(timelineRange = inheritedFilter.timelineRange)
    else config.filter

    AeonExpressiveCard(
        title = config.title,
        icon = Icons.Rounded.Payments,
        modifier = modifier,
        headerModifier = headerModifier,
        filterEnabled = showFilterButton,
        filter = displayFilter,
        baseFilter = baseFilter,
        filterRules = config.filterRules,
        filterContext = filterContext,
        onFilterChange = { callbacks.onFilterChange(config.cardId, it) },
        onSaveFilterClick = callbacks.onSaveFilterClick,
        displaySettings = { onDismiss ->
            CurrencyDisplaySettingsDialog(config = config, baseCurrency = baseCurrency, callbacks = callbacks, onDismiss = onDismiss)
        }
    ) {
        if (currencyTotals.isEmpty()) {
            Text(
                text = "No data for selected filter",
                style = AppTextStyles.caption,
                color = AppColors.contentSecondary,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else if (config.layout == "grid") {
            val chunks = currencyTotals.chunked(3)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                chunks.forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowItems.forEach { item ->
                            CurrencyBalanceItem(
                                curr = item.currency,
                                amount = item.amount,
                                inBase = item.amountInBase,
                                baseCurrency = baseCurrency,
                                showBaseConversion = config.showBaseConversion,
                                onClick = { callbacks.onCurrencyClick?.invoke(item.currency, config.filter.timelineRange) },
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
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 4.dp)
            ) {
                items(currencyTotals) { item ->
                    CurrencyBalanceItem(
                        curr = item.currency,
                        amount = item.amount,
                        inBase = item.amountInBase,
                        baseCurrency = baseCurrency,
                        showBaseConversion = config.showBaseConversion,
                        onClick = { callbacks.onCurrencyClick?.invoke(item.currency, config.filter.timelineRange) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CurrencyDisplaySettingsDialog(
    config: CurrencyBalancesCardConfig,
    baseCurrency: String,
    callbacks: CardCallbacks.CurrencyBalances,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        dismissButton = {
            TextButton(
                onClick = {
                    callbacks.onDelete()
                    onDismiss()
                },
                colors = ButtonDefaults.textButtonColors(contentColor = AppColors.error)
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
            TextButton(onClick = onDismiss) { Text("Done") }
        },
        title = {
            Text("Currency Display", style = AppTextStyles.cardTitle)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                AeonLayoutToggle(
                    layout = config.layout,
                    onLayoutChange = { newLayout -> callbacks.onLayoutChange(config.cardId, newLayout) }
                )
                AeonBaseConversionToggle(
                    baseCurrency = baseCurrency,
                    showBaseConversion = config.showBaseConversion,
                    onShowBaseConversionChange = { show -> callbacks.onShowBaseConversionChange(config.cardId, show) }
                )
            }
        },
        shape = RoundedCornerShape(32.dp),
        containerColor = AppColors.sheetSurface
    )
}

@Composable
fun CurrencyBalanceItem(
    curr: String,
    amount: BigDecimal,
    inBase: BigDecimal,
    baseCurrency: String,
    showBaseConversion: Boolean = true,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = AppColors.cardSurfaceProminent.copy(alpha = 0.4f),
        border = BorderStroke(0.5.dp, AppColors.divider.copy(alpha = 0.1f)),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                curr,
                style = AppTextStyles.transactionSwipeAction.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = AppColors.primary,
                    letterSpacing = 0.5.sp
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                formatCurrencyRight(amount, curr, shorten = true),
                style = AppTextStyles.screenSubTitle.copy(
                    fontFeatureSettings = "tnum",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                ),
                maxLines = 1
            )

            if (showBaseConversion && curr != baseCurrency) {
                Text(
                    "≈ ${formatCurrencyRight(inBase, baseCurrency, shorten = true)}",
                    style = AppTextStyles.transactionSwipeAction.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = AppColors.contentSecondary.copy(alpha = 0.6f),
                    maxLines = 1
                )
            } else if (showBaseConversion) {
                Spacer(modifier = Modifier.height(14.dp))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CurrencyBalancesCardPreview() {
    val items = listOf(
        CurrencyAggregationItem("USD", BigDecimal("15000000.00"), BigDecimal("1500.00")),
        CurrencyAggregationItem("EUR", BigDecimal("100000.00"), BigDecimal("1300.50")),
        CurrencyAggregationItem("GBP", BigDecimal("500.00"), BigDecimal("650.00"))
    )
    OssWalletTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            CurrencyBalancesCard(
                currencyTotals = items,
                baseCurrency = "USD",
                config = CurrencyBalancesCardConfig(cardId = "preview")
            )
        }
    }
}
