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
import com.osscabinet.ossWallet.ui.util.calculateNetWorthFromAccounts
import com.osscabinet.ossWallet.ui.util.formatCurrencyRight
import com.osscabinet.ossWallet.ui.util.convertAmount
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.appwidget.state.getAppWidgetState
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.model.settings.NetWorthSettings
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
        
        val settingsJson = prefs[NetWorthWidgetConfigActivity.KEY_SETTINGS]
        val widgetSettings = settingsJson?.let {
            try {
                Json.decodeFromString<NetWorthSettings>(it)
            } catch (e: Exception) {
                null
            }
        } ?: NetWorthSettings(
            timelineRange = prefs[NetWorthWidgetConfigActivity.KEY_TIMELINE]?.let {
                try {
                    Json.decodeFromString<TimelineRange>(it)
                } catch (e: Exception) {
                    TimelineRange.AllTime
                }
            } ?: TimelineRange.AllTime,
            fontSize = prefs[NetWorthWidgetConfigActivity.KEY_FONT_SIZE] ?: "Medium"
        )
        
        val timelineRange = widgetSettings.timelineRange
        val fontSizeStr = widgetSettings.fontSize
        val effectiveProfileId = widgetSettings.profileId ?: profileId

        val accountRepository = (context.applicationContext as com.osscabinet.ossWallet.OssWalletApp).container.accountRepository
        val transactionRepository = (context.applicationContext as com.osscabinet.ossWallet.OssWalletApp).container.transactionRepository
        val currencyRepository = (context.applicationContext as com.osscabinet.ossWallet.OssWalletApp).container.currencyRepository
        
        val exchangeRatesMap = currencyRepository.getExchangeRates(baseCurrency)
        val exchangeRates = exchangeRatesMap.mapValues { BigDecimal.valueOf(it.value) }

        // Try to use cached data first
        val cachedSnapshot = WidgetDataCache.get(effectiveProfileId)
        
        val allAccounts = cachedSnapshot?.accounts ?: accountRepository.getAllAccountsSync(effectiveProfileId)
        val accounts = allAccounts.filter { 
            widgetSettings.selectedAccountIds == null || it.id in widgetSettings.selectedAccountIds
        }
        
        val (netWorth, netIncome, netExpense) = if (timelineRange is TimelineRange.AllTime) {
            val nw = calculateNetWorthFromAccounts(
                accounts = accounts,
                baseCurrency = baseCurrency,
                exchangeRates = exchangeRates,
                precision = precision
            )
            
            // Use cached summaries or fetch if missing
            val summaries = cachedSnapshot?.currencySummaries ?: transactionRepository.getCurrencySummaries(effectiveProfileId)
            
            var totalIncome = BigDecimal.ZERO
            var totalExpense = BigDecimal.ZERO
            
            summaries.forEach { s ->
                val incomeInBase = convertAmount(s.totalIncome, s.currency, baseCurrency, exchangeRates, precision + 4)
                val expenseInBase = convertAmount(s.totalExpense, s.currency, baseCurrency, exchangeRates, precision + 4)
                totalIncome = totalIncome.add(incomeInBase)
                totalExpense = totalExpense.add(expenseInBase)
            }

            Triple(nw, totalIncome.setScale(precision, java.math.RoundingMode.HALF_UP), totalExpense.setScale(precision, java.math.RoundingMode.HALF_UP))
        } else {
            val (start, end) = when (timelineRange) {
                is TimelineRange.ThisPeriod -> FilterUtils.getRangeForThis(timelineRange.period)
                is TimelineRange.LastPeriod -> FilterUtils.getRangeForLast(timelineRange.period)
                is TimelineRange.CustomRange -> Pair(timelineRange.start, timelineRange.end)
                else -> Pair(0L, Long.MAX_VALUE)
            }
            
            // For range-based NW, we still need transactions for fine-grained account filtering if selectedAccountIds is present
            // but we can optimize the summary part
            val summaries = cachedSnapshot?.currencySummaries ?: transactionRepository.getCurrencySummariesInRange(effectiveProfileId, start, end)
            
            var totalIncome = BigDecimal.ZERO
            var totalExpense = BigDecimal.ZERO
            summaries.forEach { s ->
                totalIncome = totalIncome.add(convertAmount(s.totalIncome, s.currency, baseCurrency, exchangeRates, precision + 4))
                totalExpense = totalExpense.add(convertAmount(s.totalExpense, s.currency, baseCurrency, exchangeRates, precision + 4))
            }

            // Still fetch filtered transactions ONLY if we need to filter by specific accounts for the NW calculation
            // If all accounts are selected, we can optimize further, but let's keep it simple for now
            val filter = TransactionFilter(startDate = start, endDate = end)
            val transactions = transactionRepository.getFilteredTransactionsSync(effectiveProfileId, filter)
                .filter { widgetSettings.selectedAccountIds == null || it.accountId in widgetSettings.selectedAccountIds }

            val nw = calculateNetWorth(
                accounts = accounts,
                allTransactions = transactions,
                baseCurrency = baseCurrency,
                exchangeRates = exchangeRates,
                precision = precision,
                includeInitialBalance = false
            )
            
            Triple(nw, totalIncome.setScale(precision, java.math.RoundingMode.HALF_UP), totalExpense.setScale(precision, java.math.RoundingMode.HALF_UP))
        }

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
                netIncome = netIncome,
                netExpense = netExpense,
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
        netIncome: BigDecimal,
        netExpense: BigDecimal,
        currency: String,
        precision: Int,
        fontSize: String,
        timelineLabel: String
    ) {
        val netWorthFontSize = when (fontSize) {
            "Small" -> 20.sp
            "Large" -> 36.sp
            "X-Large" -> 48.sp
            else -> 28.sp
        }

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .appWidgetBackground()
                .background(Color.White)
                .cornerRadius(24.dp)
                .padding(12.dp)
                .clickable(actionStartActivity<MainActivity>()),
            verticalAlignment = Alignment.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
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
                        color = ColorProvider(Color(0xFF6200EE).copy(alpha = 0.6f))
                    )
                )
            }
            
            Spacer(modifier = GlanceModifier.defaultWeight())

            Row(
                modifier = GlanceModifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                WidgetFlowBadge(
                    label = "INCOME",
                    amount = netIncome,
                    currency = currency,
                    precision = precision,
                    color = Color(0xFF4CAF50)
                )
                Spacer(modifier = GlanceModifier.width(8.dp))
                WidgetFlowBadge(
                    label = "EXPENSE",
                    amount = netExpense,
                    currency = currency,
                    precision = precision,
                    color = Color(0xFFF44336)
                )
            }
            
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

    @Composable
    private fun WidgetFlowBadge(
        label: String,
        amount: BigDecimal,
        currency: String,
        precision: Int,
        color: Color
    ) {
        Box(
            modifier = GlanceModifier
                .background(color.copy(alpha = 0.1f))
                .cornerRadius(4.dp)
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Text(
                text = "${label}: ${formatCurrencyRight(amount, currency, precision)}",
                style = TextStyle(
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorProvider(color)
                )
            )
        }
    }
}

class NetWorthWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NetWorthWidget()
}
