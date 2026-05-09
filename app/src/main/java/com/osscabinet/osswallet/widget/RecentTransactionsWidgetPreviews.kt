@file:OptIn(
    androidx.glance.ExperimentalGlanceApi::class,
    androidx.glance.preview.ExperimentalGlancePreviewApi::class
)

package com.osscabinet.ossWallet.widget

import androidx.compose.runtime.Composable
import androidx.glance.LocalContext
import androidx.glance.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.preferencesOf
import androidx.glance.GlanceTheme
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Preview(widthDp = 200, heightDp = 150)
@Composable
fun RecentTransactionsWidgetPreviewStandard() {
    GlanceTheme {
        RecentTransactionsWidgetContent(
            context = LocalContext.current,
            prefs = getMockRtPrefs(),
            widgetSize = DpSize(200.dp, 150.dp)
        )
    }
}

@Preview(widthDp = 300, heightDp = 200)
@Composable
fun RecentTransactionsWidgetPreviewLarge() {
    GlanceTheme {
        RecentTransactionsWidgetContent(
            context = LocalContext.current,
            prefs = getMockRtPrefs(),
            widgetSize = DpSize(300.dp, 200.dp)
        )
    }
}

@Preview(widthDp = 200, heightDp = 150)
@Composable
fun RecentTransactionsWidgetPreviewLoading() {
    GlanceTheme {
        RecentTransactionsWidgetContent(
            context = LocalContext.current,
            prefs = preferencesOf(RecentTransactionsGlanceWidget.Keys.IS_LOADED to "false"),
            widgetSize = DpSize(200.dp, 150.dp)
        )
    }
}

private fun getMockRtPrefs(): Preferences {
    val mockTransactions = listOf(
        WidgetTransaction(
            id = 1,
            title = "Starbucks Coffee",
            amount = "5.50",
            currency = "USD",
            type = "expense",
            category = "Food & Drinks",
            timestamp = "10:30 AM",
            note = "Morning coffee",
            iconName = "Restaurant"
        ),
        WidgetTransaction(
            id = 2,
            title = "Salary Deposit",
            amount = "2,500.00",
            currency = "USD",
            type = "income",
            category = "Salary",
            timestamp = "Yesterday",
            note = "Monthly salary",
            iconName = "Payments"
        ),
        WidgetTransaction(
            id = 3,
            title = "Uber Ride",
            amount = "12.00",
            currency = "USD",
            type = "expense",
            category = "Transport",
            timestamp = "Oct 24",
            note = "Back home",
            iconName = "DirectionsCar"
        )
    )
    
    return preferencesOf(
        RecentTransactionsGlanceWidget.Keys.IS_LOADED to "true",
        RecentTransactionsGlanceWidget.Keys.TRANSACTIONS_JSON to Json.encodeToString(mockTransactions)
    )
}
