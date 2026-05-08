package com.osscabinet.ossWallet.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import com.osscabinet.ossWallet.OssWalletApp
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.CardRenderData

abstract class BaseOssWalletWidget : GlanceAppWidget() {

    abstract val defaultConfig: WidgetConfig

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val container = (context.applicationContext as OssWalletApp).container
        val configStore = WidgetConfigStore(context)
        val config = (configStore.getConfig(id.hashCode())
            ?: defaultConfig).withInstanceCardId(id.hashCode())
        val data = WidgetDataLoader.load(context)

        val input = CardInput(
            profileId = data.profileId,
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

        val cardConfig = config.toCardConfig()
        val renderDataMap = container.computeCardUseCase.computeAll(listOf(cardConfig), input)
        val renderData = renderDataMap[cardConfig.cardId]

        renderContent(context, id, data, renderData, config)
    }

    abstract suspend fun renderContent(
        context: Context,
        id: GlanceId,
        data: WidgetData,
        renderData: CardRenderData?,
        config: WidgetConfig
    )
}

private fun WidgetConfig.toCardConfig() = when (this) {
    is WidgetConfig.NetWorth -> cardConfig
    is WidgetConfig.CashFlow -> cardConfig
    is WidgetConfig.AccountBalance -> cardConfig
    is WidgetConfig.UpcomingPayments -> cardConfig
    is WidgetConfig.Budget -> cardConfig
}

private fun WidgetConfig.withInstanceCardId(idHash: Int): WidgetConfig = when (this) {
    is WidgetConfig.NetWorth -> copy(cardConfig = cardConfig.copy(cardId = "widget_net_worth_$idHash"))
    is WidgetConfig.CashFlow -> copy(cardConfig = cardConfig.copy(cardId = "widget_cash_flow_$idHash"))
    is WidgetConfig.AccountBalance -> copy(cardConfig = cardConfig.copy(cardId = "widget_account_$idHash"))
    is WidgetConfig.UpcomingPayments -> copy(cardConfig = cardConfig.copy(cardId = "widget_planned_$idHash"))
    is WidgetConfig.Budget -> copy(cardConfig = cardConfig.copy(cardId = "widget_budgets_$idHash"))
}
