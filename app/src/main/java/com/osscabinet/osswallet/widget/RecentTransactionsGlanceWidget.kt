package com.osscabinet.ossWallet.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.osscabinet.ossWallet.R
import kotlinx.serialization.json.Json

class RecentTransactionsGlanceWidget : BaseOssWalletWidget() {

    @Composable
    override fun RenderContent(context: Context, prefs: Preferences, size: DpSize) {
        RecentTransactionsWidgetContent(context, prefs, size)
    }

    object Keys {
        val IS_LOADED = stringPreferencesKey("rt_is_loaded")
        val TRANSACTIONS_JSON = stringPreferencesKey("rt_transactions_json")
    }
}

@Composable
internal fun RecentTransactionsWidgetContent(context: Context, prefs: Preferences, widgetSize: DpSize) {
    val scale = when {
        widgetSize.height < 100.dp -> 0.85f
        widgetSize.height < 180.dp -> 1.0f
        else -> 1.15f
    }
    
    val isLoaded = prefs[RecentTransactionsGlanceWidget.Keys.IS_LOADED] == "true"
    if (!isLoaded) {
        RecentTransactionsPlaceholder(scale)
    } else {
        RecentTransactionsBody(context, prefs, scale)
    }
}

@Composable
private fun RecentTransactionsPlaceholder(scale: Float) {
    Text(
        text = "Recent Transactions",
        style = TextStyle(
            fontSize = TextUnit(12f * scale, TextUnitType.Sp),
            fontWeight = FontWeight.Bold,
            color = GlanceTheme.colors.onSurface
        )
    )
}

@Composable
internal fun RecentTransactionsBody(context: Context, prefs: Preferences, scale: Float) {
    val json = prefs[RecentTransactionsGlanceWidget.Keys.TRANSACTIONS_JSON] ?: "[]"
    val transactions = try {
        Json.decodeFromString<List<WidgetTransaction>>(json)
    } catch (e: Exception) {
        emptyList()
    }

    Column(modifier = GlanceModifier.fillMaxSize()) {
        Text(
            text = context.getString(R.string.recent_activities).uppercase(),
            style = TextStyle(
                fontSize = TextUnit(10f * scale, TextUnitType.Sp),
                fontWeight = FontWeight.Bold,
                color = GlanceTheme.colors.onSurfaceVariant
            ),
            modifier = GlanceModifier.padding(bottom = 8.dp)
        )

        if (transactions.isEmpty()) {
            Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = context.getString(R.string.no_recent_activities),
                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant)
                )
            }
        } else {
            LazyColumn(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                items(transactions) { tx ->
                    TransactionItem(tx, scale)
                    Spacer(modifier = GlanceModifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun TransactionItem(tx: WidgetTransaction, scale: Float) {
    val amountColor = if (tx.type == "income") {
        ColorProvider(R.color.widget_income_color)
    } else {
        ColorProvider(R.color.widget_expense_color)
    }

    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon Circle
        Box(
            modifier = GlanceModifier
                .size((36f * scale).dp)
                .background(GlanceTheme.colors.secondaryContainer)
                .cornerRadius((18f * scale).dp),
            contentAlignment = Alignment.Center
        ) {
            // Placeholder for category icon
        }

        Spacer(modifier = GlanceModifier.width(12.dp))

        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = tx.title,
                style = TextStyle(
                    fontSize = TextUnit(11f * scale, TextUnitType.Sp),
                    fontWeight = FontWeight.Medium,
                    color = GlanceTheme.colors.onSurface
                ),
                maxLines = 1
            )
            Text(
                text = tx.category,
                style = TextStyle(
                    fontSize = TextUnit(9f * scale, TextUnitType.Sp),
                    color = GlanceTheme.colors.onSurfaceVariant
                ),
                maxLines = 1
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = tx.amount,
                style = TextStyle(
                    fontSize = TextUnit(11f * scale, TextUnitType.Sp),
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )
            )
            Text(
                text = tx.timestamp,
                style = TextStyle(
                    fontSize = TextUnit(8f * scale, TextUnitType.Sp),
                    color = GlanceTheme.colors.onSurfaceVariant
                )
            )
        }
    }
}
