package com.osscabinet.ossWallet.ui.components.cards.types

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.CardRenderData
import com.osscabinet.ossWallet.domain.card.CardUpdateHandler
import com.osscabinet.ossWallet.domain.model.LoanSummary
import com.osscabinet.ossWallet.domain.model.LoanType
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.settings.LoansCardConfig
import com.osscabinet.ossWallet.domain.service.currency.ExchangeRateSnapshot
import com.osscabinet.ossWallet.domain.service.management.LoanService
import com.osscabinet.ossWallet.ui.components.ConvertedWithNativeSubtext
import com.osscabinet.ossWallet.ui.components.aeonDottedUnderline
import com.osscabinet.ossWallet.ui.components.cards.AeonCardSettingsScaffold
import com.osscabinet.ossWallet.ui.components.cards.AeonExpressiveCard
import com.osscabinet.ossWallet.ui.components.cards.AeonInteractionCallbacks
import com.osscabinet.ossWallet.ui.components.cards.CardCallbacks
import com.osscabinet.ossWallet.ui.components.cards.CardLayoutProps
import com.osscabinet.ossWallet.ui.components.cards.CardSettingsSection
import com.osscabinet.ossWallet.ui.components.cards.CardWithCallbacks
import com.osscabinet.ossWallet.ui.components.cards.resolveDrillDown
import com.osscabinet.ossWallet.ui.components.cards.resolveSeeAll
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.theme.WarningColor
import com.osscabinet.ossWallet.ui.util.CurrencyDisplaySpec
import com.osscabinet.ossWallet.ui.util.CurrencyReprUi
import java.math.BigDecimal
import java.math.RoundingMode

@Composable
fun LoansCard(
    items: List<LoanSummary>,
    targetCurrency: String = "USD",
    exchangeRates: ExchangeRateSnapshot = ExchangeRateSnapshot(
        anchor = targetCurrency,
        rates = emptyMap(),
    ),
    config: LoansCardConfig,
    callbacks: CardCallbacks.Loans,
    layoutProps: CardLayoutProps = CardLayoutProps(),
) {
    val displayFilter = layoutProps.computeDisplayFilter(config.filter)

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
        onFilterChange = { callbacks.onFilterChange(config.cardId, it) },
        onSaveFilterClick = callbacks.onSaveFilterClick,
        onDelete = callbacks.onDelete,
        settingsContent = {
            LoansSettingsContent(config = config, callbacks = callbacks)
        }
    ) {
        if (items.isEmpty()) {
            Text(
                text = "No loans yet",
                style = AppTextStyles.caption,
                color = AppColors.contentSecondary,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            Column(modifier = Modifier.fillMaxWidth()) {
                items.forEachIndexed { index, summary ->
                    LoanSummaryRow(
                        summary = summary,
                        targetCurrency = targetCurrency,
                        exchangeRates = exchangeRates,
                        showDivider = index < items.lastIndex,
                        onClick = { callbacks.onLoanClick(summary.loan.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun LoansSettingsContent(
    config: LoansCardConfig,
    callbacks: CardCallbacks.Loans,
) {
    CardSettingsSection("Show Type") {
        listOf("ALL", "LENT", "BORROWED").forEach { type ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    type.lowercase().replaceFirstChar { it.uppercase() },
                    style = AppTextStyles.transactionTitle
                )
                RadioButton(
                    selected = config.showType == type,
                    onClick = { callbacks.onUpdate(config.copy(showType = type)) }
                )
            }
        }
    }
}

@Composable
private fun LoanSummaryRow(
    summary: LoanSummary,
    targetCurrency: String,
    exchangeRates: ExchangeRateSnapshot,
    showDivider: Boolean,
    onClick: () -> Unit
) {
    val accentColor =
        if (summary.loan.loanType == LoanType.LENT) AppColors.accent else WarningColor

    val progress = if (summary.loan.principalAmount > BigDecimal.ZERO)
        (summary.totalPaid.divide(summary.loan.principalAmount, 4, RoundingMode.HALF_UP)).toFloat()
            .coerceIn(0f, 1f)
    else 0f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .then(
                if (showDivider) Modifier.aeonDottedUnderline(AppColors.outlineVariant)
                else Modifier
            )
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = summary.loan.name,
                    style = AppTextStyles.listItemTitle.copy(fontWeight = FontWeight.Medium),
                    color = AppColors.contentPrimary
                )
                Text(
                    text = if (summary.loan.loanType == LoanType.LENT) "LENT TO ${summary.loan.lenderOrBorrowerName}".uppercase()
                    else "BORROWED FROM ${summary.loan.lenderOrBorrowerName}".uppercase(),
                    style = AppTextStyles.metaLabel,
                    color = accentColor.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            ConvertedWithNativeSubtext(
                amount = summary.remainingBalance + summary.interestAccrued,
                nativeCurrency = summary.loan.currency,
                targetCurrency = targetCurrency,
                rates = exchangeRates,
                primaryStyle = AppTextStyles.transactionAmount.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 17.sp
                ),
                primaryColor = AppColors.contentPrimary,
                subtextStyle = AppTextStyles.metaLabel,
                subtextColor = AppColors.contentSecondary.copy(alpha = 0.7f),
                horizontalAlignment = Alignment.End
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(AppColors.outlineVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .background(accentColor)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val principalAmount = CurrencyReprUi.formatCurrencyAnnotated(
                amount = summary.loan.principalAmount,
                currencyCode = summary.loan.currency,
                spec = CurrencyDisplaySpec(useSemanticColor = false)
            )
            Text(
                text = buildAnnotatedString {
                    append("PRINCIPAL ")
                    append(principalAmount)
                },
                style = AppTextStyles.metaLabel,
                color = AppColors.contentSecondary.copy(alpha = 0.7f)
            )
            Text(
                text = "${(progress * 100).toInt()}% PAID",
                style = AppTextStyles.metaLabel.copy(fontWeight = FontWeight.Medium),
                color = accentColor
            )
        }
    }
}

/**
 * Maps [LoansCardConfig] + [CardRenderData.Loans] to [CardWithCallbacks.Loans].
 */
fun mapLoans(
    config: LoansCardConfig,
    renderData: CardRenderData.Loans,
    input: CardInput,
    updateHandler: CardUpdateHandler,
    onDelete: ((String) -> Unit)?,
    onRecordsClick: (TransactionFilter) -> Unit,
    callbacks: AeonInteractionCallbacks,
    filterSheetEnabled: Boolean
) = CardWithCallbacks.Loans(
    renderData = renderData,
    callbacks = CardCallbacks.Loans(
        onUpdate = { newConfig -> updateHandler.updateCardLoansConfig(config.cardId, newConfig) },
        onFilterChange = updateHandler::updateCardLoansFilter,
        onLoanClick = callbacks.loanActions.onClick,
        onRecordsClick = { drillDown ->
            onRecordsClick(
                resolveDrillDown(
                    input,
                    config,
                    drillDown
                )
            )
        },
        onSeeAll = {
            onRecordsClick(
                resolveSeeAll(
                    input,
                    config
                )
            )
        },
        onDelete = onDelete?.let { { it(config.cardId) } },
        onSaveFilterClick = callbacks.filterActions.onSaveClick
    ),
    filterRules = config.filterRules,
    baseFilter = input.filter,
    filterSheetEnabled = filterSheetEnabled
)

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun LoansCardPreview() {
    val loan = com.osscabinet.ossWallet.domain.model.Loan(
        id = 1L, name = "Car Loan", principalAmount = BigDecimal("15000.00"), currency = "USD",
        startDate = System.currentTimeMillis(), lenderOrBorrowerName = "Bank",
        loanType = LoanType.BORROWED, linkedAccountId = "1", profileId = 1
    )
    val loans = listOf(
        LoanSummary(
            loan = loan,
            remainingBalance = BigDecimal("12000.00"),
            totalPaid = BigDecimal("3000.00"),
            interestAccrued = BigDecimal("150.00")
        )
    )

    OssWalletTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            LoansCard(
                items = loans,
                config = LoansCardConfig(cardId = "preview", title = "Loans"),
                callbacks = CardCallbacks.Loans(onUpdate = {}, onLoanClick = {}, onDelete = {})
            )
        }
    }
}
