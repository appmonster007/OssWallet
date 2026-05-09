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

// ── Valid sizes: min width 3 cells (180dp), height 1–2 cells (50–110dp) ──────

// 3x1 — minimum valid size, compact (1-row)
@Preview(widthDp = 180, heightDp = 50)
@Composable
fun NetWorthWidgetPreview3x1() {
    GlanceTheme {
        NetWorthWidgetContent(
            context = LocalContext.current,
            prefs = getMockPrefs(),
            widgetSize = DpSize(180.dp, 50.dp)
        )
    }
}

// 4x1 — wide compact (1-row)
@Preview(widthDp = 250, heightDp = 50)
@Composable
fun NetWorthWidgetPreview4x1() {
    GlanceTheme {
        NetWorthWidgetContent(
            context = LocalContext.current,
            prefs = getMockPrefs(),
            widgetSize = DpSize(250.dp, 50.dp)
        )
    }
}

// 3x2 — default placement size, standard (2-row)
@Preview(widthDp = 180, heightDp = 110)
@Composable
fun NetWorthWidgetPreview3x2() {
    GlanceTheme {
        NetWorthWidgetContent(
            context = LocalContext.current,
            prefs = getMockPrefs(),
            widgetSize = DpSize(180.dp, 110.dp)
        )
    }
}

// 4x2 — wide standard (2-row)
@Preview(widthDp = 250, heightDp = 110)
@Composable
fun NetWorthWidgetPreview4x2() {
    GlanceTheme {
        NetWorthWidgetContent(
            context = LocalContext.current,
            prefs = getMockPrefs(),
            widgetSize = DpSize(250.dp, 110.dp)
        )
    }
}

// 5x2 — full-width standard (2-row), covers very wide placements
@Preview(widthDp = 320, heightDp = 110)
@Composable
fun NetWorthWidgetPreview5x2() {
    GlanceTheme {
        NetWorthWidgetContent(
            context = LocalContext.current,
            prefs = getMockPrefs(),
            widgetSize = DpSize(320.dp, 110.dp)
        )
    }
}

// Loading / placeholder state at default size
@Preview(widthDp = 180, heightDp = 110)
@Composable
fun NetWorthWidgetPreviewLoading() {
    GlanceTheme {
        NetWorthWidgetContent(
            context = LocalContext.current,
            prefs = getUnloadedPrefs(),
            widgetSize = DpSize(180.dp, 110.dp)
        )
    }
}

// ── WidgetBody direct previews — maxNetWorthSp matches each preview width ─────

// 3x1 compact — tightest font clamp: (180-24)/7.2 ≈ 21.7sp, scale 0.85 → clamps to ~12sp
@Preview(widthDp = 180, heightDp = 50)
@Composable
fun WidgetBodyPreview3x1() {
    GlanceTheme {
        WidgetBody(
            prefs = getMockPrefs(),
            scale = 0.85f,
            isCompact = true,
            maxNetWorthSp = (180f - 24f) / (12f * 0.6f)
        )
    }
}

// 4x1 compact — wider, clamp: (250-24)/7.2 ≈ 31.4sp, effectively unclamped at 0.85
@Preview(widthDp = 250, heightDp = 50)
@Composable
fun WidgetBodyPreview4x1() {
    GlanceTheme {
        WidgetBody(
            prefs = getMockPrefs(),
            scale = 0.85f,
            isCompact = true,
            maxNetWorthSp = (250f - 24f) / (12f * 0.6f)
        )
    }
}

// 3x2 standard — default placement
@Preview(widthDp = 180, heightDp = 110)
@Composable
fun WidgetBodyPreview3x2() {
    GlanceTheme {
        WidgetBody(
            prefs = getMockPrefs(),
            scale = 1.0f,
            isCompact = false,
            maxNetWorthSp = (180f - 24f) / (12f * 0.6f)
        )
    }
}

// 4x2 standard — wide default
@Preview(widthDp = 250, heightDp = 110)
@Composable
fun WidgetBodyPreview4x2() {
    GlanceTheme {
        WidgetBody(
            prefs = getMockPrefs(),
            scale = 1.0f,
            isCompact = false,
            maxNetWorthSp = (250f - 24f) / (12f * 0.6f)
        )
    }
}

internal fun getMockPrefs(): Preferences = preferencesOf(
    NetWorthGlanceWidget.Keys.IS_LOADED to "true",
    NetWorthGlanceWidget.Keys.NET_WORTH to "125,000.50 $",
    NetWorthGlanceWidget.Keys.INCOME to "5,000.00 $",
    NetWorthGlanceWidget.Keys.EXPENSE to "3,200.00 $",
    NetWorthGlanceWidget.Keys.NET_WORTH_COMPARISON to "+2.5%",
    NetWorthGlanceWidget.Keys.INCOME_COMPARISON to "+8.3%",
    NetWorthGlanceWidget.Keys.EXPENSE_COMPARISON to "-4.1%",
    NetWorthGlanceWidget.Keys.PERIOD_LABEL to "This Month"
)

internal fun getUnloadedPrefs(): Preferences = preferencesOf(
    NetWorthGlanceWidget.Keys.IS_LOADED to "false"
)
