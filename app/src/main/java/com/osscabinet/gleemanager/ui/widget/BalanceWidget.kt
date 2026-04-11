package com.osscabinet.gleemanager.ui.widget

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
import com.osscabinet.gleemanager.MainActivity
import androidx.glance.appwidget.appWidgetBackground
import com.osscabinet.gleemanager.data.local.AppDatabase
import com.osscabinet.gleemanager.data.repository.TransactionRepositoryImpl
import com.osscabinet.gleemanager.ui.util.formatCurrency
import kotlinx.coroutines.flow.first
import java.util.*

class BalanceWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // SQLCipher requires passphrase. Since widgets run in a different process,
        // they must have access to the same key. In a production app, this would be 
        // retrieved from a secure KeyStore.
        val db = AppDatabase.getDatabase(context, "secret_passphrase".toByteArray())
        val repository = TransactionRepositoryImpl(db.transactionDao())
        val profileId = com.osscabinet.gleemanager.data.local.SettingsDataStore(context).currentProfileId.first()
        val transactions = repository.getAllTransactions(profileId).first()
        val totalBalance = transactions.sumOf { if (it.isIncome) it.amount else -it.amount }

        provideContent {
            BalanceWidgetContent(totalBalance)
        }
    }

    @Composable
    private fun BalanceWidgetContent(balance: Double) {
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
                text = formatCurrency(balance, "USD"), // Default to USD if currency not found, or fetch from settings
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
