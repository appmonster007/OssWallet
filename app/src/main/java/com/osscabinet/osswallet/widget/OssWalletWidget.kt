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
import com.osscabinet.ossWallet.OssWalletApp
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.CardRenderData
import com.osscabinet.ossWallet.domain.model.settings.NetWorthCardConfig
import com.osscabinet.ossWallet.domain.model.settings.ScreenContext
import kotlinx.coroutines.flow.first

class OssWalletWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val container = (context.applicationContext as OssWalletApp).container
        val settingsDataStore = container.settingsDataStore
        val profileId = settingsDataStore.currentProfileId.first()
        val baseCurrency = settingsDataStore.currency.first()
        val precision = settingsDataStore.decimalPrecision.first()

        val accounts = container.accountRepository.getAllAccounts(profileId).first()
        val categories = container.categoryRepository.categories.first()
        val rates = container.computationRepository.exchangeRates.first()

        val netWorthConfig = NetWorthCardConfig(
            cardId = "widget_net_worth",
            title = "NET WORTH",
            screenContext = ScreenContext.DASHBOARD
        )

        val input = CardInput(
            profileId = profileId,
            accounts = accounts.filter { !it.isArchived },
            categories = categories,
            exchangeRates = rates,
            baseCurrency = baseCurrency,
            precision = precision
        )

        val renderData = container.computeCardUseCase.computeAll(
            listOf(netWorthConfig), input
        )["widget_net_worth"] as? CardRenderData.NetWorth

        provideContent {
            WidgetContent(renderData?.currentMetrics?.netWorth?.toString() ?: "—", baseCurrency)
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
                text = "$amount $currency",
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = ColorProvider(Color.Black)
                )
            )
        }
    }
}
