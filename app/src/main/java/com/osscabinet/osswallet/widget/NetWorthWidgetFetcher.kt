package com.osscabinet.ossWallet.widget

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.isTransfer
import com.osscabinet.ossWallet.domain.model.settings.AccountingPerspective
import com.osscabinet.ossWallet.domain.model.settings.NetWorthCardConfig
import com.osscabinet.ossWallet.domain.service.aggregation.CardAggregationService
import com.osscabinet.ossWallet.domain.service.currency.ExchangeRateSnapshot
import com.osscabinet.ossWallet.ui.util.CurrencyReprUi
import com.osscabinet.ossWallet.widget.WidgetDataLoader.CommonData
import java.math.BigDecimal

class NetWorthWidgetFetcher : WidgetDataFetcher {
    override suspend fun updateState(
        context: Context,
        widgetId: Int,
        common: CommonData,
        prefs: MutablePreferences
    ) {
        val config = NetWorthWidgetPrefsStore(context).loadSafe(widgetId)

        val renderData = runCatching {
            CardAggregationService.computeNetWorthCard(
                queryService = common.databaseQueryService,
                profileId = common.profileId,
                input = CardInput(
                    profileId = common.profileId,
                    targetCurrency = common.baseCurrency,
                    exchangeRates = ExchangeRateSnapshot(
                        anchor = common.baseCurrency,
                        rates = common.exchangeRates
                    ),
                    filter = config.filter.copy(
                        timelineRange = config.filter.timelineRange.withExcludeFuture(true)
                    ),
                    accounts = emptyList(),
                    categories = emptyList()
                ),
                config = NetWorthCardConfig(
                    cardId = "nw_widget_$widgetId",
                    fontSize = config.fontSize,
                    showComparison = config.showComparison,
                    perspective = config.perspective,
                    excludeInternalTransfers = config.excludeInternalTransfers,
                    splitTransfers = config.splitTransfers,
                    filter = config.filter
                )
            )
        }.getOrNull()

        val netWorth = renderData?.currentPeriodMetrics?.netWorth ?: BigDecimal.ZERO
        val cashFlow = renderData?.currentPeriodMetrics?.cashFlow
        val prev = renderData?.previousPeriodMetrics
        val prevMetrics = prev?.cashFlow?.metricsInBase
        val metricsInBase = cashFlow?.metricsInBase

        val formatter = CurrencyReprUi.CurrencyNumberFormatter()
        fun fmt(v: BigDecimal?) = formatter.format(v ?: BigDecimal.ZERO, common.baseCurrency)

        // NetWorthCard.kt's two perspective branches apply showIncome/showExpense/showTransfer
        // DIFFERENTLY — this is the bit worth being precise about:
        //  - Accounting branch: each column is wrapped in `if (showIncome) { ... }` etc., so
        //    an irrelevant column is never composed at all — a real hide.
        //  - CashFlow branch: "IN"/"OUT" are ALWAYS composed; showInflow/showOutflow only force
        //    the amount to BigDecimal.ZERO (and null the comparison) when irrelevant. The
        //    column itself never disappears. Net flow has no hide condition in either branch.
        val types = config.filter.transactionTypes
        val showIncome = types.isEmpty() || TransactionType.INCOME in types
        val showExpense = types.isEmpty() || TransactionType.EXPENSE in types
        val showTransfer = types.isEmpty() || types.any { it.isTransfer }
        val showInflow = showIncome || showTransfer
        val showOutflow = showExpense || showTransfer

        val isCashFlow = config.perspective is AccountingPerspective.CashFlow

        prefs[NetWorthWidgetKeys.IS_LOADED] = "true"
        prefs[NetWorthWidgetKeys.NET_WORTH] = fmt(netWorth)
        prefs[NetWorthWidgetKeys.NET_WORTH_COMPARISON] =
            buildComparisonText(netWorth, prev?.netWorth, config.showComparison) ?: ""
        prefs[NetWorthWidgetKeys.PERIOD_LABEL] = config.timelineLabel
        prefs[NetWorthWidgetKeys.FONT_SIZE] = config.fontSize
        prefs[NetWorthWidgetKeys.BASE_CURRENCY] = common.baseCurrency
        prefs[NetWorthWidgetKeys.PERSPECTIVE_LABEL] = if (isCashFlow) "Cash Flow" else "Accounting"

        // Unified 3-cell model, shared by strip (cell1/cell2 only)/card/large:
        //   cell1 = Inflow (CashFlow) or Income (Accounting)
        //   cell2 = Outflow (CashFlow) or Expense (Accounting)
        //   cell3 = Net flow (CashFlow, highlighted) or Transfer (Accounting, plain)
        // CashFlow's cell1/cell2 never hide (zeroed instead, per the branch comment above);
        // Accounting's three cells each hide when their type is excluded by the filter.
        if (isCashFlow) {
            val inflowValue = if (showInflow) metricsInBase?.inflow ?: BigDecimal.ZERO else BigDecimal.ZERO
            val outflowValue = if (showOutflow) metricsInBase?.outflow ?: BigDecimal.ZERO else BigDecimal.ZERO
            val netFlowValue = metricsInBase?.netFlow ?: BigDecimal.ZERO

            writeCell(
                prefs, CellKeys.CELL1, "Inflow", fmt(inflowValue),
                if (showInflow) buildComparisonText(inflowValue, prevMetrics?.inflow, config.showComparison) else null,
                fmt(prevMetrics?.inflow?.abs()), visible = true
            )
            writeCell(
                prefs, CellKeys.CELL2, "Outflow", fmt(outflowValue),
                if (showOutflow) buildComparisonText(outflowValue, prevMetrics?.outflow, config.showComparison) else null,
                fmt(prevMetrics?.outflow?.abs()), visible = true
            )
            writeCell(
                prefs, CellKeys.CELL3, "Net flow", fmt(netFlowValue),
                buildComparisonText(netFlowValue, prevMetrics?.netFlow, config.showComparison),
                fmt(prevMetrics?.netFlow?.abs()), visible = true
            )
            prefs[NetWorthWidgetKeys.CELL3_HIGHLIGHT] = "true"
        } else {
            val incomeValue = metricsInBase?.income ?: BigDecimal.ZERO
            val expenseValueSigned = metricsInBase?.expense ?: BigDecimal.ZERO
            val transferValue = metricsInBase?.transfer ?: BigDecimal.ZERO

            writeCell(
                prefs, CellKeys.CELL1, "Income", fmt(incomeValue),
                if (showIncome) buildComparisonText(incomeValue, prevMetrics?.income, config.showComparison) else null,
                fmt(prevMetrics?.income?.abs()), visible = showIncome
            )
            writeCell(
                prefs, CellKeys.CELL2, "Expense", fmt(expenseValueSigned.abs()),
                if (showExpense) buildComparisonText(expenseValueSigned, prevMetrics?.expense, config.showComparison) else null,
                fmt(prevMetrics?.expense?.abs()), visible = showExpense
            )
            writeCell(
                prefs, CellKeys.CELL3, "Transfer", fmt(transferValue),
                if (showTransfer) buildComparisonText(transferValue, prevMetrics?.transfer, config.showComparison) else null,
                fmt(prevMetrics?.transfer?.abs()), visible = showTransfer
            )
            prefs[NetWorthWidgetKeys.CELL3_HIGHLIGHT] = "false"
        }
    }

    private enum class CellKeys { CELL1, CELL2, CELL3 }

    private fun writeCell(
        prefs: MutablePreferences,
        cell: CellKeys,
        label: String,
        value: String,
        comparison: String?,
        vs: String,
        visible: Boolean
    ) {
        val (labelKey, valueKey, cmpKey, vsKey, visibleKey) = when (cell) {
            CellKeys.CELL1 -> NetWorthWidgetKeys.run { listOf(CELL1_LABEL, CELL1_VALUE, CELL1_CMP, CELL1_VS, CELL1_VISIBLE) }
            CellKeys.CELL2 -> NetWorthWidgetKeys.run { listOf(CELL2_LABEL, CELL2_VALUE, CELL2_CMP, CELL2_VS, CELL2_VISIBLE) }
            CellKeys.CELL3 -> NetWorthWidgetKeys.run { listOf(CELL3_LABEL, CELL3_VALUE, CELL3_CMP, CELL3_VS, CELL3_VISIBLE) }
        }
        prefs[labelKey] = label
        prefs[valueKey] = value
        prefs[cmpKey] = comparison ?: ""
        prefs[vsKey] = vs
        prefs[visibleKey] = visible.toString()
    }
}
