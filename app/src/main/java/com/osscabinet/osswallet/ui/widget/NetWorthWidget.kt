package com.osscabinet.ossWallet.ui.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.osscabinet.ossWallet.MainActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import com.osscabinet.ossWallet.data.local.AppDatabase
import com.osscabinet.ossWallet.data.local.SettingsDataStore
import com.osscabinet.ossWallet.data.repository.AccountRepositoryImpl
import com.osscabinet.ossWallet.data.repository.TransactionRepositoryImpl
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.ui.util.calculateNetWorth
import com.osscabinet.ossWallet.ui.util.formatCurrencyRight
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.appwidget.state.getAppWidgetState
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.ui.util.FilterUtils
import com.osscabinet.ossWallet.ui.widget.config.NetWorthWidgetConfigActivity
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import com.osscabinet.ossWallet.ui.util.calculateTotalInCurrency
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import kotlinx.coroutines.flow.first
import java.math.BigDecimal

class NetWorthWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val settingsDataStore = (context.applicationContext as com.osscabinet.ossWallet.OssWalletApp).container.settingsDataStore
        
        val profileId = settingsDataStore.currentProfileId.first()
        val baseCurrency = settingsDataStore.currency.first()
        val precision = settingsDataStore.decimalPrecision.first()
        
        val prefs = getAppWidgetState(context, PreferencesGlanceStateDefinition, id)
        val timelineRangeJson = prefs[NetWorthWidgetConfigActivity.KEY_TIMELINE]
        val fontSizeStr = prefs[NetWorthWidgetConfigActivity.KEY_FONT_SIZE] ?: "Medium"
        
        val timelineRange = timelineRangeJson?.let {
            try {
                Json.decodeFromString<TimelineRange>(it)
            } catch (e: Exception) {
                TimelineRange.AllTime
            }
        } ?: TimelineRange.AllTime

        val accountRepository = (context.applicationContext as com.osscabinet.ossWallet.OssWalletApp).container.accountRepository
        val transactionRepository = (context.applicationContext as com.osscabinet.ossWallet.OssWalletApp).container.transactionRepository
        val currencyRepository = (context.applicationContext as com.osscabinet.ossWallet.OssWalletApp).container.currencyRepository
        
        val accounts = accountRepository.getAllAccountsSync(profileId)
        val exchangeRatesMap = currencyRepository.getExchangeRates(baseCurrency)
        val exchangeRates = exchangeRatesMap.mapValues { BigDecimal.valueOf(it.value) }
        
        val transactions = when (timelineRange) {
            is TimelineRange.AllTime -> {
                transactionRepository.getAllTransactionsSync(profileId)
            }
            is TimelineRange.ThisPeriod -> {
                val (start, end) = FilterUtils.getRangeForThis(timelineRange.period)
                transactionRepository.getFilteredTransactionsSync(profileId, TransactionFilter(startDate = start, endDate = end))
            }
            is TimelineRange.LastPeriod -> {
                val (start, end) = FilterUtils.getRangeForLast(timelineRange.period)
                transactionRepository.getFilteredTransactionsSync(profileId, TransactionFilter(startDate = start, endDate = end))
            }
            is TimelineRange.CustomRange -> {
                transactionRepository.getFilteredTransactionsSync(profileId, TransactionFilter(startDate = timelineRange.start, endDate = timelineRange.end))
            }
            else -> transactionRepository.getAllTransactionsSync(profileId)
        }

        val netWorth = calculateNetWorth(
            accounts = accounts,
            allTransactions = transactions,
            baseCurrency = baseCurrency,
            exchangeRates = exchangeRates,
            precision = precision,
            includeInitialBalance = timelineRange is TimelineRange.AllTime
        )

        val timelineLabel = when (timelineRange) {
            is TimelineRange.AllTime -> "ALL TIME"
            is TimelineRange.ThisPeriod -> timelineRange.period.label.uppercase()
            is TimelineRange.LastPeriod -> timelineLabelFromLastPeriod(timelineRange.period)
            is TimelineRange.CustomRange -> {
                val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy").withZone(ZoneId.systemDefault())
                val start = formatter.format(Instant.ofEpochMilli(timelineRange.start))
                val end = formatter.format(Instant.ofEpochMilli(timelineRange.end))
                "$start - $end".uppercase()
            }
            else -> "ALL TIME"
        }

        provideContent {
            NetWorthWidgetContent(
                netWorth = netWorth,
                currency = baseCurrency,
                precision = precision,
                fontSize = fontSizeStr,
                timelineLabel = timelineLabel
            )
        }
    }

    private fun timelineLabelFromLastPeriod(period: com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType): String {
        return period.label.uppercase()
    }

    @Composable
    private fun NetWorthWidgetContent(
        netWorth: BigDecimal,
        currency: String,
        precision: Int,
        fontSize: String,
        timelineLabel: String
    ) {
        val netWorthFontSize = when (fontSize) {
            "Small" -> 20.sp
            "Large" -> 36.sp
            else -> 28.sp
        }

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .appWidgetBackground()
                .background(Color.White)
                .cornerRadius(24.dp)
                .padding(16.dp)
                .clickable(actionStartActivity<MainActivity>()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "NET WORTH",
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorProvider(Color(0xFF6200EE))
                    )
                )
                Spacer(modifier = GlanceModifier.defaultWeight())
                Text(
                    text = timelineLabel,
                    style = TextStyle(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorProvider(Color.Gray)
                    )
                )
            }
            
            Spacer(modifier = GlanceModifier.defaultWeight())
            
            Text(
                text = formatCurrencyRight(netWorth, currency, precision),
                style = TextStyle(
                    fontSize = netWorthFontSize,
                    fontWeight = FontWeight.Bold,
                    color = ColorProvider(Color.Black)
                )
            )
            
            Spacer(modifier = GlanceModifier.defaultWeight())
        }
    }
}

class NetWorthWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NetWorthWidget()
}
