package com.osscabinet.ossWallet.ui.components.cards.types

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.CardRenderData
import com.osscabinet.ossWallet.domain.card.CardUpdateHandler
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.analytics.CurrencyAggregationItem
import com.osscabinet.ossWallet.domain.model.settings.CurrencyBalancesCardConfig
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.service.currency.ExchangeRateSnapshot
import com.osscabinet.ossWallet.ui.components.AeonBaseConversionToggle
import com.osscabinet.ossWallet.ui.components.AeonGridColumnsSettings
import com.osscabinet.ossWallet.ui.components.AeonLayoutToggle
import com.osscabinet.ossWallet.ui.components.AeonLazyRow
import com.osscabinet.ossWallet.ui.components.NativeWithConvertedSubtext
import com.osscabinet.ossWallet.ui.components.aeonHairlineBorder
import com.osscabinet.ossWallet.ui.components.cards.AeonCardSettingsScaffold
import com.osscabinet.ossWallet.ui.components.cards.AeonExpressiveCard
import com.osscabinet.ossWallet.ui.components.cards.AeonInteractionCallbacks
import com.osscabinet.ossWallet.ui.components.cards.CardCallbacks
import com.osscabinet.ossWallet.ui.components.cards.CardLayoutProps
import com.osscabinet.ossWallet.ui.components.cards.CardSettingsSection
import com.osscabinet.ossWallet.ui.components.cards.CardWithCallbacks
import com.osscabinet.ossWallet.ui.components.cards.resolveDrillDown
import com.osscabinet.ossWallet.ui.components.cards.resolveSeeAll
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.util.CurrencyDisplaySpec
import com.osscabinet.ossWallet.ui.util.CurrencyReprUi
import java.math.BigDecimal

// TODO(card-upgrade): add a 'hide zero balances' toggle and sort options (balance / name / recent activity).
// See docs/guides/card-upgrade-roadmap.md
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CurrencyBalancesCard(
    currencyTotals: List<CurrencyAggregationItem>,
    baseCurrency: String,
    exchangeRates: ExchangeRateSnapshot = ExchangeRateSnapshot(
        anchor = baseCurrency,
        rates = emptyMap(),
    ),
    config: CurrencyBalancesCardConfig,
    callbacks: CardCallbacks.CurrencyBalances = CardCallbacks.CurrencyBalances(),
    layoutProps: CardLayoutProps = CardLayoutProps(),
) {
    val displayFilter = layoutProps.computeDisplayFilter(config.filter)
    val currentOnFilterChange = rememberUpdatedState(callbacks.onFilterChange)

    AeonExpressiveCard(
        title = config.title,
        icon = Icons.Rounded.Payments,
        modifier = layoutProps.modifier,
        headerModifier = layoutProps.headerModifier,
        onClick = callbacks.onSeeAll,
        filterEnabled = layoutProps.showFilterButton,
        filter = displayFilter,
        baseFilter = layoutProps.baseFilter,
        filterRules = layoutProps.filterRules ?: config.filterRules,
        filterContext = layoutProps.filterContext,
        onFilterChange = { currentOnFilterChange.value.invoke(config.cardId, it) },
        onSaveFilterClick = callbacks.onSaveFilterClick,
        onDelete = callbacks.onDelete,
        settingsContent = {
            CurrencyBalancesSettingsContent(
                config = config,
                baseCurrency = baseCurrency,
                callbacks = callbacks,
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
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                maxItemsInEachRow = config.gridColumns
            ) {
                currencyTotals.forEach { item ->
                    val isLarge = config.gridColumns == 1
                    CurrencyBalanceItem(
                        curr = item.currency,
                        amount = item.amount,
                        targetCurrency = baseCurrency,
                        exchangeRates = exchangeRates,
                        showBaseConversion = config.showBaseConversion,
                        isLarge = isLarge,
                        onClick = {
                            callbacks.onCurrencyClick?.invoke(
                                item.currency,
                                config.filter.timelineRange
                            )
                        },
                        modifier = if (isLarge) Modifier.fillMaxWidth() else Modifier.weight(
                            1f
                        )
                    )
                }

                if (config.gridColumns > 1 && (currencyTotals.size % config.gridColumns != 0)) {
                    repeat(config.gridColumns - (currencyTotals.size % config.gridColumns)) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        } else {
            AeonLazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 4.dp)
            ) {
                items(currencyTotals) { item ->
                    CurrencyBalanceItem(
                        curr = item.currency,
                        amount = item.amount,
                        targetCurrency = baseCurrency,
                        exchangeRates = exchangeRates,
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

@Composable
private fun CurrencyBalancesSettingsContent(
    config: CurrencyBalancesCardConfig,
    baseCurrency: String,
    callbacks: CardCallbacks.CurrencyBalances,
) {
    val currentOnLayoutChange = rememberUpdatedState(callbacks.onLayoutChange)
    val currentOnGridColumnsChange = rememberUpdatedState(callbacks.onGridColumnsChange)
    val currentOnShowBaseConversionChange = rememberUpdatedState(callbacks.onShowBaseConversionChange)

    CardSettingsSection("Display") {
        AeonLayoutToggle(
            layout = config.layout,
            onLayoutChange = { currentOnLayoutChange.value(config.cardId, it) },
            showBackground = false
        )
        if (config.layout == "grid") {
            AeonGridColumnsSettings(
                gridColumns = config.gridColumns,
                onGridColumnsChange = { currentOnGridColumnsChange.value(config.cardId, it) },
                showBackground = false
            )
        }
        AeonBaseConversionToggle(
            baseCurrency = baseCurrency,
            showBaseConversion = config.showBaseConversion,
            onShowBaseConversionChange = {
                currentOnShowBaseConversionChange.value(
                    config.cardId,
                    it
                )
            },
            showBackground = false
        )
    }
}

@Composable
fun CurrencyBalanceItem(
    modifier: Modifier = Modifier,
    curr: String,
    amount: BigDecimal,
    targetCurrency: String,
    exchangeRates: ExchangeRateSnapshot,
    showBaseConversion: Boolean = true,
    isLarge: Boolean = false,
    onClick: () -> Unit = {},
) {
    val style = if (isLarge) AppTextStyles.heroAmount else AppTextStyles.cardSecondaryAmount
    val verticalPadding = if (isLarge) 16.dp else 14.dp
    val shape = RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius)

    Box(
        modifier = modifier
            .clip(shape)
            .background(AppColors.cardSurfaceProminent)
            .aeonHairlineBorder(shape)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = verticalPadding)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                curr.uppercase(),
                style = AppTextStyles.sectionOverline,
                color = AppColors.accent,
                modifier = Modifier.fillMaxWidth()
            )

            if (showBaseConversion) {
                NativeWithConvertedSubtext(
                    amount = amount,
                    nativeCurrency = curr,
                    targetCurrency = targetCurrency,
                    rates = exchangeRates,
                    primaryStyle = style,
                    subtextStyle = AppTextStyles.metaLabel,
                    subtextColor = AppColors.contentSecondary.copy(alpha = 0.7f),
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text(
                    text = CurrencyReprUi.formatCurrencyAnnotated(
                        amount = amount,
                        currencyCode = curr,
                        spec = CurrencyDisplaySpec(useSemanticColor = false)
                    ),
                    style = style,
                    maxLines = 1
                )
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

/**
 * Maps [CurrencyBalancesCardConfig] + [CardRenderData.CurrencyBalances] to [CardWithCallbacks.CurrencyBalances].
 */
fun mapCurrencyBalances(
    config: CurrencyBalancesCardConfig,
    renderData: CardRenderData.CurrencyBalances,
    input: CardInput,
    updateHandler: CardUpdateHandler,
    onDelete: ((String) -> Unit)?,
    onRecordsClick: (TransactionFilter) -> Unit,
    callbacks: AeonInteractionCallbacks,
    filterSheetEnabled: Boolean
) = CardWithCallbacks.CurrencyBalances(
    renderData = renderData,
    callbacks = CardCallbacks.CurrencyBalances(
        onGridColumnsChange = updateHandler::updateCardCurrencyBalancesGridColumns,
        onLayoutChange = updateHandler::updateCardCurrencyBalancesLayout,
        onShowBaseConversionChange = updateHandler::updateCardCurrencyBalancesShowBaseConversion,
        onFilterChange = updateHandler::updateCardCurrencyBalancesFilter,
        onCurrencyClick = { currency, range ->
            val drillDown = TransactionFilter(currencies = setOf(currency), timelineRange = range)
            onRecordsClick(resolveDrillDown(input, config, drillDown))
        },
        onSeeAll = { onRecordsClick(resolveSeeAll(input, config)) },
        onDelete = onDelete?.let { { it(config.cardId) } },
        onSaveFilterClick = callbacks.filterActions.onSaveClick
    ),
    filterRules = config.filterRules,
    baseFilter = input.filter,
    filterSheetEnabled = filterSheetEnabled
)
