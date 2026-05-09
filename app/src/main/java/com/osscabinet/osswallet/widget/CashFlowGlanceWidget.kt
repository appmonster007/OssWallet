package com.osscabinet.ossWallet.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.osscabinet.ossWallet.MainActivity
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.CardRenderData
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.settings.CashFlowCardConfig
import com.osscabinet.ossWallet.domain.service.aggregation.CardAggregationService
import com.osscabinet.ossWallet.ui.util.formatCurrencyRight
import java.math.BigDecimal

class CashFlowGlanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appContext = context.applicationContext
        val widgetId = GlanceAppWidgetManager(appContext).getAppWidgetId(id)
        val widgetConfig = runCatching { CashFlowWidgetPrefsStore(appContext).load(widgetId) }
            .getOrElse { CashFlowWidgetConfig() }

        val data = runCatching {
            val common = WidgetDataLoader.loadCommon(appContext)
            val renderData = CardAggregationService.computeCashFlowCard(
                queryService = common.databaseQueryService,
                profileId = common.profileId,
                input = CardInput(
                    profileId = common.profileId,
                    baseCurrency = common.baseCurrency,
                    exchangeRates = common.exchangeRates,
                    precision = common.precision,
                    filter = TransactionFilter(
                        timelineRange = widgetConfig.timelineRange,
                        excludeFuture = true
                    ),
                    accounts = emptyList(),
                    categories = emptyList()
                ),
                config = CashFlowCardConfig(
                    cardId = "cf_widget_$widgetId"
                )
            )
            Triple(renderData, common.baseCurrency, common.precision)
        }.getOrNull()

        provideContent {
            CashFlowWidgetContent(context, widgetConfig, data?.first, data?.second ?: "", data?.third ?: 2)
        }
    }
}

@Composable
fun CashFlowWidgetContent(
    context: Context,
    config: CashFlowWidgetConfig,
    renderData: CardRenderData.CashFlow?,
    baseCurrency: String,
    precision: Int
) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(12.dp)
            .clickable(
                actionStartActivity(
                    Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        putExtra(MainActivity.EXTRA_WIDGET_NAV, "analytics")
                    }
                )
            ),
        contentAlignment = Alignment.TopStart
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            Text(
                text = "Cash Flow",
                style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold)
            )
            Text(
                text = config.timelineLabel,
                style = TextStyle(fontSize = 10.sp)
            )
            
            Spacer(modifier = GlanceModifier.height(8.dp))

            val income = renderData?.result?.totalIncomeInBase ?: BigDecimal.ZERO
            val expense = renderData?.result?.totalExpensesInBase ?: BigDecimal.ZERO
            val net = income.subtract(expense)

            CashFlowRow("Income", formatCurrencyRight(income, baseCurrency, precision))
            CashFlowRow("Expense", formatCurrencyRight(expense, baseCurrency, precision))
            
            Spacer(modifier = GlanceModifier.height(4.dp))
            
            CashFlowRow("Net", formatCurrencyRight(net, baseCurrency, precision), isBold = true)
        }
    }
}

@Composable
private fun CashFlowRow(label: String, amount: String, isBold: Boolean = false) {
    Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        val fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
        Text(
            text = label,
            modifier = GlanceModifier.defaultWeight(),
            style = TextStyle(fontSize = 11.sp, fontWeight = fontWeight)
        )
        Text(
            text = amount,
            style = TextStyle(fontSize = 11.sp, fontWeight = fontWeight)
        )
    }
}
