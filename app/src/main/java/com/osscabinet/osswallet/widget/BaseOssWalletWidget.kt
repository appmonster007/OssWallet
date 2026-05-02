package com.osscabinet.ossWallet.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import com.osscabinet.ossWallet.domain.card.*
import com.osscabinet.ossWallet.domain.model.settings.*
import com.osscabinet.ossWallet.domain.service.PlannedPaymentService

abstract class BaseOssWalletWidget : GlanceAppWidget() {

    abstract val defaultConfig: WidgetConfig

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val configStore = WidgetConfigStore(context)
        val config = (configStore.getConfig(id.hashCode()) ?: defaultConfig).withInstanceCardId(id.hashCode())
        val data = WidgetDataLoader.load(context)

        val input = CardInput(
            transactions = data.transactions,
            accounts = data.accounts,
            categories = data.categories,
            exchangeRates = data.exchangeRates,
            baseCurrency = data.baseCurrency,
            precision = data.precision,
            plannedPayments = data.plannedPayments,
            budgets = data.budgets,
            loans = data.loans,
            loanPayments = data.loanPayments
        )

        val renderData = computeRenderData(input, config)
        renderContent(context, id, data, renderData, config)
    }

    private fun computeRenderData(input: CardInput, config: WidgetConfig): CardRenderData? {
        val registry = CardComputerRegistry.DEFAULT
        return when (config) {
            is WidgetConfig.NetWorth -> registry.compute(CardType.NET_WORTH, input, config.cardConfig)
            is WidgetConfig.CashFlow -> registry.compute(CardType.CASH_FLOW, input, config.cardConfig)
            is WidgetConfig.AccountBalance -> {
                val accountInput = input.copy(
                    accounts = input.accounts.filter { it.id == config.accountId },
                    transactions = input.transactions.filter { it.accountId == config.accountId }
                )
                registry.compute(CardType.ACCOUNT_HERO, accountInput, config.cardConfig)
            }
            is WidgetConfig.UpcomingPayments -> registry.compute(CardType.PLANNED_PAYMENTS, input, config.cardConfig)
            is WidgetConfig.Budget -> registry.compute(CardType.BUDGETS, input, config.cardConfig)
        }
    }

    abstract suspend fun renderContent(
        context: Context,
        id: GlanceId,
        data: WidgetData,
        renderData: CardRenderData?,
        config: WidgetConfig
    )
}

private fun WidgetConfig.withInstanceCardId(idHash: Int): WidgetConfig = when (this) {
    is WidgetConfig.NetWorth -> copy(cardConfig = cardConfig.copy(cardId = "widget_net_worth_$idHash"))
    is WidgetConfig.CashFlow -> copy(cardConfig = cardConfig.copy(cardId = "widget_cash_flow_$idHash"))
    is WidgetConfig.AccountBalance -> copy(cardConfig = cardConfig.copy(cardId = "widget_account_$idHash"))
    is WidgetConfig.UpcomingPayments -> copy(cardConfig = cardConfig.copy(cardId = "widget_planned_$idHash"))
    is WidgetConfig.Budget -> copy(cardConfig = cardConfig.copy(cardId = "widget_budgets_$idHash"))
}
