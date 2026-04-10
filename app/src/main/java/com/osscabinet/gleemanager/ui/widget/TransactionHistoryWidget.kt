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
import androidx.glance.appwidget.cornerRadius
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import com.osscabinet.gleemanager.data.local.AppDatabase
import com.osscabinet.gleemanager.data.repository.TransactionRepositoryImpl
import com.osscabinet.gleemanager.domain.model.Transaction
import kotlinx.coroutines.flow.first
import java.util.*

class TransactionHistoryWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val db = AppDatabase.getDatabase(context, "secret_passphrase".toByteArray())
        val repository = TransactionRepositoryImpl(db.transactionDao())
        val profileId = com.osscabinet.gleemanager.data.local.SettingsDataStore(context).currentProfileId.first()
        val transactions = repository.getAllTransactions(profileId).first()
            .sortedByDescending { it.timestamp }
            .take(4)
        val totalBalance = repository.getAllTransactions(profileId).first()
            .sumOf { if (it.isIncome) it.amount else -it.amount }

        provideContent {
            WidgetContent(totalBalance, transactions)
        }
    }

    @Composable
    private fun WidgetContent(balance: Double, transactions: List<Transaction>) {
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
                        text = "$${String.format(Locale.getDefault(), "%,.2f", balance)}",
                        style = TextStyle(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorProvider(Color(0xFF006064))
                        )
                    )
                }
                
                // Quick Action
                androidx.glance.layout.Box(
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
                androidx.glance.layout.Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No records", style = TextStyle(color = ColorProvider(Color.Gray), fontSize = 12.sp))
                }
            } else {
                Column(modifier = GlanceModifier.fillMaxWidth()) {
                    transactions.forEach { transaction ->
                        TransactionRow(transaction)
                        Spacer(modifier = GlanceModifier.height(4.dp))
                    }
                }
            }
        }
    }

    @Composable
    private fun TransactionRow(transaction: Transaction) {
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
                text = "${if (transaction.isIncome) "+" else "-"} $${String.format(Locale.getDefault(), "%,.2f", transaction.amount)}",
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
