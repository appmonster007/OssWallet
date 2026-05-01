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

class CashFlowWidget : BaseOssWalletWidget() {
    override val defaultConfig: WidgetConfig = WidgetConfig.CashFlow()

    override suspend fun renderContent(
        context: Context,
        id: GlanceId,
        data: WidgetData,
        renderData: CardRenderData?,
        config: WidgetConfig
    ) {
        val cashFlow = (renderData as? CardRenderData.CashFlow)?.result
        val income = cashFlow?.totalIncomeInBase ?: BigDecimal.ZERO
        val expense = cashFlow?.totalExpensesInBase ?: BigDecimal.ZERO
        provideContent { CashFlowWidgetContent(income, expense, data.baseCurrency, data.precision) }
    }
}

@Composable
private fun CashFlowWidgetContent(
    income: BigDecimal,
    expense: BigDecimal,
    currency: String,
    precision: Int
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(Color.White))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "CASH FLOW",
            style = TextStyle(
                fontWeight = FontWeight.Medium,
                fontSize = androidx.compose.ui.unit.TextUnit(11f, androidx.compose.ui.unit.TextUnitType.Sp),
                color = ColorProvider(Color.Gray)
            )
        )
        Spacer(modifier = GlanceModifier.height(6.dp))
        Row(modifier = GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
            Text(
                text = "+ ${income.setScale(precision, RoundingMode.HALF_UP).toPlainString()} $currency",
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = androidx.compose.ui.unit.TextUnit(14f, androidx.compose.ui.unit.TextUnitType.Sp),
                    color = ColorProvider(Color(0xFF4CAF50))
                )
            )
        }
        Spacer(modifier = GlanceModifier.height(2.dp))
        Row(modifier = GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
            Text(
                text = "- ${expense.abs().setScale(precision, RoundingMode.HALF_UP).toPlainString()} $currency",
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = androidx.compose.ui.unit.TextUnit(14f, androidx.compose.ui.unit.TextUnitType.Sp),
                    color = ColorProvider(Color(0xFFF44336))
                )
            )
        }
    }
}

class CashFlowWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CashFlowWidget()
}
