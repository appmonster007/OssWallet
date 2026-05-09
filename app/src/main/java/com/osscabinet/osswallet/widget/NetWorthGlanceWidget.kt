package com.osscabinet.ossWallet.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.annotation.ColorRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
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
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.osscabinet.ossWallet.MainActivity
import com.osscabinet.ossWallet.R
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * NetWorth Glance widget — matches the NetWorth dashboard card design.
 *
 * Visual design mirrors NetWorthCard.kt + FlowBadge:
 *   - Card: GlanceTheme.colors.surface background + cornerRadius(24dp) — auto light/dark via GlanceTheme
 *   - Net worth: GlanceTheme.colors.onSurface, bold, config-driven font size with height scaling
 *   - Comparison: GlanceTheme.colors.onSurfaceVariant, 11sp
 *   - Income badge: R.color.widget_badge_income_bg bg (14% alpha), R.color.widget_income_color text
 *   - Expense badge: R.color.widget_badge_expense_bg bg (14% alpha), R.color.widget_expense_color text
 *   - Badge colors resolve via values/ (light) and values-night/ (dark) — proper per-widget theming
 *   - Font/padding sizes scale linearly with widget height (reference: 140dp)
 *
 * Data flow: WidgetRefreshWorker writes pre-formatted strings to GlanceState
 * (PreferencesGlanceStateDefinition). The running session reacts to state changes via
 * currentState() recomposition — no async IO in provideGlance(), so sessions stay short-lived.
 * SizeMode.Exact provides accurate LocalSize.current per widget placement.
 */
class NetWorthGlanceWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val rawId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val opts = AppWidgetManager.getInstance(context).getAppWidgetOptions(rawId)
        val optMaxW = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH).dp
        val optMaxH = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT).dp
        Log.d("NetWorthGlanceWidget", buildString {
            append("raw options → ")
            append("minW=${opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)} ")
            append("maxW=${optMaxW} ")
            append("minH=${opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)} ")
            append("maxH=${optMaxH}")
        })
        provideContent {
            GlanceTheme {
                val prefs = currentState<Preferences>()
                val localSize = LocalSize.current
                // Some launchers populate the options bundle with MIN (landscape) values even
                // in portrait, so Glance's SizeMode.Exact ends up with a height that is half
                // the actual widget frame. Correct by preferring the larger MAX dimension when
                // the launcher has reported non-zero values for it.
                val correctedSize = DpSize(
                    width = if (optMaxW > localSize.width) optMaxW else localSize.width,
                    height = if (optMaxH > localSize.height) optMaxH else localSize.height,
                )
                Log.d("NetWorthGlanceWidget", "LocalSize=$localSize  corrected=$correctedSize")
                NetWorthWidgetContent(
                    context = context,
                    prefs = prefs,
                    widgetSize = correctedSize,
                )
            }
        }
    }

    object Keys {
        val IS_LOADED = stringPreferencesKey("nw_is_loaded")
        val NET_WORTH = stringPreferencesKey("nw_net_worth")
        val INCOME = stringPreferencesKey("nw_income")
        val EXPENSE = stringPreferencesKey("nw_expense")
        val NET_WORTH_COMPARISON = stringPreferencesKey("nw_comparison")
        val INCOME_COMPARISON = stringPreferencesKey("nw_income_comparison")
        val EXPENSE_COMPARISON = stringPreferencesKey("nw_expense_comparison")
        val PERIOD_LABEL = stringPreferencesKey("nw_period_label")
        val FONT_SIZE = stringPreferencesKey("nw_font_size")
    }
}

@Composable
internal fun NetWorthWidgetContent(context: Context, prefs: Preferences, widgetSize: DpSize) {
    // Discrete scaling buckets for better predictability and readability
    val scale = when {
        widgetSize.height < 100.dp -> 0.85f // Compact (1-row)
        widgetSize.height < 180.dp -> 1.0f  // Standard (2-row)
        else -> 1.15f                       // Large (3-row+)
    }

    val isCompact = widgetSize.height < 100.dp

    // Guarantee "-999,999 USD" (6 digits + 3-char ISO = ~12 chars) fits on one line.
    // Roboto avg char width ≈ 0.6 × sp in dp; 24dp accounts for 12dp padding each side.
    val maxNetWorthSp = ((widgetSize.width.value - 24f) / (12f * 0.6f)).coerceAtLeast(9f)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.surface)
            .cornerRadius(24.dp)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .clickable(
                actionStartActivity(
                    Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        putExtra(MainActivity.EXTRA_WIDGET_NAV, "dashboard")
                    }
                )
            ),
        verticalAlignment = Alignment.Top,
        horizontalAlignment = Alignment.Start
    ) {
        val isLoaded = prefs[NetWorthGlanceWidget.Keys.IS_LOADED] == "true"
        if (!isLoaded) {
            WidgetPlaceholder(scale)
        } else {
            WidgetBody(prefs, scale, isCompact, maxNetWorthSp)
        }
    }
}

@Composable
private fun WidgetPlaceholder(scale: Float) {
    Text(
        text = "Net Worth",
        style = TextStyle(
            fontSize = TextUnit(12f * scale, TextUnitType.Sp),
            fontWeight = FontWeight.Bold,
            color = GlanceTheme.colors.onSurface
        )
    )
    Spacer(modifier = GlanceModifier.height((4 * scale).dp))
    Text(
        text = "Tap to open app",
        style = TextStyle(
            fontSize = TextUnit(10f * scale, TextUnitType.Sp),
            color = GlanceTheme.colors.onSurfaceVariant
        )
    )
}

@Composable
internal fun WidgetBody(
    prefs: Preferences,
    scale: Float,
    isCompact: Boolean = false,
    maxNetWorthSp: Float = Float.MAX_VALUE,
) {
    val keys = NetWorthGlanceWidget.Keys

    val baseFontSize = when (prefs[keys.FONT_SIZE]) {
        "Small" -> 18f
        "Large" -> 24f
        "X-Large" -> 32f
        else -> 20f
    }
    // Clamp to maxNetWorthSp so "-999,999 USD" always fits on one line regardless of widget size.
    val netWorthFontSize = minOf(baseFontSize * scale, maxNetWorthSp)
    val subtitleFontSize = 10f * scale

    val netWorthComparison = prefs[keys.NET_WORTH_COMPARISON]?.takeIf { it.isNotEmpty() }
    val incomeComparison = prefs[keys.INCOME_COMPARISON]?.takeIf { it.isNotEmpty() }
    val expenseComparison = prefs[keys.EXPENSE_COMPARISON]?.takeIf { it.isNotEmpty() }
    val subText = netWorthComparison ?: prefs[keys.PERIOD_LABEL]

    // Unified layout: net worth fills top half, badges fill bottom half.
    // defaultWeight splits the space equally; same structure for all widget sizes.
    Column(modifier = GlanceModifier.fillMaxSize()) {

        // TOP: net worth section — mirrors badge layout (title row + amount)
        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .defaultWeight(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Title row: "NET WORTH" left, comparison/period right
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "NET WORTH",
                    style = TextStyle(
                        fontSize = TextUnit(subtitleFontSize, TextUnitType.Sp),
                        fontWeight = FontWeight.Bold,
                        color = GlanceTheme.colors.onSurfaceVariant
                    )
                )
                if (subText != null) {
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    Text(
                        text = subText,
                        style = TextStyle(
                            fontSize = TextUnit(subtitleFontSize, TextUnitType.Sp),
                            color = GlanceTheme.colors.onSurfaceVariant
                        )
                    )
                }
            }
            Spacer(modifier = GlanceModifier.height((4 * scale).dp))
            // Amount
            Text(
                text = prefs[keys.NET_WORTH] ?: "",
                style = TextStyle(
                    fontSize = TextUnit(netWorthFontSize, TextUnitType.Sp),
                    fontWeight = FontWeight.Bold,
                    color = GlanceTheme.colors.onSurface
                )
            )
        }

        // BOTTOM: income + expense badges — expands to fill lower half
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .defaultWeight(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BadgeColumn(
                label = "INCOME",
                amount = prefs[keys.INCOME] ?: "",
                comparison = incomeComparison,
                bgColorRes = R.color.widget_badge_income_bg,
                colorRes = R.color.widget_income_color,
                labelColorRes = R.color.widget_income_label,
                compColorRes = R.color.widget_income_comparison,
                scale = scale,
                isCompact = isCompact,
                modifier = GlanceModifier.defaultWeight().fillMaxHeight()
            )
            Spacer(modifier = GlanceModifier.width(8.dp))
            BadgeColumn(
                label = "EXPENSE",
                amount = prefs[keys.EXPENSE] ?: "",
                comparison = expenseComparison,
                bgColorRes = R.color.widget_badge_expense_bg,
                colorRes = R.color.widget_expense_color,
                labelColorRes = R.color.widget_expense_label,
                compColorRes = R.color.widget_expense_comparison,
                scale = scale,
                isCompact = isCompact,
                modifier = GlanceModifier.defaultWeight().fillMaxHeight()
            )
        }
    }
}

@Composable
private fun BadgeColumn(
    label: String,
    amount: String,
    comparison: String?,
    @ColorRes bgColorRes: Int,
    @ColorRes colorRes: Int,
    @ColorRes labelColorRes: Int,
    @ColorRes compColorRes: Int,
    scale: Float,
    isCompact: Boolean,
    modifier: GlanceModifier = GlanceModifier
) {
    // Compact uses tighter geometry so the badges don't crowd the net worth row.
    val horizPadding = if (isCompact) 4.dp else (10 * scale).dp
    val vertPadding = if (isCompact) 2.dp else (6 * scale).dp
    val cornerRadius = (14 * scale).dp
    val labelFontSize = if (isCompact) 7f else 10f * scale
    val amountFontSize = if (isCompact) 9f else 14f * scale

    Column(
        modifier = modifier
            .background(bgColorRes)
            .cornerRadius(cornerRadius)
            .padding(horizontal = horizPadding, vertical = vertPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Label row: label left, comparison % right
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = TextStyle(
                    fontSize = TextUnit(labelFontSize, TextUnitType.Sp),
                    fontWeight = FontWeight.Bold,
                    color = ColorProvider(labelColorRes)
                )
            )
            if (comparison != null) {
                Spacer(modifier = GlanceModifier.defaultWeight())
                Text(
                    text = comparison,
                    style = TextStyle(
                        fontSize = TextUnit(9f * scale, TextUnitType.Sp),
                        color = ColorProvider(compColorRes)
                    )
                )
            }
        }
        Spacer(modifier = GlanceModifier.height((4 * scale).dp))
        Text(
            text = amount,
            style = TextStyle(
                fontSize = TextUnit(amountFontSize, TextUnitType.Sp),
                fontWeight = FontWeight.Bold,
                color = ColorProvider(colorRes)
            )
        )
    }
}

internal fun buildComparisonText(
    current: BigDecimal,
    previous: BigDecimal?,
    showComparison: Boolean
): String? {
    if (!showComparison || previous == null || previous.signum() == 0) return null
    val change = current.subtract(previous)
    if (change.signum() == 0) return null
    val pct = change.multiply(BigDecimal("100"))
        .divide(previous.abs(), 1, RoundingMode.HALF_UP)
    val sign = if (change.signum() > 0) "+" else "-"
    return "$sign${pct.abs()}%"
}
