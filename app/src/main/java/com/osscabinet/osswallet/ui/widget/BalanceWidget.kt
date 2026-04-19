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
import com.osscabinet.ossWallet.OssWalletApp
import com.osscabinet.ossWallet.data.local.AppDatabase
import com.osscabinet.ossWallet.data.repository.TransactionRepositoryImpl
import com.osscabinet.ossWallet.ui.util.formatCurrencyRight
import kotlinx.coroutines.flow.first

import java.math.BigDecimal

class BalanceWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val container = (context.applicationContext as OssWalletApp).container
        val settingsDataStore = container.settingsDataStore
        val profileId = settingsDataStore.currentProfileId.first()

        // 1. Try to get precomputed data from cache
        val precomputedState = WidgetDataCache.get(profileId)
        val cachedResult = precomputedState?.balanceResult

        if (cachedResult != null) {
            provideContent {
                BalanceWidgetContent(cachedResult.totalBalance, cachedResult.currency, cachedResult.precision)
            }
            return
        }

        // 2. Fallback
        val accountRepository = container.accountRepository
        val currencyRepository = container.currencyRepository
        
        val baseCurrency = settingsDataStore.currency.first()
        val precision = settingsDataStore.decimalPrecision.first()
        
        val accounts = accountRepository.getAllAccountsSync(profileId)
        val exchangeRatesMap = currencyRepository.getExchangeRates(baseCurrency)
        val exchangeRates = exchangeRatesMap.mapValues { BigDecimal.valueOf(it.value) }

        val totalBalance = com.osscabinet.ossWallet.ui.util.calculateNetWorthFromAccounts(
            accounts = accounts,
            baseCurrency = baseCurrency,
            exchangeRates = exchangeRates,
            precision = precision
        )

        provideContent {
            BalanceWidgetContent(totalBalance, baseCurrency, precision)
        }
    }

    @Composable
    private fun BalanceWidgetContent(balance: BigDecimal, currency: String, precision: Int) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(16.dp)
                .background(Color.White)
                .appWidgetBackground(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "TOTAL BALANCE",
                style = TextStyle(
                    fontSize = 10.sp,
                    color = ColorProvider(Color.Gray)
                )
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                text = formatCurrencyRight(balance, currency, precision),
                style = TextStyle(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorProvider(Color(0xFF006064))
                )
            )
            Spacer(modifier = GlanceModifier.height(16.dp))
            Button(
                text = "+ QUICK ADD",
                onClick = actionStartActivity<MainActivity>(),
                modifier = GlanceModifier.fillMaxWidth()
            )
        }
    }
}

class BalanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BalanceWidget()
}
