@file:OptIn(
    androidx.glance.ExperimentalGlanceApi::class,
    androidx.glance.preview.ExperimentalGlancePreviewApi::class
)

package com.osscabinet.ossWallet.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.preferencesOf
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.preview.Preview

// ── Full widget previews (all valid sizes ≥ 3×2) ─────────────────────────────

@Preview(widthDp = 180, heightDp = 110)
@Composable
fun CashFlowWidgetPreview3x2() {
    GlanceTheme {
        CashFlowWidgetContent(
            context = LocalContext.current,
            prefs = mockCashFlowPrefs(),
            widgetSize = DpSize(180.dp, 110.dp)
        )
    }
}

@Preview(widthDp = 250, heightDp = 110)
@Composable
fun CashFlowWidgetPreview4x2() {
    GlanceTheme {
        CashFlowWidgetContent(
            context = LocalContext.current,
            prefs = mockCashFlowPrefs(),
            widgetSize = DpSize(250.dp, 110.dp)
        )
    }
}

@Preview(widthDp = 320, heightDp = 110)
@Composable
fun CashFlowWidgetPreview5x2() {
    GlanceTheme {
        CashFlowWidgetContent(
            context = LocalContext.current,
            prefs = mockCashFlowPrefs(),
            widgetSize = DpSize(320.dp, 110.dp)
        )
    }
}

@Preview(widthDp = 250, heightDp = 170)
@Composable
fun CashFlowWidgetPreview4x3() {
    GlanceTheme {
        CashFlowWidgetContent(
            context = LocalContext.current,
            prefs = mockCashFlowPrefs(),
            widgetSize = DpSize(250.dp, 170.dp)
        )
    }
}

// With base conversion disabled
@Preview(widthDp = 250, heightDp = 110)
@Composable
fun CashFlowWidgetPreviewNoConversion() {
    GlanceTheme {
        CashFlowWidgetContent(
            context = LocalContext.current,
            prefs = mockCashFlowPrefs(showBaseConversion = false),
            widgetSize = DpSize(250.dp, 110.dp)
        )
    }
}

// With comparison text
@Preview(widthDp = 250, heightDp = 110)
@Composable
fun CashFlowWidgetPreviewWithComparison() {
    GlanceTheme {
        CashFlowWidgetContent(
            context = LocalContext.current,
            prefs = mockCashFlowPrefs(comparison = "+12.3%"),
            widgetSize = DpSize(250.dp, 110.dp)
        )
    }
}

// Loading state
@Preview(widthDp = 250, heightDp = 110)
@Composable
fun CashFlowWidgetPreviewLoading() {
    GlanceTheme {
        CashFlowWidgetContent(
            context = LocalContext.current,
            prefs = preferencesOf(CashFlowGlanceWidget.Keys.IS_LOADED to "false"),
            widgetSize = DpSize(250.dp, 110.dp)
        )
    }
}

// Single currency (base currency only — no conversion rows)
@Preview(widthDp = 250, heightDp = 110)
@Composable
fun CashFlowWidgetPreviewSingleCurrency() {
    GlanceTheme {
        CashFlowWidgetContent(
            context = LocalContext.current,
            prefs = mockCashFlowPrefs(singleCurrency = true),
            widgetSize = DpSize(250.dp, 110.dp)
        )
    }
}

// ── CashFlowBody direct previews ──────────────────────────────────────────────

@Preview(widthDp = 180, heightDp = 90)
@Composable
fun CashFlowBodyPreviewStandard() {
    GlanceTheme {
        CashFlowBody(prefs = mockCashFlowPrefs(), scale = 1.0f, isCompact = false)
    }
}

@Preview(widthDp = 320, heightDp = 90)
@Composable
fun CashFlowBodyPreviewWide() {
    GlanceTheme {
        CashFlowBody(prefs = mockCashFlowPrefs(), scale = 1.0f, isCompact = false)
    }
}

// ── Mock data helpers ─────────────────────────────────────────────────────────

private val fullCurrencyJson = """
    [
      {"code":"USD","income":"5,000.00 $","incomeBase":"5,000.00 $","expense":"3,500.00 $","expenseBase":"3,500.00 $"},
      {"code":"EUR","income":"1,000.00 €","incomeBase":"1,100.00 $","expense":"800.00 €","expenseBase":"880.00 $"},
      {"code":"GBP","income":"500.00 £","incomeBase":"620.00 $","expense":"200.00 £","expenseBase":"248.00 $"}
    ]
""".trimIndent()

private val singleCurrencyJson = """
    [
      {"code":"USD","income":"6,720.00 $","incomeBase":"6,720.00 $","expense":"4,628.00 $","expenseBase":"4,628.00 $"}
    ]
""".trimIndent()

internal fun mockCashFlowPrefs(
    showBaseConversion: Boolean = true,
    comparison: String = "",
    singleCurrency: Boolean = false
): Preferences = preferencesOf(
    CashFlowGlanceWidget.Keys.IS_LOADED to "true",
    CashFlowGlanceWidget.Keys.INCOME_TOTAL to "6,720.00 $",
    CashFlowGlanceWidget.Keys.EXPENSE_TOTAL to "4,628.00 $",
    CashFlowGlanceWidget.Keys.PERIOD_LABEL to "This Month",
    CashFlowGlanceWidget.Keys.BASE_CURRENCY to "USD",
    CashFlowGlanceWidget.Keys.SHOW_BASE_CONVERSION to if (showBaseConversion) "true" else "false",
    CashFlowGlanceWidget.Keys.CURRENCY_LIST to if (singleCurrency) singleCurrencyJson else fullCurrencyJson,
    CashFlowGlanceWidget.Keys.COMPARISON to comparison
)
