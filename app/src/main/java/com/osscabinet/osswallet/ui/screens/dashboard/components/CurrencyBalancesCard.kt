package com.osscabinet.ossWallet.ui.screens.dashboard.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.card.CardFilterRules
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.analytics.CurrencyAggregationItem
import com.osscabinet.ossWallet.domain.model.settings.CurrencyBalancesCardConfig
import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter.formatAuto
import com.osscabinet.ossWallet.ui.components.AeonBaseConversionToggle
import com.osscabinet.ossWallet.ui.components.AeonLayoutToggle
import com.osscabinet.ossWallet.ui.components.cards.AeonExpressiveCard
import com.osscabinet.ossWallet.ui.components.cards.CardCallbacks
import com.osscabinet.ossWallet.ui.components.cards.CardSettingsSection
import com.osscabinet.ossWallet.ui.components.cards.CardSettingsSheet
import com.osscabinet.ossWallet.ui.components.cards.FilterSelectionContext
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import java.math.BigDecimal

@Composable
fun CurrencyBalancesCard(
    currencyTotals: List<CurrencyAggregationItem>,
    baseCurrency: String,
    config: CurrencyBalancesCardConfig,
    callbacks: CardCallbacks.CurrencyBalances = CardCallbacks.CurrencyBalances(),
    filterContext: FilterSelectionContext = FilterSelectionContext(),
    baseFilter: TransactionFilter = TransactionFilter(),
    inheritedFilter: TransactionFilter = TransactionFilter(),
    filterRules: CardFilterRules? = null,
    screenOwnsTimeline: Boolean = false,
    showFilterButton: Boolean = true,
    modifier: Modifier = Modifier,
    headerModifier: Modifier = Modifier,
) {
    val displayFilter =
        if (screenOwnsTimeline || inheritedFilter.timelineRange !is com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.AllTime)
            config.filter.copy(timelineRange = inheritedFilter.timelineRange)
        else config.filter

    AeonExpressiveCard(
        title = config.title,
        icon = Icons.Rounded.Payments,
        modifier = modifier,
        headerModifier = headerModifier,
        onClick = callbacks.onSeeAll,
        filterEnabled = showFilterButton,
        filter = displayFilter,
        baseFilter = baseFilter,
        filterRules = filterRules ?: config.filterRules,
        filterContext = filterContext,
        onFilterChange = { callbacks.onFilterChange(config.cardId, it) },
        onSaveFilterClick = callbacks.onSaveFilterClick,
        displaySettings = { onDismiss ->
            CurrencyBalancesSettingsSheet(
                config = config,
                baseCurrency = baseCurrency,
                callbacks = callbacks,
                onDismiss = onDismiss
            )
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
                                onClick = {
                                    callbacks.onCurrencyClick?.invoke(
                                        item.currency,
                                        config.filter.timelineRange
                                    )
                                },
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
                        onClick = {
                            callbacks.onCurrencyClick?.invoke(
                                item.currency,
                                config.filter.timelineRange
                            )
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrencyBalancesSettingsSheet(
    config: CurrencyBalancesCardConfig,
    baseCurrency: String,
    callbacks: CardCallbacks.CurrencyBalances,
    onDismiss: () -> Unit
) {
    CardSettingsSheet(
        title = "Currency Balances",
        onDismiss = onDismiss,
        onDelete = callbacks.onDelete
    ) {
        CardSettingsSection("Display") {
            AeonLayoutToggle(
                layout = config.layout,
                onLayoutChange = { callbacks.onLayoutChange(config.cardId, it) }
            )
            AeonBaseConversionToggle(
                baseCurrency = baseCurrency,
                showBaseConversion = config.showBaseConversion,
                onShowBaseConversionChange = {
                    callbacks.onShowBaseConversionChange(
                        config.cardId,
                        it
                    )
                }
            )
        }
    }
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
                formatAuto(amount, curr, shorten = true),
                style = AppTextStyles.screenSubTitle.copy(
                    fontFeatureSettings = "tnum",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                ),
                maxLines = 1
            )

            if (showBaseConversion && curr != baseCurrency) {
                Text(
                    "≈ ${formatAuto(inBase, baseCurrency, shorten = true)}",
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

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun CurrencyBalancesCardPreview() {
    val items = listOf(
        CurrencyAggregationItem("USD", BigDecimal("5000.00"), BigDecimal("5000.00")),
        CurrencyAggregationItem("EUR", BigDecimal("1200.00"), BigDecimal("1320.00")),
        CurrencyAggregationItem("GBP", BigDecimal("800.00"), BigDecimal("1040.00"))
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
