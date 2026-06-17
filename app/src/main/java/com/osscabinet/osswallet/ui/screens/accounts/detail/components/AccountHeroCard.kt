package com.osscabinet.ossWallet.ui.screens.accounts.detail.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import com.osscabinet.ossWallet.domain.card.CardFilterRules
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.AccountStructureType
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.isTransfer
import com.osscabinet.ossWallet.domain.model.analytics.AccountFlowMetrics
import com.osscabinet.ossWallet.domain.model.analytics.AccountSummaryItem
import com.osscabinet.ossWallet.domain.model.settings.AccountHeroCardConfig
import com.osscabinet.ossWallet.domain.model.settings.CashFlowDisplayMode
import com.osscabinet.ossWallet.domain.model.settings.UiIdentity
import com.osscabinet.ossWallet.ui.util.CurrencyDisplaySpec
import com.osscabinet.ossWallet.ui.util.CurrencyPartStyles
import com.osscabinet.ossWallet.ui.util.CurrencyReprUi
import com.osscabinet.ossWallet.ui.components.AppIconMap
import com.osscabinet.ossWallet.ui.components.cards.AeonExpressiveCard
import com.osscabinet.ossWallet.ui.components.cards.CardCallbacks
import com.osscabinet.ossWallet.ui.components.cards.CardSettingsSection
import com.osscabinet.ossWallet.ui.components.cards.CardSettingsSheet
import com.osscabinet.ossWallet.ui.components.cards.FilterSelectionContext
import com.osscabinet.ossWallet.ui.components.entry.EntrySwitchRow
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.LocalThemeMode
import com.osscabinet.ossWallet.ui.theme.LocalUiIdentity
import com.osscabinet.ossWallet.ui.util.applyAccountTint
import java.math.BigDecimal

@Composable
fun AccountHeroCard(
    summary: AccountSummaryItem,
    baseCurrency: String,
    config: AccountHeroCardConfig,
    modifier: Modifier = Modifier,
    headerModifier: Modifier = Modifier,
    filterRules: CardFilterRules? = null,
    baseFilter: TransactionFilter = TransactionFilter(),
    onFilterChange: (TransactionFilter) -> Unit = {},
    onSaveFilterClick: (TransactionFilter) -> Unit = {},
    filterContext: FilterSelectionContext = FilterSelectionContext(),
    inheritedFilter: TransactionFilter = TransactionFilter(),
    screenOwnsTimeline: Boolean = false,
    showFilterButton: Boolean = true,
    callbacks: CardCallbacks.AccountHero = CardCallbacks.AccountHero()
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
        if (isDarkBg) Color(0xFF7FBF8E) else Color(0xFF2C6E49)
    } else {
        if (isDarkBg) Color(0xFF4DCE85) else Color(0xFF1E9B4F)
    }

    val negColor = if (isH) {
        if (isDarkBg) Color(0xFFD88379) else Color(0xFF9B3B30)
    } else {
        if (isDarkBg) Color(0xFFE27974) else Color(0xFFC8463F)
    }

    val accColor = if (isH) {
        if (isDarkBg) Color(0xFFD6C189) else Color(0xFF8A6F3C)
    } else {
        if (isDarkBg) Color(0xFFDDE2FA) else Color(0xFF2F3A88)
    }

    val subColor = if (isDarkBg) contentColor.copy(alpha = 0.7f) else AppColors.contentSecondary

    val balanceInBase = summary.balanceInBase

    AeonExpressiveCard(
        title = account.name,
        subtitle = account.structureType.displayName,
        subtitleColor = subColor,
        icon = icon,
        accentColor = contentColor,
        containerColor = containerColor,
        contentColor = contentColor,
        backgroundIcon = icon,
        modifier = modifier,
        headerModifier = headerModifier,
        filterEnabled = showFilterButton,
        filter = config.filter,
        baseFilter = baseFilter,
        filterRules = filterRules ?: config.filterRules,
        onFilterChange = { onFilterChange(it) },
        onSaveFilterClick = onSaveFilterClick,
        filterContext = filterContext,
        displaySettings = { onDismiss ->
            AccountHeroSettingsSheet(config = config, callbacks = callbacks, onDismiss = onDismiss)
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Main Balance Section
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "TOTAL BALANCE",
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
                    color = contentColor
                )

                if (account.currencyCode != baseCurrency) {
                    Text(
                        text = CurrencyReprUi.formatCurrencyAnnotated(
                            amount = balanceInBase,
                            currencyCode = baseCurrency,
                            spec = CurrencyDisplaySpec(isApproximate = true, useSemanticColor = false)
                        ),
                        style = AppTextStyles.transactionTitle.copy(
                            fontWeight = FontWeight.Medium,
                            color = contentColor.copy(alpha = 0.7f)
                        )
                    )
                }
            }

            // Income/Expense Flow Column (Tabular)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val excludeSameCcy = config.excludeSameCurrencyTransfers
                val types = config.filter.transactionTypes
                val showIncome = types.isEmpty() || TransactionType.INCOME in types
                val showExpense = types.isEmpty() || TransactionType.EXPENSE in types
                val showTransfer = types.isEmpty() || types.any { it.isTransfer }

                val flow = summary.currentPeriodFlow
                val prevFlow = summary.previousPeriodFlow

                when (config.displayMode) {
                    CashFlowDisplayMode.TYPE_DISTINCTION -> {
                        if (showIncome) {
                            FlowSummaryItem(
                                label = "INCOME",
                                amount = flow.income,
                                currency = account.currencyCode,
                                icon = Icons.AutoMirrored.Rounded.TrendingUp,
                                color = posColor,
                                contentColor = contentColor,
                                previousAmount = prevFlow?.income,
                                onClick = {
                                    callbacks.onClickIncome?.invoke(
                                        config.filter.copy(
                                            transactionTypes = setOf(
                                                TransactionType.INCOME
                                            )
                                        )
                                    )
                                }
                            )
                        }
                        if (showExpense) {
                            FlowSummaryItem(
                                label = "EXPENSE",
                                amount = flow.expense,
                                currency = account.currencyCode,
                                icon = Icons.AutoMirrored.Rounded.TrendingDown,
                                color = negColor,
                                contentColor = contentColor,
                                previousAmount = prevFlow?.expense,
                                onClick = {
                                    callbacks.onClickExpense?.invoke(
                                        config.filter.copy(
                                            transactionTypes = setOf(
                                                TransactionType.EXPENSE
                                            )
                                        )
                                    )
                                }
                            )
                        }
                        if (showTransfer) {
                            val tx = if (excludeSameCcy) flow.netCrossCcyTransfer else flow.netTransfer
                            val prevTx = if (prevFlow != null) {
                                if (excludeSameCcy) prevFlow.netCrossCcyTransfer else prevFlow.netTransfer
                            } else null

                            FlowSummaryItem(
                                label = "TRANSFER",
                                amount = tx,
                                currency = account.currencyCode,
                                icon = Icons.Rounded.SwapHoriz,
                                color = accColor,
                                contentColor = contentColor,
                                previousAmount = prevTx,
                                forceShowPlus = true,
                                onClick = {
                                    callbacks.onClickTransfer?.invoke(
                                        config.filter.copy(
                                            transactionTypes = TransactionType.TRANSFER_TYPES
                                        )
                                    )
                                }
                            )
                        }
                    }

                    CashFlowDisplayMode.INFLOW_OUTFLOW -> {
                        val txIn = if (excludeSameCcy) flow.crossCcyTransferIn else flow.transferIn
                        val txOut = if (excludeSameCcy) flow.crossCcyTransferOut else flow.transferOut

                        val prevTxIn = if (excludeSameCcy) prevFlow?.crossCcyTransferIn else prevFlow?.transferIn
                        val prevTxOut = if (excludeSameCcy) prevFlow?.crossCcyTransferOut else prevFlow?.transferOut

                        val prevInflow = prevFlow?.let { (it.income) + (prevTxIn ?: BigDecimal.ZERO) }
                        val prevOutflow = prevFlow?.let { (it.expense) + (prevTxOut ?: BigDecimal.ZERO) }

                        FlowSummaryItem(
                            label = "INFLOW",
                            amount = (if (showIncome) flow.income else BigDecimal.ZERO) + (if (showTransfer) txIn else BigDecimal.ZERO),
                            currency = account.currencyCode,
                            icon = Icons.AutoMirrored.Rounded.TrendingUp,
                            color = posColor,
                            contentColor = contentColor,
                            previousAmount = prevInflow,
                            onClick = {
                                callbacks.onClickInflow?.invoke(
                                    config.filter.copy(
                                        transactionTypes = buildSet {
                                            if (showIncome) add(TransactionType.INCOME)
                                            if (showTransfer) add(TransactionType.TRANSFER_INFLOW)
                                        }
                                    )
                                )
                            }
                        )
                        FlowSummaryItem(
                            label = "OUTFLOW",
                            amount = (if (showExpense) flow.expense else BigDecimal.ZERO) + (if (showTransfer) txOut else BigDecimal.ZERO),
                            currency = account.currencyCode,
                            icon = Icons.AutoMirrored.Rounded.TrendingDown,
                            color = negColor,
                            contentColor = contentColor,
                            previousAmount = prevOutflow,
                            onClick = {
                                callbacks.onClickOutflow?.invoke(
                                    config.filter.copy(
                                        transactionTypes = buildSet {
                                            if (showExpense) add(TransactionType.EXPENSE)
                                            if (showTransfer) add(TransactionType.TRANSFER_OUTFLOW)
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
                        currencyBalances.toList().sortedBy { it.first }.forEach { (curr, bal) ->
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
                                            currencyCode = null,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountHeroSettingsSheet(
    config: AccountHeroCardConfig,
    callbacks: CardCallbacks.AccountHero,
    onDismiss: () -> Unit
) {
    CardSettingsSheet(title = "Account Hero", onDismiss = onDismiss, onDelete = {}) {
        CardSettingsSection("Display") {
            EntrySwitchRow(
                label = "Show Currency Badges",
                subtitle = "Show breakdown of multi-currency balances",
                checked = config.showCurrencyBadges,
                onCheckedChange = { callbacks.onShowCurrencyBadgesChange(config.cardId, it) },
                isFlush = true
            )
            EntrySwitchRow(
                label = "Show Comparison",
                subtitle = "Show change from previous period",
                checked = config.showComparison,
                onCheckedChange = { callbacks.onShowComparisonChange(config.cardId, it) },
                isFlush = true
            )
        }
        CardSettingsSection("Layout") {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                CashFlowDisplayMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = config.displayMode == mode,
                        onClick = { callbacks.onDisplayModeChange(config.cardId, mode) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = CashFlowDisplayMode.entries.size
                        ),
                        label = {
                            Text(
                                when (mode) {
                                    CashFlowDisplayMode.TYPE_DISTINCTION -> "Type"
                                    CashFlowDisplayMode.INFLOW_OUTFLOW -> "In / Out"
                                }
                            )
                        }
                    )
                }
            }
            if (config.displayMode == CashFlowDisplayMode.INFLOW_OUTFLOW) {
                com.osscabinet.ossWallet.ui.components.AeonSettingsToggle(
                    label = "Exclude same-currency transfers",
                    description = "Only used in Inflow / Outflow mode",
                    checked = config.excludeSameCurrencyTransfers,
                    onCheckedChange = {
                        callbacks.onExcludeSameCurrencyTransfersChange(config.cardId, it)
                    }
                )
            }
        }
    }
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
        color = contentColor.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, contentColor.copy(alpha = 0.12f)),
        modifier = modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color.copy(alpha = 0.9f),
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                style = AppTextStyles.overlineLabel.copy(
                    letterSpacing = 0.5.sp,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = contentColor.copy(alpha = 0.5f),
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = CurrencyReprUi.formatCurrencyAnnotated(
                    amount = amount,
                    currencyCode = currency,
                    spec = CurrencyDisplaySpec(
                        useSemanticColor = false,
                        forceShowPlus = forceShowPlus,
                        shortenThreshold = 20
                    )
                ),
                style = AppTextStyles.transactionAmount.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                ),
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

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
                        modifier = Modifier.widthIn(min = 44.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.End,
                        maxLines = 1
                    )
                } else {
                    Box(modifier = Modifier.width(44.dp))
                }
            }
        }
    }
}

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
    com.osscabinet.ossWallet.ui.theme.OssWalletTheme(theme = theme) {
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
