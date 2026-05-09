package com.osscabinet.ossWallet.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.util.Log
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
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
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
import org.json.JSONArray
import org.json.JSONException

/**
 * Cash Flow Glance widget — table layout showing income + expense per currency.
 *
 * Layout: title row → column headers → scrollable LazyColumn of currency rows → totals footer.
 * Each row: currency code | income amount | expense amount.
 * Base conversion shown as a smaller ≈ line under each amount for foreign currencies.
 *
 * Data flow: WidgetRefreshWorker writes pre-formatted strings to GlanceState.
 * No async IO in provideContent — state changes trigger immediate recomposition.
 */
class CashFlowGlanceWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val rawId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val opts = AppWidgetManager.getInstance(context).getAppWidgetOptions(rawId)
        val optMaxW = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH).dp
        val optMaxH = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT).dp
        Log.d("CashFlowGlanceWidget", buildString {
            append("raw options → ")
            append("minW=${opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)} ")
            append("maxW=$optMaxW ")
            append("minH=${opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)} ")
            append("maxH=$optMaxH")
        })
        provideContent {
            GlanceTheme {
                val prefs = currentState<Preferences>()
                val localSize = LocalSize.current
                val correctedSize = DpSize(
                    width = if (optMaxW > localSize.width) optMaxW else localSize.width,
                    height = if (optMaxH > localSize.height) optMaxH else localSize.height,
                )
                Log.d("CashFlowGlanceWidget", "LocalSize=$localSize  corrected=$correctedSize")
                CashFlowWidgetContent(
                    context = context,
                    prefs = prefs,
                    widgetSize = correctedSize,
                )
            }
        }
    }

    object Keys {
        val IS_LOADED = stringPreferencesKey("cf_is_loaded")
        val INCOME_TOTAL = stringPreferencesKey("cf_income_total")
        val EXPENSE_TOTAL = stringPreferencesKey("cf_expense_total")
        val COMPARISON = stringPreferencesKey("cf_comparison")
        val PERIOD_LABEL = stringPreferencesKey("cf_period_label")
        val BASE_CURRENCY = stringPreferencesKey("cf_base_currency")
        val SHOW_BASE_CONVERSION = stringPreferencesKey("cf_show_base_conversion")
        val SHOW_TOTALS = stringPreferencesKey("cf_show_totals")
        /**
         * JSON array: [{"code":"USD","income":"5,000.00 $","incomeBase":"5,000.00 $",
         *               "expense":"3,500.00 $","expenseBase":"3,500.00 $"}, ...]
         */
        val CURRENCY_LIST = stringPreferencesKey("cf_currency_list")
    }
}

// ---------------------------------------------------------------------------
// Data model
// ---------------------------------------------------------------------------

internal data class CurrencyRow(
    val code: String,
    val income: String,
    val incomeBase: String,
    val expense: String,
    val expenseBase: String
)

internal fun parseCurrencyList(json: String?): List<CurrencyRow> {
    if (json.isNullOrBlank()) return emptyList()
    return try {
        val arr = JSONArray(json)
        (0 until arr.length()).mapNotNull { i ->
            val obj = arr.getJSONObject(i)
            val code = obj.optString("code", "")
            if (code.isBlank()) return@mapNotNull null
            CurrencyRow(
                code = code,
                income = obj.optString("income", ""),
                incomeBase = obj.optString("incomeBase", ""),
                expense = obj.optString("expense", ""),
                expenseBase = obj.optString("expenseBase", "")
            )
        }
    } catch (_: JSONException) {
        emptyList()
    }
}

// ---------------------------------------------------------------------------
// Composables
// ---------------------------------------------------------------------------

@Composable
internal fun CashFlowWidgetContent(context: Context, prefs: Preferences, widgetSize: DpSize) {
    val scale = when {
        widgetSize.height < 100.dp -> 0.85f
        widgetSize.height < 180.dp -> 1.0f
        else -> 1.15f
    }
    val isCompact = widgetSize.height < 100.dp

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
        val isLoaded = prefs[CashFlowGlanceWidget.Keys.IS_LOADED] == "true"
        if (!isLoaded) {
            CashFlowPlaceholder(scale)
        } else {
            CashFlowBody(prefs, scale, isCompact)
        }
    }
}

@Composable
private fun CashFlowPlaceholder(scale: Float) {
    Text(
        text = "Cash Flow",
        style = TextStyle(
            fontSize = TextUnit(12f * scale, TextUnitType.Sp),
            fontWeight = FontWeight.Bold,
            color = GlanceTheme.colors.onSurface
        )
    )
    Spacer(modifier = GlanceModifier.height((4f * scale).dp))
    Text(
        text = "Tap to open app",
        style = TextStyle(
            fontSize = TextUnit(10f * scale, TextUnitType.Sp),
            color = GlanceTheme.colors.onSurfaceVariant
        )
    )
}

@Composable
internal fun CashFlowBody(prefs: Preferences, scale: Float, isCompact: Boolean = false) {
    val keys = CashFlowGlanceWidget.Keys

    val incomeTotal = prefs[keys.INCOME_TOTAL] ?: ""
    val expenseTotal = prefs[keys.EXPENSE_TOTAL] ?: ""
    val periodLabel = prefs[keys.PERIOD_LABEL] ?: ""
    val comparison = prefs[keys.COMPARISON]?.takeIf { it.isNotEmpty() }
    val subText = comparison ?: periodLabel.takeIf { it.isNotBlank() }
    val baseCurrency = prefs[keys.BASE_CURRENCY] ?: ""
    val showBaseConversion = prefs[keys.SHOW_BASE_CONVERSION] != "false"
    val showTotals = prefs[keys.SHOW_TOTALS] != "false"
    val rows = parseCurrencyList(prefs[keys.CURRENCY_LIST])

    // Font sizes — same scale system as NetWorth widget subtitles/badges
    val headerSp = 10f * scale
    val colHeaderSp = 8f * scale
    val codeSp = 8f * scale
    val amountSp = 10f * scale
    val baseSp = 8f * scale
    val totalSp = 10f * scale

    // Fixed currency code column width — ISO codes are always 3 chars
    val codeColWidth = 36.dp

    Column(modifier = GlanceModifier.fillMaxSize()) {

        // ── Title row: "CASH FLOW" left  ·  period/comparison right ──────────
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "CASH FLOW",
                style = TextStyle(
                    fontSize = TextUnit(headerSp, TextUnitType.Sp),
                    fontWeight = FontWeight.Bold,
                    color = GlanceTheme.colors.onSurfaceVariant
                )
            )
            if (subText != null) {
                Spacer(modifier = GlanceModifier.defaultWeight())
                Text(
                    text = subText,
                    style = TextStyle(
                        fontSize = TextUnit(headerSp, TextUnitType.Sp),
                        color = GlanceTheme.colors.onSurfaceVariant
                    )
                )
            }
        }

        Spacer(modifier = GlanceModifier.height(5.dp))

        if (isCompact) {
            // Compact: just show totals side-by-side
            CompactTotalsRow(
                incomeTotal = incomeTotal,
                expenseTotal = expenseTotal,
                scale = scale
            )
        } else {
            // ── Column headers: (gap) | INCOME | EXPENSE ─────────────────────
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = GlanceModifier.width(codeColWidth))
                Column(
                    modifier = GlanceModifier.defaultWeight(),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "INCOME",
                        style = TextStyle(
                            fontSize = TextUnit(colHeaderSp, TextUnitType.Sp),
                            fontWeight = FontWeight.Bold,
                            color = ColorProvider(R.color.widget_income_label)
                        )
                    )
                }
                Column(
                    modifier = GlanceModifier.defaultWeight(),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "EXPENSE",
                        style = TextStyle(
                            fontSize = TextUnit(colHeaderSp, TextUnitType.Sp),
                            fontWeight = FontWeight.Bold,
                            color = ColorProvider(R.color.widget_expense_label)
                        )
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(3.dp))
            TableDivider()
            Spacer(modifier = GlanceModifier.height(2.dp))

            // ── Currency rows — LazyColumn scrolls through all currencies ────
            LazyColumn(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                items(rows) { row ->
                    CurrencyTableRow(
                        row = row,
                        baseCurrency = baseCurrency,
                        showBaseConversion = showBaseConversion,
                        codeColWidth = codeColWidth,
                        codeSp = codeSp,
                        amountSp = amountSp,
                        baseSp = baseSp,
                        scale = scale
                    )
                }
            }

            // ── Totals footer (optional) ──────────────────────────────────────
            if (showTotals) {
                Spacer(modifier = GlanceModifier.height(2.dp))
                TableDivider()
                Spacer(modifier = GlanceModifier.height(3.dp))

                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TOTAL",
                        style = TextStyle(
                            fontSize = TextUnit(totalSp, TextUnitType.Sp),
                            fontWeight = FontWeight.Bold,
                            color = GlanceTheme.colors.onSurface
                        ),
                        modifier = GlanceModifier.width(codeColWidth)
                    )
                    Column(
                        modifier = GlanceModifier.defaultWeight(),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = incomeTotal,
                            style = TextStyle(
                                fontSize = TextUnit(totalSp, TextUnitType.Sp),
                                fontWeight = FontWeight.Bold,
                                color = ColorProvider(R.color.widget_income_color)
                            )
                        )
                    }
                    Column(
                        modifier = GlanceModifier.defaultWeight(),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = expenseTotal,
                            style = TextStyle(
                                fontSize = TextUnit(totalSp, TextUnitType.Sp),
                                fontWeight = FontWeight.Bold,
                                color = ColorProvider(R.color.widget_expense_color)
                            )
                        )
                    }
                }
            }
        }
    }
}

// ── One currency row ──────────────────────────────────────────────────────────

@Composable
private fun CurrencyTableRow(
    row: CurrencyRow,
    baseCurrency: String,
    showBaseConversion: Boolean,
    codeColWidth: androidx.compose.ui.unit.Dp,
    codeSp: Float,
    amountSp: Float,
    baseSp: Float,
    scale: Float
) {
    val isForeign = row.code != baseCurrency
    val showConversion = showBaseConversion && isForeign

    Column(modifier = GlanceModifier.fillMaxWidth()) {
        // Main row: code | income (right) | expense (right)
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = row.code,
                style = TextStyle(
                    fontSize = TextUnit(codeSp, TextUnitType.Sp),
                    fontWeight = FontWeight.Bold,
                    color = GlanceTheme.colors.onSurfaceVariant
                ),
                modifier = GlanceModifier.width(codeColWidth)
            )
            Column(
                modifier = GlanceModifier.defaultWeight(),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = row.income,
                    style = TextStyle(
                        fontSize = TextUnit(amountSp, TextUnitType.Sp),
                        fontWeight = FontWeight.Bold,
                        color = ColorProvider(R.color.widget_income_color)
                    )
                )
                if (showConversion) {
                    Text(
                        text = "≈ ${row.incomeBase}",
                        style = TextStyle(
                            fontSize = TextUnit(baseSp, TextUnitType.Sp),
                            color = ColorProvider(R.color.widget_income_comparison)
                        )
                    )
                }
            }
            Column(
                modifier = GlanceModifier.defaultWeight(),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = row.expense,
                    style = TextStyle(
                        fontSize = TextUnit(amountSp, TextUnitType.Sp),
                        fontWeight = FontWeight.Bold,
                        color = ColorProvider(R.color.widget_expense_color)
                    )
                )
                if (showConversion) {
                    Text(
                        text = "≈ ${row.expenseBase}",
                        style = TextStyle(
                            fontSize = TextUnit(baseSp, TextUnitType.Sp),
                            color = ColorProvider(R.color.widget_expense_comparison)
                        )
                    )
                }
            }
        }

        Spacer(modifier = GlanceModifier.height((3f * scale).dp))
    }
}

// ── Compact summary (height < 100dp) ─────────────────────────────────────────

@Composable
private fun CompactTotalsRow(incomeTotal: String, expenseTotal: String, scale: Float) {
    val sp = 9f * scale
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "IN",
            style = TextStyle(
                fontSize = TextUnit(7f * scale, TextUnitType.Sp),
                fontWeight = FontWeight.Bold,
                color = ColorProvider(R.color.widget_income_label)
            )
        )
        Spacer(modifier = GlanceModifier.width(4.dp))
        Text(
            text = incomeTotal,
            style = TextStyle(
                fontSize = TextUnit(sp, TextUnitType.Sp),
                fontWeight = FontWeight.Bold,
                color = ColorProvider(R.color.widget_income_color)
            )
        )
        Spacer(modifier = GlanceModifier.defaultWeight())
        Text(
            text = "OUT",
            style = TextStyle(
                fontSize = TextUnit(7f * scale, TextUnitType.Sp),
                fontWeight = FontWeight.Bold,
                color = ColorProvider(R.color.widget_expense_label)
            )
        )
        Spacer(modifier = GlanceModifier.width(4.dp))
        Text(
            text = expenseTotal,
            style = TextStyle(
                fontSize = TextUnit(sp, TextUnitType.Sp),
                fontWeight = FontWeight.Bold,
                color = ColorProvider(R.color.widget_expense_color)
            )
        )
    }
}

// ── Thin table divider ────────────────────────────────────────────────────────

@Composable
private fun TableDivider() {
    Spacer(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(0.5.dp)
            .background(GlanceTheme.colors.onSurfaceVariant)
    )
}
