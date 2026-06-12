package com.osscabinet.ossWallet.ui.screens.dashboard.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.card.CardFilterRules
import com.osscabinet.ossWallet.domain.model.CashFlowType
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.analytics.CashFlowBreakdownItem
import com.osscabinet.ossWallet.domain.model.analytics.CashFlowResult
import com.osscabinet.ossWallet.domain.model.settings.CashFlowCardConfig
import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter.formatAuto
import com.osscabinet.ossWallet.ui.components.AeonBaseConversionToggle
import com.osscabinet.ossWallet.ui.components.AeonFlowBadge
import com.osscabinet.ossWallet.ui.components.cards.AeonExpressiveCard
import com.osscabinet.ossWallet.ui.components.cards.CardCallbacks
import com.osscabinet.ossWallet.ui.components.cards.CardSettingsSection
import com.osscabinet.ossWallet.ui.components.cards.CardSettingsSheet
import com.osscabinet.ossWallet.ui.components.cards.FilterSelectionContext
import com.osscabinet.ossWallet.ui.components.GlassSurface
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.GlassTokens
import com.osscabinet.ossWallet.ui.theme.GlassVariant
import com.osscabinet.ossWallet.ui.theme.LocalThemeMode
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.theme.copyStyle
import com.osscabinet.ossWallet.ui.util.applyFlowTint
import java.math.BigDecimal

@Composable
fun CashFlowCard(
    cashFlowResult: CashFlowResult?,
    baseCurrency: String,
    config: CashFlowCardConfig,
    callbacks: CardCallbacks.CashFlow = CardCallbacks.CashFlow(),
    filterContext: FilterSelectionContext = FilterSelectionContext(),
    baseFilter: TransactionFilter = TransactionFilter(),
    inheritedFilter: TransactionFilter = TransactionFilter(),
    filterRules: CardFilterRules? = null,
    screenOwnsTimeline: Boolean = false,
    showFilterButton: Boolean = true,
    modifier: Modifier = Modifier,
    headerModifier: Modifier = Modifier,
) {
    val totalIncomeInBase = cashFlowResult?.totalIncomeInBase ?: BigDecimal.ZERO
    val totalExpenseInBase = cashFlowResult?.totalExpensesInBase ?: BigDecimal.ZERO

    val displayFilter =
        if (screenOwnsTimeline || inheritedFilter.timelineRange !is com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.AllTime)
            config.filter.copy(timelineRange = inheritedFilter.timelineRange)
        else config.filter

    AeonExpressiveCard(
        title = config.title,
        icon = Icons.Rounded.Analytics,
        modifier = modifier,
        headerModifier = headerModifier,
        filterEnabled = showFilterButton,
        filter = displayFilter,
        baseFilter = baseFilter,
        filterRules = filterRules ?: config.filterRules,
        filterContext = filterContext,
        onFilterChange = { callbacks.onFilterChange(config.cardId, it) },
        onSaveFilterClick = callbacks.onSaveFilterClick,
        displaySettings = { onDismiss ->
            CashFlowSettingsSheet(
                config = config,
                baseCurrency = baseCurrency,
                callbacks = callbacks,
                onDismiss = onDismiss
            )
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AeonFlowCard(
                label = "Income",
                totalInBase = totalIncomeInBase,
                baseCurrency = baseCurrency,
                type = CashFlowType.INCOME,
                incomeColor = AppColors.positive,
                modifier = Modifier.weight(1f),
                onClick = {
                    callbacks.onClickIncome?.invoke(
                        config.filter.copy(
                            transactionTypes = setOf(
                                TransactionType.INCOME
                            )
                        )
                    )
                }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    cashFlowResult?.breakdown?.forEach { item ->
                        AeonFlowBreakdownItem(
                            curr = item.currency,
                            amount = item.income,
                            inBase = item.incomeInBase,
                            baseCurrency = baseCurrency,
                            showBaseConversion = config.showBaseConversion,
                            onClick = {
                                callbacks.onClickIncome?.invoke(
                                    config.filter.copy(
                                        transactionTypes = setOf(TransactionType.INCOME),
                                        currencies = setOf(item.currency)
                                    )
                                )
                            }
                        )
                    }
                }
            }

            AeonFlowCard(
                label = "Expense",
                totalInBase = totalExpenseInBase,
                baseCurrency = baseCurrency,
                type = CashFlowType.EXPENSE,
                expenseColor = AppColors.negative,
                modifier = Modifier.weight(1f),
                onClick = {
                    callbacks.onClickExpense?.invoke(
                        config.filter.copy(
                            transactionTypes = setOf(
                                TransactionType.EXPENSE
                            )
                        )
                    )
                }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    cashFlowResult?.breakdown?.forEach { item ->
                        AeonFlowBreakdownItem(
                            curr = item.currency,
                            amount = item.expense,
                            inBase = item.expenseInBase,
                            baseCurrency = baseCurrency,
                            showBaseConversion = config.showBaseConversion,
                            onClick = {
                                callbacks.onClickExpense?.invoke(
                                    config.filter.copy(
                                        transactionTypes = setOf(TransactionType.EXPENSE),
                                        currencies = setOf(item.currency)
                                    )
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CashFlowSettingsSheet(
    config: CashFlowCardConfig,
    baseCurrency: String,
    callbacks: CardCallbacks.CashFlow,
    onDismiss: () -> Unit
) {
    CardSettingsSheet(title = "Cash Flow", onDismiss = onDismiss, onDelete = callbacks.onDelete) {
        CardSettingsSection("Display") {
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
internal fun AeonFlowBreakdownItem(
    curr: String,
    amount: BigDecimal,
    inBase: BigDecimal,
    baseCurrency: String,
    showBaseConversion: Boolean = true,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            curr,
            style = AppTextStyles.overlineLabel.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Black
            ),
            color = LocalContentColor.current.copy(alpha = 0.6f)
        )
        Column(horizontalAlignment = Alignment.End) {
            Text(
                formatAuto(amount, curr),
                style = AppTextStyles.listItemTitle.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                ),
                color = LocalContentColor.current
            )
            if (showBaseConversion && curr != baseCurrency) {
                Text(
                    "≈ ${formatAuto(inBase, baseCurrency)}",
                    style = AppTextStyles.metaLabel.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = LocalContentColor.current.copy(alpha = 0.55f)
                )
            }
        }
    }
}

@Composable
fun AeonFlowCard(
    label: String,
    totalInBase: BigDecimal,
    baseCurrency: String,
    type: CashFlowType,
    modifier: Modifier = Modifier,
    incomeColor: Color = AppColors.positive,
    expenseColor: Color = AppColors.negative,
    onClick: () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    val color = if (type == CashFlowType.INCOME) incomeColor else expenseColor
    val mode = LocalThemeMode.current
    val (containerColor, contentColor) = color.applyFlowTint(mode)
    val isDark = mode.isDarkTheme

    GlassSurface(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(AeonTheme.dimensions.containerCornerRadius),
        glassVariant = GlassVariant.Item,
        hazeStyle = GlassTokens.itemStyle(isDark).copyStyle(
            bgColor = containerColor
        ),
        fallbackColor = containerColor,
        borderColor = contentColor.copy(alpha = 0.1f),
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            Column(
                modifier = Modifier.padding(AeonTheme.dimensions.cardPadding),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        label.uppercase(),
                        style = AppTextStyles.sectionOverline,
                        color = contentColor.copy(alpha = 0.8f)
                    )
                    AeonFlowBadge(
                        totalInBase,
                        type,
                        baseCurrency,
                        incomeColor = color,
                        expenseColor = color
                    )
                }

                content()
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Light Mode", showBackground = true)
@Composable
fun CashFlowCardLightPreview() {
    OssWalletTheme(theme = "Light") {
        CashFlowCardPreviewContent()
    }
}

@androidx.compose.ui.tooling.preview.Preview(
    name = "Dark Mode",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun CashFlowCardDarkPreview() {
    OssWalletTheme(theme = "Dark") {
        CashFlowCardPreviewContent()
    }
}

@androidx.compose.ui.tooling.preview.Preview(
    name = "Amoled Mode",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun CashFlowCardAmoledPreview() {
    OssWalletTheme(theme = "Amoled") {
        CashFlowCardPreviewContent()
    }
}

@Composable
private fun CashFlowCardPreviewContent() {
    val sampleBreakdown = listOf(
        CashFlowBreakdownItem(
            "USD",
            BigDecimal("5000.00"),
            BigDecimal("5000.00"),
            BigDecimal("3500.00"),
            BigDecimal("3500.00"),
            BigDecimal.ZERO,
            BigDecimal.ZERO
        ),
        CashFlowBreakdownItem(
            "EUR",
            BigDecimal("1000.00"),
            BigDecimal("1100.00"),
            BigDecimal("800.00"),
            BigDecimal("880.00"),
            BigDecimal.ZERO,
            BigDecimal.ZERO
        )
    )
    val result = CashFlowResult(
        totalIncomeInBase = BigDecimal("6100.00"),
        totalExpensesInBase = BigDecimal("4380.00"),
        totalTransfersInBase = BigDecimal.ZERO,
        netInBase = BigDecimal("1720.00"),
        breakdown = sampleBreakdown,
        filteredTransactions = emptyList()
    )

    Box(modifier = Modifier.padding(16.dp)) {
        CashFlowCard(
            cashFlowResult = result,
            baseCurrency = "USD",
            config = CashFlowCardConfig(cardId = "preview")
        )
    }
}
