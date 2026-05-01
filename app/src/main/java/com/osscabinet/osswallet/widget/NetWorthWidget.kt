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

class NetWorthWidget : BaseOssWalletWidget() {
    override val defaultConfig: WidgetConfig = WidgetConfig.NetWorth()

    override suspend fun renderContent(
        context: Context,
        id: GlanceId,
        data: WidgetData,
        renderData: CardRenderData?,
        config: WidgetConfig
    ) {
        val netWorth = (renderData as? CardRenderData.NetWorth)?.value ?: BigDecimal.ZERO
        val currency = data.baseCurrency
        val precision = data.precision
        provideContent { NetWorthWidgetContent(netWorth, currency, precision) }
    }
}

@Composable
private fun NetWorthWidgetContent(
    netWorth: BigDecimal,
    currency: String,
    precision: Int
) {
    val isPositive = netWorth >= BigDecimal.ZERO
    val amountColor = if (isPositive) Color(0xFF4CAF50) else Color(0xFFF44336)
    val amountText = "${netWorth.setScale(precision, RoundingMode.HALF_UP).toPlainString()} $currency"

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(Color.White))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "NET WORTH",
            style = TextStyle(
                fontWeight = FontWeight.Medium,
                fontSize = androidx.compose.ui.unit.TextUnit(11f, androidx.compose.ui.unit.TextUnitType.Sp),
                color = ColorProvider(Color.Gray)
            )
        )
        Spacer(modifier = GlanceModifier.height(4.dp))
        Text(
            text = amountText,
            style = TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = androidx.compose.ui.unit.TextUnit(20f, androidx.compose.ui.unit.TextUnitType.Sp),
                color = ColorProvider(amountColor)
            )
        )
    }
}

class NetWorthWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NetWorthWidget()
}
