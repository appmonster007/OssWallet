package com.osscabinet.ossWallet.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.osscabinet.ossWallet.data.local.AppDatabase
import com.osscabinet.ossWallet.data.local.SettingsDataStore
import com.osscabinet.ossWallet.data.mapper.toDomain
import com.osscabinet.ossWallet.domain.card.*
import com.osscabinet.ossWallet.domain.model.settings.NetWorthCardConfig
import com.osscabinet.ossWallet.domain.model.settings.ScreenContext
import kotlinx.coroutines.flow.first
import java.math.BigDecimal

class OssWalletWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val settingsDataStore = SettingsDataStore(context)
        val profileId = settingsDataStore.currentProfileId.first()
        val baseCurrency = settingsDataStore.currency.first()
        val precision = settingsDataStore.decimalPrecision.first()

        val db = AppDatabase.getDatabase(context, byteArrayOf())
        val accounts = db.accountDao().getAllAccountsSync(profileId).map { it.toDomain() }
        val transactions = db.transactionDao().getAllTransactionsSync(profileId).map { it.toDomain() }
        val categories = db.categoryDao().getAllCategoriesSync().map { it.toDomain() }
        val rates = db.currencyDao().getAllRates()
        
        val exchangeRates = rates.associate { it.currencyCode to BigDecimal.valueOf(it.rate) }

        val netWorthConfig = NetWorthCardConfig(
            cardId = "widget_net_worth",
            title = "NET WORTH",
            screenContext = ScreenContext.DASHBOARD
        )

        val input = CardInput(
            transactions = transactions,
            accounts = accounts,
            categories = categories,
            exchangeRates = exchangeRates,
            baseCurrency = baseCurrency,
            precision = precision
        )

        val renderData = CardComputerRegistry.DEFAULT.compute(
            type = CardType.NET_WORTH,
            input = input,
            config = netWorthConfig
        ) as CardRenderData.NetWorth

        provideContent {
            WidgetContent(renderData.value.toString(), baseCurrency)
        }
    }

    @Composable
    private fun WidgetContent(amount: String, currency: String) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(Color.White))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Net Worth",
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    color = ColorProvider(Color.Gray)
                )
            )
            Spacer(modifier = GlanceModifier.height(8.dp))
            Text(
                text = " ",
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = ColorProvider(Color.Black)
                )
            )
        }
    }
}
