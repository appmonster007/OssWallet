package com.osscabinet.ossWallet.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.*
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.*
import androidx.glance.text.*
import androidx.glance.unit.ColorProvider
import com.osscabinet.ossWallet.domain.card.CardRenderData
import java.math.BigDecimal
import java.math.RoundingMode

class AccountBalanceWidget : BaseOssWalletWidget() {
    override val defaultConfig: WidgetConfig = WidgetConfig.AccountBalance(accountId = "")

    override suspend fun renderContent(
        context: Context,
        id: GlanceId,
        data: WidgetData,
        renderData: CardRenderData?,
        config: WidgetConfig
    ) {
        val accountConfig = config as? WidgetConfig.AccountBalance
        val heroData = renderData as? CardRenderData.AccountHero
        val accountName = accountConfig?.accountName
            ?: data.accounts.find { it.id == accountConfig?.accountId }?.name
            ?: "Account"
        val balance = heroData?.balance ?: BigDecimal.ZERO
        val currency = heroData?.account?.currencyCode ?: data.baseCurrency
        provideContent { AccountBalanceWidgetContent(accountName, balance, currency, data.precision) }
    }
}

@Composable
private fun AccountBalanceWidgetContent(
    accountName: String,
    balance: BigDecimal,
    currency: String,
    precision: Int
) {
    val isPositive = balance >= BigDecimal.ZERO
    val balanceColor = if (isPositive) Color(0xFF1976D2) else Color(0xFFF44336)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(Color.White))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = accountName.uppercase(),
            style = TextStyle(
                fontWeight = FontWeight.Medium,
                fontSize = androidx.compose.ui.unit.TextUnit(11f, androidx.compose.ui.unit.TextUnitType.Sp),
                color = ColorProvider(Color.Gray)
            )
        )
        Spacer(modifier = GlanceModifier.height(4.dp))
        Text(
            text = "${balance.setScale(precision, RoundingMode.HALF_UP).toPlainString()} $currency",
            style = TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = androidx.compose.ui.unit.TextUnit(20f, androidx.compose.ui.unit.TextUnitType.Sp),
                color = ColorProvider(balanceColor)
            )
        )
    }
}

class AccountBalanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = AccountBalanceWidget()
}
