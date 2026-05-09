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
import androidx.glance.appwidget.updateAll
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.osscabinet.ossWallet.MainActivity
import com.osscabinet.ossWallet.OssWalletApp
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.CardRenderData
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.settings.NetWorthCardConfig
import com.osscabinet.ossWallet.domain.service.aggregation.CardAggregationService
import com.osscabinet.ossWallet.ui.util.formatCurrencyRight
import kotlinx.coroutines.flow.first
import java.math.BigDecimal
import java.math.RoundingMode

class NetWorthGlanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appContext = context.applicationContext
        val widgetId = GlanceAppWidgetManager(appContext).getAppWidgetId(id)
        val widgetConfig = runCatching { NetWorthWidgetPrefsStore(appContext).load(widgetId) }
            .getOrElse { NetWorthWidgetConfig() }

        data class FetchResult(
            val renderData: CardRenderData.NetWorth?,
            val baseCurrency: String,
            val precision: Int
        )

        val fetched = runCatching {
            val container = (appContext as OssWalletApp).container
            val settingsStore = container.settingsDataStore
            val profileId = settingsStore.currentProfileId.first()
            val baseCurrency = settingsStore.currency.first()
            val precision = settingsStore.decimalPrecision.first()
            val exchangeRates = container.db.currencyDao().getAllRates()
                .associate { it.currencyCode to BigDecimal.valueOf(it.rate) }

            val renderData = CardAggregationService.computeNetWorthCard(
                queryService = container.databaseQueryService,
                profileId = profileId,
                input = CardInput(
                    profileId = profileId,
                    baseCurrency = baseCurrency,
                    exchangeRates = exchangeRates,
                    precision = precision,
                    filter = TransactionFilter(
                        timelineRange = widgetConfig.timelineRange,
                        excludeFuture = true
                    ),
                    accounts = emptyList(),
                    categories = emptyList()
                ),
                config = NetWorthCardConfig(
                    cardId = "widget_$widgetId",
                    fontSize = widgetConfig.fontSize,
                    showComparison = widgetConfig.showComparison
                )
            )
            FetchResult(renderData, baseCurrency, precision)
        }.getOrElse { FetchResult(null, "", 2) }

        provideContent {
            NetWorthWidgetContent(context, widgetConfig, fetched.renderData, fetched.baseCurrency, fetched.precision)
        }
    }
}

@Composable
fun NetWorthWidgetContent(
    context: Context,
    config: NetWorthWidgetConfig,
    renderData: CardRenderData.NetWorth?,
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
                        putExtra(MainActivity.EXTRA_WIDGET_NAV, "dashboard")
                    }
                )
            ),
        contentAlignment = Alignment.TopStart
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            // Net worth amount
            val netWorth = renderData?.currentMetrics?.netWorth ?: BigDecimal.ZERO
            val fontSize = when (config.fontSize) {
                "Small" -> 16.sp
                "Large" -> 24.sp
                "X-Large" -> 28.sp
                else -> 20.sp
            }

            Text(
                text = formatCurrencyRight(netWorth, baseCurrency, precision),
                style = TextStyle(
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold
                )
            )

            // Comparison and Period Label
            val prevMetrics = renderData?.previousMetrics
            val comparisonText = buildComparisonText(netWorth, prevMetrics?.netWorth, config.showComparison)
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (comparisonText != null) {
                    Text(
                        text = comparisonText,
                        style = TextStyle(fontSize = 10.sp)
                    )
                    Spacer(modifier = GlanceModifier.width(8.dp))
                }
                Text(
                    text = config.timelineLabel,
                    style = TextStyle(fontSize = 10.sp)
                )
            }

            Spacer(modifier = GlanceModifier.height(12.dp))

            // Income / Expense Row
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                val income = renderData?.currentMetrics?.cashFlow?.totalIncomeInBase ?: BigDecimal.ZERO
                val expense = renderData?.currentMetrics?.cashFlow?.totalExpensesInBase ?: BigDecimal.ZERO

                BadgeColumn(
                    label = "INCOME",
                    amount = formatCurrencyRight(income, baseCurrency, precision),
                    comparison = buildComparisonText(income, prevMetrics?.cashFlow?.totalIncomeInBase, config.showComparison),
                    modifier = GlanceModifier.defaultWeight()
                )
                
                Spacer(modifier = GlanceModifier.width(8.dp))

                BadgeColumn(
                    label = "EXPENSE",
                    amount = formatCurrencyRight(expense, baseCurrency, precision),
                    comparison = buildComparisonText(expense, prevMetrics?.cashFlow?.totalExpensesInBase, config.showComparison),
                    modifier = GlanceModifier.defaultWeight()
                )
            }
        }
    }
}

@Composable
private fun BadgeColumn(
    label: String,
    amount: String,
    comparison: String?,
    modifier: GlanceModifier = GlanceModifier
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = TextStyle(fontSize = 8.sp, fontWeight = FontWeight.Bold)
            )
            if (comparison != null) {
                Spacer(modifier = GlanceModifier.width(4.dp))
                Text(
                    text = comparison,
                    style = TextStyle(fontSize = 8.sp)
                )
            }
        }
        Text(
            text = amount,
            style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold)
        )
    }
}

private fun buildComparisonText(
    current: BigDecimal,
    previous: BigDecimal?,
    showComparison: Boolean
): String? {
    if (!showComparison || previous == null || previous.signum() == 0) return null
    val change = current.subtract(previous)
    if (change.signum() == 0) return null
    val pct = change.multiply(BigDecimal("100"))
        .divide(previous.abs(), 1, RoundingMode.HALF_UP)
    val arrow = if (change.signum() > 0) "▲" else "▼"
    val sign = if (change.signum() > 0) "+" else ""
    return "$arrow $sign${pct}%"
}
