package com.osscabinet.ossWallet.ui.components.cards.types

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.CardRenderData
import com.osscabinet.ossWallet.domain.card.CardUpdateHandler
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.AccountStructureType
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.analytics.AccountFlowMetrics
import com.osscabinet.ossWallet.domain.model.analytics.AccountSummaryItem
import com.osscabinet.ossWallet.domain.model.settings.AccountHeroCardConfig
import com.osscabinet.ossWallet.domain.model.settings.AccountingPerspective
import com.osscabinet.ossWallet.domain.model.settings.UiIdentity
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.ui.components.AeonSettingsToggle
import com.osscabinet.ossWallet.ui.components.AppIconMap
import com.osscabinet.ossWallet.ui.components.cards.AeonExpressiveCard
import com.osscabinet.ossWallet.ui.components.cards.AeonInteractionCallbacks
import com.osscabinet.ossWallet.ui.components.cards.CardCallbacks
import com.osscabinet.ossWallet.ui.components.cards.CardLayoutProps
import com.osscabinet.ossWallet.ui.components.cards.CardPerspectiveSettings
import com.osscabinet.ossWallet.ui.components.cards.CardSettingsSection
import com.osscabinet.ossWallet.ui.components.cards.CardWithCallbacks
import com.osscabinet.ossWallet.ui.components.cards.resolveDrillDown
import com.osscabinet.ossWallet.ui.components.cards.resolveSeeAll
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.HNegativeDark
import com.osscabinet.ossWallet.ui.theme.HNegativeLight
import com.osscabinet.ossWallet.ui.theme.HPositiveDark
import com.osscabinet.ossWallet.ui.theme.HPositiveLight
import com.osscabinet.ossWallet.ui.theme.LocalThemeMode
import com.osscabinet.ossWallet.ui.theme.LocalUiIdentity
import com.osscabinet.ossWallet.ui.theme.NegativeAmountColor
import com.osscabinet.ossWallet.ui.theme.NegativeAmountDark
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.theme.PositiveAmountColor
import com.osscabinet.ossWallet.ui.theme.PositiveAmountDark
import com.osscabinet.ossWallet.ui.util.CurrencyDisplaySpec
import com.osscabinet.ossWallet.ui.util.CurrencyPartStyles
import com.osscabinet.ossWallet.ui.util.CurrencyReprUi
import com.osscabinet.ossWallet.ui.util.applyAccountTint
import com.osscabinet.ossWallet.ui.util.formatCurrency
import java.math.BigDecimal

@Composable
fun AccountHeroCard(
    summary: AccountSummaryItem,
    baseCurrency: String,
    config: AccountHeroCardConfig,
    callbacks: CardCallbacks.AccountHero = CardCallbacks.AccountHero(),
    layoutProps: CardLayoutProps = CardLayoutProps(),
) {
    val account = summary.account
    val accountColor = try {
        Color(account.colorHex.toColorInt())
    } catch (_: Exception) {
        AppColors.primary
    }

    val icon = AppIconMap[account.icon] ?: Icons.Rounded.AccountBalance

    val mode = LocalThemeMode.current
    val (containerColor, contentColor) = accountColor.applyAccountTint(mode)

    val isDarkBg = containerColor.luminance() < 0.5
    val isH = LocalUiIdentity.current == UiIdentity.H

    val posColor = if (isH) {
        if (isDarkBg) HPositiveDark else HPositiveLight
    } else {
        if (isDarkBg) PositiveAmountDark else PositiveAmountColor
    }

    val negColor = if (isH) {
        if (isDarkBg) HNegativeDark else HNegativeLight
    } else {
        if (isDarkBg) NegativeAmountDark else NegativeAmountColor
    }

    val subColor = if (isDarkBg) contentColor.copy(alpha = 0.7f) else AppColors.contentSecondary

    val balanceInBase = summary.balanceInBase

    val displayFilter = layoutProps.computeDisplayFilter(config.filter)

    AeonExpressiveCard(
        title = account.name,
        subtitle = account.structureType.displayName,
        subtitleColor = subColor,
        icon = icon,
        accentColor = contentColor,
        containerColor = containerColor,
        contentColor = contentColor,
        backgroundIcon = icon,
        modifier = layoutProps.modifier,
        headerModifier = layoutProps.headerModifier,
        filterEnabled = layoutProps.showFilterButton,
        filter = displayFilter,
        baseFilter = layoutProps.baseFilter,
        filterRules = layoutProps.filterRules ?: config.filterRules,
        filterContext = layoutProps.filterContext,
        onFilterChange = { callbacks.onFilterChange(config.cardId, it) },
        onSaveFilterClick = callbacks.onSaveFilterClick,
        onDelete = callbacks.onDelete,
        settingsContent = {
            AccountHeroSettingsContent(
                config = config,
                callbacks = callbacks,
            )
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Main Balance Section
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = if (config.perspective is AccountingPerspective.Accounting) "TOTAL BALANCE" else "NET CASH FLOW",
                    style = AppTextStyles.overlineLabel.copy(
                        letterSpacing = 1.5.sp
                    ),
                    color = contentColor.copy(alpha = 0.6f)
                )

                val symbolStyle = SpanStyle(
                    fontWeight = FontWeight.Light,
                    color = contentColor.copy(alpha = 0.9f),
                    fontSize = 24.sp
                )

                Text(
                    text = CurrencyReprUi.formatCurrencyAnnotated(
                        amount = summary.balance,
                        currencyCode = account.currencyCode,
                        spec = CurrencyDisplaySpec(useSemanticColor = true),
                        partStyles = CurrencyPartStyles(symbol = symbolStyle)
                    ),
                    style = AppTextStyles.heroAmountLarge.copy(
                        fontSize = 36.sp,
                        letterSpacing = (-1).sp
                    ),
                    color = contentColor,
                    modifier = Modifier.clickable {
                        if (config.perspective is AccountingPerspective.Accounting) {
                            callbacks.onClickNet?.invoke(config.filter)
                        } else {
                            callbacks.onClickNetFlow?.invoke(config.filter)
                        }
                    }
                )

                if (account.currencyCode != baseCurrency) {
                    Text(
                        text = CurrencyReprUi.formatCurrencyAnnotated(
                            amount = balanceInBase,
                            currencyCode = baseCurrency,
                            spec = CurrencyDisplaySpec(
                                isApproximate = true,
                                useSemanticColor = false
                            )
                        ),
                        style = AppTextStyles.transactionTitle.copy(
                            fontWeight = FontWeight.Medium,
                            color = contentColor.copy(alpha = 0.7f)
                        )
                    )
                }
            }

            // Income/Expense/Inflow/Outflow Grid
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val flow = summary.currentPeriodFlow
                val prevFlow = summary.previousPeriodFlow

                if (config.perspective is AccountingPerspective.Accounting) {
                    // Row 1: Income / Expense
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FlowSummaryItem(
                            label = "INCOME",
                            amount = flow.income,
                            currency = account.currencyCode,
                            icon = Icons.AutoMirrored.Rounded.TrendingUp,
                            color = posColor,
                            contentColor = contentColor,
                            previousAmount = prevFlow?.income,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                callbacks.onClickIncome?.invoke(
                                    config.filter.copy(transactionTypes = setOf(TransactionType.INCOME))
                                )
                            }
                        )
                        FlowSummaryItem(
                            label = "EXPENSE",
                            amount = flow.expense,
                            currency = account.currencyCode,
                            icon = Icons.AutoMirrored.Rounded.TrendingDown,
                            color = negColor,
                            contentColor = contentColor,
                            previousAmount = prevFlow?.expense,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                callbacks.onClickExpense?.invoke(
                                    config.filter.copy(transactionTypes = setOf(TransactionType.EXPENSE))
                                )
                            }
                        )
                    }

                    // Row 2: Transfer In / Transfer Out
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FlowSummaryItem(
                            label = "TRF IN",
                            amount = flow.transferIn,
                            currency = account.currencyCode,
                            icon = Icons.Rounded.SwapHoriz,
                            color = contentColor.copy(alpha = 0.9f),
                            contentColor = contentColor,
                            previousAmount = prevFlow?.transferIn,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                callbacks.onClickTransfer?.invoke(
                                    config.filter.copy(transactionTypes = setOf(TransactionType.TRANSFER_INFLOW))
                                )
                            }
                        )
                        FlowSummaryItem(
                            label = "TRF OUT",
                            amount = flow.transferOut,
                            currency = account.currencyCode,
                            icon = Icons.Rounded.SwapHoriz,
                            color = contentColor.copy(alpha = 0.9f),
                            contentColor = contentColor,
                            previousAmount = prevFlow?.transferOut,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                callbacks.onClickTransfer?.invoke(
                                    config.filter.copy(transactionTypes = setOf(TransactionType.TRANSFER_OUTFLOW))
                                )
                            }
                        )
                    }
                } else {
                    // Cash Flow Perspective: strictly Inflow and Outflow
                    val inflow = flow.income + flow.transferIn
                    val outflow = flow.expense + flow.transferOut
                    val prevInflow = prevFlow?.let { it.income + it.transferIn }
                    val prevOutflow = prevFlow?.let { it.expense + it.transferOut }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FlowSummaryItem(
                            label = "INFLOW",
                            amount = inflow,
                            currency = account.currencyCode,
                            icon = Icons.AutoMirrored.Rounded.TrendingUp,
                            color = posColor,
                            contentColor = contentColor,
                            previousAmount = prevInflow,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                callbacks.onClickInflow?.invoke(
                                    config.filter.copy(
                                        transactionTypes = buildSet {
                                            add(TransactionType.INCOME)
                                            add(TransactionType.TRANSFER_INFLOW)
                                        }
                                    )
                                )
                            }
                        )
                        FlowSummaryItem(
                            label = "OUTFLOW",
                            amount = outflow,
                            currency = account.currencyCode,
                            icon = Icons.AutoMirrored.Rounded.TrendingDown,
                            color = negColor,
                            contentColor = contentColor,
                            previousAmount = prevOutflow,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                callbacks.onClickOutflow?.invoke(
                                    config.filter.copy(
                                        transactionTypes = buildSet {
                                            add(TransactionType.EXPENSE)
                                            add(TransactionType.TRANSFER_OUTFLOW)
                                        }
                                    )
                                )
                            }
                        )
                    }
                }
            }

            // Multi-Currency Section
            val currencyBalances = summary.currencyBalances
            if (config.showCurrencyBadges && currencyBalances.isNotEmpty() && currencyBalances.size > 1) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "DISTRIBUTION",
                        style = AppTextStyles.overlineLabel.copy(
                            letterSpacing = 1.5.sp
                        ),
                        color = contentColor.copy(alpha = 0.8f)
                    )

                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        currencyBalances.entries.sortedBy { it.key }.forEach { (curr, bal) ->
                            Surface(
                                color = contentColor.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(0.5.dp, contentColor.copy(alpha = 0.18f)),
                                modifier = Modifier.clickable {
                                    callbacks.onClickCurrency?.invoke(
                                        curr
                                    )
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(
                                        horizontal = 10.dp,
                                        vertical = 8.dp
                                    ),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        curr,
                                        style = AppTextStyles.overlineLabel.copy(
                                            fontWeight = FontWeight.Black,
                                            fontSize = 10.sp
                                        ),
                                        color = contentColor
                                    )
                                    Text(
                                        text = CurrencyReprUi.formatCurrencyAnnotated(
                                            amount = bal,
                                            currencyCode = curr,
                                            spec = CurrencyDisplaySpec(useSemanticColor = false)
                                        ),
                                        style = AppTextStyles.transactionTitle.copy(
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = (-0.2).sp
                                        ),
                                        color = contentColor
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

@Composable
private fun AccountHeroSettingsContent(
    config: AccountHeroCardConfig,
    callbacks: CardCallbacks.AccountHero,
) {
    CardSettingsSection("Display") {
        AeonSettingsToggle(
            label = "Show Currency Badges",
            description = "Show breakdown of multi-currency balances",
            checked = config.showCurrencyBadges,
            onCheckedChange = { callbacks.onShowCurrencyBadgesChange(config.cardId, it) },
            showBackground = false
        )
        AeonSettingsToggle(
            label = "Show Comparison",
            description = "Show change from previous period",
            checked = config.showComparison,
            onCheckedChange = { callbacks.onShowComparisonChange(config.cardId, it) },
            showBackground = false
        )
    }
    CardPerspectiveSettings(
        perspective = config.perspective,
        onPerspectiveChange = { callbacks.onPerspectiveChange(config.cardId, it) },
        excludeInternalTransfers = config.excludeInternalTransfers,
        onExcludeInternalTransfersChange = {
            callbacks.onExcludeInternalTransfersChange(
                config.cardId,
                it
            )
        }
    )
}

// Badges sit directly on the full-bleed account color surface, so we use semi-transparent
// contentColor overlays — they lighten/darken the background just enough to read as a distinct chip.
@Composable
private fun FlowSummaryItem(
    label: String,
    amount: BigDecimal,
    currency: String,
    icon: ImageVector,
    color: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    previousAmount: BigDecimal? = null,
    forceShowPlus: Boolean = false,
    onClick: () -> Unit = {}
) {
    Surface(
        color = contentColor.copy(alpha = 0.14f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(0.5.dp, contentColor.copy(alpha = 0.18f)),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor.copy(alpha = 0.7f),
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = label.uppercase(),
                        style = AppTextStyles.overlineLabel.copy(
                            letterSpacing = 1.sp,
                            fontSize = 9.sp
                        ),
                        color = contentColor.copy(alpha = 0.6f)
                    )
                }

                if (previousAmount != null) {
                    val change = amount.subtract(previousAmount)
                    if (change.signum() != 0 && previousAmount.signum() != 0) {
                        val percentage = change.multiply(BigDecimal("100"))
                            .divide(previousAmount.abs(), 1, java.math.RoundingMode.HALF_UP)
                        val up = change.signum() > 0

                        val displayPercentage = if (percentage.abs() > BigDecimal("999")) {
                            "${if (percentage.signum() > 0) ">999" else "<-999"}%"
                        } else {
                            "${if (up) "+" else ""}$percentage%"
                        }

                        Text(
                            text = "${if (up) "▲" else "▼"} $displayPercentage",
                            style = AppTextStyles.metaLabel.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp
                            ),
                            color = color,
                            maxLines = 1
                        )
                    }
                }
            }
            Text(
                text = formatCurrency(
                    amount = amount,
                    currencyCode = currency,
                    spec = CurrencyDisplaySpec(
                        useSemanticColor = false,
                        forceShowPlus = forceShowPlus,
                        shortenThreshold = 13
                    )
                ),
                style = AppTextStyles.screenSubTitle.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp
                ),
                color = color
            )
        }
    }
}

/**
 * Maps [AccountHeroCardConfig] + [CardRenderData.AccountHero] to [CardWithCallbacks.AccountHero].
 */
fun mapAccountHero(
    config: AccountHeroCardConfig,
    renderData: CardRenderData.AccountHero,
    input: CardInput,
    updateHandler: CardUpdateHandler,
    onDelete: ((String) -> Unit)?,
    onRecordsClick: (TransactionFilter) -> Unit,
    callbacks: AeonInteractionCallbacks,
    filterSheetEnabled: Boolean
) = CardWithCallbacks.AccountHero(
    renderData = renderData,
    callbacks = CardCallbacks.AccountHero(
        onShowCurrencyBadgesChange = updateHandler::updateCardHeroShowCurrencyBadges,
        onShowComparisonChange = updateHandler::updateCardShowComparison,
        onPerspectiveChange = updateHandler::updateCardPerspective,
        onExcludeInternalTransfersChange = updateHandler::updateCardExcludeInternalTransfers,
        onFilterChange = updateHandler::updateCardHeroFilter,
        onClickIncome = { drillDown -> onRecordsClick(resolveDrillDown(input, config, drillDown)) },
        onClickExpense = { drillDown -> onRecordsClick(resolveDrillDown(input, config, drillDown)) },
        onClickTransfer = { drillDown -> onRecordsClick(resolveDrillDown(input, config, drillDown)) },
        onClickInflow = { drillDown -> onRecordsClick(resolveDrillDown(input, config, drillDown)) },
        onClickOutflow = { drillDown -> onRecordsClick(resolveDrillDown(input, config, drillDown)) },
        onClickNet = { drillDown -> onRecordsClick(resolveDrillDown(input, config, drillDown)) },
        onClickNetFlow = { drillDown -> onRecordsClick(resolveDrillDown(input, config, drillDown)) },
        onClickCurrency = { currency ->
            val drillDown = TransactionFilter(currencies = setOf(currency))
            onRecordsClick(resolveDrillDown(input, config, drillDown))
        },
        onSeeAll = { onRecordsClick(resolveSeeAll(input, config)) },
        onClick = { callbacks.accountActions.onClick(renderData.summary.account.id) },
        onDelete = onDelete?.let { { it(config.cardId) } },
        onSaveFilterClick = callbacks.filterActions.onSaveClick
    ),
    filterRules = config.filterRules,
    baseFilter = input.filter,
    filterSheetEnabled = filterSheetEnabled
)

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "Account Hero - Light")
@Composable
fun AccountHeroCardLightPreview() {
    AccountHeroCardPreviewContent("Light")
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "Account Hero - Dark")
@Composable
fun AccountHeroCardDarkPreview() {
    AccountHeroCardPreviewContent("Dark")
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "Account Hero - Amoled")
@Composable
fun AccountHeroCardAmoledPreview() {
    AccountHeroCardPreviewContent("Amoled")
}

@Composable
private fun AccountHeroCardPreviewContent(theme: String) {
    val account = Account(
        "1",
        "Wealth Fund",
        "USD",
        AccountStructureType.INVESTMENT,
        "#004D40",
        "AccountBalance",
        emptyList()
    )
    val summary = AccountSummaryItem(
        account = account,
        currencyBalances = mapOf(
            "USD" to BigDecimal("1800000.00"),
            "EUR" to BigDecimal("45000.00"),
            "GBP" to BigDecimal("12000.00")
        ),
        balance = BigDecimal("245800.50"),
        balanceInBase = BigDecimal("245800.50"),
        currentPeriodFlow = AccountFlowMetrics(
            income = BigDecimal("15000.00"),
            expense = BigDecimal("4200.00")
        )
    )
    OssWalletTheme(theme = theme) {
        Box(
            modifier = Modifier
                .background(AppColors.background)
                .padding(24.dp)
        ) {
            AccountHeroCard(
                summary = summary,
                baseCurrency = "USD",
                config = AccountHeroCardConfig(cardId = "preview")
            )
        }
    }
}
