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
        
        val allAccounts = accountRepository.getAllAccountsSync(effectiveProfileId)
        val accounts = allAccounts.filter { 
            widgetSettings.selectedAccountIds == null || it.id in widgetSettings.selectedAccountIds
        }
        val exchangeRatesMap = currencyRepository.getExchangeRates(baseCurrency)
        val exchangeRates = exchangeRatesMap.mapValues { BigDecimal.valueOf(it.value) }
        
        val (netWorth, netIncome, netExpense) = if (timelineRange is TimelineRange.AllTime) {
            val nw = calculateNetWorthFromAccounts(
                accounts = accounts,
                baseCurrency = baseCurrency,
                exchangeRates = exchangeRates,
                precision = precision
            )
            // For All Time, we can still calculate total income/expense from all transactions
            val allTransactions = transactionRepository.getAllTransactionsSync(effectiveProfileId)
            val filteredTransactions = allTransactions.filter { 
                widgetSettings.selectedAccountIds == null || it.accountId in widgetSettings.selectedAccountIds 
            }
            val income = filteredTransactions.filter { it.isIncome }
                .fold(BigDecimal.ZERO) { acc, t -> 
                    acc.add(convertAmount(t.amount, t.currency, baseCurrency, exchangeRates, precision + 4))
                }
            val expense = filteredTransactions.filter { !it.isIncome }
                .fold(BigDecimal.ZERO) { acc, t -> 
                    acc.add(convertAmount(t.amount, t.currency, baseCurrency, exchangeRates, precision + 4))
                }
            Triple(nw, income.setScale(precision, java.math.RoundingMode.HALF_UP), expense.setScale(precision, java.math.RoundingMode.HALF_UP))
        } else {
            val filter = when (timelineRange) {
                is TimelineRange.ThisPeriod -> {
                    val (start, end) = FilterUtils.getRangeForThis(timelineRange.period)
                    TransactionFilter(startDate = start, endDate = end)
                }
                is TimelineRange.LastPeriod -> {
                    val (start, end) = FilterUtils.getRangeForLast(timelineRange.period)
                    TransactionFilter(startDate = start, endDate = end)
                }
                is TimelineRange.CustomRange -> {
                    TransactionFilter(startDate = timelineRange.start, endDate = timelineRange.end)
                }
                else -> TransactionFilter()
            }
            
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

            
            val income = transactions.filter { it.isIncome }
                .fold(BigDecimal.ZERO) { acc, t -> 
                    acc.add(convertAmount(t.amount, t.currency, baseCurrency, exchangeRates, precision + 4))
                }
            val expense = transactions.filter { !it.isIncome }
                .fold(BigDecimal.ZERO) { acc, t -> 
                    acc.add(convertAmount(t.amount, t.currency, baseCurrency, exchangeRates, precision + 4))
                }
            
            Triple(nw, income.setScale(precision, java.math.RoundingMode.HALF_UP), expense.setScale(precision, java.math.RoundingMode.HALF_UP))
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
