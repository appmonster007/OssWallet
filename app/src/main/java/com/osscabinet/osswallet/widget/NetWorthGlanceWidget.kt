@file:Suppress("RestrictedApi") // Glance's resource-based ColorProvider(@ColorRes) is public but flagged across library groups

package com.osscabinet.ossWallet.widget

import android.content.Context
import androidx.annotation.ColorRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.osscabinet.ossWallet.R

class NetWorthGlanceWidget : BaseOssWalletWidget() {

    @Composable
    override fun RenderContent(context: Context, prefs: Preferences, size: DpSize) {
        NetWorthWidgetContent(prefs, size)
    }

    object Keys {
        val IS_LOADED            = stringPreferencesKey("nw_is_loaded")
        val NET_WORTH            = stringPreferencesKey("nw_net_worth")
        val INC                  = stringPreferencesKey("nw_inc")
        val EXP                  = stringPreferencesKey("nw_exp")
        val NET_WORTH_COMPARISON = stringPreferencesKey("nw_comparison")
        val PERIOD_LABEL         = stringPreferencesKey("nw_period_label")
        val FONT_SIZE            = stringPreferencesKey("nw_font_size")
    }
}

private val STRIP_HEIGHT = 130.dp
private val WIDE4_WIDTH  = 300.dp
private val WIDE5_WIDTH  = 390.dp

@Composable
internal fun NetWorthWidgetContent(prefs: Preferences, widgetSize: DpSize) {
    val isLoaded = prefs[NetWorthGlanceWidget.Keys.IS_LOADED] == "true"
    if (!isLoaded) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            Text(
                text = "NET WORTH",
                style = TextStyle(
                    fontSize = TextUnit(8f, TextUnitType.Sp),
                    fontWeight = FontWeight.Bold,
                    color = ColorProvider(R.color.widget_on_surface_variant)
                )
            )
            Text(
                text = "Loading…",
                style = TextStyle(
                    fontSize = TextUnit(10f, TextUnitType.Sp),
                    color = ColorProvider(R.color.widget_on_surface_variant)
                )
            )
        }
        return
    }

    val keys = NetWorthGlanceWidget.Keys
    val baseFontSize = when (prefs[keys.FONT_SIZE]) {
        "Small"   -> 18f
        "Large"   -> 24f
        "X-Large" -> 32f
        else      -> 20f
    }
    val netWorth    = prefs[keys.NET_WORTH] ?: ""
    val inc         = prefs[keys.INC] ?: ""
    val exp         = prefs[keys.EXP] ?: ""
    val changeText  = prefs[keys.NET_WORTH_COMPARISON]?.takeIf { it.isNotEmpty() }
    val periodLabel = prefs[keys.PERIOD_LABEL] ?: ""

    val isStrip = widgetSize.height < STRIP_HEIGHT
    val isWide4 = widgetSize.width  >= WIDE4_WIDTH
    val isWide5 = widgetSize.width  >= WIDE5_WIDTH

    when {
        isStrip && !isWide4            -> NwStrip3x1(netWorth, changeText, baseFontSize)
        isStrip && isWide4 && !isWide5 -> NwStrip4x1(netWorth, inc, exp, changeText, baseFontSize)
        isStrip                        -> NwStrip5x1(netWorth, inc, exp, changeText, baseFontSize)
        !isStrip && !isWide4           -> NwCard3x2(netWorth, inc, exp, changeText, periodLabel, baseFontSize)
        else                           -> NwCard4x2(netWorth, inc, exp, changeText, periodLabel, baseFontSize)
    }
}

@Composable
private fun NwStrip3x1(netWorth: String, changeText: String?, baseFontSize: Float) {
    Column(modifier = GlanceModifier.fillMaxSize()) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "NET WORTH",
                style = TextStyle(
                    fontSize = TextUnit(8f, TextUnitType.Sp),
                    fontWeight = FontWeight.Bold,
                    color = ColorProvider(R.color.widget_on_surface_variant)
                )
            )
            if (changeText != null) {
                Spacer(modifier = GlanceModifier.defaultWeight())
                Text(
                    text = changeText,
                    style = TextStyle(
                        fontSize = TextUnit(8f, TextUnitType.Sp),
                        color = ColorProvider(R.color.widget_accent)
                    )
                )
            }
        }
        Text(
            text = netWorth,
            style = TextStyle(
                fontSize = TextUnit(baseFontSize, TextUnitType.Sp),
                fontWeight = FontWeight.Bold,
                color = ColorProvider(R.color.widget_on_surface)
            )
        )
    }
}

@Composable
private fun NwStrip4x1(netWorth: String, inc: String, exp: String, changeText: String?, baseFontSize: Float) {
    Row(modifier = GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = "NET WORTH",
                style = TextStyle(
                    fontSize = TextUnit(8f, TextUnitType.Sp),
                    fontWeight = FontWeight.Bold,
                    color = ColorProvider(R.color.widget_on_surface_variant)
                )
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = netWorth,
                    style = TextStyle(
                        fontSize = TextUnit(baseFontSize, TextUnitType.Sp),
                        fontWeight = FontWeight.Bold,
                        color = ColorProvider(R.color.widget_on_surface)
                    )
                )
                if (changeText != null) {
                    Spacer(modifier = GlanceModifier.width(6.dp))
                    Text(
                        text = changeText,
                        style = TextStyle(
                            fontSize = TextUnit(8f, TextUnitType.Sp),
                            color = ColorProvider(R.color.widget_accent)
                        )
                    )
                }
            }
        }
        Spacer(modifier = GlanceModifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            NwBadgePill("INC", inc, R.color.widget_positive, R.color.widget_positive_container)
            Spacer(modifier = GlanceModifier.height(4.dp))
            NwBadgePill("EXP", exp, R.color.widget_negative, R.color.widget_negative_container)
        }
    }
}

@Composable
private fun NwStrip5x1(netWorth: String, inc: String, exp: String, changeText: String?, baseFontSize: Float) {
    Row(modifier = GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = "NET WORTH",
                style = TextStyle(
                    fontSize = TextUnit(8f, TextUnitType.Sp),
                    fontWeight = FontWeight.Bold,
                    color = ColorProvider(R.color.widget_on_surface_variant)
                )
            )
            Text(
                text = netWorth,
                style = TextStyle(
                    fontSize = TextUnit(baseFontSize, TextUnitType.Sp),
                    fontWeight = FontWeight.Bold,
                    color = ColorProvider(R.color.widget_on_surface)
                )
            )
        }
        Column(modifier = GlanceModifier.defaultWeight(), horizontalAlignment = Alignment.End) {
            NwBadgePill("INC", inc, R.color.widget_positive, R.color.widget_positive_container)
            Spacer(modifier = GlanceModifier.height(4.dp))
            NwBadgePill("EXP", exp, R.color.widget_negative, R.color.widget_negative_container)
        }
        if (changeText != null) {
            Spacer(modifier = GlanceModifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "CHG",
                    style = TextStyle(
                        fontSize = TextUnit(7f, TextUnitType.Sp),
                        color = ColorProvider(R.color.widget_on_surface_variant)
                    )
                )
                Text(
                    text = changeText,
                    style = TextStyle(
                        fontSize = TextUnit(10f, TextUnitType.Sp),
                        fontWeight = FontWeight.Bold,
                        color = ColorProvider(R.color.widget_accent)
                    )
                )
            }
        }
    }
}

@Composable
private fun NwCard3x2(
    netWorth: String,
    inc: String,
    exp: String,
    changeText: String?,
    periodLabel: String,
    baseFontSize: Float
) {
    Column(modifier = GlanceModifier.fillMaxSize()) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "NET WORTH",
                style = TextStyle(
                    fontSize = TextUnit(8f, TextUnitType.Sp),
                    fontWeight = FontWeight.Bold,
                    color = ColorProvider(R.color.widget_on_surface_variant)
                )
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            if (periodLabel.isNotBlank()) {
                Text(
                    text = periodLabel,
                    style = TextStyle(
                        fontSize = TextUnit(8f, TextUnitType.Sp),
                        color = ColorProvider(R.color.widget_on_surface_variant)
                    )
                )
            }
        }
        Spacer(modifier = GlanceModifier.defaultWeight())
        Text(
            text = netWorth,
            style = TextStyle(
                fontSize = TextUnit(baseFontSize + 2f, TextUnitType.Sp),
                fontWeight = FontWeight.Bold,
                color = ColorProvider(R.color.widget_on_surface)
            )
        )
        if (changeText != null) {
            Text(
                text = changeText,
                style = TextStyle(
                    fontSize = TextUnit(9f, TextUnitType.Sp),
                    color = ColorProvider(R.color.widget_accent)
                )
            )
        }
        Spacer(modifier = GlanceModifier.height(8.dp))
        Row(modifier = GlanceModifier.fillMaxWidth().height(52.dp)) {
            NwBadgeCard(
                label = "INC",
                amount = inc,
                colorRes = R.color.widget_positive,
                bgColorRes = R.color.widget_positive_container,
                modifier = GlanceModifier.defaultWeight().fillMaxHeight()
            )
            Spacer(modifier = GlanceModifier.width(8.dp))
            NwBadgeCard(
                label = "EXP",
                amount = exp,
                colorRes = R.color.widget_negative,
                bgColorRes = R.color.widget_negative_container,
                modifier = GlanceModifier.defaultWeight().fillMaxHeight()
            )
        }
    }
}

@Composable
private fun NwCard4x2(
    netWorth: String,
    inc: String,
    exp: String,
    changeText: String?,
    periodLabel: String,
    baseFontSize: Float
) {
    Column(modifier = GlanceModifier.fillMaxSize()) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "NET WORTH",
                style = TextStyle(
                    fontSize = TextUnit(8f, TextUnitType.Sp),
                    fontWeight = FontWeight.Bold,
                    color = ColorProvider(R.color.widget_on_surface_variant)
                )
            )
            if (changeText != null) {
                Spacer(modifier = GlanceModifier.width(8.dp))
                Text(
                    text = changeText,
                    style = TextStyle(
                        fontSize = TextUnit(8f, TextUnitType.Sp),
                        color = ColorProvider(R.color.widget_accent)
                    )
                )
            }
            Spacer(modifier = GlanceModifier.defaultWeight())
            if (periodLabel.isNotBlank()) {
                Text(
                    text = periodLabel,
                    style = TextStyle(
                        fontSize = TextUnit(8f, TextUnitType.Sp),
                        color = ColorProvider(R.color.widget_on_surface_variant)
                    )
                )
            }
        }
        Spacer(modifier = GlanceModifier.defaultWeight())
        Text(
            text = netWorth,
            style = TextStyle(
                fontSize = TextUnit(baseFontSize + 6f, TextUnitType.Sp),
                fontWeight = FontWeight.Bold,
                color = ColorProvider(R.color.widget_on_surface)
            )
        )
        Spacer(modifier = GlanceModifier.height(8.dp))
        Row(modifier = GlanceModifier.fillMaxWidth().height(52.dp)) {
            NwBadgeCard(
                label = "INC",
                amount = inc,
                colorRes = R.color.widget_positive,
                bgColorRes = R.color.widget_positive_container,
                modifier = GlanceModifier.defaultWeight().fillMaxHeight()
            )
            Spacer(modifier = GlanceModifier.width(8.dp))
            NwBadgeCard(
                label = "EXP",
                amount = exp,
                colorRes = R.color.widget_negative,
                bgColorRes = R.color.widget_negative_container,
                modifier = GlanceModifier.defaultWeight().fillMaxHeight()
            )
        }
    }
}

@Composable
private fun NwBadgePill(
    label: String,
    amount: String,
    @ColorRes colorRes: Int,
    @ColorRes bgColorRes: Int
) {
    Row(
        modifier = GlanceModifier
            .background(ColorProvider(bgColorRes))
            .cornerRadius(8.dp)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = TextStyle(
                fontSize = TextUnit(7f, TextUnitType.Sp),
                fontWeight = FontWeight.Bold,
                color = ColorProvider(colorRes)
            )
        )
        Spacer(modifier = GlanceModifier.defaultWeight())
        Text(
            text = amount,
            style = TextStyle(
                fontSize = TextUnit(9f, TextUnitType.Sp),
                fontWeight = FontWeight.Bold,
                color = ColorProvider(colorRes)
            )
        )
    }
}

@Composable
private fun NwBadgeCard(
    label: String,
    amount: String,
    @ColorRes colorRes: Int,
    @ColorRes bgColorRes: Int,
    modifier: GlanceModifier = GlanceModifier
) {
    Column(
        modifier = modifier
            .background(ColorProvider(bgColorRes))
            .cornerRadius(12.dp)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = TextStyle(
                fontSize = TextUnit(7f, TextUnitType.Sp),
                fontWeight = FontWeight.Bold,
                color = ColorProvider(colorRes)
            )
        )
        Text(
            text = amount,
            style = TextStyle(
                fontSize = TextUnit(11f, TextUnitType.Sp),
                fontWeight = FontWeight.Bold,
                color = ColorProvider(colorRes)
            )
        )
    }
}
