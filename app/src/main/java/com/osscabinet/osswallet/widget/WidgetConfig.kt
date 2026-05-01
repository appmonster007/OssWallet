package com.osscabinet.ossWallet.widget

import com.osscabinet.ossWallet.domain.model.settings.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@Serializable
@JsonClassDiscriminator("widgetType")
sealed class WidgetConfig {

    @Serializable
    @SerialName("net_worth")
    data class NetWorth(
        val cardConfig: NetWorthCardConfig = NetWorthCardConfig(cardId = "widget_net_worth")
    ) : WidgetConfig()

    @Serializable
    @SerialName("cash_flow")
    data class CashFlow(
        val cardConfig: CashFlowCardConfig = CashFlowCardConfig(cardId = "widget_cash_flow")
    ) : WidgetConfig()

    @Serializable
    @SerialName("account_balance")
    data class AccountBalance(
        val accountId: String,
        val accountName: String = "",
        val cardConfig: AccountHeroCardConfig = AccountHeroCardConfig(cardId = "widget_account")
    ) : WidgetConfig()

    @Serializable
    @SerialName("upcoming_payments")
    data class UpcomingPayments(
        val cardConfig: PlannedPaymentsCardConfig = PlannedPaymentsCardConfig(cardId = "widget_planned")
    ) : WidgetConfig()

    @Serializable
    @SerialName("budget")
    data class Budget(
        val cardConfig: BudgetsCardConfig = BudgetsCardConfig(cardId = "widget_budgets")
    ) : WidgetConfig()
}
