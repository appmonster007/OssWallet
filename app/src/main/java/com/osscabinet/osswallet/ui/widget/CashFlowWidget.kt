package com.osscabinet.ossWallet.ui.widget

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.actionStartActivity
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
import com.osscabinet.ossWallet.OssWalletApp
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.ui.util.formatCurrencyRight
import com.osscabinet.ossWallet.ui.util.convertAmount
import kotlinx.coroutines.flow.first
import java.math.BigDecimal
import java.math.RoundingMode

class CashFlowWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val container = (context.applicationContext as OssWalletApp).container
        val transactionRepository = container.transactionRepository
        val settingsDataStore = container.settingsDataStore
        val currencyRepository = container.currencyRepository

        val profileId = settingsDataStore.currentProfileId.first()
        val baseCurrency = settingsDataStore.currency.first()
        val precision = settingsDataStore.decimalPrecision.first()

        val transactions = transactionRepository.getAllTransactions(profileId).first()
        val exchangeRatesMap = currencyRepository.getExchangeRates(baseCurrency)
        val exchangeRates = exchangeRatesMap.mapValues { BigDecimal.valueOf(it.value) }

        val income = transactions.filter { it.isIncome }
            .fold(BigDecimal.ZERO) { acc, t -> 
                acc.add(convertAmount(t.amount, t.currency, baseCurrency, exchangeRates, precision + 2)) 
            }
        val expense = transactions.filter { !it.isIncome }
            .fold(BigDecimal.ZERO) { acc, t -> 
                acc.add(convertAmount(t.amount, t.currency, baseCurrency, exchangeRates, precision + 2)) 
            }

        provideContent {
            CashFlowContent(income, expense, baseCurrency, precision)
        }
    }

    @Composable
    private fun CashFlowContent(income: BigDecimal, expense: BigDecimal, currency: String, precision: Int) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(16.dp)
                .background(Color.White)
                .appWidgetBackground()
                .cornerRadius(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "CASH FLOW",
                style = TextStyle(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorProvider(Color(0xFF6200EE))
                )
            )
            Spacer(modifier = GlanceModifier.height(12.dp))
            
            Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = GlanceModifier.defaultWeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "INCOME",
                        style = TextStyle(fontSize = 10.sp, color = ColorProvider(Color.Gray))
                    )
                    Text(
                        text = formatCurrencyRight(income, currency, precision),
                        style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ColorProvider(Color(0xFF2E7D32)))
                    )
                }
                
                Spacer(modifier = GlanceModifier.width(16.dp))
                
                Column(modifier = GlanceModifier.defaultWeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "EXPENSE",
                        style = TextStyle(fontSize = 10.sp, color = ColorProvider(Color.Gray))
                    )
                    Text(
                        text = formatCurrencyRight(expense, currency, precision),
                        style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ColorProvider(Color(0xFFC62828)))
                    )
                }
            }
        }
    }
}

class CashFlowWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CashFlowWidget()
}
