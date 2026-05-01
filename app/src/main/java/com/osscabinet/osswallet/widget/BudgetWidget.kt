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
import com.osscabinet.ossWallet.domain.model.BudgetAlertLevel
import com.osscabinet.ossWallet.domain.model.BudgetProgress

class BudgetWidget : BaseOssWalletWidget() {
    override val defaultConfig: WidgetConfig = WidgetConfig.Budget()

    override suspend fun renderContent(
        context: Context,
        id: GlanceId,
        data: WidgetData,
        renderData: CardRenderData?,
        config: WidgetConfig
    ) {
        val items = (renderData as? CardRenderData.Budgets)?.items?.take(3) ?: emptyList()
        provideContent { BudgetWidgetContent(items) }
    }
}

@Composable
private fun BudgetWidgetContent(items: List<BudgetProgress>) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(Color.White))
            .padding(12.dp)
    ) {
        Text(
            text = "BUDGETS",
            style = TextStyle(
                fontWeight = FontWeight.Medium,
                fontSize = androidx.compose.ui.unit.TextUnit(11f, androidx.compose.ui.unit.TextUnitType.Sp),
                color = ColorProvider(Color.Gray)
            )
        )
        Spacer(modifier = GlanceModifier.height(6.dp))
        if (items.isEmpty()) {
            Text(
                text = "No active budgets",
                style = TextStyle(
                    color = ColorProvider(Color.Gray),
                    fontSize = androidx.compose.ui.unit.TextUnit(12f, androidx.compose.ui.unit.TextUnitType.Sp)
                )
            )
        } else {
            items.forEach { progress ->
                val barColor = when (progress.alertLevel) {
                    BudgetAlertLevel.EXCEEDED -> Color(0xFFF44336)
                    BudgetAlertLevel.WARNING -> Color(0xFFFF9800)
                    else -> Color(0xFF4CAF50)
                }
                val pct = (progress.percentage * 100).toInt().coerceIn(0, 100)
                Column(modifier = GlanceModifier.fillMaxWidth().padding(vertical = 3.dp)) {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = progress.budget.name,
                            modifier = GlanceModifier.defaultWeight(),
                            style = TextStyle(
                                fontSize = androidx.compose.ui.unit.TextUnit(12f, androidx.compose.ui.unit.TextUnitType.Sp),
                                color = ColorProvider(Color.Black)
                            )
                        )
                        Text(
                            text = "$pct%",
                            style = TextStyle(
                                fontWeight = FontWeight.Bold,
                                fontSize = androidx.compose.ui.unit.TextUnit(12f, androidx.compose.ui.unit.TextUnitType.Sp),
                                color = ColorProvider(barColor)
                            )
                        )
                    }
                }
            }
        }
    }
}

class BudgetWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BudgetWidget()
}
