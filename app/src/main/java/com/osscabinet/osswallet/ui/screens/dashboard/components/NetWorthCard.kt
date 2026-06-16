package com.osscabinet.ossWallet.ui.screens.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.card.CardFilterRules
import com.osscabinet.ossWallet.domain.card.NetWorthMetrics
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.analytics.CashFlowResult
import com.osscabinet.ossWallet.domain.model.settings.NetWorthCardConfig
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.ui.util.CurrencyDisplaySpec
import com.osscabinet.ossWallet.ui.util.CurrencyPartStyles
import com.osscabinet.ossWallet.ui.util.CurrencyReprUi
import com.osscabinet.ossWallet.ui.components.cards.AeonExpressiveCard
import com.osscabinet.ossWallet.ui.components.cards.CardCallbacks
import com.osscabinet.ossWallet.ui.components.entry.EntrySwitchRow
import com.osscabinet.ossWallet.ui.components.cards.CardSettingsSection
import com.osscabinet.ossWallet.ui.components.cards.CardSettingsSheet
import com.osscabinet.ossWallet.ui.components.cards.FilterSelectionContext
import com.osscabinet.ossWallet.ui.components.AeonHazeProvider
import com.osscabinet.ossWallet.ui.components.aeonDashedTopRule
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.theme.ThemePreviews
import java.math.BigDecimal
import java.math.RoundingMode

@Composable
fun NetWorthCard(
    currentMetrics: NetWorthMetrics,
    baseCurrency: String,
    displayCurrency: String = baseCurrency,
    config: NetWorthCardConfig,
    previousMetrics: NetWorthMetrics? = null,
    callbacks: CardCallbacks.NetWorth = CardCallbacks.NetWorth(),
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
        if (screenOwnsTimeline || inheritedFilter.timelineRange !is TimelineRange.AllTime)
            config.filter.copy(timelineRange = inheritedFilter.timelineRange)
        else config.filter

    AeonExpressiveCard(
        title = config.title,
        icon = Icons.Rounded.AccountBalance,
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
            NetWorthSettingsSheet(config = config, callbacks = callbacks, onDismiss = onDismiss)
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)
        ) {
            val baseFontSize = when (config.fontSize) {
                "Small" -> 24.sp
                "Large" -> 32.sp
                "X-Large" -> 42.sp
                else -> 28.sp
            }

            Column(verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingXs)) {
                val contentColor = AppColors.contentPrimary

                val symbolStyle = SpanStyle(
                    fontWeight = FontWeight.Light,
                    color = contentColor.copy(alpha = 0.6f),
                    fontSize = (baseFontSize.value * 0.7f).sp
                )

                Text(
                    text = CurrencyReprUi.formatCurrencyAnnotated(
                        amount = currentMetrics.netWorth,
                        currencyCode = displayCurrency,
                        spec = CurrencyDisplaySpec(useSemanticColor = false),
                        partStyles = CurrencyPartStyles(symbol = symbolStyle)
                    ),
                    style = AppTextStyles.heroAmountLarge.copy(
                        fontSize = baseFontSize,
                        letterSpacing = (-1).sp
                    ),
                    color = contentColor
                )

                // Mono trend readout — "▲ +8.2%  VS PRIOR PERIOD"
                if (config.showComparison && previousMetrics != null) {
                    val change = currentMetrics.netWorth.subtract(previousMetrics.netWorth)
                    if (change.signum() != 0 && previousMetrics.netWorth.signum() != 0) {
                        val percentage = change.multiply(BigDecimal("100"))
                            .divide(previousMetrics.netWorth.abs(), 1, RoundingMode.HALF_UP)
                        val up = change.signum() > 0
                        val trendColor = if (up) AppColors.positive else AppColors.negative
                        Text(
                            text = buildAnnotatedString {
                                withStyle(SpanStyle(color = trendColor)) {
                                    append("${if (up) "▲" else "▼"} ${if (up) "+" else ""}$percentage%")
                                }
                                append("   ")
                                withStyle(SpanStyle(color = AppColors.contentSecondary.copy(alpha = 0.7f))) {
                                    append("VS PRIOR PERIOD")
                                }
                            },
                            style = AppTextStyles.metaLabel.copy(fontWeight = FontWeight.Medium)
                        )
                    }
                }
            }

            // Twin income / expense columns split by a vertical hairline.
            Row(
                horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingLg),
                modifier = Modifier
                    .fillMaxWidth()
                    .aeonDashedTopRule()
                    .padding(top = 14.dp)
                    .height(IntrinsicSize.Min)
            ) {
                NetWorthFlowColumn(
                    label = "INCOME",
                    amount = currentMetrics.cashFlow.totalIncomeInBase,
                    currency = displayCurrency,
                    color = AppColors.positive,
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            callbacks.onClickIncome?.invoke(
                                config.filter.copy(transactionTypes = setOf(TransactionType.INCOME))
                            )
                        },
                    previousAmount = if (config.showComparison) previousMetrics?.cashFlow?.totalIncomeInBase else null
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(AppColors.outlineVariant)
                )
                NetWorthFlowColumn(
                    label = "EXPENSE",
                    amount = currentMetrics.cashFlow.totalExpensesInBase,
                    currency = displayCurrency,
                    color = AppColors.negative,
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            callbacks.onClickExpense?.invoke(
                                config.filter.copy(transactionTypes = setOf(TransactionType.EXPENSE))
                            )
                        },
                    previousAmount = if (config.showComparison) previousMetrics?.cashFlow?.totalExpensesInBase else null
                )
            }
        }
    }
}

@Composable
private fun NetWorthFlowColumn(
    label: String,
    amount: BigDecimal,
    currency: String,
    color: Color,
    modifier: Modifier = Modifier,
    previousAmount: BigDecimal? = null
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingSm)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = label,
                style = AppTextStyles.sectionOverline,
                color = AppColors.contentSecondary.copy(alpha = 0.7f)
            )
            if (previousAmount != null) {
                val change = amount.subtract(previousAmount)
                if (change.signum() != 0 && previousAmount.signum() != 0) {
                    val percentage = change.multiply(BigDecimal("100"))
                        .divide(previousAmount.abs(), 1, RoundingMode.HALF_UP)
                    val up = change.signum() > 0
                    Text(
                        text = "${if (up) "▲" else "▼"} ${if (up) "+" else ""}$percentage%",
                        style = AppTextStyles.metaLabel.copy(fontWeight = FontWeight.Medium),
                        color = color
                    )
                }
            }
        }
        Text(
            text = CurrencyReprUi.formatCurrencyAnnotated(
                amount = amount,
                currencyCode = currency,
                spec = CurrencyDisplaySpec(useSemanticColor = false)
            ),
            style = AppTextStyles.transactionAmount.copy(
                fontWeight = FontWeight.Normal,
                fontSize = 19.sp
            ),
            color = color
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NetWorthSettingsSheet(
    config: NetWorthCardConfig,
    callbacks: CardCallbacks.NetWorth,
    onDismiss: () -> Unit
) {
    CardSettingsSheet(title = "Net Worth", onDismiss = onDismiss, onDelete = callbacks.onDelete) {
        CardSettingsSection("Font Size") {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                listOf("Small", "Medium", "Large", "X-Large").forEachIndexed { index, size ->
                    SegmentedButton(
                        selected = config.fontSize == size,
                        onClick = { callbacks.onFontSizeChange(config.cardId, size) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = 4),
                        label = {
                            Text(
                                when (size) {
                                    "Small" -> "S"
                                    "Medium" -> "M"
                                    "Large" -> "L"
                                    else -> "XL"
                                }
                            )
                        }
                    )
                }
            }
        }
        CardSettingsSection("Display") {
            EntrySwitchRow(
                label = "Show Comparison",
                subtitle = "Show change from previous period",
                checked = config.showComparison,
                onCheckedChange = { callbacks.onShowComparisonChange(config.cardId, it) },
                isFlush = true
            )
        }
    }
}

@ThemePreviews
@Composable
@Preview(heightDp = 360)
fun NetWorthCardGlassPreview() {
    val currentMetrics = NetWorthMetrics(
        netWorth = BigDecimal("152500.75"),
        cashFlow = CashFlowResult(
            totalIncomeInBase = BigDecimal("4500000.00"),
            totalExpensesInBase = BigDecimal("2350.50"),
            totalTransfersInBase = BigDecimal.ZERO,
            netInBase = BigDecimal("2149.50"),
            breakdown = emptyList(),
            filteredTransactions = emptyList()
        )
    )
    val previousMetrics = NetWorthMetrics(
        netWorth = BigDecimal("14100.25"),
        cashFlow = CashFlowResult(
            totalIncomeInBase = BigDecimal("4200.00"),
            totalExpensesInBase = BigDecimal("2100.00"),
            totalTransfersInBase = BigDecimal.ZERO,
            netInBase = BigDecimal("2100.00"),
            breakdown = emptyList(),
            filteredTransactions = emptyList()
        )
    )

    OssWalletTheme {
        AeonHazeProvider(drawContours = true) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(400.dp)
            ) {
                NetWorthCard(
                    currentMetrics = currentMetrics,
                    previousMetrics = previousMetrics,
                    baseCurrency = "USD",
                    config = NetWorthCardConfig(cardId = "net_worth", showComparison = true)
                )
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Light Mode", showBackground = true)
@Composable
fun NetWorthCardLightPreview() {
    OssWalletTheme(theme = "Light") {
        NetWorthCardPreviewContent()
    }
}

@androidx.compose.ui.tooling.preview.Preview(
    name = "Dark Mode",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun NetWorthCardDarkPreview() {
    OssWalletTheme(theme = "Dark") {
        NetWorthCardPreviewContent()
    }
}

@androidx.compose.ui.tooling.preview.Preview(
    name = "Amoled Mode",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun NetWorthCardAmoledPreview() {
    OssWalletTheme(theme = "Amoled") {
        NetWorthCardPreviewContent()
    }
}

@Composable
private fun NetWorthCardPreviewContent() {
    val currentMetrics = NetWorthMetrics(
        netWorth = BigDecimal("15250.75"),
        cashFlow = CashFlowResult(
            totalIncomeInBase = BigDecimal("4500.00"),
            totalExpensesInBase = BigDecimal("2350.50"),
            totalTransfersInBase = BigDecimal.ZERO,
            netInBase = BigDecimal("2149.50"),
            breakdown = emptyList(),
            filteredTransactions = emptyList()
        )
    )
    val previousMetrics = NetWorthMetrics(
        netWorth = BigDecimal("14100.25"),
        cashFlow = CashFlowResult(
            totalIncomeInBase = BigDecimal("4200.00"),
            totalExpensesInBase = BigDecimal("2100.00"),
            totalTransfersInBase = BigDecimal.ZERO,
            netInBase = BigDecimal("2100.00"),
            breakdown = emptyList(),
            filteredTransactions = emptyList()
        )
    )

    Box(modifier = Modifier.padding(16.dp)) {
        NetWorthCard(
            currentMetrics = currentMetrics,
            previousMetrics = previousMetrics,
            baseCurrency = "USD",
            config = NetWorthCardConfig(cardId = "net_worth", showComparison = true)
        )
    }
}
