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
import androidx.glance.action.clickable
import com.osscabinet.ossWallet.OssWalletApp
import com.osscabinet.ossWallet.data.local.AppDatabase
import com.osscabinet.ossWallet.data.repository.TransactionRepositoryImpl
import com.osscabinet.ossWallet.ui.util.formatCurrencyRight
import com.osscabinet.ossWallet.domain.model.Transaction
import kotlinx.coroutines.flow.first

import java.math.BigDecimal

class TransactionHistoryWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val container = (context.applicationContext as OssWalletApp).container
        val transactionRepository = container.transactionRepository
        val accountRepository = container.accountRepository
        val currencyRepository = container.currencyRepository
        val settingsDataStore = container.settingsDataStore

        val profileId = settingsDataStore.currentProfileId.first()
        val baseCurrency = settingsDataStore.currency.first()
        val precision = settingsDataStore.decimalPrecision.first()

        val recentTransactions = transactionRepository.getAllTransactionsSync(profileId)
            .sortedByDescending { it.timestamp }
            .take(4)

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
            WidgetContent(totalBalance, recentTransactions, baseCurrency, precision)
        }
    }

    @Composable
    private fun WidgetContent(balance: BigDecimal, transactions: List<Transaction>, currency: String, precision: Int) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(16.dp)
                .background(Color.White)
                .appWidgetBackground()
                .cornerRadius(32.dp),
            verticalAlignment = Alignment.Top,
            horizontalAlignment = Alignment.Start
        ) {
            // Header
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = "TOTAL BALANCE",
                        style = TextStyle(
                            fontSize = 10.sp,
                            color = ColorProvider(Color.Gray),
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = formatCurrencyRight(balance, currency, precision),
                        style = TextStyle(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorProvider(Color(0xFF006064))
                        )
                    )
                }
                
                // Quick Action
                Box(
                    modifier = GlanceModifier
                        .background(Color(0xFF006064))
                        .cornerRadius(16.dp)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .clickable(actionStartActivity<MainActivity>()),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "ADD",
                        style = TextStyle(color = ColorProvider(Color.White), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(16.dp))
            
            // Divider-less History (using tonal shifts via background for items)
            Text(
                text = "RECENT ACTIVITY",
                style = TextStyle(fontSize = 9.sp, color = ColorProvider(Color.Gray), fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = GlanceModifier.height(8.dp))

            if (transactions.isEmpty()) {
                Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No records", style = TextStyle(color = ColorProvider(Color.Gray), fontSize = 12.sp))
                }
            } else {
                Column(modifier = GlanceModifier.fillMaxWidth()) {
                    transactions.forEach { transaction ->
                        TransactionRow(transaction, precision)
                        Spacer(modifier = GlanceModifier.height(4.dp))
                    }
                }
            }
        }
    }

    @Composable
    private fun TransactionRow(transaction: Transaction, precision: Int) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(Color(0xFFF5F5F5))
                .cornerRadius(12.dp)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = transaction.title,
                    style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold)
                )
                Text(
                    text = transaction.category,
                    style = TextStyle(fontSize = 10.sp, color = ColorProvider(Color.Gray))
                )
            }
            Text(
                text = formatCurrencyRight(if (transaction.isIncome) transaction.amount else -transaction.amount, transaction.currency, precision),
                style = TextStyle(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorProvider(if (transaction.isIncome) Color(0xFF2E7D32) else Color.Black)
                )
            )
        }
    }
}

class TransactionHistoryWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TransactionHistoryWidget()
}
