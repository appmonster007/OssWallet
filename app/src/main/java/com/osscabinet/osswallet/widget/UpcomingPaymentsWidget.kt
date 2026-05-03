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
import com.osscabinet.ossWallet.domain.model.UpcomingPaymentItem
import java.text.SimpleDateFormat
import java.util.*

class UpcomingPaymentsWidget : BaseOssWalletWidget() {
    override val defaultConfig: WidgetConfig = WidgetConfig.UpcomingPayments()

    override suspend fun renderContent(
        context: Context,
        id: GlanceId,
        data: WidgetData,
        renderData: CardRenderData?,
        config: WidgetConfig
    ) {
        val items = (renderData as? CardRenderData.PlannedPayments)?.upcoming
            ?.take(1) ?: emptyList()
        provideContent { UpcomingPaymentsWidgetContent(items) }
    }
}

@Composable
private fun UpcomingPaymentsWidgetContent(items: List<UpcomingPaymentItem>) {
    val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(Color.White))
            .padding(12.dp)
    ) {
        Text(
            text = "UPCOMING",
            style = TextStyle(
                fontWeight = FontWeight.Medium,
                fontSize = androidx.compose.ui.unit.TextUnit(11f, androidx.compose.ui.unit.TextUnitType.Sp),
                color = ColorProvider(Color.Gray)
            )
        )
        Spacer(modifier = GlanceModifier.height(6.dp))
        if (items.isEmpty()) {
            Text(
                text = "No upcoming payments",
                style = TextStyle(
                    color = ColorProvider(Color.Gray),
                    fontSize = androidx.compose.ui.unit.TextUnit(12f, androidx.compose.ui.unit.TextUnitType.Sp)
                )
            )
        } else {
            items.forEach { item ->
                val dateStr = dateFormat.format(Date(item.scheduledDate))
                val overdueColor = if (item.isOverdue) Color(0xFFF44336) else Color.Black
                Row(
                    modifier = GlanceModifier.fillMaxWidth().padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = dateStr,
                        modifier = GlanceModifier.defaultWeight(),
                        style = TextStyle(
                            fontSize = androidx.compose.ui.unit.TextUnit(11f, androidx.compose.ui.unit.TextUnitType.Sp),
                            color = ColorProvider(Color.Gray)
                        )
                    )
                    Text(
                        text = item.plannedPayment.title,
                        modifier = GlanceModifier.defaultWeight(),
                        style = TextStyle(
                            fontSize = androidx.compose.ui.unit.TextUnit(12f, androidx.compose.ui.unit.TextUnitType.Sp),
                            color = ColorProvider(overdueColor)
                        )
                    )
                    Text(
                        text = "${item.plannedPayment.amount.toPlainString()} ${item.plannedPayment.currency}",
                        style = TextStyle(
                            fontWeight = FontWeight.Medium,
                            fontSize = androidx.compose.ui.unit.TextUnit(12f, androidx.compose.ui.unit.TextUnitType.Sp),
                            color = ColorProvider(Color(0xFFF44336))
                        )
                    )
                }
            }
        }
    }
}

class UpcomingPaymentsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = UpcomingPaymentsWidget()
}
