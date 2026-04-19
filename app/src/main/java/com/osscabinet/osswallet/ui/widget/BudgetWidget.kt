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
import com.osscabinet.ossWallet.domain.model.Budget
import com.osscabinet.ossWallet.ui.util.formatCurrencyRight
import kotlinx.coroutines.flow.first
import java.math.BigDecimal

class BudgetWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val container = (context.applicationContext as OssWalletApp).container
        val settingsDataStore = container.settingsDataStore
        val profileId = settingsDataStore.currentProfileId.first()

        // 1. Try to get precomputed data from cache
        val precomputedState = WidgetDataCache.get(profileId)
        val cachedResult = precomputedState?.budgetResult

        if (cachedResult != null) {
            provideContent {
                BudgetContent(cachedResult.budgets, cachedResult.currency, cachedResult.precision)
            }
            return
        }

        // 2. Fallback
        val budgetRepository = container.budgetRepository

        val budgets = budgetRepository.getBudgets().first()
        val currency = settingsDataStore.currency.first()
        val precision = settingsDataStore.decimalPrecision.first()

        provideContent {
            BudgetContent(budgets, currency, precision)
        }
    }

    @Composable
    private fun BudgetContent(budgets: List<Budget>, currency: String, precision: Int) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(16.dp)
                .background(Color.White)
                .appWidgetBackground()
                .cornerRadius(24.dp),
            verticalAlignment = Alignment.Top,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "BUDGETS",
                style = TextStyle(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorProvider(Color(0xFF6200EE))
                )
            )
            Spacer(modifier = GlanceModifier.height(8.dp))

            if (budgets.isEmpty()) {
                Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No budgets set", style = TextStyle(color = ColorProvider(Color.Gray), fontSize = 12.sp))
                }
            } else {
                val displayBudgets = budgets.take(3)
                Column(modifier = GlanceModifier.fillMaxWidth()) {
                    displayBudgets.forEach { budget ->
                        BudgetRow(budget, precision)
                        Spacer(modifier = GlanceModifier.height(8.dp))
                    }
                }
            }
        }
    }

    @Composable
    private fun BudgetRow(budget: Budget, precision: Int) {
        Column(modifier = GlanceModifier.fillMaxWidth()) {
            Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = budget.category,
                    style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                    modifier = GlanceModifier.defaultWeight()
                )
                Text(
                    text = "${(budget.progress * 100).toInt()}%",
                    style = TextStyle(
                        fontSize = 10.sp,
                        color = ColorProvider(if (budget.isOverBudget) Color.Red else Color.DarkGray)
                    )
                )
            }
            Spacer(modifier = GlanceModifier.height(4.dp))
            // Simple progress bar
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(Color(0xFFEEEEEE))
                    .cornerRadius(3.dp)
            ) {
                // Since Glance doesn't support fillMaxWidth(fraction) like Compose UI,
                // we have to be careful. In Glance, we can use a Row with weights to simulate a progress bar.
                Row(modifier = GlanceModifier.fillMaxSize()) {
                    if (budget.progress > 0) {
                        Box(
                            modifier = GlanceModifier
                                .defaultWeight()
                                .height(6.dp)
                                .background(if (budget.isOverBudget) Color.Red else Color(0xFF6200EE))
                                .cornerRadius(3.dp),
                            content = {}
                        )
                    }
                    if (budget.progress < 1) {
                        Spacer(modifier = GlanceModifier.defaultWeight())
                    }
                }
            }
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                text = "${formatCurrencyRight(budget.spent, budget.currency, precision)} of ${formatCurrencyRight(budget.limit, budget.currency, precision)}",
                style = TextStyle(fontSize = 9.sp, color = ColorProvider(Color.Gray))
            )
        }
    }
}

class BudgetWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BudgetWidget()
}
